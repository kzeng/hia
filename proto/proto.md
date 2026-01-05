这是一个 Android App 页面，风格用 Material 3, 运行在安卓PAD （10.1 inch, 16:10, 1200*1920） 上

1. 盘点（书架图书）拍照页面
页面用途：
记录拍照地点信息和拍照功能。

页面包含：
- 顶部：标题是「图书盘点」
- 中间 （左栏）：记录拍照地点信息；需要记录的信息如下 ：
楼层 	：如：01 两位数字，范围：(01-99)
区域 	：如：02 两位数字，范围：(01-99)
架号 	：如：03 两位数字，范围：(01-99)
正反面号 	：如：01 两位数字，范围：(01-99)
列号		：如：04 两位数字，范围：(01-99)
点位号  	：如：1  一位数字，范围：(1-9)
注意，下拉列表的取值范围, 另外不要展示下拉列表展开的效果

- 中间 （右栏）：后置摄像头摄像视频窗口 + 拍摄按钮
- 浮动按钮 （导航内容： 图书盘点（高亮）， 照片管理， 系统设置）

处理逻辑： 
拍照图片的命名规则： 如 01020301041-1751422638.png, 其中‘-’ 前半部分01020301041表示  楼层、区域、架号、正反面号、列号、点位号； ‘-’ 后半部分1751422638表示时间戳。


整体感觉：
- 简洁 / 工业风 / 偏工具

2. 照片管理页面
页面用途：
对盘点拍照的图片进行管理，如：预览图片详情，删除单张图，删除整个图片目录，上传整个图片目录

页面包含：
- 顶部：标题是「照片管理」
- 中间 （左栏 宽度25%）：纵向列表形式； 文件夹图标 + 文件夹名称（文件夹名称有年月日数字组成，如： 20251231，20260105 ）
- 中间 （右栏）： 图片 4x4 GRID 展示。当图片很多时，底部带分页导航栏

处理逻辑：
GRID 中图片下方带文件名，如 01020301041-1751422638.png
点击单张图片可以预览大图，和详细信息，如文件名，拍摄时间， 拍摄位置等信息。 拍摄位置信息，如楼层、区域、架号、正反面号、列号、点位号 可根据文件名个命名规则推导出来。
长按左栏文件夹名称， 弹出快捷菜单： 
删除文件夹操作， 确认后可以删除整个文件夹及文件夹下所有图片文件
上传文件夹操作，


照片保存目录名：pic//01（默认固定）+ {照片文件名前10位} + 01（默认固定摄像头ID）// book // {时间戳: 照片文件名13至22位} //   {照片文件名第11位}.png


- 浮动按钮 （导航内容： 图书盘点， 照片管理， 系统设置）

整体感觉：
- 简洁 / 工业风 / 偏工具

3. 系统设置页面
页面用途：
APP系统相关设置

页面包含：
- 顶部：标题是「系统设置」
- 中间 （左栏, 卡片布局， 一类配置在一个卡片中）： 
FTP配置
---------------
服务器 （文本输入框， 如：192.168.10.10）
端口   （文本输入框  如：21）
用户名 （文本输入框   如：ftpuser）
密码   （密码输入框  如：ftpuser）
保存配置按钮

- 中间 （右栏， 卡片布局）：
系统信息: CPU,RAM,...
APP信息： 
Logo
APP名称： Handheld Inventory Assistant (HIA) 手持盘点助手 
版本信息： 如：1.0,
版权信息: boku@2026

- 浮动按钮 （导航内容： 图书盘点， 照片管理， 系统设置）

整体感觉：
- 简洁 / 工业风 / 偏工具




# HIA Inventory App

A Kotlin Android app using Jetpack Compose Material 3 for book inventory, location tagging, photo management, and upload. This repository ships with a working UI skeleton matching the provided prototypes. Business logic can be added later.

## Requirements
- Android Studio 2025.2
- JDK 17 (required by AGP 8.2)
- Kotlin 1.9.22
- Material 3 (Jetpack Compose)
- Minimum SDK 23; Target SDK 33 (Android 13)

## Project Setup
1. Open Android Studio and select "Open". Choose the folder hia-inventory-app.
2. Android Studio will import Gradle settings from [settings.gradle.kts](settings.gradle.kts) and sync dependencies.
3. If you see the error "Gradle wrapper jar not found":
     - Install Gradle locally, then generate the wrapper:

     ### Ubuntu 22
     ```bash
     sudo apt update
     sudo apt install gradle
     cd hia-inventory-app
     gradle wrapper
     ./gradlew clean
     ./gradlew assembleDebug
     ```

     ### Windows (PowerShell)
     ```powershell
     winget install Gradle.Gradle
     Push-Location "C:\Users\zen82746.LI\Desktop\hia\hia-inventory-app"
     gradle wrapper
     .\gradlew.bat clean
     .\gradlew.bat assembleDebug
     ```

4. In Android Studio, use "Build > Make Project" or run the Gradle task assembleDebug.

## What’s Implemented
- Compose Material 3 UI scaffolds
    - Inventory screen: [app/src/main/kotlin/com/example/hia/ui/screens/InventoryScreen.kt](app/src/main/kotlin/com/example/hia/ui/screens/InventoryScreen.kt)
    - Photos screen: [app/src/main/kotlin/com/example/hia/ui/screens/PhotosScreen.kt](app/src/main/kotlin/com/example/hia/ui/screens/PhotosScreen.kt)
    - Settings screen: [app/src/main/kotlin/com/example/hia/ui/screens/SettingsScreen.kt](app/src/main/kotlin/com/example/hia/ui/screens/SettingsScreen.kt)
- Bottom navigation + NavHost in [app/src/main/kotlin/com/example/hia/MainActivity.kt](app/src/main/kotlin/com/example/hia/MainActivity.kt)
- Material 3 theme in [app/src/main/kotlin/com/example/hia/ui/theme/Theme.kt](app/src/main/kotlin/com/example/hia/ui/theme/Theme.kt) and [Type.kt](app/src/main/kotlin/com/example/hia/ui/theme/Type.kt)
- Gradle configured for Compose:
    - Root plugins in [build.gradle.kts](build.gradle.kts)
    - Repositories in [settings.gradle.kts](settings.gradle.kts)
    - Compose setup and dependencies in [app/build.gradle.kts](app/build.gradle.kts)

## Notes
- Manifest uses Theme.Material3.DayNight.NoActionBar for Compose: [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml)
- Permissions declared for camera and location; upload/storage logic to be added later.
- Compose BOM and Navigation Compose are used; Kotlin compiler extension set to 1.5.12.


## Next Steps
- Wire real inventory data and location capture.
- Integrate camera/gallery and upload flows.
- Replace placeholder texts with strings.xml resources where needed.
1. Clone the repository.
2. Open the project in Android Studio 2025.2.
3. Ensure JDK 17 is selected (AGP 8 requires Java 17).
4. Let Android Studio sync Gradle; it will download/update the Gradle wrapper as needed.
5. Build and run the application on an Android 13 device/emulator.


## Future Enhancements
- Implement additional business logic for inventory and photo management.
- Improve user interface based on user feedback.
 - Match UI precisely to `proto/*.png` designs.