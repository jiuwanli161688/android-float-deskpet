# 悬浮桌宠

一只浮在其它应用上面的 Android 桌宠。本地运行，不联网、没有广告。

第一次打开先看几页引导，再留下账号（只在本机），然后选陪伴、起名字。登录后进入「今日陪伴」。女角色风格：御姐 / 萝莉 / 清纯风；男角色：温柔邻家 / 清爽少年 / 沉稳学长 / 大叔。不取名时会从名字册里选一个。可拖动、点一下会跳/眨眼，待机会走路、打盹、躲到屏幕边上探头。打开穿透后触摸会穿过桌宠，方便点底下的微信或浏览器。

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
./gradlew test
```

## 安装 Debug APK

已归档（也是 CI / 本机产物的副本）：

- Debug：`releases/float-deskpet-1.3.3-debug.apk`
- Release（debug 签名，可直接装）：`releases/float-deskpet-1.3.3-release.apk`

Gradle 原始输出：

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

```bash
adb install -r releases/float-deskpet-1.3.3-debug.apk
```

装好后打开「悬浮桌宠」→ 登记或走进来 → 选角色和名字 → 右下角小按钮里「请来身边」→ 系统页打开「允许显示在其他应用的上层」。建议再允许通知，才能用通知栏隐藏或退出。

## 怎么玩

- 单击：跳跃反应 + 气泡（可选系统语音）
- 双击：轮换表情（开心 / 害羞 / 犯困 / 低落）
- 长按：弹出 摸摸 / 喂食 / 睡觉（男生文案略有不同）
- 甩一下：带惯性滑行，碰到边会弹
- 待久了：自己走路、打盹，再久一点会半藏在左/右边缘；点探头或摇一摇手机可唤回来
- 右下角菜单可换角色、改名、换风格、换装（女生：日常 / 睡衣 / 出行；男生：日常 / 出行）、静音、气泡语音

## 国产系统注意

后台被杀后窗口会消失，这是系统省电，不是没卸干净。

- 小米 / 红米：应用管理 → 悬浮桌宠 → 省电策略「无限制」；允许自启动、后台弹出界面。
- 华为 / 荣耀：应用启动管理 → 手动管理 → 自启动 / 关联启动 / 后台活动。
- OPPO / 一加 / vivo：关电池优化，允许悬浮窗和后台运行。
- 通用：设置 → 应用 → 电池 → 不优化。

强制停止或通知栏「退出」会卸掉悬浮窗并停服务，不应留下残影。若仍看见残影，把应用从最近任务划掉后再进一次今日陪伴点休息。

## 以后换角色帧

女生「日常」装替换这些文件，**文件名不要改**：

| 文件 | 用途 |
| --- | --- |
| `app/src/main/res/drawable-nodpi/pet_idle_0.webp` | 待机（睁眼） |
| `app/src/main/res/drawable-nodpi/pet_idle_1.webp` | 待机眨眼 |
| `app/src/main/res/drawable-nodpi/pet_idle_2.webp` | 待机微侧头 |
| `app/src/main/res/drawable-nodpi/pet_tap_0.webp` | 点击：跳 |
| `app/src/main/res/drawable-nodpi/pet_tap_1.webp` | 点击：挥手 |
| `app/src/main/res/drawable-nodpi/pet_sleep_0.webp` | 打盹 |
| `app/src/main/res/drawable-nodpi/pet_shy_0.webp` | 害羞 |

睡衣帧：`pet_pj_*`；出行帽衫帧：`pet_hd_*`。

男生日常：`pet_m_idle_*` / `pet_m_tap_*` / `pet_m_sleep_0` / `pet_m_shy_0`。出行：`pet_m_hd_*`。

要求：竖图、透明底、清晰描边，高度大约 512–1024。不要用糊掉的实拍抠图，小窗上看不清。改完重新 `./gradlew assembleDebug`。

女风格独立造型：`pet_f_yujie_*` / `pet_f_luoli_*` / `pet_f_qingchun_*`。男风格：`pet_m_wenrou_*` / `pet_m_qingshuang_*` / `pet_m_chenwen_*` / `pet_m_dashu_*`。

同名预览也在 `art/character/`。

## 版本

见 `releases/CHANGELOG.md`。当前 `1.3.3`。
