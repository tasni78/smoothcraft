# SmoothCraft 流畅工坊

[![Build](https://img.shields.io/badge/build-local%20verified-brightgreen)](#七构建)
![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-62B47A)
![Fabric](https://img.shields.io/badge/Fabric-0.20.1%2B-DBB69B)
![Java](https://img.shields.io/badge/Java-25-orange)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

面向 **Minecraft Java 26.1.2 / Fabric** 的性能优化模组，客户端和服务端都能装。

- **客户端**：在几乎不改变原版观感的前提下，把该省下的绘制开销省掉，把帧数还给你。
- **服务端**：盯住 MSPT，服务器吃力时只对「远处的掉落物和经验球」降频，平时完全不干预。

> 两端可选装：只装客户端 → 渲染优化全生效；只装服务端 → TPS 守护生效；两边都装 → 各管各的一半。

---

## 安装

| 依赖 | 版本 |
| --- | --- |
| Minecraft | 26.1.2 |
| Fabric Loader | 0.20.1 或更高 |
| Fabric API | 0.155.3+26.1.2 或更高 |
| Java | 25 |

1. 到 [Releases](https://github.com/tasni78/smoothcraft/releases) 下载最新版 jar（或按第七节自行构建）；
2. 丢进 `.minecraft/mods/`（服务端则是服务器的 `mods/`）；
3. 确认同目录里有 **Fabric API**，启动即可。

首次启动会在 `config/smoothcraft.json` 生成默认配置，不改也能直接用。

## 一、客户端做了什么

| 模块 | 注入点 | 原理 | 默认阈值 |
| --- | --- | --- | --- |
| 实体渲染剔除（分类） | `EntityRenderDispatcher#shouldRender` | 超距实体这一帧直接不画，按类别给不同距离 | 生物 80 / 掉落物 32 / 其他 96 格 |
| 方块实体剔除 | `BlockEntityRenderDispatcher#tryExtractRenderState` | 超距返回 `null` 跳过 | 64 格 |
| 粒子预算 | `ParticleEngine#add` | 每刻新建粒子超预算直接丢弃（连对象都不创建） | 400 / 刻 |
| **粒子距离过滤** | `ParticleEngine#makeParticle` | 火把火焰、各类烟、营火烟、岩浆滴落，超过距离不再生成 | 24 格 |
| 名牌距离限制 | `EntityRenderer#shouldShowName` | 远处不画名字 | 32 格 |
| 阴影剔除 | `EntityRenderer#getShadowRadius` | 远处实体不画落地阴影（返回 0 原版就跳过） | 48 格 |
| 云层跳过（可选） | `CloudRenderer#render` | 完全关闭云 | 默认关 |
| 星空跳过（可选） | `SkyRenderer#renderStars` | 不画星星 | 默认关 |
| **帧率守护** | `Minecraft#getFps()` 每秒采样 | 掉帧自动收缩剔除距离 + 粒子预算，回血后缓慢恢复 | 目标 60 FPS |
| **性能面板** | Fabric HUD API | 实时显示 FPS / 剔除数 / 丢弃粒子 / 自适应比例 | F8 开关 |

## 二、服务端做了什么

| 模块 | 注入点 | 原理 | 默认阈值 |
| --- | --- | --- | --- |
| **远处实体降频** | `ServerLevel#tickNonPassenger` | 服务器吃力时，32 格外的掉落物/经验球隔一刻 tick 一次 | MSPT > 45ms 才启动 |
| TPS 监控 | `MinecraftServer#getCurrentSmoothedTickTime` | 每刻记录 MSPT，供降频判断与 `/smoothcraft status` 展示 | — |
| 管理命令 | Fabric Command API | `/smoothcraft status` / `/smoothcraft reload` | 需要 GM 权限 |

降频的三道保险，任何一条不满足就完全不碰：

1. **服务器不忙就不干预**：MSPT 低于阈值（默认 45ms，约等于 TPS 22）时，一个实体都不跳过。
2. **只碰白名单类型**：默认只有 `minecraft:item` 和 `minecraft:experience_orb`。
3. **玩家附近不碰**：32 格内有任何玩家，这个实体就正常 tick。

所以最坏情况也只是「远处掉落物动得慢半拍」，不影响拾取、消失计时和玩法逻辑。

几个设计上的取舍，说清楚更放心：

- **只影响「画不画 / 算不算这一帧」，不改游戏规则**。渲染剔除不碰实体逻辑，服务端降频不碰近处实体。
- **掉落物单独一档阈值**。刷怪塔、农场附近能堆上千个掉落物/经验球，每个都要走一遍渲染管线，但视觉上最不重要——所以压到 32 格，这是本模组收益最大的单项。
- **玩家永远渲染**（`keepPlayersAlways`），PVP 时不会看不到远处对手。
- **帧率守护是渐变的**，收缩比例有下限（默认 0.4），远处的生物是「逐渐淡出」而不是突然消失，也不会因为一两帧抖动来回跳。
- **粒子预算跟着自适应联动**：掉帧时预算等比缩小，「少粒子 → 少开销 → 回血」形成正反馈，比只砍距离见效更快。
- **客户端剔除走的是官方本来就允许的 `null` 路径**：`LevelRenderer` 拿到 `null` 会跳过方块实体，`createParticle` 本身也判空，不是硬改渲染流程。

## 三、设置界面与快捷键

按 **`F6`** 打开图形设置界面，所有功能都能在里面开关和调参，不用再手改 JSON。

分三页，避免任何 GUI 缩放下把控件挤出去：

| 页 | 内容 |
| --- | --- |
| 渲染 | 实体剔除 + 分类距离、方块实体剔除、名牌距离、阴影剔除 |
| 粒子与画面 | 粒子预算、预算联动、粒子距离过滤、云层、星空 |
| 守护与服务端 | 帧率守护、目标帧率、收缩下限、性能面板、服务端降频与阈值 |

改动**即时生效**（能立刻看到帧率变化），点「保存并关闭」写入文件；点「取消」则丢弃本次改动、恢复文件里的值。直接按 Esc 关闭等同于保存，避免「看着改了其实没存」。

| 键 | 作用 |
| --- | --- |
| `F6` | 打开设置界面 |
| `F8` | 开关性能面板 |
| `F9` | 开关整个模组（排查兼容性时一键静音所有优化） |
| `F7` | 热重载配置文件（手工改 JSON 之后用） |

按键可以在「选项 → 控制 → 杂项」里重新绑定。

性能面板长这样：

```
SmoothCraft  143 FPS
剔除  实体 12  方块实体 5  阴影 8
粒子  丢弃 87  距离过滤 126 / 秒
自适应 100%   实体距离 96 格
```

最后一行在掉帧状态会变成橙色，说明帧率守护正在收缩距离。

## 四、服务器命令

```
/smoothcraft status
SmoothCraft | TPS 19.8 | MSPT 12.4ms | 近1秒降频 0 个实体 | 累计 0 | 阈值 45ms | 已启用

/smoothcraft reload
SmoothCraft 配置已重新加载
```

`status` 里三个数最有用：TPS 看当前健康度，MSPT 看单刻耗时，**近1秒降频数**非零说明服务器确实吃紧了、模组正在帮忙。

## 五、配置

客户端在 `.minecraft/config/smoothcraft.json`，服务端在服务器根目录 `config/smoothcraft.json`，结构一致（各读各的）。

```json
{
  "enabled": true,
  "entityCulling": {
    "enabled": true,
    "maxDistance": 96.0,
    "mobsDistance": 80.0,
    "itemsDistance": 32.0,
    "playersDistance": 128.0,
    "cullShadows": true,
    "shadowDistance": 48.0,
    "keepPlayersAlways": true
  },
  "blockEntityCulling": { "enabled": true, "maxDistance": 64.0 },
  "particleLimiting": { "enabled": true, "maxPerTick": 400, "followAdaptive": true },
  "particleFilter": {
    "enabled": true,
    "maxDistance": 24.0,
    "types": [
      "minecraft:flame", "minecraft:small_flame", "minecraft:soul_fire_flame",
      "minecraft:smoke", "minecraft:large_smoke",
      "minecraft:campfire_cosy_smoke", "minecraft:campfire_signal_smoke",
      "minecraft:lava", "minecraft:dripping_lava", "minecraft:falling_lava",
      "minecraft:spore_blossom_air", "minecraft:crimson_spore", "minecraft:warped_spore"
    ]
  },
  "serverThrottle": {
    "enabled": true,
    "maxDistance": 32.0,
    "msptThreshold": 45.0,
    "skipRatio": 2,
    "entityTypes": ["minecraft:item", "minecraft:experience_orb"]
  },
  "nameTagCulling": { "enabled": true, "maxDistance": 32.0 },
  "rendering": { "disableClouds": false, "skipStars": false },
  "adaptive": { "enabled": true, "targetFps": 60, "minScale": 0.4 },
  "hud": { "enabled": true, "x": 4, "y": 4, "background": true }
}
```

- `particleFilter.types` 想加自己讨厌的粒子，填注册表 ID 即可（`minecraft:` 前缀可省略）。
- 服务端改完用 `/smoothcraft reload`，客户端按 F7，都不用重启。
- 老版本配置缺字段会自动补默认值。
- 排查兼容性时把 `"enabled": false` 一关（客户端也可按 F9），本模组全部逻辑立刻失效。

## 六、项目结构

```
smoothcraft/
├── build.gradle                 Loom 配置（无 mappings 段，26.x 不需要）
├── gradle.properties            版本号集中在这里
├── gradlew / gradlew.bat        wrapper（指向腾讯云镜像）
├── CHANGELOG.md
├── LICENSE
└── src/main/
    ├── java/com/smoothcraft/
    │   ├── SmoothCraft.java            通用入口：TPS 采样 + 注册命令
    │   ├── client/
    │   │   ├── SmoothCraftClient.java  客户端入口
    │   │   ├── PerfGovernor.java       帧率守护 / 自适应调度
    │   │   ├── PerfHud.java            性能面板
    │   │   ├── SmoothCraftKeys.java    F6~F9 快捷键
    │   │   ├── Stats.java              每秒统计
    │   │   └── gui/
    │   │       └── SmoothCraftConfigScreen.java  图形设置界面（三页）
    │   ├── config/
    │   │   ├── SmoothCraftConfig.java  JSON 配置（Gson）
    │   │   └── ResolvedConfig.java     字符串 ID → 注册表对象（带缓存）
    │   ├── mixin/                      9 个注入点，见上文表格
    │   └── server/
    │       ├── ServerPerf.java         MSPT 采样与降频判断
    │       └── SmoothCraftCommand.java /smoothcraft 命令
    └── resources/
        ├── fabric.mod.json
        ├── smoothcraft.mixins.json     client 8 个 + server 1 个
        └── assets/smoothcraft/         图标 + 中英文语言文件
```

## 七、构建

需要 **JDK 25**（26.x 的最低要求），Gradle 由 wrapper 自动处理。

```bash
# Windows
gradlew.bat build

# macOS / Linux
./gradlew build
```

产物：`build/libs/smoothcraft-1.1.0.jar`。

> wrapper 默认指向腾讯云 Gradle 镜像（国内直连 `services.gradle.org` 经常卡死）。  
> 在海外的话把 `gradle/wrapper/gradle-wrapper.properties` 里的 `distributionUrl` 换回官方地址即可。

## 八、在开发环境里跑

```bash
gradlew.bat runClient   # 客户端
gradlew.bat runServer   # 专用服务器（首次需要 eula=true）
```

## 九、26.x 开发要留意的四个变化

这几个坑踩过就知道有多费时间，先写在这：

1. **Minecraft 26.x 客户端 jar 不再混淆**，`net/minecraft/client/Minecraft.class` 就是原名打包。  
   所以不需要 refmap，也不需要 `mappings` 配置（官方模板里确实没有），Mixin 写原名就能解析。
2. **Fabric 从 26.x 起不再发布 yarn 映射**，社区统一用 Mojang 官方命名。  
   网上大量 1.20/1.21 教程里的 yarn 名字（如 `WorldRenderer`）在这里是 `LevelRenderer`，`ResourceLocation` 是 `Identifier`。照抄会一路编译报错。
3. **HUD 改成了「提取渲染状态」模式**。老教程的 `HudRenderCallback` + `GuiGraphics.drawString` 已经不适用，  
   现在是 `HudElementRegistry` + `HudElement#extractRenderState(GuiGraphicsExtractor, DeltaTracker)`，  
   画文字要调 `g.text(font, str, x, y, color)`。同理，按键 API 也搬到了 `KeyMappingHelper`，分类从字符串变成了 `KeyMapping.Category` 常量。
4. **权限系统重做了**。`CommandSourceStack#hasPermission(int)` 没有了，改成 `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`  
   配合 `PermissionSet` / `PermissionCheck`，照老写法编译不过。

核对类名的土办法（很好用）：

```bash
javap -p -classpath <minecraft-merged.jar> net.minecraft.client.renderer.entity.EntityRenderDispatcher
```

jar 在 `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.1.2/` 下面。

## 十、后续计划

- 客户端 HUD 里显示服务端 TPS（走自定义 payload 同步）
- 界面里直接预览剔除效果（高亮被剔除的实体）
- 掉落物数量过多时的合并渲染
- 生物 AI 节流 / 刷怪笼限流（风险更高，需要单独开关）

## 十一、许可

本项目采用 **[MIT 许可证](LICENSE)** —— 可以自由使用、修改、分发、商用，保留版权声明即可。

- 源码仓库：<https://github.com/tasni78/smoothcraft>
- 问题反馈：<https://github.com/tasni78/smoothcraft/issues>
- 修改记录：[CHANGELOG.md](CHANGELOG.md)
