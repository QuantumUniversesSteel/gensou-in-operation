# 飞行工程实现

本文维护灵梦式测试飞行资格与状态延续的实际结构、接入方式和技术理由。工具链与开发命令以 [工程概览](README.md) 为准，玩家规则及实现边界见 [飞行设计](../design/flight.md)，计划与验证证据从 [003 任务](../../work/003-flight-prototype/TASK.md) 查看。

## 代码职责

| 类 | 当前职责 |
| --- | --- |
| [item/FlightTestSwitchItem](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/item/FlightTestSwitchItem.java) | 在逻辑服务端检查使用条件、调用资格开关并发送本地化反馈，返回交互结果 |
| [flight/ReimuTestFlight](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/flight/ReimuTestFlight.java) | 管理自身资格来源、保存状态的恢复、旁观前状态和同步 |
| [flight/ReimuTestFlightState](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/flight/ReimuTestFlightState.java)、[flight/FlightAttachments](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/flight/FlightAttachments.java) | 不可变的服务端状态及其序列化附件注册 |
| [flight/ReimuTestFlightEvents](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/flight/ReimuTestFlightEvents.java) | 连接玩家数据加载、登录、维度变化、玩家重建和死亡清理 |
| [mixin/ServerPlayerGameModeMixin](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/mixin/ServerPlayerGameModeMixin.java)、[mixin/ServerPlayerMixin](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/mixin/ServerPlayerMixin.java) | 在成功的服务端模式切换中维护旁观记录，并将模式与飞行更新一起发送 |

公共入口 `GensouInOperation` 通过 `DeferredRegister.Items#registerItem` 创建测试开关，并注册飞行附件。测试开关的模型引用原版纸纹理作为占位，未将原版图片复制到仓库。

## 资格来源与持久化

测试飞行通过 `NeoForgeMod.CREATIVE_FLIGHT` 属性上的临时修饰符提供资格，来源 ID 为 `gensou_in_operation:reimu_test_flight`。授予前检查该来源是否已存在，撤销时只移除同一 ID；不修改属性基础值或清空其他修饰符，也不直接改写 `mayfly` 或强制令玩家起飞。

资格与启停仍接入原版 / NeoForge，没有自定义飞行网络协议或操纵代码。撤销自身来源后，只有综合 `mayFly()` 已为假且玩家仍在飞行时，才关闭 `flying` 并发送原版能力包；创造、旁观和其他有效来源继续由各自机制决定。

服务端玩家附件 `gensou_in_operation:reimu_test_flight` 使用 `Codec` 保存 `enabled` 及可选的 `flying_before_spectator`。普通模式的实际飞行位复用原版 `Abilities.flying` 存档，不另外逐 tick 维护副本。临时属性修饰符仍不写入属性存档，由附件重建；缺少附件的旧存档默认没有本模组资格，不根据创造模式或外部属性推断开启。附件不启用 `copyOnDeath`，只在服务端使用。

## 生命周期接入

`LoadFromFile` 在模式初始化和首次能力包之前恢复资格，同时暂存原飞行位，覆盖服务器强制登录模式的情况；登录结束后，仅旁观模式继续保留这份记忆。

常规维度转换保留原玩家；非死亡 Clone 在 NeoForge 复制附件之后重建来源和飞行位，再在 Respawn 包之后同步。玩家 tick、退出及加载检查实际死亡，死亡 Clone 再作清理；末地字幕期间被移除但生命值仍为正的玩家不视为死亡。没有 UUID 会话表，也不每 tick 强制起飞。

## 模式切换与同步

公开模式事件发生在变更之前且可以取消或改写目标，因此用两个小范围服务端 Mixin 包住实际模式赋值及后续发包点。成功赋值后记录或恢复旁观前状态；取消和无变化的请求不触发。

拥有本模组来源时，将模式、完整 `CREATIVE_FLIGHT` 属性及最终能力组成一个原版 `ClientboundBundlePacket`，避免客户端在旧旁观模式下短暂处理停飞并回传错误状态；保留原版即时隐身更新。其他玩家保留原发包路径。配置见 [Mixin JSON](../../src/main/resources/gensou_in_operation.mixins.json) 及 [元数据模板](../../src/main/templates/META-INF/neoforge.mods.toml)；使用 NeoForge 已携带的 MixinExtras，没有增加玩法前置。

登录、维度转换及非死亡重建也通过原版 bundle 按“属性→能力”补发恢复状态；发送完整属性实例以保留外部修饰符。后续改键的源码评估与候选路线见 [003 输入评估](../../work/003-flight-prototype/RESULT.md#2026-11-03复测反馈与状态延续规则修订)，该路线尚未接入当前实现。

## 测试实现与证据入口

[飞行生命周期测试](../../src/gameTest/java/dev/quantumuniversessteel/gensou_in_operation/gametest/FlightLifecycleGameTests.java) 位于独立 GameTest 源集，使用真实 `ServerPlayer`、保存加载及生命周期入口，连接使用内存通道。测试连接接入服务器 tick，模拟传送与区块确认和静止移动上报，并检查实际发出的属性、能力包；测试成功或失败都会清理玩家、临时监听器及连接。`empty.nbt` 是本项目生成的 3×3×3 空测试结构。

这些检查覆盖服务端状态和发包，不模拟真实客户端接收和应用。自动化结果、独立审查与真人反馈分别记录在 [003 结果](../../work/003-flight-prototype/RESULT.md)；已执行阶段的操作资料保留在 [复测清单](../../work/003-flight-prototype/MANUAL_TEST.md)。运行入口见工程概览，后续验证范围按 [协作规则](../../AGENTS.md#模组工程与验证) 选择。
