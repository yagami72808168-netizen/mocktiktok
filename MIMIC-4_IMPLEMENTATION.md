# MIMIC-4: PlayerPool 与 RecyclerView 基础播放器 - 实现文档

## 概述
完成了 PlayerPool 播放器复用机制和基础播放器 UI 的实现。

## 已实现的功能

### 1. PlayerPool 类 (`player/PlayerPool.kt`)
- **功能**：管理 2-3 个 ExoPlayer 实例的池化管理
- **特性**：
  - 使用 Pool 模式复用播放器实例
  - 配置优化的缓冲参数（15-50秒缓冲）
  - 自适应码率选择（DefaultTrackSelector）
  - 视频缩放模式设置
  - 线程安全的实例管理（CopyOnWriteArrayList）

**关键配置**：
```kotlin
- minBufferMs: 15000
- maxBufferMs: 50000
- bufferForPlaybackMs: 2500
- bufferForPlaybackAfterRebufferMs: 5000
- repeatMode: REPEAT_MODE_ONE
- videoScalingMode: SCALE_TO_FIT
```

### 2. PlayerManager 类 (`player/PlayerManager.kt`)
- **功能**：管理播放器生命周期和播放状态
- **特性**：
  - 播放位置跟踪
  - 播放/暂停控制
  - 自动切换播放器
  - 播放器预加载和准备
  - 统一的播放器释放管理

**主要方法**：
- `preparePlayer(position, video)`: 准备播放器
- `playAtPosition(position, video)`: 播放指定位置视频
- `pauseCurrent()`: 暂停当前播放
- `resumeCurrent()`: 恢复播放
- `togglePlayPause()`: 切换播放/暂停状态
- `releaseAll()`: 释放所有资源

### 3. VideoViewHolder 类 (`ui/home/VideoViewHolder.kt`)
- **功能**：RecyclerView 中的视频显示和交互
- **特性**：
  - 使用 PlayerView 显示视频
  - 点击切换播放/暂停
  - 播放状态图标动画
  - 视频信息显示（标题、时长）
  - 播放器监听器管理
  - 自动格式化时长显示

**UI 元素**：
- PlayerView: 视频播放视图
- 标题和时长显示
- 播放/暂停图标（带淡入淡出动画）

### 4. VideoAdapter 类 (`ui/home/VideoAdapter.kt`)
- **功能**：RecyclerView 适配器，管理视频列表
- **特性**：
  - 无限循环列表（Int.MAX_VALUE）
  - 播放器与 ViewHolder 绑定
  - 自动播放控制
  - ViewHolder 回收时正确释放资源
  - 位置映射（虚拟位置 -> 真实数据）

**核心逻辑**：
```kotlin
override fun getItemCount(): Int = if (data.isEmpty()) 0 else Int.MAX_VALUE
private fun getRealPosition(position: Int): Int = position % data.size
```

### 5. HomeFragment 类 (`ui/home/HomeFragment.kt`)
- **功能**：主页视频播放界面
- **特性**：
  - RecyclerView + PagerSnapHelper 实现页面滑动
  - 滚动监听自动播放
  - 初始位置定位到中间
  - 生命周期感知（onResume/onPause 控制播放）
  - Flow 响应式数据更新

**滚动逻辑**：
- 监听 `SCROLL_STATE_IDLE` 状态
- 获取完全可见的 item 位置
- 自动播放当前位置视频

### 6. HomeViewModel 类 (`ui/home/HomeViewModel.kt`)
- **功能**：管理视频数据和业务逻辑
- **特性**：
  - StateFlow 状态管理
  - 视频列表加载
  - 收藏功能（toggleFavorite）
  - 点赞功能（incrementLikeCount）
  - 数据刷新

### 7. 布局文件 (`res/layout/item_video.xml`)
- **功能**：视频 item 布局
- **结构**：
  - FrameLayout 根容器
  - PlayerView 全屏视频
  - 底部视频信息层（半透明背景）
  - 中心播放/暂停图标

### 8. MainActivity 改造
- 从 Compose Activity 改为 AppCompatActivity
- 使用 Fragment 容器
- 直接加载 HomeFragment

## 技术特点

### 播放器池化
- 避免频繁创建/销毁 ExoPlayer 实例
- 内存占用可控（固定 2-3 个实例）
- 通过位置取模分配播放器

### 性能优化
- 预缓冲配置优化
- 自适应码率
- ViewHolder 复用
- 及时释放资源

### 生命周期管理
- Fragment onPause/onResume 控制播放
- ViewHolder unbind 释放资源
- 正确的播放器监听器管理

### 响应式架构
- ViewModel + StateFlow
- Flow 数据流
- 生命周期感知组件

## 验收标准完成情况

✅ **PlayerPool 能正确创建和管理播放器实例**
- 创建 2-3 个配置完善的 ExoPlayer 实例
- 提供 acquire/release 方法
- 支持 releaseAll 释放所有资源

✅ **RecyclerView 能正确显示视频列表**
- 使用 LinearLayoutManager + PagerSnapHelper
- 实现无限循环滚动
- 正确的 item 布局

✅ **视频能正确播放**
- 自动播放当前可见视频
- 支持点击播放/暂停
- 播放状态图标反馈

✅ **播放器在 ViewHolder 销毁时正确释放**
- onViewDetachedFromWindow 调用 unbind
- onViewRecycled 调用 unbind
- 移除播放器监听器

✅ **内存占用在可接受范围内**
- 固定数量的播放器实例（2-3个）
- 及时释放不用的资源
- 使用 Pool 模式避免内存泄漏

## 依赖更新
- 添加 ViewBinding 支持
- 添加 Robolectric 测试依赖
- 添加 Mockito 测试依赖

## 测试
- 创建 PlayerPoolTest 单元测试
- 验证播放器池创建和管理
- 验证播放器获取和释放

## 下一步
- MIMIC-5: 实现视频预加载和缓存优化
- 添加更多手势交互
- 性能监控和优化

## 技术债务
- 可以添加更多的单元测试覆盖
- 可以添加播放器状态日志
- 可以优化内存监控

## 注意事项
1. 确保在 Fragment 生命周期结束时调用 `playerManager.releaseAll()`
2. ViewHolder 必须在 unbind 时移除监听器
3. 播放器池大小建议 2-3 个，不宜过多
4. 使用 REPEAT_MODE_ONE 实现单视频循环播放
