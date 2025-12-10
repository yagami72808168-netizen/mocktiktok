package com.light.mimictiktok.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
/**
 * 缩略图缓存管理器 - 提供 LRU 缓存策略和磁盘存储管理
 * 支持内存缓存、磁盘缓存、过期清理和大小限制
 */
class ThumbnailCache(
    private val context: Context
) {
    companion object {
        private const val TAG = "ThumbnailCache"
        private const val DISK_CACHE_SUBDIR = "thumbnails"
        private const val DEFAULT_MEMORY_CACHE_SIZE = 50 * 1024 * 1024L // 50MB
        private const val DEFAULT_DISK_CACHE_SIZE = 200 * 1024 * 1024L // 200MB
        private const val MAX_FILE_AGE = 7 * 24 * 60 * 60 * 1000L // 7天
        private const val CLEANUP_CHECK_INTERVAL = 60 * 1000L // 1分钟
        private const val MAX_MEMORY_ENTRIES = 200
    }

    // 内存缓存 - 使用 LRU 策略
    private val memoryCache = object : LinkedHashMap<String, CachedThumbnail>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, CachedThumbnail>): Boolean {
            return size > MAX_MEMORY_ENTRIES || currentMemorySize() > memoryCacheSize
        }

        private fun currentMemorySize(): Long {
            return values.sumOf { it.fileSizeBytes }
        }
    }

    private val memoryCacheMutex = Mutex()
    private val memoryCacheSize = DEFAULT_MEMORY_CACHE_SIZE
    private val diskCacheDir = File(context.cacheDir, DISK_CACHE_SUBDIR).apply {
        if (!exists()) mkdirs()
    }

    // 磁盘缓存限制
    private var diskCacheSize = DEFAULT_DISK_CACHE_SIZE
    private var currentDiskCacheSize = 0L
    private val cleanupMutex = Mutex()
    private var lastCleanupTime = 0L

    // 内存使用统计
    private val diskCacheIndex = ConcurrentHashMap<String, DiskCacheEntry>()

    init {
        // 启动定期清理任务
        startCleanupTask()
        // 初始化磁盘缓存统计
        initializeDiskCacheStats()
    }

    /**
     * 获取缩略图 - 优先从内存缓存获取，其次从磁盘缓存获取
     * @param key 缓存键
     * @param sourceInfo 源信息（URI 或路径）
     * @return 缩略图文件路径或 null
     */
    suspend fun getThumbnail(
        key: String,
        sourceInfo: String
    ): String? = withContext(Dispatchers.IO) {
        
        // 1. 检查内存缓存
        memoryCacheMutex.withLock {
            memoryCache[key]?.let { cached ->
                // 更新访问时间
                cached.lastAccessed = System.currentTimeMillis()
                Log.d(TAG, "Memory cache hit for key: $key")
                return@withLock cached.filePath
            }
        }

        // 2. 检查磁盘缓存
        val diskEntry = diskCacheIndex[key]
        if (diskEntry != null && File(diskEntry.filePath).exists()) {
            // 更新磁盘缓存访问时间
            diskEntry.lastAccessed = System.currentTimeMillis()
            diskEntry.accessCount++
            
            // 将热点数据加载到内存缓存
            moveToMemoryCache(key, diskEntry.filePath, diskEntry.fileSizeBytes)
            
            Log.d(TAG, "Disk cache hit for key: $key")
            return@withContext diskEntry.filePath
        }

        // 3. 缓存未命中
        Log.d(TAG, "Cache miss for key: $key")
        null
    }

    /**
     * 存储缩略图到缓存
     * @param key 缓存键
     * @param filePath 缩略图文件路径
     * @param sourceInfo 源信息
     */
    suspend fun putThumbnail(
        key: String,
        filePath: String,
        sourceInfo: String
    ) = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.w(TAG, "Thumbnail file does not exist: $filePath")
                return@withContext
            }

            val fileSize = file.length()
            
            // 存储到内存缓存（如果大小合适）
            if (fileSize <= memoryCacheSize / 4) { // 不超过内存缓存的1/4
                memoryCacheMutex.withLock {
                    memoryCache[key] = CachedThumbnail(
                        filePath = filePath,
                        fileSizeBytes = fileSize,
                        lastAccessed = System.currentTimeMillis(),
                        sourceInfo = sourceInfo
                    )
                }
                Log.d(TAG, "Stored thumbnail in memory cache: $key")
            }

            // 存储到磁盘缓存索引
            diskCacheIndex[key] = DiskCacheEntry(
                filePath = filePath,
                fileSizeBytes = fileSize,
                lastAccessed = System.currentTimeMillis(),
                accessCount = 0,
                sourceInfo = sourceInfo
            )

            currentDiskCacheSize += fileSize
            
            // 检查是否需要清理磁盘缓存
            if (currentDiskCacheSize > diskCacheSize) {
                cleanupDiskCache()
            }

            Log.d(TAG, "Thumbnail cached: $key (${fileSize} bytes)")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache thumbnail: $key", e)
        }
    }

    /**
     * 预加载缩略图到缓存
     * @param key 缓存键
     * @param thumbnailResult 缩略图生成结果
     */
    suspend fun preloadThumbnail(
        key: String,
        thumbnailResult: ThumbnailResult.Success
    ) {
        when (thumbnailResult) {
            is ThumbnailResult.Success -> {
                putThumbnail(key, thumbnailResult.filePath, thumbnailResult.uri.toString())
            }
            is ThumbnailResult.Failure -> {
                Log.w(TAG, "Skipping preload for failed thumbnail: $key")
            }
        }
    }

    /**
     * 批量预加载缩略图
     * @param thumbnailMap 缩略图键值对
     * @param maxConcurrency 最大并发数
     */
    suspend fun preloadThumbnails(
        thumbnailMap: Map<String, ThumbnailResult.Success>,
        maxConcurrency: Int = 3
    ) = withContext(Dispatchers.IO) {
        val semaphore = kotlinx.coroutines.sync.Semaphore(maxConcurrency)
        
        thumbnailMap.forEach { (key, result) ->
            semaphore.acquire()
            try {
                preloadThumbnail(key, result)
            } finally {
                semaphore.release()
            }
        }
    }

    /**
     * 移除指定缓存条目
     * @param key 缓存键
     */
    suspend fun removeThumbnail(key: String) = withContext(Dispatchers.IO) {
        memoryCacheMutex.withLock {
            memoryCache.remove(key)
        }
        
        diskCacheIndex[key]?.let { entry ->
            diskCacheIndex.remove(key)
            currentDiskCacheSize -= entry.fileSizeBytes
            
            try {
                File(entry.filePath).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete cache file: ${entry.filePath}", e)
            }
        }
        
        Log.d(TAG, "Removed thumbnail from cache: $key")
    }

    /**
     * 清空所有缓存
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cleanupMutex.withLock {
            // 清空内存缓存
            memoryCacheMutex.withLock {
                memoryCache.clear()
            }
            
            // 清空磁盘缓存
            diskCacheIndex.clear()
            currentDiskCacheSize = 0L
            
            // 删除所有缓存文件
            diskCacheDir.listFiles()?.forEach { file ->
                try {
                    file.delete()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete cache file: ${file.name}", e)
                }
            }
            
            Log.d(TAG, "Cache cleared successfully")
        }
    }

    /**
     * 获取缓存统计信息
     * @return 缓存统计
     */
    suspend fun getCacheStats(): CacheInfo {
        return withContext(Dispatchers.IO) {
            val memoryEntries = memoryCacheMutex.withLock { memoryCache.size }
            val memorySize = memoryCacheMutex.withLock {
                memoryCache.values.sumOf { it.fileSizeBytes }
            }
            
            val diskEntries = diskCacheIndex.size
            val diskSize = currentDiskCacheSize
            
            CacheInfo(
                memoryEntries = memoryEntries,
                memorySizeBytes = memorySize,
                diskEntries = diskEntries,
                diskSizeBytes = diskSize,
                hitRatio = calculateHitRatio(),
                totalEntries = memoryEntries + diskEntries,
                totalSizeBytes = memorySize + diskSize
            )
        }
    }

    /**
     * 设置磁盘缓存大小限制
     * @param sizeBytes 缓存大小（字节）
     */
    fun setDiskCacheSize(sizeBytes: Long) {
        diskCacheSize = sizeBytes
        if (currentDiskCacheSize > diskCacheSize) {
            GlobalScope.launch {
                cleanupDiskCache()
            }
        }
    }

    /**
     * 手动触发缓存清理
     */
    suspend fun performCleanup() = withContext(Dispatchers.IO) {
        cleanupDiskCache()
        cleanupMemoryCache()
    }

    private suspend fun moveToMemoryCache(key: String, filePath: String, fileSize: Long) {
        if (fileSize <= memoryCacheSize / 4) {
            memoryCacheMutex.withLock {
                memoryCache[key] = CachedThumbnail(
                    filePath = filePath,
                    fileSizeBytes = fileSize,
                    lastAccessed = System.currentTimeMillis(),
                    sourceInfo = ""
                )
            }
        }
    }

    private suspend fun cleanupDiskCache() = cleanupMutex.withLock {
        if (currentDiskCacheSize <= diskCacheSize) return@withLock

        // 按照访问时间排序，删除最久未使用的文件
        val entries = diskCacheIndex.values.sortedBy { it.lastAccessed }
        var sizeToFree = currentDiskCacheSize - diskCacheSize

        entries.forEach { entry ->
            if (sizeToFree <= 0) return@forEach

            val file = File(entry.filePath)
            if (file.exists()) {
                val fileSize = file.length()
                try {
                    file.delete()
                    diskCacheIndex.remove(entry.getKey())
                    currentDiskCacheSize -= fileSize
                    sizeToFree -= fileSize
                    Log.d(TAG, "Cleaned disk cache file: ${file.name}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete cache file: ${file.name}", e)
                }
            }
        }
    }

    private suspend fun cleanupMemoryCache() {
        memoryCacheMutex.withLock {
            val entries = memoryCache.entries.sortedBy { it.value.lastAccessed }
            val targetSize = memoryCacheSize * 0.8 // 保留80%的内存缓存
            
            while (currentMemorySize() > targetSize && memoryCache.isNotEmpty()) {
                val eldest = entries.first()
                memoryCache.remove(eldest.key)
                Log.d(TAG, "Cleaned memory cache entry: ${eldest.key}")
            }
        }
    }

    private suspend fun currentMemorySize(): Long {
        return memoryCacheMutex.withLock {
            memoryCache.values.sumOf { it.fileSizeBytes }
        }
    }

    private fun calculateHitRatio(): Float {
            // 这里可以实现更复杂的命中率计算
            return 0.0f
        }

        private fun initializeDiskCacheStats() {
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        diskCacheDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.endsWith(".jpg")) {
                            val fileSize = file.length()
                            val fileName = file.name.substringBefore(".jpg")
                            
                            diskCacheIndex[fileName] = DiskCacheEntry(
                                filePath = file.absolutePath,
                                fileSizeBytes = fileSize,
                                lastAccessed = file.lastModified(),
                                accessCount = 0,
                                sourceInfo = ""
                            )
                            currentDiskCacheSize += fileSize
                        }
                    }
                    Log.d(TAG, "Initialized disk cache with ${diskCacheIndex.size} files")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize disk cache stats", e)
                }
            }
        }
    }

    private fun startCleanupTask() {
        GlobalScope.launch {
            while (true) {
                delay(CLEANUP_CHECK_INTERVAL)
                try {
                    // 清理过期文件
                    cleanupExpiredFiles()
                    // 定期清理缓存
                    if (System.currentTimeMillis() - lastCleanupTime > CLEANUP_CHECK_INTERVAL * 10) {
                        performCleanup()
                        lastCleanupTime = System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cleanup task failed", e)
                }
            }
        }
    }

    private fun cleanupExpiredFiles() {
        val now = System.currentTimeMillis()
        val expiredFiles = diskCacheDir.listFiles()?.filter { file ->
            file.isFile && file.name.endsWith(".jpg") && 
            (now - file.lastModified()) > MAX_FILE_AGE
        } ?: emptyList()

        expiredFiles.forEach { file ->
            try {
                val fileName = file.name.substringBefore(".jpg")
                file.delete()
                diskCacheIndex.remove(fileName)
                currentDiskCacheSize -= file.length()
                Log.d(TAG, "Cleaned expired cache file: ${file.name}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete expired cache file: ${file.name}", e)
            }
        }
    }

    }

/**
 * 内存缓存条目
 */
private data class CachedThumbnail(
    val filePath: String,
    val fileSizeBytes: Long,
    var lastAccessed: Long,
    val sourceInfo: String
)

/**
 * 磁盘缓存条目
 */
private data class DiskCacheEntry(
    val filePath: String,
    val fileSizeBytes: Long,
    var lastAccessed: Long,
    var accessCount: Int,
    val sourceInfo: String
) {
    fun getKey(): String = File(filePath).name.substringBefore(".jpg")
}

/**
 * 缓存统计信息
 */
data class CacheInfo(
    val memoryEntries: Int,
    val memorySizeBytes: Long,
    val diskEntries: Int,
    val diskSizeBytes: Long,
    val hitRatio: Float,
    val totalEntries: Int,
    val totalSizeBytes: Long
) {
    val memorySizeMB: Double
        get() = memorySizeBytes / (1024.0 * 1024.0)
    
    val diskSizeMB: Double
        get() = diskSizeBytes / (1024.0 * 1024.0)
    
    val totalSizeMB: Double
        get() = totalSizeBytes / (1024.0 * 1024.0)
    
    val isEmpty: Boolean
        get() = totalEntries == 0
    
    override fun toString(): String {
        return "CacheInfo(memory=$memoryEntries(${String.format("%.1f", memorySizeMB)}MB), " +
               "disk=$diskEntries(${String.format("%.1f", diskSizeMB)}MB), " +
               "total=$totalEntries(${String.format("%.1f", totalSizeMB)}MB), " +
               "hitRatio=${String.format("%.1f", hitRatio * 100)}%)"
    }
}
