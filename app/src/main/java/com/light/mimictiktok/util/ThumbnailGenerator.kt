package com.light.mimictiktok.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * 缩略图生成器 - 使用 MediaMetadataRetriever 生成视频缩略图
 * 支持异步生成、错误处理和资源管理
 */
class ThumbnailGenerator(
    private val context: Context
) {
    companion object {
        private const val TAG = "ThumbnailGenerator"
        private const val THUMBNAIL_FORMAT = "JPEG"
        private const val DEFAULT_QUALITY = 85
        private const val DEFAULT_WIDTH = 480
        private const val DEFAULT_HEIGHT = 800
        private const val CLEANUP_THRESHOLD = 7 * 24 * 60 * 60 * 1000L // 7天
    }

    private val ioDispatcher = Dispatchers.IO
    private val cacheDir = File(context.cacheDir, "thumbnails").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    /**
     * 生成缩略图 - 异步版本
     * @param videoUri 视频 URI
     * @param width 缩略图宽度
     * @param height 缩略图高度
     * @param quality 压缩质量 (0-100)
     * @param callback 回调函数
     */
    fun generateThumbnailAsync(
        videoUri: Uri,
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT,
        quality: Int = DEFAULT_QUALITY,
        callback: (Result<ThumbnailResult>) -> Unit
    ) {
        CoroutineScope(ioDispatcher).launch {
            try {
                val result = generateThumbnail(videoUri, width, height, quality)
                withContext(Dispatchers.Main) {
                    callback(Result.success(result))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate thumbnail: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(Result.failure(e))
                }
            }
        }
    }

    /**
     * 生成缩略图 - 同步版本
     * @param videoUri 视频 URI
     * @param width 缩略图宽度
     * @param height 缩略图高度
     * @param quality 压缩质量
     * @return 缩略图结果
     */
    fun generateThumbnail(
        videoUri: Uri,
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT,
        quality: Int = DEFAULT_QUALITY
    ): ThumbnailResult {
        val retriever = MediaMetadataRetriever()
        
        try {
            // 设置数据源
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                retriever.setDataSource(context, videoUri)
            } else {
                @Suppress("DEPRECATION")
                retriever.setDataSource(videoUri.path)
            }

            // 获取帧
            val bitmap = retriever.frameAtTime?.let { frameBitmap ->
                resizeBitmap(frameBitmap, width, height)
            } ?: throw IllegalStateException("Failed to extract frame from video")

            // 压缩位图
            val compressedBitmap = compressBitmap(bitmap, quality)

            // 生成缓存文件名
            val cacheKey = generateCacheKey(videoUri, width, height)
            val cacheFile = File(cacheDir, "$cacheKey.jpg")

            // 保存到缓存
            saveToCache(compressedBitmap, cacheFile, quality)

            return ThumbnailResult.Success(
                filePath = cacheFile.absolutePath,
                width = compressedBitmap.width,
                height = compressedBitmap.height,
                fileSize = cacheFile.length(),
                uri = videoUri
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail: ${e.message}", e)
            throw e
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release MediaMetadataRetriever: ${e.message}")
            }
        }
    }

    /**
     * 从视频路径生成缩略图
     * @param videoPath 视频文件路径
     * @param width 缩略图宽度
     * @param height 缩略图高度
     * @param quality 压缩质量
     * @return 缩略图结果
     */
    fun generateThumbnailFromPath(
        videoPath: String,
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT,
        quality: Int = DEFAULT_QUALITY
    ): ThumbnailResult {
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(videoPath)
            
            val bitmap = retriever.frameAtTime?.let { frameBitmap ->
                resizeBitmap(frameBitmap, width, height)
            } ?: throw IllegalStateException("Failed to extract frame from video")

            val compressedBitmap = compressBitmap(bitmap, quality)

            val cacheKey = generateCacheKey(videoPath, width, height)
            val cacheFile = File(cacheDir, "$cacheKey.jpg")

            saveToCache(compressedBitmap, cacheFile, quality)

            return ThumbnailResult.Success(
                filePath = cacheFile.absolutePath,
                width = compressedBitmap.width,
                height = compressedBitmap.height,
                fileSize = cacheFile.length(),
                uri = null
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail from path: ${e.message}", e)
            throw e
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release MediaMetadataRetriever: ${e.message}")
            }
        }
    }

    /**
     * 批量生成缩略图
     * @param videoUris 视频 URI 列表
     * @param maxConcurrency 最大并发数
     * @param progressCallback 进度回调
     * @return 缩略图结果列表
     */
    suspend fun generateThumbnailsBatch(
        videoUris: List<Uri>,
        maxConcurrency: Int = 3,
        progressCallback: ((Int, Int) -> Unit)? = null
    ): List<ThumbnailResult> = withContext(ioDispatcher) {
        val semaphore = Semaphore(maxConcurrency)
        val results = mutableListOf<ThumbnailResult>()
        
        videoUris.forEachIndexed { index, uri ->
            semaphore.acquire()
            try {
                val result = generateThumbnail(uri)
                results.add(result)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate thumbnail for $uri: ${e.message}", e)
                results.add(ThumbnailResult.Failure(uri, e.message ?: "Unknown error"))
            } finally {
                semaphore.release()
                progressCallback?.invoke(index + 1, videoUris.size)
            }
        }
        
        results.toList()
    }

    /**
     * 获取缩略图缓存统计信息
     * @return 缓存统计信息
     */
    suspend fun getCacheStats(): CacheStats = withContext(ioDispatcher) {
        val files = cacheDir.listFiles()?.filter { it.isFile && it.name.endsWith(".jpg") } ?: emptyList()
        
        CacheStats(
            fileCount = files.size,
            totalSizeBytes = files.sumOf { it.length() },
            lastModified = files.maxOfOrNull { it.lastModified() } ?: 0L
        )
    }

    /**
     * 清理过期的缓存文件
     * @param maxAgeMillis 最大年龄（毫秒）
     */
    suspend fun cleanExpiredCache(maxAgeMillis: Long = CLEANUP_THRESHOLD) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".jpg")) {
                if (now - file.lastModified() > maxAgeMillis) {
                    try {
                        file.delete()
                        Log.d(TAG, "Cleaned expired cache file: ${file.name}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete cache file: ${file.name}", e)
                    }
                }
            }
        }
    }

    private fun resizeAndCompressBitmap(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        quality: Int
    ): Bitmap {
        // 计算缩放比例，保持宽高比
        val scaleX = targetWidth.toFloat() / source.width
        val scaleY = targetHeight.toFloat() / source.height
        val scale = minOf(scaleX, scaleY, 1.0f) // 防止放大

        val newWidth = (source.width * scale).toInt()
        val newHeight = (source.height * scale).toInt()

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    private fun saveToCache(bitmap: Bitmap, cacheFile: File, quality: Int) {
        try {
            FileOutputStream(cacheFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            }
        } catch (e: IOException) {
            throw IOException("Failed to save thumbnail to cache: ${e.message}", e)
        }
    }

    /**
     * 生成缓存键
     */
    fun generateCacheKey(source: Any, width: Int, height: Int): String {
        val input = when (source) {
            is Uri -> source.toString()
            is String -> source
            else -> source.toString()
        }
        
        val md = MessageDigest.getInstance("MD5")
        val hash = md.digest("$input:$width:$height".toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * 调整位图大小
     */
    private fun resizeBitmap(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        // 计算缩放比例，保持宽高比
        val scaleX = targetWidth.toFloat() / source.width
        val scaleY = targetHeight.toFloat() / source.height
        val scale = minOf(scaleX, scaleY, 1.0f) // 防止放大

        val newWidth = (source.width * scale).toInt()
        val newHeight = (source.height * scale).toInt()

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    /**
     * 压缩位图
     */
    private fun compressBitmap(
        source: Bitmap,
        quality: Int
    ): Bitmap {
        // 对于位图压缩，我们主要依赖保存时的质量设置
        // 这里可以添加额外的压缩逻辑，如降低色彩深度等
        return source
    }
}

/**
 * 缩略图生成结果
 */
sealed class ThumbnailResult {
    data class Success(
        val filePath: String,
        val width: Int,
        val height: Int,
        val fileSize: Long,
        val uri: Uri?
    ) : ThumbnailResult()

    data class Failure(
        val uri: Uri?,
        val error: String
    ) : ThumbnailResult()
}

/**
 * 缓存统计信息
 */
data class CacheStats(
    val fileCount: Int,
    val totalSizeBytes: Long,
    val lastModified: Long
) {
    val totalSizeMB: Double
        get() = totalSizeBytes / (1024.0 * 1024.0)
    
    val isEmpty: Boolean
        get() = fileCount == 0
}

/**
 * 缩略图生成配置
 */
data class ThumbnailConfig(
    val width: Int = 480,
    val height: Int = 800,
    val quality: Int = 85,
    val format: String = "JPEG"
)
