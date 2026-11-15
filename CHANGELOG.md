# SmoothCraft 更新记录

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.0.0] - 2026-11-15

首个正式版。面向 Minecraft **26.1.2** 与 Fabric **0.20.1+**，Java 25。

### 客户端

- **实体渲染剔除**（`EntityRenderDispatcher#shouldRender`）：按类别分档 —— 生物 80 格、掉落物/经验球 32 格、玩家 128 格、其他 96 格
- **方块实体剔除**（`BlockEntityRenderDispatcher#tryExtractRenderState`）：64 格外返回 `null` 跳过
- **粒子预算**（`ParticleEngine#add`）：每刻上限 400，可跟随帧率守护等比缩小
- **粒子距离过滤**（`ParticleEngine#makeParticle`）：火把火焰、各类烟、营火烟、岩浆滴落等 13 种环境粒子，24 格外不再生成
- **名牌距离限制**（`EntityRenderer#shouldShowName`）：32 格外不显示名字
- **阴影剔除**（`EntityRenderer#getShadowRadius`）：48 格外不绘制落地阴影
- **云层 / 星空跳过**（`CloudRenderer#render`、`SkyRenderer#renderStars`）：可选，默认关闭
- **帧率守护**：每秒采样 FPS，掉帧时收缩剔除距离与粒子预算，回血后缓慢恢复（下限 40%）
- **性能面板**：FPS / 各类剔除数 / 丢弃粒子数 / 自适应比例，`F8` 开关
- **快捷键**：`F7` 热重载配置、`F8` 面板、`F9` 总开关

### 服务端

- **远处实体降频**（`ServerLevel#tickNonPassenger`）：MSPT 超过 45ms 时，32 格外的掉落物与经验球隔一刻 tick 一次
- **TPS 监控**：`MinecraftServer#getCurrentSmoothedTickTime()` 每刻采样
- **管理命令**：`/smoothcraft status`、`/smoothcraft reload`（需 GM 权限）

### 工程

- 配置热重载，老配置缺字段自动补默认值
- 中英双语语言文件
- Gradle wrapper 指向腾讯云镜像，国内构建不必等官方源
