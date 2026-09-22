# 悬浮桌宠

一只浮在其它应用上面的 Android 桌宠。本地运行，不联网、无账号、无广告。

默认角色是二次元 Q 版少女「杏杏」：可拖动、点一下会跳/眨眼，待机会轻微呼吸。打开穿透后触摸会穿过她，方便点底下的微信或浏览器。

## 环境

- JDK 17+（本仓库按 17 编译，JDK 21 也可）
- Android SDK Platform 34、Build-Tools 34.0.0
- 在仓库根目录放 `local.properties`：

```
sdk.dir=/你的/Android/Sdk
```

## 编译

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## 安装 Debug APK

已归档（也是 CI / 本机产物的副本）：

- Debug：`releases/float-deskpet-1.0.0-debug.apk`
- Release（debug 签名，可直接装）：`releases/float-deskpet-1.0.0-release.apk`

Gradle 原始输出：

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

```bash
adb install -r releases/float-deskpet-1.0.0-debug.apk
```

装好后打开「悬浮桌宠」→「去授权」→ 系统页打开「允许显示在其他应用的上层」→ 返回应用点「召唤桌宠」。建议再允许通知，才能用通知栏隐藏或退出。

## 国产系统注意

后台被杀后窗口会消失，这是系统省电，不是没卸干净。

- 小米 / 红米：应用管理 → 悬浮桌宠 → 省电策略「无限制」；允许自启动、后台弹出界面。
- 华为 / 荣耀：应用启动管理 → 手动管理 → 自启动 / 关联启动 / 后台活动。
- OPPO / 一加 / vivo：关电池优化，允许悬浮窗和后台运行。
- 通用：设置 → 应用 → 电池 → 不优化。

强制停止或通知栏「退出」会卸掉悬浮窗并停服务，不应留下残影。若仍看见残影，把应用从最近任务划掉后再进一次设置页点「让她休息」。

## 以后换角色帧

替换这些文件，**文件名不要改**：

| 文件 | 用途 |
| --- | --- |
| `app/src/main/res/drawable-nodpi/pet_idle_0.webp` | 待机（睁眼） |
| `app/src/main/res/drawable-nodpi/pet_idle_1.webp` | 待机眨眼 |
| `app/src/main/res/drawable-nodpi/pet_idle_2.webp` | 待机微侧头 |
| `app/src/main/res/drawable-nodpi/pet_tap_0.webp` | 点击：跳 |
| `app/src/main/res/drawable-nodpi/pet_tap_1.webp` | 点击：挥手 |

要求：竖图、透明底、清晰描边，高度大约 512–1024。不要用糊掉的实拍抠图，小窗上看不清。改完重新 `./gradlew assembleDebug`。

同名预览也在 `art/character/`。

## 版本

见 `releases/CHANGELOG.md`。当前 `1.0.0`。
