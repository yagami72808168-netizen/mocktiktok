# MimicTikTok 项目概览

## 项目信息

**项目名称**: mimic_tiktok  
**项目描述**: 一个本地 TikTok 风格的视频播放器 Android 应用  
**项目类型**: Android Native App (Kotlin + Jetpack Compose/Traditional Layout)  
**状态**: Phase 1 (基础架构) - 刚刚启动 🚀

---

## 项目目标

实现一个功能完整、性能优异的本地视频播放器应用，具有以下核心特性：

✅ **核心功能**
- 支持本地视频文件播放
- 无限循环列表（RecyclerView）
- 双击放大、三击更大倍数缩放
- 长按点赞（模拟点赞数增长）
- 收藏夹管理
- 搜索功能
- 沉浸模式（隐藏 UI）

✅ **技术特性**
- ExoPlayer 播放器复用（PlayerPool）
- Room 数据库本地持久化
- Kotlin Coroutines 异步处理
- RecyclerView 无限滚动
- Coil/Glide 图片加载和缓存

✅ **用户体验**
- 沉浸式全屏播放
- 丝滑的手势交互
- 快速的视频切换
- 漂亮的 UI 动画

---

## 项目结构

```
mimic_tiktok/
├── app/                                  # 应用模块
│   ├── src/main/
│   │   ├── java/com/mimictiktok/
│   │   │   ├── ui/                      # UI 层
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── home/               # 主播放页面
│   │   │   │   │   ├── HomeFragment.kt
│   │   │   │   │   ├── VideoAdapter.kt
│   │   │   │   │   └── VideoViewHolder.kt
│   │   │   │   ├── collection/         # 收藏夹页面
│   │   │   │   ├── settings/           # 设置页面
│   │   │   │   └── search/             # 搜索页面
│   │   │   ├── data/                   # 数据层
│   │   │   │   ├── db/                # Room 数据库
│   │   │   │   │   ├── VideoEntity.kt
│   │   │   │   │   ├── PlaylistEntity.kt
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   └── AppDao.kt
│   │   │   │   ├── repository/        # Repository 层
│   │   │   │   │   └── VideoRepository.kt
│   │   │   │   └── model/             # 数据模型
│   │   │   ├── player/                 # 播放器管理
│   │   │   │   └── PlayerPool.kt
│   │   │   ├── util/                   # 工具类
│   │   │   │   ├── MediaScanUtil.kt
│   │   │   │   ├── ThumbnailUtil.kt
│   │   │   │   ├── PermissionUtil.kt
│   │   │   │   ├── GestureHandler.kt
│   │   │   │   └── AnimationUtil.kt
│   │   │   └── di/                     # 依赖注入
│   │   ├── res/
│   │   │   ├── layout/                # XML 布局
│   │   │   │   ├── activity_main.xml
│   │   │   │   ├── fragment_home.xml
│   │   │   │   └── item_video.xml
│   │   │   ├── values/               # 资源文件
│   │   │   │   ├── strings.xml
│   │   │   │   ├── colors.xml
│   │   │   │   └── dimens.xml
│   │   │   └── drawable/             # 图片资源
│   │   └── AndroidManifest.xml        # 应用清单
│   ├── build.gradle.kts               # 应用构建配置
│   └── proguard-rules.pro
├── build.gradle.kts                   # 项目级构建配置
├── settings.gradle.kts
├── gradle.properties
└── gradle/versionCatalogs/
    └── libs.versions.toml            # 版本目录（可选）

📄 文档文件
├── README.md                          # 项目主说明
├── PROJECT_OVERVIEW.md               # 项目概览 (本文件)
├── TASK_MAP.md                       # 22个任务完整规划
├── PHASE_1_TASKS.md                  # 第一阶段详细任务
├── PHASE_1_PROGRESS.md               # 第一阶段进度跟踪
├── mimic_tiktok.txt                  # 原始需求和规范文档
└── tiktok_style_player_ui_wireframe.md # UI 设计和线框图说明
```

---

## 技术栈

### 核心依赖

| 库 | 版本 | 用途 |
|----|-----|------|
| Kotlin | 2.0+ | 编程语言 |
| AndroidX | 1.10+ | Android 基础库 |
| ExoPlayer | 2.21.0 | 视频播放 |
| Room | 2.5.0 | 本地数据库 |
| Coroutines | 1.7.0 | 异步编程 |
| Coil | 2.3.0 | 图片加载 |
| Material Design | 1.9.0 | UI 设计系统 |
| RecyclerView | 1.3.0 | 列表显示 |

### 开发工具

- **IDE**: Android Studio Flamingo 或更新版本
- **Build Tool**: Gradle 8.0+
- **AGP**: 8.12+
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 33+ (Android 13+)

---

## 项目阶段规划

### 🚀 第一阶段 (基础架构) - **现在** ⚡

**目标**: 建立项目基础架构和核心模块  
**工期**: 约 18 小时  
**包含任务**: 7 个

**关键交付**:
- ✅ 项目结构和依赖
- ✅ Room 数据库完整实现
- ✅ 媒体导入功能
- ✅ 基础播放器（PlayerPool + RecyclerView）
- ✅ 无限循环播放
- ✅ 权限管理

[详见 PHASE_1_TASKS.md](./PHASE_1_TASKS.md)

---

### 📱 第二阶段 (用户交互) - 下一步

**目标**: 实现完整的手势交互和动画效果  
**工期**: 约 9.5 小时  
**包含任务**: 4 个

**关键功能**:
- 手势识别（双击、三击、长按）
- 放大和平移缩放
- 点赞动画和效果
- 暂停/播放控制

---

### ⭐ 第三阶段 (高级功能)

**目标**: 实现高级功能模块  
**工期**: 约 15 小时  
**包含任务**: 6 个

**关键功能**:
- 收藏夹管理和展示
- 右滑进入用户主页
- 搜索功能
- 沉浸模式
- 数据导出/导入
- 播放历史记录

---

### 🎯 第四阶段 (优化与上线)

**目标**: 性能优化和发布上线  
**工期**: 约 14 小时  
**包含任务**: 5 个

**关键工作**:
- 性能优化
- 内存管理
- 单元测试
- 国际化支持
- 发布版本打包

---

## 快速开始

### 环境要求

```bash
# 检查版本
java -version          # JDK 11+
gradle -version        # Gradle 8.0+
which adb             # Android SDK tools
```

### 构建项目

```bash
# 克隆或进入项目目录
cd mimic_tiktok

# 构建 Debug 版本
./gradlew assembleDebug

# 运行单元测试
./gradlew test

# 运行 instrumented 测试
./gradlew connectedAndroidTest

# 生成 Release 版本（需要签名配置）
./gradlew assembleRelease

# 检查代码
./gradlew lint
```

### 导入 Android Studio

1. 打开 Android Studio
2. 选择 "Open" → 选择项目目录
3. 等待 Gradle 同步完成
4. 在 Android 虚拟设备或真机上运行

---

## 第一阶段任务列表

### 启动的 7 个任务

| # | Task ID | 任务名称 | 优先级 | 工时 | 状态 |
|---|---------|--------|------|------|------|
| 1 | MIMIC-1 | 项目基础架构与依赖配置 | P0 | 2h | ⭕ Ready |
| 2 | MIMIC-2 | Room 数据库模型与 Repository | P0 | 3h | ⭕ Ready |
| 3 | MIMIC-3 | 媒体选择器与播放列表导入 | P0 | 3h | ⭕ Ready |
| 4 | MIMIC-4 | PlayerPool 与 RecyclerView 基础播放器 | P0 | 4h | ⭕ Ready |
| 5 | MIMIC-5 | 无限循环与初始定位 | P0 | 2h | ⭕ Ready |
| 6 | MIMIC-6 | 缩略图生成与缓存 | P1 | 2h | ⭕ Ready |
| 7 | MIMIC-7 | 权限管理与基础设置 | P0 | 2h | ⭕ Ready |

**总工时**: 18 小时 | **完成度**: 0%

[详细信息见 PHASE_1_PROGRESS.md](./PHASE_1_PROGRESS.md)

---

## 关键设计决策

### 1. 播放器复用策略（PlayerPool）

**问题**: 每个列表项都创建 ExoPlayer 会导致内存溢出  
**解决方案**: 维护 2-3 个 ExoPlayer 实例，通过 Pool 模式分配给需要播放的 Item  
**好处**: 内存占用低、播放流畅、快速切换

### 2. 无限循环实现

**问题**: 列表滚动到底部会卡住  
**解决方案**: 使用虚拟化列表（Int.MAX_VALUE），通过 position % size 映射真实数据  
**好处**: 无视觉跳变、流畅无缝、用户体验好

### 3. 数据库设计

**方案**: Room ORM + Kotlin Flow  
**好处**: 类型安全、自动迁移、响应式编程、异步处理

### 4. 权限管理

**方案**: 区分 Android 13+ 和 12 以下，使用 SAF 多选  
**好处**: 最小权限原则、Scoped Storage 兼容、用户隐私保护

---

## 代码风格和规范

### Kotlin 代码规范

- **命名**: CamelCase for classes/functions, UPPER_CASE for constants
- **格式**: 遵循 Kotlin 官方风格指南
- **文档**: KDoc 注释主要公共 API
- **包组织**: 按功能分层（ui, data, player, util）

### 测试规范

- **单元测试**: 数据层（Repository, DAO）
- **集成测试**: UI 交互和播放流程
- **测试框架**: JUnit4, Mockito, 或 Robolectric

### 提交规范

```
[PHASE-#][TASK-ID] Brief description

Optional detailed explanation about what was changed and why.

Task: MIMIC-1
```

---

## 常见问题 (FAQ)

### Q1: 为什么使用 ExoPlayer 而不是 MediaPlayer？

**A**: ExoPlayer 提供更好的自定义性、更多的格式支持、更好的性能和更新的维护。

### Q2: Room 数据库文件位置在哪里？

**A**: 在 `/data/data/com.mimictiktok/databases/videos.db` (需要 root 权限查看)

### Q3: 如何支持更多的视频格式？

**A**: ExoPlayer 已经支持大多数常见格式（MP4, MKV, WebM 等）。如需自定义，在 ExoPlayer 初始化时配置。

### Q4: 缩略图会占用多少存储空间？

**A**: 取决于视频分辨率，通常每个缩略图 50-200KB。缓存目录会自动清理。

### Q5: 支持哪些 Android 版本？

**A**: Min SDK 24 (Android 7.0)，目标 SDK 33+ (Android 13+)

---

## 贡献指南

### 开发流程

1. **创建特性分支**
   ```bash
   git checkout -b feature/MIMIC-1-task-name
   ```

2. **开发和提交**
   ```bash
   git add .
   git commit -m "[PHASE-1][MIMIC-1] Brief description"
   ```

3. **代码审查**
   - 提交 Pull Request
   - 等待 Code Review
   - 修复反馈意见

4. **合并主分支**
   ```bash
   git merge feature/MIMIC-1-task-name
   ```

---

## 性能指标目标

| 指标 | 目标 | 说明 |
|-----|------|------|
| 启动时间 | < 3s | 冷启动时间 |
| 滑动帧率 | ≥ 55 FPS | 列表滑动流畅度 |
| 内存占用 | < 150 MB | 播放时内存占用 |
| 电池消耗 | < 5% 每小时 | 连续播放一小时 |
| ANR | 0 | 无应用无响应 |
| Crash | 0 | 无崩溃 |

---

## 资源链接

### 官方文档

- 📚 [Android Developer Docs](https://developer.android.com)
- 🎬 [ExoPlayer Docs](https://exoplayer.dev)
- 🗄️ [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- 🔀 [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

### 相关资源

- 🎨 [Material Design 3](https://m3.material.io)
- 📱 [Android Best Practices](https://developer.android.com/guide)
- 🧪 [Android Testing Guide](https://developer.android.com/training/testing)

---

## 联系和支持

- **项目主页**: [项目 Git 仓库地址]
- **问题跟踪**: [Issues 链接]
- **讨论区**: [Discussions 链接]

---

## 许可证

[选择适当的开源许可证，如 MIT, Apache 2.0 等]

---

## 更新日志

### v0.1.0 (Phase 1 - 基础架构) - 2024-XX-XX

**新增**:
- ✅ 项目基础结构
- ✅ Room 数据库
- ✅ PlayerPool 播放器复用
- ✅ RecyclerView 列表
- ✅ 无限循环播放
- ✅ 权限管理

---

**最后更新**: 2024年  
**项目状态**: 🚀 Phase 1 刚刚启动，准备开始开发！

