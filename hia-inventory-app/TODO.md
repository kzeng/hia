# UI
## 图书盘点

## 照片管理

## 系统设置
- 系统信息 
  CPU：
  RAM:
  Disk: used/total
  操作系统：

app/src/main/res/
 ├─ mipmap-mdpi/ic_launcher.png
 ├─ mipmap-hdpi/ic_launcher.png
 ├─ mipmap-xhdpi/ic_launcher.png
 ├─ mipmap-xxhdpi/ic_launcher.png
 └─ mipmap-xxxhdpi/ic_launcher.png

# Featues
## 图书盘点
- *拍照成功后，以Snackbar方式给一个简短的提示。例如“照片已保存”或“拍照成功” ；持续时间：使用 SnackbarDuration.Short（约 3 秒）


## 照片管理



## 照片管理
- *照片管理页面左侧栏的文件夹列表，可以通过读取DCIM 目录下的date文件夹（yyymmdd）获取
- *点击左侧栏的文件夹列表中文件夹，遍历其下所有图片并在右侧Grid 区域显示，当图片很多时，Grid 区域下方需要有分页导航栏。
- *点击Grid 区域的单张图片，Full-screen Dialog 方式显示大图，再次点击返回。
定义实现可能性：完全支持通过组合其他组件或手势检测器实现上述功能，确保符合 M3 视觉风格（动态颜色、动画、elevation 等）。
上一张/下一张（滑动导航）：在 Dialog 内集成 HorizontalPager（Jetpack Compose Pager 库）或 ViewPager2（传统 Views），实现左右滑动切换图片。
手势捏合缩放：使用 Compose 的 graphicsLayer + detectTransformGestures（检测缩放、平移、旋转），或第三方库（如 PhotoView）结合 M3 主题。支持双击放大、边界限制等。
重叠显示文字详情：在图片上方/下方叠加 Text 或 Column（使用 Box 布局），实现半透明 overlay（如图片描述、日期）。可添加淡入/淡出动画，或点击图片切换显示/隐藏。

文字详细信息，如文件名，拍摄时间， 拍摄位置等信息。 拍摄位置信息，如楼层、区域、架号、正反面号、列号、点位号 可根据文件名的命名规则推导出来。
楼层：   文件名1-2位
区域：   文件名3-4位
架号：   文件名5-6位
正反面号 文件名7-8位
列号：   文件名9-10位
点位号： 文件名11位

时间戳： 文件名13-22位 单位：秒； 需要转成YYYY-MM-DD HH:mm:ss


- *长按左侧栏的文件夹列表中文件夹，选择删除该文件夹，实现删除该文件夹功能（提示用户确认）


机器人打点的MarkID规则：01 02 03 01 04 01楼层---区域---架号---正反面号---列号---点位号

- *长按左侧栏的文件夹列表中文件夹，选择上传该文件夹需要以下处理逻辑实现
  1. 扫描DCIM 目录是否存在pic文件夹，如果存在，删除。
  2. 将图片重命名并拷贝到路径 DCIM\pic\01（默认固定）+ {照片文件名前10位} + 01（默认固定摄像头ID）\ book \ {时间戳: 照片文件名13至22位} \  {照片文件名第11位}.{扩展名不变}
  3. 将pic 目录上传到ftp server。 ftp 连接配置，如：主机，端口，账号，密码 从app 系统系统配置中取。
  4. 显示上传进度，上传结束，给出简要反馈信息。
  5. 支持ftp 续传功能（重复上传时，增量上传不要全量上传）

--------------------

图书盘点页面左栏 新增一个参数：架层 ：如：03  两位数字，范围：(01-99) 放在‘点位号’后面配置
拍照图片的命名规则(新的)： 如 0102030104103-1751422638.png, 其中‘-’ 前半部分0102030104103表示  楼层、区域、架号、正反面号、列号、点位号、架层； ‘-’ 后半部分1751422638表示时间戳。

所以 文件命名预览 这里也需要相应修改， 点位号后面多了两位数字(架层)， 如： 0102030104103-<timestamp>.png

照片保存路径名也需要相应修改：
pic//01（默认固定）+ {照片文件名前10位} + {照片文件名12-13位} // book // {时间戳: 照片文件名15至24位} //   {照片文件名第11位}.png
相当于’默认固定摄像头ID 01‘ 位置 被 ’架层‘ 取代。 时间戳往后偏移2位。

图片大图上的详情问题也可能需要修改，增加 ’架层‘， 时间戳往后偏移2位再解析。



## 系统设置
- *对ftp 配置数据进行保存，建议使用Preferences DataStore方式数据持续化。 ftp 数据保存前需交验合法性。
- *测试连接的按钮功能 测试ftp配置的连通性。



# Optimize
测试发现拍照的速度有点慢，从点击拍照到弹出’照片已保存‘消息，大约需要2-3秒， 这里可能包含自动对焦和写文件的过程总时间。 
拍照逻辑这一块是否存在优化的可能？

拍照速度优化：
可以明显提速，关键是避免“YUV→Bitmap→PNG”的双重编码，并让 CameraX 直接把照片写进 MediaStore（JPEG）。另外启用最小延迟模式、适当降低分辨率也会加速对焦/曝光等待与写盘。

建议改动（主打速度的方案）

用 ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY。
通过 OutputFileOptions 直接保存 JPEG 到 MediaStore（无需手动转 Bitmap/PNG）。
可选：设定 4:3 目标比例，jpegQuality≈85，进一步提升速度/体积平衡。

说明

现在不再走 imageProxyToBitmap 与 saveBitmapToDcimDateFolder，CPU/IO 大幅减少，通常可把总时延压到 1 秒内（视设备而定）。
最小延迟模式会减少等待 AF/AE 完全收敛的时间，拍摄响应更快；如需更稳的曝光/对焦，可改回 MAXIMIZE_QUALITY。
若必须严格使用 .png 后缀，请确认是否允许改为 .jpg；PNG 是无损压缩，编码很慢。若必须 PNG，建议：降低目标分辨率（例如 setTargetResolution 1280×960），或拍完后台异步转 PNG，但拍摄当下仍会慢。

### FAQ：最高质量 JPG vs PNG（质量与时空效率）
- 画质
  - 实拍照片：JPEG（q≈95–100）与 PNG 肉眼差异很小；极端细节/反复编辑才更易见差。
  - 文本/线稿/UI 截图：PNG 更锐利（无损、无振铃/马赛克）；JPEG 可能出现压缩纹理。
- 体积（空间效率）
  - 同分辨率实拍照片：JPEG 通常比 PNG 小 3–10 倍。
- 性能（时间效率）
  - 编码/写盘：JPEG 明显更快；CameraX 原生输出 JPEG，管线更短（可快 2–10 倍，设备依赖）。
- 特性
  - PNG 支持透明通道；JPEG 通常具备 EXIF、旋转标记、缩略图等。

结论与建议
- 默认：使用 JPEG（jpegQuality≈85–95），后缀 .jpg，获得更好的“时空效率”（更快、更小）。
- 仅当需要无损或透明通道时使用 PNG。
- 若业务强制 PNG：降低分辨率或后台异步转 PNG；拍摄当下仍建议先保存 JPEG 以提速。

落地到本应用
- 拍照：ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY + 直接写 MediaStore（JPEG），jpegQuality≈90。
- 命名/解析/上传：优先兼容 .jpg；如需保留 .png，两个后缀均支持。
- 设置项：新增“保存格式（JPG/PNG）”并在说明中提示性能差异。

# ISSUES

# UI
## 图书盘点
<!-- ...existing content... -->

### 左侧“文件夹列表”可视性与滚动优化
问题：当文件夹很多时，非滚动容器（如普通 Column）会导致超出视口的项不可见。

方案（Compose + M3）：
1) 使用 LazyColumn + rememberLazyListState，天然虚拟化与垂直滚动；确保 Modifier.fillMaxHeight().
2) 顶部加入搜索框（按 yyyymmdd 模糊过滤），减少列表长度。
3) 按月份分组并使用 stickyHeader 显示“2026-01”等分组标题，提升可扫读性。
4) 可选：叠加自定义细窄滚动条（Overlay），或集成第三方 FastScroller（Compose）以快速拖拽。
5) 交互：选中项保持可见（onSelect 后调用 animateScrollToItem），长按弹出菜单（删除/上传）。
6) 状态：记住滚动位置（firstVisibleIndex/offset），返回时恢复；避免闪烁。
7) 无障碍：滚动条触控目标≥48dp，StickyHeader加 contentDescription；搜索框支持TalkBack。

示例（简化伪代码，仅展示结构）：
```kotlin
// 左栏示例
@Composable
fun FolderSidebar(
  foldersByMonth: Map<YearMonth, List<Folder>>,
  onSelect: (Folder) -> Unit,
  modifier: Modifier = Modifier
) {
  val listState = rememberLazyListState()
  Box(modifier) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth()
    ) {
      // 搜索框占位（可放外层）
      // stickyHeader 需要 foundation.lazy.Experimental
      foldersByMonth.forEach { (month, items) =>
        stickyHeader {
          Surface(tonalElevation = 2.dp) { Text(text = month.format(), modifier = Modifier.padding(8.dp)) }
        }
        items(items, key = { it.path }) { folder =>
          FolderRow(folder = folder, onClick = { onSelect(folder) }, onLongClick = { /* 删除/上传 */ })
        }
      }
    }

    // 可选：自定义滚动条（根据 listState 计算 thumb 高度与位置）
    VerticalScrollbar(
      listState = listState,
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .fillMaxHeight()
        .width(4.dp)
    )
  }
}

// 选中项后确保可见
suspend fun ensureVisible(listState: LazyListState, index: Int) {
  val range = listState.layoutInfo.visibleItemsInfo.map { it.index }
  if (index !in range) listState.animateScrollToItem(index)
}
```

依赖建议：
- 若需“拖拽快滑”手感，考虑引入 FastScroller（Compose 第三方库），仅用于左栏；保持 M3 主题色彩。
- 不引库时，使用 Overlay 细滚动条 + 点击跳转“月份索引”按钮（浮动侧栏）。

性能要点：
- LazyColumn items 使用稳定 key（文件夹路径/日期）。
- deriveStateOf 处理过滤与分组；避免每次重组全量排序。
- 大量数据时分页加载或分批构建列表。

## 照片管理
// ...existing content...

### 左侧文件夹列表（派生 DCIM/yyyymmdd）
- 使用上述 LazyColumn + stickyHeader 方案；支持搜索与长按菜单。
- 选中后刷新右侧 Grid；Grid 的分页导航与左栏互不遮挡。

# Featues
## 照片管理
// ...existing content...

- 左侧栏列表：改为 LazyColumn，添加搜索与月份 stickyHeader；可选自定义滚动条或 FastScroller。

# Optimize
PNG 拍摄提速方案
- 默认方案（异步转码，提升“时间效率”）
  - 拍摄：CameraX ImageCapture 使用 CAPTURE_MODE_MINIMIZE_LATENCY，直接保存 JPEG 到 MediaStore；拍后立刻弹 Snackbar（≈1 秒内）。
  - 转码：WorkManager 后台将 JPEG 转成 PNG，并按命名/路径规则移动与重命名；完成后更新列表并（可选）删除原 JPEG。
  - 实现要点：
    - 复用 YuvToRgbConverter/Bitmap，避免每次分配；转码用 IO 与 Default Dispatcher，禁止阻塞主线程。
    - 进度：在“照片管理”页显示后台队列与进度；失败自动重试（指数退避）。
    - 上传：当 PNG 就绪后再入队上传；支持增量续传。
- 直出 PNG（业务强制 PNG 时）
  - 分辨率：降低到 1280×960 或 1600×1200（4:3），显著缩短编码与写盘。
  - NDK：libyuv 做 YUV_420_888→ARGB 加速；libpng 编码，设置 compressionLevel=1–3、FILTER_NONE（优先速度）。
  - 管线与内存：
    - 预分配与复用 DirectByteBuffer/ByteArrayOutputStream；避免拷贝链路（YUV→RGB→PNG 流式写入 FileOutputStream）。
    - 按批写盘，使用 MediaStore is_pending 标记，完成后清除；避免 UI 等待。
  - 线程：编码在单独线程池执行；主线程仅负责快照与提示。
- 共同优化
  - 目标比例 4:3；对焦/曝光使用 CAPTURE_MODE_MINIMIZE_LATENCY。
  - 禁止在 UI 线程执行 PNG 压缩；所有重任务走后台。
  - 错误与回退：若 PNG 转码失败，保留 JPEG 并提示用户重试。
- 设置与可配置项
  - 设置项：保存格式（JPG/PNG/自动），当选 PNG 时提示“拍摄当下更慢，建议后台转码”。
  - PNG 转码压缩等级（1–3）与分辨率（预设档位）可选。

## Diagnostics：拍照实现现状检查（JPEG vs PNG）
- 静态检索（代码层）
  - 搜索关键字：ImageCapture、setCaptureMode、CAPTURE_MODE_MINIMIZE_LATENCY/MAXIMIZE_QUALITY
  - 搜索保存路径：OutputFileOptions、MediaStore.Images.Media（JPEG 直写标志）
  - 搜索管线迹象：ImageProxy、YuvToRgbConverter、Bitmap.compress(format=PNG)、FileOutputStream(".png")
- 运行时验证（日志与耗时）
  - 在 takePhoto/onImageSaved/onError 周围记录耗时与管线标记：
    - Log：[Camera] captureStart、[Camera] saved(uri=..., ext=.jpg/.png)、duration=XXX ms
    - Log：[Pipeline] path=JPEG(MediaStore) 或 path=YUV→Bitmap→PNG
- 判定标准
  - JPEG 直写：扩展名 .jpg、MediaStore Uri、CAPTURE_MODE_MINIMIZE_LATENCY；总时延≈0.8–1.5s
  - PNG 管线：出现 Bitmap.compress PNG 或 libpng；总时延≈2–4s（设备依赖）
- 快速检索命令
  - Windows：findstr /s /i "ImageCapture CAPTURE_MODE OutputFileOptions ImageProxy Bitmap.compress PNG" *.kt *.java
  - Git Bash：rg -n "ImageCapture|CAPTURE_MODE|OutputFileOptions|ImageProxy|Bitmap\.compress|PNG" app/src
- 后续动作
  - 若当前为 PNG 管线：按“PNG 拍摄提速方案”迁移或改为后台转码。
  - 若为 JPEG 直写：确保 jpegQuality≈85–95，并兼容命名/上传逻辑的 .jpg。

----------------------------

下面是项目的“交接用”环境与目标机信息总结，便于在其他机器上编译运行与上线。

开发环境与工具链
- 项目类型：Android 应用（Kotlin + Jetpack Compose）
- 构建系统与版本
  - Gradle: 8.2（见 hia-inventory-app/gradle/wrapper/gradle-wrapper.properties）
  - Android Gradle Plugin: 8.2.2（见 hia-inventory-app/build.gradle.kts）
  
构建与安装命令（当前验证通过）
- Debug 构建（在 hia-inventory-app 目录下执行）：
  - ANDROID_SDK_ROOT="$HOME/Android/Sdk" "$HOME/.gradle/wrapper/dists/gradle-8.13-bin/ap7pdhvhnjtc6mxtzz89gkh0c/gradle-8.13/bin/gradle" --no-daemon assembleDebug
- 也可以使用项目自带 Gradle Wrapper：
  - ANDROID_SDK_ROOT="$HOME/Android/Sdk" ./gradlew --no-daemon assembleDebug
- 安装 Debug APK 到设备（在 hia-inventory-app 目录下执行）：
  - ./gradlew installDebug
  - Kotlin: 1.9.23（见 hia-inventory-app/build.gradle.kts）
  - JDK 要求：AGP 8.2 需 JDK 17（即使源/目标兼容设置为 1.8）
  - Java 源/目标兼容：1.8（见 hia-inventory-app/app/build.gradle.kts）
- Android SDK
  - compileSdk: 34
  - targetSdk: 33
  - minSdk: 23（以上均见 hia-inventory-app/app/build.gradle.kts）
- Compose
  - Compose Compiler: 1.5.12
  - Compose BOM: 2024.02.01（见 hia-inventory-app/app/build.gradle.kts）
- 应用信息
  - applicationId/namespace：com.example.hia
  - versionName: 1.0.3，versionCode: 1（见 hia-inventory-app/app/build.gradle.kts）
- 主要依赖
  - Material3、Compose UI/Foundation、Navigation Compose 2.7.6
  - CameraX 1.3.2、Coil 2.4.0、DataStore 1.0.0
  - Apache Commons Net (FTP) 3.9.0、Google Material 1.12.0

签名与发布
- Release 签名通过环境变量配置（未设置则回落到 debug 签名）
  - ANDROID_KEYSTORE_PATH、ANDROID_KEYSTORE_PASSWORD、ANDROID_KEY_ALIAS、ANDROID_KEY_PASSWORD
  - 见 build.gradle.kts

VS Code 集成与任务
- 任务文件：.vscode/tasks.json（含 Build/Install/ADB/Git 相关任务）
- 说明：任务使用“系统 Gradle”路径。Windows 下可改用项目包装器 gradlew.bat，或在 tasks.json 中配置本机 Gradle 可执行路径。

Windows 下构建与安装（命令行）
````bat
:: 在工作区根目录：c:\Users\zen82746.LI\Desktop\hia
:: Debug 构建
.\hia-inventory-app\gradlew.bat -p ".\hia-inventory-app" assembleDebug

:: 安装到已连接设备
.\hia-inventory-app\gradlew.bat -p ".\hia-inventory-app" installDebug

:: 列出设备
adb devices
````

运行时权限与平台行为
- Manifest 权限（见 hia-inventory-app/app/src/main/AndroidManifest.xml）
  - CAMERA、INTERNET
  - READ_MEDIA_IMAGES（Android 13+）
  - READ_EXTERNAL_STORAGE（≤ Android 12，maxSdkVersion=32）
  - WRITE_EXTERNAL_STORAGE（≤ Android 9，10+ 使用 MediaStore）
- 存储/媒体
  - Android 10+ 使用 MediaStore 读写/删除图片（删除经 MediaStore.createDeleteRequest）
  - 操作 DCIM/yyyyMMdd 目录；支持图片浏览与放大
- FTP
  - 设置页配置服务器、端口、账号、密码；DataStore 持久化；支持连接测试与目录上传

目标设备与环境
- Android 6.0+（minSdk 23），推荐 Android 13（targetSdk 33，compile 34）
- 需有后置摄像头、可用存储空间、网络连接（FTP）
- 首次启动授予 CAMERA/存储 等权限；上传前在设置页测试 FTP 连接

交接与新机初始化清单
- 安装 JDK 17、Android SDK 与 Platform Tools（adb）
- 使用项目自带 gradlew.bat 或安装 Gradle 8.2
- 如需发布签名版，在系统环境变量设置 ANDROID_KEYSTORE_* 四项
- VS Code 中检查 tasks.json 的 Gradle/adb 路径是否符合本机
- 首次运行后在设置页填写 FTP 信息并“测试连接”

快速定位关键信息的文件
- hia-inventory-app/build.gradle.kts（AGP、Kotlin、插件）
- hia-inventory-app/app/build.gradle.kts（SDK 等级、Compose、依赖、版本、签名）
- hia-inventory-app/gradle/wrapper/gradle-wrapper.properties（Gradle 版本）
- hia-inventory-app/app/src/main/AndroidManifest.xml（权限/组件）
- .vscode/tasks.json（构建/安装任务与工具路径）