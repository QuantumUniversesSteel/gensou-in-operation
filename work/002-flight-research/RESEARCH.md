# 飞行参考实现调研

初次记录：2026-10-27；2026-10-28 补充默认键位、骑乘退出与碰撞依据。本文保存源码阅读的版本、事实与技术判断；当前玩法约定和操纵方案统一维护在 [飞行设计](../../docs/design/flight.md)，本任务结果见 [RESULT](RESULT.md)。

证据来自作者公开仓库与本项目已有依赖源码，没有安装试玩这些参考模组，也没有执行本项目构建或飞行测试。以下“源码可见”不表示已经验证手感、多人同步或本项目兼容性。

## 参考版本与固定源码

分支名仅帮助定位。提交和源码链接固定到本次读取的快照，后续分支更新不能自动替代这些证据；模组版本是该快照构建配置中的值，不推断它是最新发布版。

| 参考 | 本次版本与分支 | 固定源码与版本依据 |
| --- | --- | --- |
| Angel Ring | Minecraft 1.21.1；3.0.0+beta.3；`dev/3.0/1.21.1`；提交 `a8489e5` | [版本配置](https://github.com/DenisMasterHerobrine/AngelRing/blob/a8489e52999eed5d77a6f344afa350aeaaee4636/gradle.properties)、[AngelRingItem](https://github.com/DenisMasterHerobrine/AngelRing/blob/a8489e52999eed5d77a6f344afa350aeaaee4636/neoforge/src/main/java/dev/denismasterherobrine/angelring/neoforge/item/AngelRingItem.java) |
| Flight API（Angel Ring 使用的库） | Minecraft 1.21.1；1.1.1；`master`；提交 `17977bf` | [版本配置](https://github.com/DenisMasterHerobrine/FlightAPI/blob/17977bf622a4be4606c17f736a391f0d8b27de52/gradle.properties)、[FlightManager](https://github.com/DenisMasterHerobrine/FlightAPI/blob/17977bf622a4be4606c17f736a391f0d8b27de52/common/src/main/java/dev/denismasterherobrine/flightapi/manager/FlightManager.java)、[MixinPlayerAbilities](https://github.com/DenisMasterHerobrine/FlightAPI/blob/17977bf622a4be4606c17f736a391f0d8b27de52/common/src/main/java/dev/denismasterherobrine/flightapi/mixin/MixinPlayerAbilities.java) |
| Mekanism | Minecraft 1.21.1；10.7.19；`1.21.x`；提交 `bcd7a8b` | [版本配置](https://github.com/mekanism/Mekanism/blob/bcd7a8bf594cff9614eb12238fe3776f19da24d9/gradle.properties)、[重力调节模块](https://github.com/mekanism/Mekanism/blob/bcd7a8bf594cff9614eb12238fe3776f19da24d9/src/main/java/mekanism/common/content/gear/mekasuit/ModuleGravitationalModulatingUnit.java) |
| Hexerei | Minecraft 1.20.1；0.4.2.3；仓库 `Hexerei-1.19` 的 `1.20.1` 分支；提交 `d46fa90` | [版本配置](https://github.com/JoeFoxe/Hexerei-1.19/blob/d46fa9004eade34991b844f1e1e8d8771ed9fcf7/gradle.properties)、[BroomEntity](https://github.com/JoeFoxe/Hexerei-1.19/blob/d46fa9004eade34991b844f1e1e8d8771ed9fcf7/src/main/java/net/joefoxe/hexerei/client/renderer/entity/custom/BroomEntity.java)、[ModKeyBindings](https://github.com/JoeFoxe/Hexerei-1.19/blob/d46fa9004eade34991b844f1e1e8d8771ed9fcf7/src/main/java/net/joefoxe/hexerei/config/ModKeyBindings.java) |
| EvilCraft | Minecraft 1.21.1；1.2.99；`master-1.21-lts`；提交 `2b67228` | [版本配置](https://github.com/CyclopsMC/EvilCraft/blob/2b67228d3dcd1a8b4c06ca60ca06fdb5eebf9e18/gradle.properties)、[EntityBroom](https://github.com/CyclopsMC/EvilCraft/blob/2b67228d3dcd1a8b4c06ca60ca06fdb5eebf9e18/src/main/java/org/cyclops/evilcraft/entity/item/EntityBroom.java)、[ItemBroom](https://github.com/CyclopsMC/EvilCraft/blob/2b67228d3dcd1a8b4c06ca60ca06fdb5eebf9e18/src/main/java/org/cyclops/evilcraft/item/ItemBroom.java) |
| NeoForge 与本项目依赖 | 本地源码构件 21.1.264；同时核对官方 `1.21.1` 分支提交 `624c929` 中的相关声明 | [21.1.264 sources 构件](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.264/neoforge-21.1.264-sources.jar)、[NeoForgeMod](https://github.com/NeoForged/NeoForge/blob/624c929f49920c250166e2a8df7af87f16d220ef/src/main/java/net/neoforged/neoforge/common/NeoForgeMod.java)、[IPlayerExtension](https://github.com/NeoForged/NeoForge/blob/624c929f49920c250166e2a8df7af87f16d220ef/src/main/java/net/neoforged/neoforge/common/extensions/IPlayerExtension.java) |

NeoForge 的分支提交是补充引用，不声称它就是 21.1.264 的发布提交。EvilCraft 的普通 `master-1.21` 分支在调研时已指向较新的游戏版本，因此这里明确使用 1.21.1 的长期维护分支。Hexerei 的接口属于旧版本，只能在适配后用于本项目。

## 源码可见的实现逻辑

### 创造式飞行

Angel Ring 的 `onEquip` 申请飞行；`curioTick` 处理经验或耐久消耗及失效；`onUnequip` 会检查是否仍装备另一枚戒指，再决定是否释放。它展示了能力来源、持续条件和退出的分工，本身未在物品类中重写玩家位移。对应的 Flight API 使用玩家对应的控制者与等待队列，并更新飞行权限和当前飞行状态。

Flight API 的这一快照会在启用时设置当前飞行状态，因此不能把它直接等同于“只授予权限，等待玩家主动起飞”。本次读取的 `MixinPlayerAbilities` 拦截范围是 `readNbt` 中的字段写入；不能据库的概述推断它能阻止任意模组在任意位置改写飞行状态。是否引入此库尚未决定。

Mekanism 的 `adjustAttributes` 为有足够能源的对应装备添加 `NeoForgeMod.CREATIVE_FLIGHT` 修饰；`tickServer` 在实际飞行时处理消耗，并为加速单独计费。额外移动在客户端和服务端分别处理。可借鉴的是“允许飞行”“正在飞行”“额外操纵”和“成本”分开处理，不需要因此采用它的装备或能源体系。

本地 NeoForge 21.1.264 的 `NeoForgeMod` 声明了可同步的 `CREATIVE_FLIGHT` 布尔属性；`IPlayerExtension#mayFly` 综合原有能力字段与该属性，并明确不建议直接设置 `Abilities#mayfly`。候选做法是按来源添加和撤销本模组自己的修饰，让采用同一属性机制的有效来源共存；这不构成对所有外部模组兼容的保证。

### Hexerei：按键转向的扫帚

`BroomEntity#controlBoat` 使用左右输入改变转向，前后输入改变推进；跳跃输入上升，`broomDescend` 下降。`ModKeyBindings` 中下降动作默认绑定左 Ctrl。这个默认值仅是参考模组的选择，不是本项目的决定。

`updateMotion` 对水平运动和转向施加衰减，对垂直运动另作衰减与限速；`positionRider` 和 `applyYawToEntity` 还会调整乘客朝向。因而它提供的是带惯性和乘客朝向处理的载具控制，不应仅凭“按键转向”宣称已经支持完全自由的镜头。

`tick` 中可见本地控制、远端插值以及状态和旋转同步路径；缺少有效刷毛时会改变重力与操纵条件。这里确认的是代码分支存在，未验证其联网行为或把刷毛机制列入本项目范围。

2026-10-28 补充：同一快照的 `BroomEntity#canBeCollidedWith` 返回真；无人骑乘时，`tick` 根据 `floatMode` 设置无重力，缺少有效刷毛时会清除该模式。`setFloatMode` 与保存、同步路径表明这是一个可选状态，不表示任何扫帚在任何条件下都会悬停。这些机制可供研究空中停靠与实体承托，但本次未试玩验证下车后站立或移动平台的稳定性。该类的 `checkFallDamage` 没有有效处理代码，不能将扫帚自身的坠落处理等同于乘客获授下车保护。

### EvilCraft：视角转向的扫帚

`EntityBroom#updateMountedServer` 读取骑乘者的水平和俯仰朝向，对转向与速度作平滑，再计算三维推进方向。因此它的视角方案包含俯仰控制爬升与下降，不能只描述为鼠标控制左右方向。`updateMountedClient` 为其他客户端处理位置和朝向插值。

`ItemBroom#use` 可生成并骑乘扫帚实体；`EntityBroom#onDismount` 包含物品回收、背包与存活条件，跨维度时另有标记。扫帚的物品、实体和玩家状态需要共同处理。其源码直接开关玩家 `mayfly` 字段，这部分不作为本项目的候选权限范式；权限处理仍以当前 NeoForge 机制为依据。

2026-10-28 补充：同一快照在非维度迁移、玩家存活且背包有空间的普通生存下车流程中，把扫帚物品放回背包并移除实体；不能以此作为“下车后保留脚下承托平台”的直接范例。满包、死亡和维度迁移等条件不满足时，不应仅凭注释推断扫帚总会正常回收或掉落。该退出方法也没有显式施加缓降效果。

### 本地 Minecraft 1.21.1 输入与坠落行为

另读取了本项目现有 NeoForm 生成源码构件中的 `net.minecraft.client.player.LocalPlayer` 与 `net.minecraft.world.entity.player.Player`。定位依据是项目依赖版本和类、方法名，不依赖维护者本机的绝对缓存路径；未把生成源码复制进仓库。

- `LocalPlayer#aiStep` 的创造飞行升降分支读取跳跃与 `input.shiftKeyDown`，潜行输入会引发下降；`isShiftKeyDown` 也读取这项输入。因此阻止下降与保留工具潜行交互必须一起处理，具体工具仍需验证。
- `LocalPlayer#rideTick` 及骑乘输入涉及下车状态；采用坐骑方案时，Shift 低速不能未经处理就与下车重叠。
- `Player#causeFallDamage` 在 `mayFly()` 为真时跳过正常坠落伤害。因此候选能力的保护边界需要同时考虑“具备飞行资格但未起飞”的状态。

2026-10-28 补充读取同一构件的 `net.minecraft.client.Options`、`net.minecraft.client.Minecraft#handleKeybinds` 与 `Player#getFlyingSpeed`：

- 原版默认跳跃为空格、潜行为左 Shift、疾跑为左 Ctrl；默认按键表中未绑定 V。
- `keySaveHotbarActivator` 默认 C；创造模式下按住它并按数字栏位键会保存快捷栏，不能把 C 视为完全空闲的键。
- 玩家自身创造式飞行的水平速度会受疾跑状态影响；因此保留或复用 Ctrl 时，需同时考虑原版疾跑行为。

以上是默认绑定和源码分支的事实，不代表本机实际改键、外部模组键位或组合操作的手感已经验证。

同日为下车方案补充核对以下 Minecraft 1.21.1 源码位置：

- `Entity#getDismountLocationForPassenger` 的默认返回值位于载具碰撞箱顶部；`LivingEntity#dismountVehicle` 在适用条件下采用这一位置。它只说明下车位置，不能自动保证承托、头顶空间或退出后的移动安全。
- `Entity#collide` 会查询实体碰撞；`EntityGetter#getEntityCollisions` 按碰撞条件把实体包围盒作为碰撞形状。普通实体默认不可被碰撞，骑乘时排除同载具关系；真正退出骑乘后的实体碰撞可以成为承托的基础，但没有由此自动建立移动平台跟随关系。
- `Boat#getDismountLocationForPassenger` 会尝试周围落点及可容纳玩家的姿态；`DismountHelper` 提供落脚高度、方块碰撞、世界边界及部分危险位置的检查。可参考这些机制区分“附近能站稳下车”与“没有落脚处”，不能把单个着地标志或已有帮助方法视为完整的安全保证。
- `LivingEntity#travel` 与 `aiStep` 可见缓降效果影响下落重力并重置坠落距离。这支持把玩家缓降作为独立保护候选；固定时长的缓降与保护到首次落地不是同一承诺，单次清空坠落距离也不能保护其后任意长的下落。

上述读取没有复制生成源码或实现新功能。退出方向、承托候选的优先级及高级功能的暂缓安排以飞行设计中的最新决定为准；这里保留各方案的源码依据，不将它们作为当前并行开发要求。承托稳定性仍需独立服务端与联机验证。

## 对本项目的候选技术判断

灵梦式飞行可先以玩家创造式飞行为基础，扫帚可先以独立骑乘实体为基础。前者重点验证建造中的低速与工具交互，后者验证移动响应、升降与制动。正式载体、额外前置、能源和成长机制均未由这次调研决定；后续操纵选择以飞行设计为准，不能将参考模组的操作自动带入本项目。

操纵方案的优劣属于基于代码和使用场景的推断，不能代替试玩；当前候选及理由只在 [飞行设计](../../docs/design/flight.md) 维护。后续原型需要单独约定实验环境，客户端手感和服务端同步也应分别验证。

## 复查入口与限制

复查时先从上表的固定版本配置确认游戏和模组版本，再定位对应输入、移动、装备或下车方法。NeoForge 相关判断还可对照 21.1.264 的 sources 构件；引用其他版本时重新标注差异。

本任务只保存研究摘要与引用，没有复制参考模组代码、引入新依赖或证明上述方案已在本项目运行。后续实际选用代码或接口时，应核对所选版本的 API 与许可，并重新建立项目内验证证据。
