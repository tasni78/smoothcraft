# SmoothCraft Bug 审查报告

- **审查日期**：2026-11-22
- **审查范围**：`src/main/java` 全部 30 个 Java 文件、`smoothcraft.mixins.json`、语言文件
- **审查方式**：三路并行静态审查（配置/入口、统计/调度、Mixin/GUI）+ 对调用链与 26.1.2 字节码的人工逐条复核
- **说明**：本报告只列**能说清因果、可复现**的 bug。历史已修复的两个问题（`configVersion` 迁移失效、`Stats.flush()` 被开关挡住）经核实当前代码正确，**不再重复列入**。当前工作区有未提交改动（1.0.6 的统计修复 + 闪白修复），以下行号均以当前工作区代码为准。

---

## 一览

| 编号 | 严重程度 | 一句话 | 主要位置 |
|---|---|---|---|
| ~~BUG-01~~ | ✅ 已修复 | 配置重载失败时反而用默认值覆盖磁盘文件，配置不可逆丢失 | `SmoothCraftConfig.java:264-290` |
| BUG-02 | 中等 | 切换世界不重置帧率守护，收缩系数与统计跨世界残留 | `PerfGovernor.java:101-108`、`FrameResetMixin.java:32-40` |
| BUG-03 | 中等 | 视距爬坡功能关闭后状态不清空，再次开启瞬间跳满、HUD 残留 | `RenderDistanceRamp.java:48-52,88-93` |
| ~~BUG-04~~ | ✅ 已修复 | 设置界面内按 F7 会替换配置实例，界面上未保存的修改静默丢失 | `SmoothCraftKeys.java:69-71`、`SmoothCraftConfigScreen.java:35-38` |
| BUG-05 | 轻微 | 注册表解析缓存无同步，单人游戏下客户端/服务端线程竞态 | `ResolvedConfig.java:21-26,41-78` |
| BUG-06 | 轻微 | HUD 显示的"实体距离"未套用实际剔除下限，数值偏小 | `PerfHud.java:111` |
| BUG-07 | 轻微 | 面板编辑器的贴边参考线在松开鼠标后残留，重置后画到错误一侧 | `SmoothCraftHudEditorScreen.java:76-83,133-140` |

---

## BUG-01｜配置重载失败时用默认值覆盖磁盘文件（中等）

**位置**：`config/SmoothCraftConfig.java:264-290`（`load()` 与 `reload()`）

**现象**：`reload()` 的注释写的是"重新读盘，失败则保留当前配置"，但它直接调用 `load()`。而 `load()` 在读盘异常或读到 `null` 时，会执行 `loaded = new SmoothCraftConfig()`，随后**无条件** `instance = loaded; save();`。于是配置一解析失败，内存配置就被整包换成默认值，并且立刻把默认值写回磁盘，覆盖原文件。

**复现步骤**：
1. 用 F6 设置界面配好一批自定义参数并保存；
2. 手工编辑 `config/smoothcraft.json`，制造一个语法错误（如漏逗号）；
3. 进游戏按 F7（或服务器执行 `/smoothcraft reload`）；
4. 日志打印"读取失败，将使用默认值"，所有设置变回默认，磁盘文件被默认配置覆盖——**用户原有配置再也找不回**。

**根因**：`load()` 把"启动时的首次加载"和"运行中的重载"两种语义混在一起。启动失败用默认值是合理的；重载失败应该用旧值，但这里共用了一条路径。

**修复建议**：拆出 `private static SmoothCraftConfig tryReadFromDisk()`，失败返回 `null`，不碰 `instance`、不写盘。`reload()` 仅在读盘成功时 `fillDefaults()` 后替换 `instance`，失败则记警告并直接 `return`。`load()` 保留启动默认创建逻辑。注意 GUI"取消"按钮（`SmoothCraftConfigScreen.java:229`）也调用 `load()`，改动时要保证它仍能正确从磁盘还原。

---

## BUG-02｜切换世界不重置帧率守护，状态跨世界残留（中等）

**位置**：`client/PerfGovernor.java:101-108`（`reset()` 定义）＋ `mixin/FrameResetMixin.java:32-40`（调用点缺失）

**现象**：`reset()` 注释明确写着"重置所有状态（切换世界时调用）"，但全项目**唯一调用点**是 `SmoothCraftKeys.java:64`——仅当玩家按 F9 关闭模组时触发。真正处理世界切换的 `FrameResetMixin.smoothcraft$onLevelSet` 只重置了 `RenderDistanceRamp` 和 `WorldLoadTimer`，**漏掉了 `PerfGovernor.reset()`**。

**复现步骤**：
1. 在世界 A 跑图掉帧，`scale` 收缩到 0.4；
2. 切换到世界 B（或退出重进）；
3. 世界 B 明明流畅，但：
   - 剔除距离仍按 0.4 倍运行，且只在 `fps > target*1.1` 时每次 +0.02，约需 30 次采样（≈30 秒）才恢复满值；
   - 头 1 秒内 HUD 的剔除统计仍是世界 A 的残留数字。

**修复建议**：在 `FrameResetMixin.smoothcraft$onLevelSet` 的两条分支里都补上 `PerfGovernor.reset()`（`Stats` 会随之清零）。

---

## BUG-03｜视距爬坡关闭后状态不清空，再次开启瞬间跳满（中等）

**位置**：`client/RenderDistanceRamp.java:48-52`（`apply()` 早退分支）、`:88-93`（`status()`）

**现象**：`apply()` 在 `!cfg.enabled || !cfg.worldLoad.enabled || !cfg.worldLoad.rampEnabled` 时直接 `return configured`，**不重置** `startedAt` / `currentLimit` / `currentTarget`。而 `status()` 只依据 `ramping()`（`startedAt >= 0`）和这两个字段判断，完全不检查配置开关。

**复现步骤**：
1. 开启 `rampEnabled` 进入世界，爬坡进行中（HUD 显示"视距 X→Y"）；
2. 通过 F7 或设置界面关闭 `rampEnabled`；
3. 现象 A：`apply()` 不再更新，但残留字段让 `status()` 继续返回爬坡文本——一个已关闭的功能仍显示在 HUD 上；
4. 现象 B：再次开启 `rampEnabled`，`elapsed = now - startedAt` 用的是关闭前的旧时间戳，通常早已超阈值，`allowed >= configured` 立即成立，视距瞬间跳满——爬坡形同虚设。

**修复建议**：在 `apply()` 的开关关闭分支里清空状态（`startedAt=-1; pending=false; currentLimit=-1; currentTarget=-1;`）；或至少让 `status()` 开头也检查同一组开关并返回 `null`。

---

## BUG-04｜设置界面内按 F7 导致未保存修改静默丢失（中等）

**位置**：`client/SmoothCraftKeys.java:69-71`、`client/gui/SmoothCraftConfigScreen.java:35-38,222-225`

**现象**：`handleInput()` 在 `END_CLIENT_TICK` 里无条件调用，`reloadConfig.consumeClick()` 没有像 F6 那样检查 `mc.screen`，所以在设置界面打开时按 F7 仍会触发 `SmoothCraftConfig.reload()`。而设置界面构造时持有的是旧实例引用 `cfg = SmoothCraftConfig.get()`（记为 A），所有控件的 setter 闭包都写入 A；`reload()` 会从磁盘新建实例 B 并令 `instance = B`。此后：

- 界面上显示的是 A 的值（用户看到的仍是自己改过的样子）；
- 点"保存并关闭"调用 `SmoothCraftConfig.save()`，保存的是 `instance`（B，来自磁盘、不含 A 的改动）；
- 结果：reload 前后在界面上做的所有修改**全部丢失**，且用户以为已经保存。

**复现步骤**：F6 打开设置 → 改动几个滑块（不要点保存）→ 按 F7 → 点"保存并关闭" → 重开界面，改动没了。

**修复建议**：让 F7 在 `mc.screen != null` 时不触发热重载（与 F6 的守卫对称）；或让 GUI 在 reload 后重新绑定新实例（重建界面）。同理 F8/F9 在界面内也建议一并屏蔽，避免界面复选框与实际配置不同步。

---

## BUG-05｜注册表解析缓存无同步，单人游戏下双线程竞态（轻微）

**位置**：`config/ResolvedConfig.java:21-26,41-78`

**现象**：`cachedConfig / cachedParticleIds / cachedEntityIds / particleTypes / entityTypes` 都是普通非 `volatile` 静态字段，`refreshIfNeeded()` 完全无同步。但两个入口来自不同线程：`particleTypes()` ← `ParticleFilterMixin`（客户端渲染线程）；`entityTypes()` ← `ServerPerf`（单人游戏的集成服务端线程）。两线程可同时进入 `refreshIfNeeded()` 读写同一组字段。引用赋值是原子的，不会崩溃，但可能出现并发重复解析、或某线程短暂读到"缓存标记已更新、集合还是旧值"的不一致（例如热重载后某一帧仍用旧的粒子过滤集合）。

**复现步骤**：单人游戏低帧率且 MSPT 超过阈值时，客户端生成粒子与服务端降频实体同时触发解析，或此时按 F7，竞态窗口最大。影响很小、肉眼难复现。

**修复建议**：给 `particleTypes()` / `entityTypes()` 的缓存访问加 `synchronized`，或用同一把锁保护这几个字段；最低限度把两个集合字段声明为 `volatile`。

---

## BUG-06｜HUD"实体距离"未套用剔除下限，显示值偏小（轻微）

**位置**：`client/PerfHud.java:111`（对照 `client/PerfGovernor.java:41-43`）

**现象**：HUD 显示 `Math.round(cfg.entityCulling.maxDistance * scale)`，但实际剔除阈值走 `PerfGovernor.scale(threshold)`，后者带下限 `Math.max(8.0, baseDistance * scale)`。当 `maxDistance * scale < 8` 时，HUD 显示的数字低于实际生效距离，误导玩家。（附注：`PerfGovernor.effectiveEntityDistance()` 全项目无人调用，是死代码，不能作为依据。）

**复现步骤**：把 `entityCulling.maxDistance` 调到较低值（如 <40），且帧率守护把 `scale` 收缩到接近 0.2。

**修复建议**：HUD 改为 `Math.round(PerfGovernor.scale(cfg.entityCulling.maxDistance))`，直接复用剔除侧的公式，避免两套计算漂移。

---

## BUG-07｜面板编辑器贴边参考线残留（轻微）

**位置**：`client/gui/SmoothCraftHudEditorScreen.java:76-83`（绘制）、`:133-140`（`mouseReleased`）、`:54-58`（重置按钮）

**现象**：注释写的是"拖动时显示贴边参考线"，青色边框的绘制有 `if (this.dragging)` 保护（第 88 行），但贴边参考线（第 76-83 行）**没有** `dragging` 判断，只依赖 `snappedLeftRight` / `snappedTopBottom` 标志。这两个标志只在 `applyPosition()` 里更新，而后者仅在 `mouseDragged()` 调用；`mouseReleased()` 只把 `dragging` 置 false，**不清这两个标志**，"重置到左上角"按钮同样不清。

**复现步骤**：
1. 在编辑器里把面板拖到左/右边缘，参考线出现；松开左键 → 边框消失，但青色竖线仍留在屏幕边缘。
2. 贴边后点"重置到左上角" → 面板回到 (4,4)，但 `snappedLeftRight` 残留为 true，`lineX = Math.round(posX) <= 0 ? 0 : width - 1` 中 `posX=4 > 0`，参考线被错误画到**右边缘**（纵向同理画到下边缘），与面板实际位置矛盾。

**修复建议**：在 `mouseReleased()` 和重置按钮回调里把 `snappedLeftRight` / `snappedTopBottom` 复位为 false；或直接在第 76/80 行的绘制条件加上 `this.dragging &&`，让参考线严格只在拖动期间显示（更简洁）。

---

## Mixin / GUI 专项核对（本轮独立验证）

对 15 个 Mixin 和两个 GUI 界面做了逐文件核对，并用 26.1.2 的字节码 / 反编译验证了几处高风险落点：

- **Mixin 注入签名全部有效**：反编译 26.1.2 客户端 jar 校验了全部 15 个注入目标（`RenderSection.getRenderOrigin/isDirtyFromPlayer/rebuildSectionAsync`、`WaitingForPlayerChunk.isReady` 及 record 字段 `player/level`、`AbstractSignRenderer.extractRenderState`、`ItemClusterRenderState.getRenderedAmount` 静态方法、`ParticleEngine.makeParticle/add/tick`、`Options.getEffectiveRenderDistance` 等），签名全部匹配，不会因注入失败导致运行时崩溃。
- **注入顺序 / cancel 行为正确**：`EntityCullMixin` 的距离剔除（48-52）确实先于掉落物聚合（55-62）；所有 Mixin 的 `cfg.enabled` 与子开关判齐全（`ServerThrottleMixin` 自身未判，但 `ServerPerf.shouldThrottle:63-64` 内部已判，等效）。
- **设置界面页数与实际匹配**：`PAGES=6` 与 `case 0..4 + default` 共 6 页一致，翻页边界正确，全部滑块的 min/max/step/初值换算数学自洽。
- **Mixin 声明与文件一一对应**：`smoothcraft.mixins.json` 声明的 14 个 client + 1 个 server Mixin，与磁盘文件双向核对无缺失、无遗漏。
- **告示牌文字剔除是可逆的**（排除一个疑似 bug）：`SignTextCullMixin` 用共享 `EMPTY_TEXT` 顶替远处文字，一度怀疑"走近后文字不恢复"。字节码确认原版 `AbstractSignRenderer#extractRenderState` **每帧都调 `getFrontText()` / `getBackText()` 重写** `SignRenderState.frontText/backText`，所以空文本只影响当帧，走近后自动恢复。此路没问题。
- **HUD 编辑器鼠标坐标**：`MouseButtonEvent` 的 `x/y` 由 `MouseHandler` 传入的是已换算到 GUI 空间的坐标，与 `Screen` 的 `width/height` 同一坐标系，`insidePanel` / `applyPosition` 的换算自洽，未发现缩放错位。
- **掉落物聚合的调用点**：`shouldRender` 在 `EntityRenderDispatcher` 中经 `EntityRenderer.shouldRender` 判定，聚合发生在"通过可见性判断之后"，"先剔除后聚合"的顺序在代码里得到保证。

这一路**未发现新的确定性 bug**；经验球未排除准星实体、每帧/每秒计数混排的问题见下方"其他观察"。

---

## 其他观察（非确定 bug，供参考）

- **`effectiveEntityDistance()` 是死代码**：`PerfGovernor.java:35-38` 定义了该方法但无人调用，可删除或让 HUD 改用它（与 BUG-06 的修复合并考虑）。
- **经验球聚合未保护准星实体**：`EntityCullMixin` 对掉落物聚合检查了 `entity != mc.crosshairPickEntity`，对经验球聚合（第 64-70 行）没有同样保护。同格还有其他经验球在画，影响极小，但两处行为不一致，建议对齐。
- **HUD 量纲混排**：剔除/粒子显示为"每秒"，同格聚合/份数压制/节流显示的是"每帧瞬时值"，玩家可能误读。属显示语义问题，非逻辑错误。

---

## 已确认无问题的部分

- 历史两个 bug 已正确修复：`configVersion=0 → CURRENT=3` 迁移、`Stats.flush()` 位于开关判断之前。
- 快捷键注册（F6/F7/F8/F9）的映射与日志一致，除 BUG-04 外无重复注册或空指针问题。
- `SmoothCraft` / `SmoothCraftClient` 的命令、tick 事件、HUD 元素注册均只执行一次，初始化顺序无问题。
- `PerfGovernor` 的 `++ticks` 无 off-by-one，各 clamp 边界正确，无除零/溢出。
- `ChunkCompileBudget` 的玩家改动/近身豁免、"跳过即下一帧重试"语义正确。
- `ItemAggregator` 的哈希键在每帧清空的语义下无实质碰撞。
- `WorldLoadTimer` 与 `LoadingWaitMixin` 的毫秒单位一致，超时放行方向正确。
- `ServerPerf` 的 `1000.0/smoothed` 有 `<=0` 保护，`skipRatio` 有 `Math.max(2,...)`，`phase % ratio` 的模数 ≥2。

---

## 修复进度

- ✅ **BUG-01**（已修复，2026-11-22）：拆出 `tryReadFromDisk()`，`reload()` 读盘失败时保留当前配置、不写盘。启动 `load()` 仍用默认值兜底。已实测：故意写入非法 JSON 后重载，实例保留 / 值保留 / 磁盘未被覆盖三项全过，正常重载仍生效。
- ✅ **BUG-04**（已修复，2026-11-22）：F7 热重载加了 `mc.screen == null` 守卫，与 F6 对称，设置界面内按 F7 不再触发重载。
- ⬜ **BUG-02 / BUG-03**：影响切世界与爬坡的实际手感，待修。
- ⬜ **BUG-05 / BUG-06 / BUG-07**：轻微，待修（BUG-07 是纯视觉，改一行条件即可）。

> BUG-01 / BUG-04 的修复**尚未提交**（工作区改动）。其余 bug 仍只做了定位和分析，未改动代码。
