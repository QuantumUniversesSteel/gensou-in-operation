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

| 入口或模块 | 当前职责 |
| --- | --- |
| [GensouInOperation](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/GensouInOperation.java) | 公共模组入口；汇集注册、事件挂接与配置 |
| [GensouInOperationClient](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/GensouInOperationClient.java) | 客户端入口；注册配置界面并处理客户端初始化事件 |
| [Config](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/Config.java) | 模板配置项及其校验 |
| [item/](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/item) | 自定义物品交互行为 |
| [flight/](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/flight) | 飞行资格、保存状态及生命周期处理 |
| [mixin/](../../src/main/java/dev/quantumuniversessteel/gensou_in_operation/mixin) | 对原版行为的局部接入 |

模板示例内容尚未清理。具体功能的类职责和技术细节从下方 [工程专题索引](#工程专题与资料) 按需读取。

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

`gameTestServer` 加载主模组与 `gensou_in_operation_tests`；普通客户端、独立服务端和数据生成运行只加载主模组。测试源集通过现有 ModDevGradle 获取 Minecraft / NeoForge API，使用独立 Java 包以避免模组间重复导出，不进入主 JAR，也没有增加正式玩法前置或额外测试框架。API 入门见 [1.21.1 Game Tests](https://docs.neoforged.net/docs/1.21.1/misc/gametest/)，具体测试实现见对应工程专题。

上述命令是开发入口；是否实际执行及其结果记录在对应任务的 `RESULT.md` 中。构建完成不等于游戏行为、联机同步或操作手感已验证。

## 资源与生成流程

[neoforge.mods.toml 模板](../../src/main/templates/META-INF/neoforge.mods.toml) 由 `generateModMetadata` 任务展开变量，写入 `build/generated/sources/modMetadata` 并加入主资源集合。修改元数据应更新配置或模板来源，构建产物会在后续生成时被替换。

数据生成运行配置将输出指向 `src/generated/resources/`，该路径也已加入主资源集合。后续引入实际生成器时，在相应任务中确定产物的版本管理方式；当前忽略规则已经排除生成缓存，未整体忽略生成资源目录。

## 许可与第三方内容

根目录的 [LICENSE](../../LICENSE) 是本项目原创内容的默认许可。NeoForge MDK 模板保留 [TEMPLATE_LICENSE.txt](../../TEMPLATE_LICENSE.txt) 中的原始版权与许可声明；项目许可不替代 Minecraft、《东方 Project》或其他第三方内容的权利与使用条件。

引入依赖、复制或改编外部代码、使用第三方素材时，在相关工程专题或任务记录中保存来源、版本（适用时）、许可证及使用方式，并核对具体许可证版本、例外条款和分发条件。若影响项目当前的发布许可，先说明兼容性与可选方案，再确定引入方式。发布包须保留所需的许可文本和署名。

## 工程专题与资料

| 专题 | 何时读取 |
| --- | --- |
| [飞行工程实现](flight.md) | 修改测试飞行入口、资格来源、保存恢复、模式或维度接入、同步与相关测试时 |

现有映射配置与具体 API 应结合所用版本的实际依赖阅读。原模板提供的 [Mojang 映射说明](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md) 保留作为相关资料入口。

在 IDEA 中可通过 `External Libraries` 或跳转定义查看工程加载的 Minecraft 与 NeoForge 源码。当前物品行为可从对应版本的 [Items](https://docs.neoforged.net/docs/1.21.1/items/)、[交互流程](https://docs.neoforged.net/docs/1.21.1/items/interactionpipeline/)和[逻辑端说明](https://docs.neoforged.net/docs/1.21.1/concepts/sides/)入手；方法签名、返回值及两端行为仍须与实际依赖核对，不能只依据其他版本的教程。
