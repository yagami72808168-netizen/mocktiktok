# MimicTikTok - 本地 TikTok 风格视频播放器

🎬 一个功能完整的本地 TikTok 风格视频播放器 Android 应用，使用 Kotlin、ExoPlayer 和 Room 构建。

[![Android](https://img.shields.io/badge/android-7.0+-green.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0+-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](#)

---

## ✨ 核心特性

### 🎯 播放功能
- 📱 全屏沉浸式视频播放
- 🔄 无限循环播放列表
- 🎬 ExoPlayer 播放器复用（PlayerPool）
- ⚡ 高性能内存管理

### 👆 交互功能
- 👆 双击放大（1.5x - 2.0x）
- 👆👆👆 三击更大倍数（3.0x）
- 📍 放大后支持平移
- ❤️ 长按点赞（模拟点赞数增长）
- 📊 点赞动画效果

### 📚 管理功能
- 📋 创建和管理收藏夹（播放列表）
- 🔍 搜索视频和收藏夹
- 📤 导出收藏夹数据（JSON）
- 📥 导入收藏夹数据
- 📜 播放历史记录

### 🔧 系统特性
- 🌙 沉浸模式（隐藏顶部和底部 UI）
- 🔐 运行时权限管理
- 🌍 Android 6-13+ 兼容性
- 🗂️ Scoped Storage 支持
- 💾 本地 Room 数据库持久化

---

## 🚀 快速开始

### 环境要求

```
- Android Studio Flamingo 或更新版本
- JDK 11 或更高版本
- Android SDK API 33+ 
- 最低支持 Android 7.0 (API 24)
```

### 构建项目

```bash
# 1. 克隆项目
git clone [项目地址]
cd mimic_tiktok

# 2. 在 Android Studio 中打开
# File → Open → 选择项目目录

# 3. 等待 Gradle 同步完成

# 4. 构建项目
./gradlew assembleDebug

# 5. 运行应用
./gradlew installDebug
```

或使用 Android Studio 直接运行：
1. 打开项目
2. 连接 Android 设备或启动模拟器
3. 点击 "Run" (Shift + F10)

---

## 📁 项目结构

```
mimic_tiktok/
│
├── app/src/main/java/com/mimictiktok/
│   ├── ui/                    # UI 层（视图和 Fragment）
│   │   ├── MainActivity.kt
│   │   ├── home/             # 主播放页面
│   │   ├── collection/       # 收藏夹管理
│   │   ├── settings/         # 设置页面
│   │   └── search/           # 搜索页面
│   │
│   ├── data/                 # 数据层
│   │   ├── db/              # Room 数据库实现
│   │   ├── repository/      # Repository 数据访问层
│   │   └── model/           # 数据模型类
│   │
│   ├── player/              # 播放器管理
│   │   └── PlayerPool.kt    # 播放器复用池
│   │
│   ├── util/                # 工具类
│   │   ├── MediaScanUtil.kt      # 媒体扫描
│   │   ├── ThumbnailUtil.kt      # 缩略图生成
│   │   ├── PermissionUtil.kt     # 权限管理
│   │   ├── GestureHandler.kt     # 手势识别
│   │   └── AnimationUtil.kt      # 动画工具
│   │
│   └── di/                  # 依赖注入（可选 Hilt）
│
├── app/src/main/res/
│   ├── layout/             # XML 布局文件
│   ├── values/             # 字符串、颜色、尺寸等资源
│   └── drawable/           # 图片和矢量资源
│
├── 📄 mimic_tiktok.txt          # 详细需求文档
├── 📄 tiktok_style_player_ui_wireframe.md  # UI 设计文档
├── 📄 TASK_MAP.md               # 22个任务规划
├── 📄 PHASE_1_TASKS.md          # 第一阶段任务详情
├── 📄 PHASE_1_PROGRESS.md       # 第一阶段进度跟踪
├── 📄 PROJECT_OVERVIEW.md       # 项目概览
└── README.md (本文件)            # 项目说明
```

---

## 🛠️ 技术栈

### 核心库

| 库 | 版本 | 功能 |
|---|------|------|
| **Kotlin** | 2.0+ | 编程语言 |
| **ExoPlayer** | 2.21.0 | 视频播放 |
| **Room** | 2.5.0 | 本地数据库 |
| **Coroutines** | 1.7.0 | 异步编程 |
| **RecyclerView** | 1.3.0 | 列表组件 |
| **Coil** | 2.3.0 | 图片加载 |
| **Material Design** | 1.9.0 | UI 设计 |
| **AndroidX** | 1.10+ | 框架库 |

### Gradle 依赖

```kotlin
dependencies {
    // Core
    implementation "androidx.core:core-ktx:1.10.0"
    implementation "androidx.appcompat:appcompat:1.6.1"
    
    // ExoPlayer
    implementation "com.google.android.exoplayer:exoplayer:2.21.0"
    
    // Room
    implementation "androidx.room:room-runtime:2.5.0"
    implementation "androidx.room:room-ktx:2.5.0"
    kapt "androidx.room:room-compiler:2.5.0"
    
    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0"
    
    // UI
    implementation "androidx.recyclerview:recyclerview:1.3.0"
    implementation "com.google.android.material:material:1.9.0"
    
    // Image Loading
    implementation "io.coil-kt:coil:2.3.0"
}
```

---

## 📋 项目规划

### 🚀 第一阶段 (基础架构) - **现在**

| # | Task ID | 任务 | 优先级 | 状态 |
|---|---------|------|------|------|
| 1 | MIMIC-1 | 项目基础架构与依赖 | P0 | ⭕ |
| 2 | MIMIC-2 | Room 数据库 | P0 | ⭕ |
| 3 | MIMIC-3 | 媒体选择与导入 | P0 | ⭕ |
| 4 | MIMIC-4 | 播放器与列表 | P0 | ⭕ |
| 5 | MIMIC-5 | 无限循环 | P0 | ⭕ |
| 6 | MIMIC-6 | 缩略图缓存 | P1 | ⭕ |
| 7 | MIMIC-7 | 权限管理 | P0 | ⭕ |

🎯 **第一阶段目标**: 建立基础架构，实现基本播放功能

[查看详细规划 →](./PHASE_1_TASKS.md)

### 📱 第二阶段 (用户交互)

- MIMIC-8: 手势识别与双击放大
- MIMIC-9: 三击放大与平移缩放
- MIMIC-10: 长按点赞与动画
- MIMIC-11: 暂停/播放与音量

### ⭐ 第三阶段 (高级功能)

- MIMIC-12: 收藏夹管理
- MIMIC-13: 右滑进入用户主页
- MIMIC-14: 搜索功能
- MIMIC-15: 沉浸模式
- MIMIC-16: 数据导出/导入
- MIMIC-17: 播放历史

### 🎯 第四阶段 (优化上线)

- MIMIC-18: 性能优化
- MIMIC-19: 单元测试
- MIMIC-20: 文档和注释
- MIMIC-21: 国际化
- MIMIC-22: 发布版本

[查看完整规划 →](./TASK_MAP.md)

---

## 💡 关键设计

### PlayerPool 播放器复用

避免频繁创建 ExoPlayer，改用 Pool 模式管理 2-3 个实例：

```kotlin
class PlayerPool(context: Context, poolSize: Int = 2) {
    private val players = ArrayList<SimpleExoPlayer>()
    
    fun acquire(index: Int): SimpleExoPlayer {
        return players[index % players.size]
    }
}
```

**优点**: 低内存占用、快速切换、流畅播放

### 无限循环列表

使用虚拟化列表和 position % size 映射：

```kotlin
class VideoAdapter : RecyclerView.Adapter<VideoViewHolder>() {
    override fun getItemCount() = if (data.isEmpty()) 0 else Int.MAX_VALUE
    
    fun realPosition(position: Int) = position % data.size
}
```

**优点**: 无视觉跳变、流畅无缝、内存占用恒定

### Room 数据库

使用 Room ORM 和 Kotlin Flow 进行响应式数据访问：

```kotlin
@Dao
interface AppDao {
    @Query("SELECT * FROM videos ORDER BY dateTaken DESC")
    fun getAllVideosFlow(): Flow<List<VideoEntity>>
}
```

**优点**: 类型安全、自动迁移、异步处理

---

## 📱 使用指南

### 首次启动

1. **权限请求**: 应用会请求访问媒体文件的权限
2. **导入视频**: 点击 "导入视频" 选择本地视频文件
3. **开始播放**: 点击视频开始播放

### 基本操作

| 操作 | 效果 |
|-----|------|
| 向上/向下滑动 | 切换到上一个/下一个视频 |
| 单击屏幕中心 | 暂停/播放 |
| 双击 | 放大 2.0 倍（再次双击还原） |
| 快速三击 | 放大 3.0 倍 |
| 放大时拖动 | 平移视图查看不同区域 |
| 长按点赞按钮 | 点赞（模拟数字增长） |
| 右滑 | 进入收藏夹主页 |

---

## 🔧 开发指南

### 代码风格

遵循 Kotlin 官方风格指南：

```kotlin
// 好
class VideoAdapter(
    private val context: Context,
    private val pool: PlayerPool
) : RecyclerView.Adapter<VideoViewHolder>()

// 命名约定
const val MAX_PLAYER_POOL_SIZE = 3  // 常量
val videoList: List<VideoEntity> = emptyList()  // 变量
fun loadVideos() {}  // 函数
```

### 测试

```bash
# 运行单元测试
./gradlew test

# 运行 instrumented 测试
./gradlew connectedAndroidTest

# 查看覆盖率
./gradlew testDebugCoverage
```

### 构建发布版本

```bash
# 1. 配置签名密钥 (keystore)
# 2. 生成 release 版本
./gradlew assembleRelease

# 3. APK 位置
# app/build/outputs/apk/release/app-release.apk
```

---

## 🐛 已知问题

| 问题 | 状态 | 计划修复 |
|-----|------|--------|
| Android 12 Scoped Storage 兼容性 | 🟡 进行中 | Phase 1 |
| ExoPlayer 内存泄漏 | 🟡 进行中 | Phase 1 |
| 高帧率视频播放优化 | 🔴 待处理 | Phase 4 |

---

## 📚 文档

- 📄 [详细需求文档](./mimic_tiktok.txt) - 完整的功能和技术规范
- 📄 [UI 设计文档](./tiktok_style_player_ui_wireframe.md) - UI 线框图和设计细节
- 📄 [项目概览](./PROJECT_OVERVIEW.md) - 项目全面介绍
- 📄 [任务规划](./TASK_MAP.md) - 22 个任务的完整规划
- 📄 [第一阶段](./PHASE_1_TASKS.md) - 第一阶段详细任务说明
- 📄 [进度跟踪](./PHASE_1_PROGRESS.md) - 第一阶段进度跟踪

---

## 🤝 贡献指南

### 开发流程

1. **Fork & Clone**
   ```bash
   git clone [项目地址]
   git checkout -b feature/MIMIC-XX-description
   ```

2. **开发**
   ```bash
   # 实现功能
   # 编写测试
   # 提交代码
   ```

3. **提交 PR**
   - 参考格式: `[PHASE-1][MIMIC-1] Brief description`
   - 包含相关 Issue 号
   - 附加测试结果

4. **Code Review**
   - 修复审查意见
   - 通过测试检查
   - 合并到主分支

### 代码质量标准

- ✅ 遵循 Kotlin 风格指南
- ✅ 编写单元测试（数据层）
- ✅ 无 Lint 高危问题
- ✅ KDoc 文档完整
- ✅ 无内存泄漏

---

## 📊 性能目标

| 指标 | 目标 | 说明 |
|-----|------|------|
| 启动时间 | < 3s | 冷启动 |
| 帧率 | ≥ 55 FPS | 列表滑动 |
| 内存占用 | < 150 MB | 播放中 |
| 电池消耗 | < 5% 每小时 | 连续播放 |

---

## 🎯 功能检查清单

### 第一阶段
- [ ] 项目结构就绪
- [ ] 依赖配置完成
- [ ] Room 数据库实现
- [ ] 媒体导入功能
- [ ] 基础播放器
- [ ] 无限循环播放
- [ ] 缩略图缓存
- [ ] 权限管理

### 第二阶段
- [ ] 双击放大
- [ ] 三击放大
- [ ] 点赞动画
- [ ] 暂停/播放

[查看完整检查清单](./PHASE_1_PROGRESS.md)

---

## ⚙️ 系统要求

### 最低要求
- **Android**: 7.0 (API 24)
- **内存**: 256 MB
- **存储**: 50 MB

### 推荐配置
- **Android**: 12.0+ (API 31+)
- **内存**: 4 GB+
- **存储**: 1 GB+ (视频存储)

### 支持列表
- ✅ Android 7.0 - 14.0
- ✅ 手机和平板
- ✅ 横屏和竖屏

---

## 📞 支持和反馈

- 🐛 [报告 Bug](https://github.com/yourrepo/issues)
- 💡 [功能建议](https://github.com/yourrepo/discussions)
- 📧 [联系我们](mailto:support@mimictiktok.dev)

---

## 📄 许可证

本项目采用 [MIT License](./LICENSE) 许可证。

---

## 🙏 致谢

感谢以下开源项目的支持：
- [ExoPlayer](https://exoplayer.dev)
- [Room](https://developer.android.com/training/data-storage/room)
- [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
- [Coil](https://github.com/coil-kt/coil)

---

## 🔗 相关链接

- 🌐 [Android Developer Docs](https://developer.android.com)
- 🎬 [ExoPlayer Documentation](https://exoplayer.dev)
- 📚 [Kotlin Guide](https://kotlinlang.org/docs)
- 🎨 [Material Design 3](https://m3.material.io)

---

**项目状态**: 🚀 **Phase 1 启动中**  
**最后更新**: 2024年  
**维护者**: Development Team

---

> 💬 有问题？查看 [FAQ](#) 或提出 [Issue](https://github.com/yourrepo/issues)

