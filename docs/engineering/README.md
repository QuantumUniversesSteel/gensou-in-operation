# 工程概览

本文说明当前仓库的工程基线、代码结构和开发入口。项目介绍与整体阶段见 [根 README](../../README.md)。

首次参与开发时，先从本页确认工具链与运行方法，再按任务查阅 [玩法设计](../design/README.md) 和相关工程专题。协作与文档维护要求见 [AGENTS.md](../../AGENTS.md)；任务记录、验证证据、接续方式及 Git 流程从 [工作入口](../../work/README.md) 查看。

## 当前工程基线

当前工程基于 NeoForge 模板。下表是实际构建配置的阅读摘要；调整版本时，以配置为准并在同一变更中更新本节。

| 项目 | 当前配置 | 配置来源 |
| --- | --- | --- |
| Minecraft | `1.21.1` | [gradle.properties](../../gradle.properties) |
| NeoForge | `21.1.264` | [gradle.properties](../../gradle.properties) |
| Java 工具链 | `21` | [build.gradle](../../build.gradle) |
| ModDevGradle | `2.0.148` | [build.gradle](../../build.gradle) |
| Gradle Wrapper | `9.2.1` | [Wrapper 配置](../../gradle/wrapper/gradle-wrapper.properties) |
| Parchment 映射 | Minecraft `1.21.1`，映射 `2024.11.17` | [gradle.properties](../../gradle.properties) |

当前 MODID 为 `gensou_in_operation`，显示名为 `Gensou in Operation`；构建用 Group ID 为 `dev.quantumuniversessteel`，Java 基础包为 `dev.quantumuniversessteel.gensou_in_operation`。仓库与根目录名保持 `gensou-in-operation`。模组版本值仍沿用模板的 `1.0.0`，不表示已经正式发布。

工程命名在首次占位测试物品提交后统一调整，依据与验证见 [005 工程命名结果](../../work/005-project-identity/RESULT.md)。本次没有为旧 `examplemod:*` 注册标识提供存档映射，也不自动迁移旧配置；使用新的独立测试世界，保留旧测试存档供查看，不将旧物品消失视为新功能已验证。

2026-11-04 将作者包前缀与 Group ID 从 `io.github.quantumuniversessteel` 改为 `dev.quantumuniversessteel`，同步主源码、GameTest 源码和本页链接。该次调整没有更改 MODID、注册标识、资源命名空间或配置文件名，不增加游戏存档迁移；`flight` 等内部包层次保持不变。两次命名调整的检查分别记录在上述 005 结果中。

当前没有启用额外的玩法模组依赖；构建文件里的 JEI 等声明都是注释示例。

## 目录与文件职责

| 位置 | 在当前工程中的职责 |
| --- | --- |
| [src/main/java](../../src/main/java) | 模组 Java 源码，包括公共入口、客户端入口、自定义物品行为、测试飞行资格和模板配置 |
| [src/main/resources](../../src/main/resources) | 英文与中文语言文件、测试开关的占位模型和 Mixin 配置；使用 `gensou_in_operation` 资源命名空间 |
| [src/gameTest](../../src/gameTest) | 独立的服务端集成测试模组、测试代码与空结构；仅 GameTest 运行加载，不进入正式 JAR |
| [src/main/templates](../../src/main/templates) | 模组元数据模板，经构建时变量展开后加入资源 |
| `src/generated/resources/` | 已配置的数据生成输出与资源来源，当前尚无生成器实现 |
| [gradle.properties](../../gradle.properties) | 游戏、加载器、映射与模组元数据参数，以及 Gradle 运行选项 |
| [build.gradle](../../build.gradle) | 插件、Java 工具链、依赖、运行配置、资源生成与打包 |
| [settings.gradle](../../settings.gradle) | 插件仓库与 Java 工具链解析配置 |
| [gradle/wrapper](../../gradle/wrapper)、[gradlew](../../gradlew)、[gradlew.bat](../../gradlew.bat) | 仓库使用的 Gradle 版本及跨平台启动入口 |
| [.github/workflows/build.yml](../../.github/workflows/build.yml) | 在 push / pull request 时配置 JDK 21，执行 Gradle 构建和服务端集成测试 |
| [.codex/config.toml](../../.codex/config.toml)、[.codex/agents](../../.codex/agents) | 项目内的子智能体并发设置与具名角色配置，不参与模组打包 |
| `build/` | 编译、生成元数据、报告与 JAR 等构建产物 |
| `run/` | 开发运行时的配置、日志、存档等本地数据 |
| `.gradle/`、`.idea/` | 本地构建缓存与 IDE 配置 |

本地构建产物、运行数据和上述缓存目录由 [.gitignore](../../.gitignore) 排除。任务资料与本地临时目录的存放、忽略和提交流程统一见 [工作入口](../../work/README.md)。项目文档不参与当前模组资源打包。

子智能体的职责与调用条件见 [AGENTS.md](../../AGENTS.md#按需角色)，模型和推理强度只在各角色 TOML 中维护。项目配置不设置主会话模型或改写全局权限；这些可共享文件按内容纳入版本管理，不整体忽略 `.codex/`。配置采用 [Codex 独立角色文件](https://learn.chatgpt.com/docs/agent-configuration/subagents#custom-agents) 的形式，实际客户端加载与验证结果记录在相应任务中，不能从文件存在推断当前会话已生效。

## 现有代码结构

| 类 | 当前职责 |
| --- | --- |
| [GensouInOperation](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/GensouInOperation.java) | 公共模组入口；注册占位测试开关、飞行附件和模板示例内容，挂接事件并注册配置 |
| [GensouInOperationClient](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/GensouInOperationClient.java) | 客户端入口；注册配置界面并处理客户端初始化事件 |
| [Config](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/Config.java) | 模板配置项及其校验 |
| [item/FlightTestSwitchItem](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/item/FlightTestSwitchItem.java) | 测试开关的物品行为；在逻辑服务端检查使用条件、调用资格开关并发送本地化反馈，返回交互结果 |
| [flight/ReimuTestFlight](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/flight/ReimuTestFlight.java) | 管理自身资格来源、保存状态的恢复、旁观前状态和同步 |
| [flight/ReimuTestFlightState](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/flight/ReimuTestFlightState.java)、[flight/FlightAttachments](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/flight/FlightAttachments.java) | 不可变的服务端状态及其序列化附件注册 |
| [flight/ReimuTestFlightEvents](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/flight/ReimuTestFlightEvents.java) | 连接玩家数据加载、登录、维度变化、玩家重建和死亡清理 |
| [mixin/ServerPlayerGameModeMixin](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/mixin/ServerPlayerGameModeMixin.java)、[mixin/ServerPlayerMixin](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/mixin/ServerPlayerMixin.java) | 在成功的服务端模式切换中维护旁观记录，并将模式与飞行更新一起发送 |

自定义物品行为目前放在 `item/` 包中，由公共入口通过 `DeferredRegister.Items#registerItem` 创建对应类型；模板示例内容尚未清理。测试开关的模型引用原版纸纹理作为占位，未将原版图片复制到仓库。

测试飞行通过 `NeoForgeMod.CREATIVE_FLIGHT` 属性上的临时修饰符提供资格，来源 ID 为 `gensou_in_operation:reimu_test_flight`。授予前检查该来源是否已存在，撤销时只移除同一 ID；不修改属性基础值或清空其他修饰符，也不直接改写 `mayfly` 或强制令玩家起飞。

资格与启停仍接入原版 / NeoForge，没有自定义飞行网络协议或操纵代码。撤销自身来源后，只有综合 `mayFly()` 已为假且玩家仍在飞行时，才关闭 `flying` 并发送原版能力包；创造、旁观和其他有效来源继续由各自机制决定。

服务端玩家附件 `gensou_in_operation:reimu_test_flight` 使用 `Codec` 保存 `enabled` 及可选的 `flying_before_spectator`。普通模式的实际飞行位复用原版 `Abilities.flying` 存档，不另外逐 tick 维护副本。临时属性修饰符仍不写入属性存档，由附件重建；缺少附件的旧存档默认没有本模组资格，不根据创造模式或外部属性推断开启。附件不启用 `copyOnDeath`，只在服务端使用。

`LoadFromFile` 在模式初始化和首次能力包之前恢复资格，同时暂存原飞行位，覆盖服务器强制登录模式的情况；登录结束后，仅旁观模式继续保留这份记忆。常规维度转换保留原玩家；非死亡 Clone 在 NeoForge 复制附件之后重建来源和飞行位，再在 Respawn 包之后同步。玩家 tick、退出及加载检查实际死亡，死亡 Clone 再作清理；末地字幕期间被移除但生命值仍为正的玩家不视为死亡。没有 UUID 会话表，也不每 tick 强制起飞。

公开模式事件发生在变更之前且可以取消或改写目标，因此用两个小范围服务端 Mixin 包住实际模式赋值及后续发包点。成功赋值后记录或恢复旁观前状态；取消和无变化的请求不触发。拥有本模组来源时，将模式、完整 `CREATIVE_FLIGHT` 属性及最终能力组成一个原版 `ClientboundBundlePacket`，避免客户端在旧旁观模式下短暂处理停飞并回传错误状态；保留原版即时隐身更新。其他玩家保留原发包路径。配置见 [Mixin JSON](../../src/main/resources/gensou_in_operation.mixins.json) 及元数据模板；使用 NeoForge 已携带的 MixinExtras，没有增加玩法前置。

登录、维度转换及非死亡重建也通过原版 bundle 按“属性→能力”补发恢复状态；发送完整属性实例以保留外部修饰符。自动化检查发包内容与顺序，客户端实际表现由真人复测确认；两类证据分别记录在 [003 结果](../../work/003-flight-prototype/RESULT.md)。使用条件与玩法规则见 [飞行设计](../design/flight.md#灵梦式飞行的状态延续)，后续改键路线见 [003 输入评估](../../work/003-flight-prototype/RESULT.md#2026-11-03复测反馈与状态延续规则修订)。

## 开发入口

开发环境采用 Java 21；NeoForge 1.21.1 的官方入门说明要求 64 位 JDK / JVM。将仓库根目录作为 Gradle 工程导入 IDE，首次导入可能需要下载依赖与准备游戏源码。参见 [对应版本的 NeoForge 入门说明](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)。

在仓库根目录使用 Gradle Wrapper。Windows PowerShell 下的常用命令为：

| 目的 | 命令 | 说明 |
| --- | --- | --- |
| 查看 Wrapper 和运行它的 JVM | `.\gradlew.bat --version` | JVM 信息与项目配置的编译工具链应分别核对 |
| 构建模组 | `.\gradlew.bat build` | JAR 输出到 `build/libs/`，名称来自模组标识与版本配置 |
| 服务端集成测试 | `.\gradlew.bat runGameTestServer` | 编译并加载独立测试模组，在 `build/gametest/` 运行后退出；普通 `build` 不自动执行它 |
| 启动开发客户端 | `.\gradlew.bat runClient` | 使用当前 `client` 运行配置 |
| 启动开发独立服务端 | `.\gradlew.bat runServer` | 使用当前 `server` 运行配置及其 `--nogui` 参数 |

其他系统使用 `./gradlew` 入口，例如 CI 中的 `./gradlew build`。命令及构建输出的用法参见 [NeoForge 构建与运行说明](https://docs.neoforged.net/docs/1.21.1/gettingstarted/#building-and-testing-your-mod)。独立服务端首次启动的准备步骤见同页的 Server Testing。

`build.gradle` 还提供 `data` 运行配置，目前没有数据生成器。运行配置的行为参见 [ModDevGradle Runs](https://github.com/neoforged/ModDevGradle#runs)。

`gameTestServer` 加载主模组与 `gensou_in_operation_tests`；普通客户端、独立服务端和数据生成运行只加载主模组。测试源集通过现有 ModDevGradle 获取 Minecraft / NeoForge API，没有增加正式玩法前置或额外测试框架。测试模组使用独立 Java 包，避免两个模组导出同一个包；其 `empty.nbt` 是本项目生成的 3×3×3 空测试结构。

当前 [飞行生命周期测试](../../src/gameTest/java/dev/quantumuniversessteel/gensou_in_operation/gametest/FlightLifecycleGameTests.java) 使用真实 `ServerPlayer`、保存加载及生命周期入口，连接使用内存通道。测试连接接入服务器 tick，模拟传送与区块确认和静止移动上报，并检查实际发出的属性、能力包；不把它等同于真实客户端已接收和应用同步。测试成功或失败都会清理玩家、临时监听器及连接。API 入门见 [1.21.1 Game Tests](https://docs.neoforged.net/docs/1.21.1/misc/gametest/)，方法仍以实际依赖核实。

上述命令是开发入口；是否实际执行及其结果记录在对应任务的 `RESULT.md` 中。构建完成不等于游戏行为、联机同步或操作手感已验证。

## 资源与生成流程

[neoforge.mods.toml 模板](../../src/main/templates/META-INF/neoforge.mods.toml) 由 `generateModMetadata` 任务展开变量，写入 `build/generated/sources/modMetadata` 并加入主资源集合。修改元数据应更新配置或模板来源，构建产物会在后续生成时被替换。

数据生成运行配置将输出指向 `src/generated/resources/`，该路径也已加入主资源集合。后续引入实际生成器时，在相应任务中确定产物的版本管理方式；当前忽略规则已经排除生成缓存，未整体忽略生成资源目录。

## 许可与第三方内容

根目录的 [LICENSE](../../LICENSE) 是本项目原创内容的默认许可。NeoForge MDK 模板保留 [TEMPLATE_LICENSE.txt](../../TEMPLATE_LICENSE.txt) 中的原始版权与许可声明；项目许可不替代 Minecraft、《东方 Project》或其他第三方内容的权利与使用条件。

引入依赖、复制或改编外部代码、使用第三方素材时，在相关工程专题或任务记录中保存来源、版本（适用时）、许可证及使用方式，并核对具体许可证版本、例外条款和分发条件。若影响项目当前的发布许可，先说明兼容性与可选方案，再确定引入方式。发布包须保留所需的许可文本和署名。

## 工程专题与资料

目前本页覆盖已有工程所需的概览。随着实现出现可独立维护的数据生命周期、同步或自动化约定，再新增专题并在此登记入口；暂不创建空白约定文件。

现有映射配置与具体 API 应结合所用版本的实际依赖阅读。原模板提供的 [Mojang 映射说明](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md) 保留作为相关资料入口。

在 IDEA 中可通过 `External Libraries` 或跳转定义查看工程加载的 Minecraft 与 NeoForge 源码。当前物品行为可从对应版本的 [Items](https://docs.neoforged.net/docs/1.21.1/items/)、[交互流程](https://docs.neoforged.net/docs/1.21.1/items/interactionpipeline/)和[逻辑端说明](https://docs.neoforged.net/docs/1.21.1/concepts/sides/)入手；方法签名、返回值及两端行为仍须与实际依赖核对，不能只依据其他版本的教程。
