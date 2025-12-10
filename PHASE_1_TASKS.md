# 第一阶段（基础架构）任务清单

## 阶段说明

**第一阶段（基础架构）**是 mimic_tiktok 项目的基础建设阶段，主要目标是建立项目的核心架构、数据层、播放器基础和UI框架。完成此阶段后，应能实现一个基础的本地视频播放器，支持基本的滑动播放和数据持久化。

### 阶段交付成果
- 完整的项目结构和依赖配置
- Room数据库完整实现
- PlayerPool和RecyclerView基础播放
- 无限循环和基础手势支持
- 权限和基础设置

---

## 任务列表

### 第1阶段共包含 7 个任务：

#### ✓ Task 1.1：项目基础架构与依赖配置
**类型**: 基础设施 | **优先级**: P0 | **预期工时**: 2小时

**需求描述**:
- 创建标准 Android 项目结构
- 添加所有必要的依赖（ExoPlayer, Room, Coroutines 等）
- 创建各功能包结构（ui, data, player, util, di）
- 配置 Gradle 和版本目录管理

**涉及文件**:
- `build.gradle.kts` (app module 及 root)
- `settings.gradle.kts`
- `gradle.properties`
- `libs.versions.toml` (如使用版本目录)
- 目录结构创建

**验收标准**:
- [ ] 项目能成功构建
- [ ] 所有依赖正确导入（无冲突）
- [ ] MainActivity 能启动且无崩溃
- [ ] 项目包结构完整：ui/, data/, player/, util/, di/

**关键代码片段**:
```kotlin
dependencies {
    // Core Android
    implementation "androidx.core:core-ktx:1.10.0"
    implementation "androidx.appcompat:appcompat:1.6.1"
    
    // Lifecycle & ViewModel
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.6.1"
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1"
    
    // RecyclerView
    implementation "androidx.recyclerview:recyclerview:1.3.0"
    
    // Room
    implementation "androidx.room:room-runtime:2.5.0"
    implementation "androidx.room:room-ktx:2.5.0"
    kapt "androidx.room:room-compiler:2.5.0"
    
    // ExoPlayer
    implementation "com.google.android.exoplayer:exoplayer:2.21.0"
    
    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0"
    
    // Image Loading
    implementation "io.coil-kt:coil:2.3.0"
    
    // Material Design
    implementation "com.google.android.material:material:1.9.0"
}
```

---

#### ✓ Task 1.2：Room 数据库模型与 Repository 实现
**类型**: 数据层 | **优先级**: P0 | **预期工时**: 3小时

**需求描述**:
- 实现 `VideoEntity` 数据类（包含视频元数据）
- 实现 `PlaylistEntity` 和 `PlaylistVideoCrossRef` 关联表
- 实现 `AppDatabase` 和 `AppDao`
- 实现 `VideoRepository` 基础方法
- 支持 Room 的 Flow 和 suspend 函数

**涉及文件**:
- `app/src/main/java/com/mimictiktok/data/db/VideoEntity.kt`
- `app/src/main/java/com/mimictiktok/data/db/PlaylistEntity.kt`
- `app/src/main/java/com/mimictiktok/data/db/PlaylistVideoCrossRef.kt`
- `app/src/main/java/com/mimictiktok/data/db/PlaylistWithVideos.kt`
- `app/src/main/java/com/mimictiktok/data/db/AppDatabase.kt`
- `app/src/main/java/com/mimictiktok/data/db/AppDao.kt`
- `app/src/main/java/com/mimictiktok/data/repository/VideoRepository.kt`

**验收标准**:
- [ ] 所有 Entity 类正确定义并有适当注解
- [ ] 主键、外键关系正确
- [ ] AppDatabase 单例实现正确
- [ ] DAO 包含所有必要的查询和操作方法
- [ ] Repository 包装 DAO 并提供业务逻辑接口
- [ ] 单元测试验证：插入/查询/更新操作正确

**关键代码片段**:
```kotlin
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String, // 文件路径或hash
    val path: String,
    val title: String?,
    val duration: Long,
    val dateTaken: Long,
    val size: Long,
    val coverPath: String?,
    val isFavorite: Boolean = false,
    val likeCount: Long = 0L
)

@Database(entities = [VideoEntity::class, PlaylistEntity::class, PlaylistVideoCrossRef::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    
    companion object {
        private var instance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            return instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "videos.db").build().also { instance = it }
        }
    }
}

@Dao
interface AppDao {
    @Query("SELECT * FROM videos ORDER BY dateTaken DESC")
    fun getAllVideosFlow(): Flow<List<VideoEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(vararg videos: VideoEntity)
    
    @Update
    suspend fun updateVideo(video: VideoEntity)
}
```

---

#### ✓ Task 1.3：媒体选择器与播放列表导入
**类型**: 功能实现 | **优先级**: P0 | **预期工时**: 3小时

**需求描述**:
- 实现 `MediaScanUtil` 媒体扫描工具
- 使用 SAF (Storage Access Framework) 实现多视频选择
- 读取视频文件元数据（时长、大小、日期等）
- 自动创建和保存播放列表
- 处理 Android 11+ 的 Scoped Storage

**涉及文件**:
- `app/src/main/java/com/mimictiktok/util/MediaScanUtil.kt`
- `app/src/main/java/com/mimictiktok/ui/MainActivity.kt` (集成选择器)
- `AndroidManifest.xml` (权限声明)

**验收标准**:
- [ ] 能使用 ACTION_OPEN_DOCUMENT_MULTIPLE 选择多个视频
- [ ] 正确读取视频元数据（时长、大小、修改日期）
- [ ] 所选视频正确插入数据库
- [ ] 创建的播放列表能正确关联选中的视频
- [ ] 权限请求正常工作
- [ ] 能处理用户拒绝权限的情况

**关键代码片段**:
```kotlin
class MediaScanUtil {
    fun getVideoMetadata(context: Context, uri: Uri): VideoMetadata? {
        // 使用 MediaMetadataRetriever 获取视频元数据
    }
    
    fun createPlaylistFromSelection(
        context: Context, 
        repository: VideoRepository, 
        uris: List<Uri>,
        playlistName: String
    ) {
        // 1. 提取元数据
        // 2. 创建 VideoEntity 对象
        // 3. 插入数据库
        // 4. 创建播放列表关联
    }
}
```

---

#### ✓ Task 1.4：PlayerPool 与 RecyclerView 基础播放器
**类型**: 核心功能 | **优先级**: P0 | **预期工时**: 4小时

**需求描述**:
- 实现 `PlayerPool` 类管理和复用 ExoPlayer 实例
- 实现 `VideoAdapter` 和 `VideoViewHolder`
- 创建 `item_video.xml` 布局文件
- 在 `HomeFragment` 中集成 RecyclerView
- 配置 `LinearLayoutManager` 和 `PagerSnapHelper`
- 实现播放器的绑定和解绑逻辑

**涉及文件**:
- `app/src/main/java/com/mimictiktok/player/PlayerPool.kt`
- `app/src/main/java/com/mimictiktok/ui/home/VideoAdapter.kt`
- `app/src/main/java/com/mimictiktok/ui/home/VideoViewHolder.kt`
- `app/src/main/java/com/mimictiktok/ui/home/HomeFragment.kt`
- `app/src/main/res/layout/item_video.xml`
- `app/src/main/java/com/mimictiktok/MainActivity.kt`

**验收标准**:
- [ ] PlayerPool 能创建和管理 2 个 ExoPlayer 实例
- [ ] 播放器实例正确分配给 ViewHolder
- [ ] 视频能正常播放
- [ ] 滑动时自动切换到下一个视频并播放
- [ ] 播放器绑定和解绑生命周期正确
- [ ] 没有内存泄漏（PlayerView.player = null 在 onViewDetachedFromWindow）
- [ ] 滑动流畅，无卡顿

**关键代码片段**:
```kotlin
class PlayerPool(private val context: Context, poolSize: Int = 2) {
    private val players = ArrayList<SimpleExoPlayer>()
    
    init {
        repeat(poolSize) {
            val player = SimpleExoPlayer.Builder(context).build()
            player.repeatMode = Player.REPEAT_MODE_OFF
            players.add(player)
        }
    }
    
    fun acquire(index: Int): SimpleExoPlayer {
        return players[index % players.size]
    }
    
    fun releaseAll() {
        players.forEach { it.release() }
        players.clear()
    }
}

// VideoAdapter and ViewHolder implementation
class VideoAdapter(
    private val context: Context,
    private val pool: PlayerPool,
    private val repository: VideoRepository
) : RecyclerView.Adapter<VideoViewHolder>() {
    
    var data: List<VideoEntity> = emptyList()
    
    override fun getItemCount(): Int = if (data.isEmpty()) 0 else Int.MAX_VALUE
    
    private fun realPos(pos: Int): VideoEntity = data[pos % data.size]
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = realPos(position)
        val player = pool.acquire(position)
        holder.bind(video, player)
    }
}
```

---

#### ✓ Task 1.5：无限循环与初始定位
**类型**: 功能优化 | **优先级**: P0 | **预期工时**: 2小时

**需求描述**:
- 实现虚拟化列表滚动，使用 Int.MAX_VALUE 映射
- 计算并实现初始定位到中间位置
- 确保循环滚动无视觉跳变
- 测试滚动到末尾后自动循环

**涉及文件**:
- `app/src/main/java/com/mimictiktok/ui/home/VideoAdapter.kt` (修改)
- `app/src/main/java/com/mimictiktok/ui/home/HomeFragment.kt` (修改)

**验收标准**:
- [ ] 列表初始定位正确（定位到中间位置）
- [ ] 持续向下滑动不会到达列表末尾
- [ ] 滚动流畅，没有视觉跳变
- [ ] 循环播放正常，视频按顺序循环
- [ ] 内存占用稳定（不因 Int.MAX_VALUE 而增长）

**算法**:
```kotlin
// 初始定位计算
val startIndex = if (adapter.data.isNotEmpty()) {
    Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2 % adapter.data.size)
} else {
    0
}
recyclerView.scrollToPosition(startIndex)

// 位置映射
fun realPosition(position: Int): Int {
    return if (data.isEmpty()) 0 else position % data.size
}
```

---

#### ✓ Task 1.6：缩略图生成与缓存
**类型**: 功能实现 | **优先级**: P1 | **预期工时**: 2小时

**需求描述**:
- 后台生成视频第一帧作为缩略图
- 保存缩略图到 app 内部 cache 目录
- 存储缩略图路径到 VideoEntity.coverPath
- 使用 Coil 库处理图片加载和缓存
- 支持快速图片展示（优先展示缓存）

**涉及文件**:
- `app/src/main/java/com/mimictiktok/util/ThumbnailUtil.kt`
- `app/src/main/java/com/mimictiktok/data/repository/VideoRepository.kt` (修改)

**验收标准**:
- [ ] 能提取视频第一帧为图片
- [ ] 缩略图正确保存到 cache 目录
- [ ] 路径正确存储到数据库
- [ ] 列表加载时快速显示缩略图
- [ ] Coil 缓存正常工作

**关键实现**:
```kotlin
class ThumbnailUtil {
    fun generateThumbnail(context: Context, videoPath: String): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoPath)
            val bitmap = retriever.frameAtTime
            // 保存到 cache 目录
            val file = File(context.cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 85, FileOutputStream(file))
            file.absolutePath
        } finally {
            retriever.release()
        }
    }
}
```

---

#### ✓ Task 1.7：权限管理与基础设置
**类型**: 系统集成 | **优先级**: P0 | **预期工时**: 2小时

**需求描述**:
- 实现运行时权限请求（READ_EXTERNAL_STORAGE / READ_MEDIA_VIDEO）
- 处理权限请求结果和拒绝情况
- 创建基础 SettingsFragment
- 实现权限检查工具函数
- 支持 Android 6-13+ 权限兼容性

**涉及文件**:
- `app/src/main/java/com/mimictiktok/util/PermissionUtil.kt`
- `app/src/main/java/com/mimictiktok/ui/settings/SettingsFragment.kt`
- `app/src/main/java/com/mimictiktok/MainActivity.kt` (集成权限请求)
- `AndroidManifest.xml`

**验收标准**:
- [ ] 应用启动时正确请求权限
- [ ] 权限被拒绝时有合适的提示
- [ ] 能正确检查权限状态
- [ ] 支持 Android 11+ 的 Scoped Storage
- [ ] SettingsFragment 能正常加载
- [ ] 无权限相关的崩溃

**清单和权限代码**:
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

<!-- 在 manifest 中声明 queries for MediaStore -->
<queries>
    <intent>
        <action android:name="android.intent.action.OPEN_DOCUMENT" />
        <data android:mimeType="video/*" />
    </intent>
</queries>
```

```kotlin
class PermissionUtil {
    companion object {
        fun isReadMediaGranted(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}
```

---

## 第一阶段总结

### 任务依赖关系
```
Task 1.1 (基础架构)
    ↓
Task 1.2 (Room数据库) 
    ↓
├── Task 1.3 (媒体选择)
├── Task 1.4 (播放器)
│   ↓
│   Task 1.5 (无限循环)
├── Task 1.6 (缩略图)
└── Task 1.7 (权限管理)
```

### 预期总工时
- 总计：约 18 小时（开发 + 测试）
- 并行可能：Task 1.3, 1.4, 1.6, 1.7 可并行进行

### 质量指标
- 代码覆盖率：≥ 60% (数据层)
- 零 Crash 率（权限和生命周期处理）
- 内存稳定（播放器复用）
- 无 ANR（主线程操作）

### 交付物清单
1. ✓ 完整的项目结构和所有包目录
2. ✓ Room 数据库完整实现（Entity、DAO、Database）
3. ✓ VideoRepository 数据访问层
4. ✓ PlayerPool 和播放器管理
5. ✓ RecyclerView 列表实现
6. ✓ 无限循环播放功能
7. ✓ 缩略图生成和缓存
8. ✓ 权限管理和运行时请求
9. ✓ 基础设置页面
10. ✓ 单元和集成测试

---

## 后续阶段规划

**第二阶段（用户交互）**：
- 手势识别（双击、三击、长按）
- 放大和平移
- 点赞动画和效果

**第三阶段（高级功能）**：
- 收藏夹管理
- 搜索功能
- 沉浸模式

**第四阶段（优化和上线）**：
- 性能优化
- 内存管理优化
- 发布版本编译

---

## 注意事项

1. **保持向后兼容**：确保代码在 Android API 24+ 上运行
2. **避免主线程阻塞**：所有数据库操作使用 suspend 或 Flow
3. **生命周期管理**：正确处理 Activity/Fragment 和播放器生命周期
4. **内存管理**：及时释放 ExoPlayer 实例，使用 WeakReference 避免内存泄漏
5. **测试覆盖**：为 Repository 和 PlayerPool 编写单元测试

