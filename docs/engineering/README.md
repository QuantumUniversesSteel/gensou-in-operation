# 工程概览

本文说明当前仓库的工程基线、代码结构和开发入口。项目整体阶段见 [根 README](../../README.md)，具体任务的执行与验证证据见 [工作索引](../../work/README.md)。

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

模组标识仍为模板的 `examplemod`，显示名为 `Example Mod`，Java 包为 `com.example.examplemod`。`1.0.0` 也是当前模板元数据中的版本值，不能据此推断已经完成正式发布。命名与元数据的调整应作为明确工作处理。

当前没有启用额外的玩法模组依赖；构建文件里的 JEI 等声明都是注释示例。

## 目录与文件职责

| 位置 | 在当前工程中的职责 |
| --- | --- |
| [src/main/java](../../src/main/java) | 模组 Java 源码，目前只有模板的三个类 |
| [src/main/resources](../../src/main/resources) | 手工维护的游戏资源，目前只有示例英文语言文件 |
| [src/main/templates](../../src/main/templates) | 模组元数据模板，经构建时变量展开后加入资源 |
| `src/generated/resources/` | 已配置的数据生成输出与资源来源，当前尚无生成器实现 |
| [gradle.properties](../../gradle.properties) | 游戏、加载器、映射与模组元数据参数，以及 Gradle 运行选项 |
| [build.gradle](../../build.gradle) | 插件、Java 工具链、依赖、运行配置、资源生成与打包 |
| [settings.gradle](../../settings.gradle) | 插件仓库与 Java 工具链解析配置 |
| [gradle/wrapper](../../gradle/wrapper)、[gradlew](../../gradlew)、[gradlew.bat](../../gradlew.bat) | 仓库使用的 Gradle 版本及跨平台启动入口 |
| [.github/workflows/build.yml](../../.github/workflows/build.yml) | 在 push / pull request 时配置 JDK 21 并执行 Gradle 构建 |
| [.codex/config.toml](../../.codex/config.toml)、[.codex/agents](../../.codex/agents) | 项目内的子智能体并发设置与具名角色配置，不参与模组打包 |
| `build/` | 编译、生成元数据、报告与 JAR 等构建产物 |
| `run/` | 开发运行时的配置、日志、存档等本地数据 |
| `.gradle/`、`.idea/` | 本地构建缓存与 IDE 配置 |

本地构建产物、运行数据和上述缓存目录由 [.gitignore](../../.gitignore) 排除。任务资料与本地临时目录的存放、忽略和提交流程统一见 [工作入口](../../work/README.md)。项目文档不参与当前模组资源打包。

子智能体的职责与调用条件见 [AGENTS.md](../../AGENTS.md#按需角色)，模型和推理强度只在各角色 TOML 中维护。项目配置不设置主会话模型或改写全局权限；这些可共享文件按内容纳入版本管理，不整体忽略 `.codex/`。配置采用 [Codex 独立角色文件](https://learn.chatgpt.com/docs/agent-configuration/subagents#custom-agents) 的形式，实际客户端加载与验证结果记录在相应任务中，不能从文件存在推断当前会话已生效。

## 现有代码结构

| 类 | 当前职责 |
| --- | --- |
| [ExampleMod](../../src/main/java/com/example/examplemod/ExampleMod.java) | 公共模组入口；注册示例方块、物品与创造模式标签页，挂接事件并注册配置 |
| [ExampleModClient](../../src/main/java/com/example/examplemod/ExampleModClient.java) | 客户端入口；注册配置界面并处理客户端初始化事件 |
| [Config](../../src/main/java/com/example/examplemod/Config.java) | 模板配置项及其校验 |

当前代码以这三个模板类组织。后续形成实际模块时更新本节，尚未落地的架构方案标为提案。

## 开发入口

开发环境采用 Java 21；NeoForge 1.21.1 的官方入门说明要求 64 位 JDK / JVM。将仓库根目录作为 Gradle 工程导入 IDE，首次导入可能需要下载依赖与准备游戏源码。参见 [对应版本的 NeoForge 入门说明](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)。

在仓库根目录使用 Gradle Wrapper。Windows PowerShell 下的常用命令为：

| 目的 | 命令 | 说明 |
| --- | --- | --- |
| 查看 Wrapper 和运行它的 JVM | `.\gradlew.bat --version` | JVM 信息与项目配置的编译工具链应分别核对 |
| 构建模组 | `.\gradlew.bat build` | JAR 输出到 `build/libs/`，名称来自模组标识与版本配置 |
| 启动开发客户端 | `.\gradlew.bat runClient` | 使用当前 `client` 运行配置 |
| 启动开发独立服务端 | `.\gradlew.bat runServer` | 使用当前 `server` 运行配置及其 `--nogui` 参数 |

其他系统使用 `./gradlew` 入口，例如 CI 中的 `./gradlew build`。命令及构建输出的用法参见 [NeoForge 构建与运行说明](https://docs.neoforged.net/docs/1.21.1/gettingstarted/#building-and-testing-your-mod)。独立服务端首次启动的准备步骤见同页的 Server Testing。

`build.gradle` 还提供 `data` 与 `gameTestServer` 运行配置，可从 IDE 的运行配置中进入。目前源码中没有数据生成器或 GameTest 用例。运行配置的行为参见 [ModDevGradle Runs](https://github.com/neoforged/ModDevGradle#runs)。

上述命令是开发入口；是否实际执行及其结果记录在对应任务的 `RESULT.md` 中。构建完成不等于游戏行为、联机同步或操作手感已验证。

## 资源与生成流程

[neoforge.mods.toml 模板](../../src/main/templates/META-INF/neoforge.mods.toml) 由 `generateModMetadata` 任务展开变量，写入 `build/generated/sources/modMetadata` 并加入主资源集合。修改元数据应更新配置或模板来源，构建产物会在后续生成时被替换。

数据生成运行配置将输出指向 `src/generated/resources/`，该路径也已加入主资源集合。后续引入实际生成器时，在相应任务中确定产物的版本管理方式；当前忽略规则已经排除生成缓存，未整体忽略生成资源目录。

## 工程专题与资料

目前本页覆盖已有工程所需的概览。随着实现出现可独立维护的数据生命周期、同步或自动化约定，再新增专题并在此登记入口；暂不创建空白约定文件。

现有映射配置与具体 API 应结合所用版本的实际依赖阅读。原模板提供的 [Mojang 映射说明](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md) 保留作为相关资料入口。
