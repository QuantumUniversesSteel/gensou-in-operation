# 工作记录与 Git 流程

本页说明任务记录的存放与版本管理方式，并提供任务入口。项目整体阶段见 [根 README](../README.md)；具体任务的当前状态与下一步只在对应 `TASK.md` 维护。

## 任务入口

| 编号 | 工作 | 任务 | 结果 |
| --- | --- | --- | --- |
| 001 | 建立并复核项目文档与 Git 流程 | [TASK](001-project-documentation/TASK.md) | [RESULT](001-project-documentation/RESULT.md) |
| 002 | 飞行参考实现调研与操纵方案评估 | [TASK](002-flight-research/TASK.md) | [RESULT](002-flight-research/RESULT.md) |
| 003 | 飞行操纵与扫帚退出原型 | [TASK](003-flight-prototype/TASK.md) | [RESULT](003-flight-prototype/RESULT.md) |

## 资料放在哪里

| 位置 | 放什么 | Git 处理 |
| --- | --- | --- |
| `work/<编号>-<主题>/` | 精简的 `TASK.md`、有成果后写的 `RESULT.md`、必要规格或可复用的复现资料 | 可纳入版本管理，适合公开、接续和追溯；按实际变更选择文件提交 |
| `.local/work/<编号>-<主题>/` | 临时摘录、私人笔记、原始检查输出等无需共享的资料 | 被根 `.gitignore` 忽略，通常只存在本机，不随 Git 提交和推送 |
| `build/`、`run/` | 构建产物、游戏日志、测试存档等已有工具生成的数据 | 沿用模板忽略规则，保留在原目录 |

模组代码和游戏资源仍在 `src/` 等工程目录中开发。`work/` 保存任务记录，不另放一份模组源码。

两个工作目录都是普通文件夹，没有自动搬运或同步机制。开始任务时直接在 `work/` 写可公开的任务入口；临时材料需要保留时才创建同名的 `.local/work/` 目录，两处无需逐文件对应。可以全程只有 `work/`，不必先在 `.local/work/` 完成一份草稿再搬过去。

## 一项工作的流程

1. 需要持续实施、保存重要取舍或跨聊天接续时，建立任务目录并写 `TASK.md`。同一目标的修订和复核继续使用原目录，例如本次复核沿用 001；新目标再分配编号，小修改无需建目录。
2. 开发或研究期间持续更新任务的简短进度。可选的临时材料留在 `.local/work/<同一任务>/`，工具生成的日志和产物继续使用原有目录。
3. 将值得保留的结论、复现方法和必要证据摘要整理进任务文件；原始材料可以继续留在本机。接续所需的信息应能从 `work/` 读到，不能只链接一份别人无法取得的本地笔记。
4. 有阶段成果时写或更新 `RESULT.md`，标明阶段与验证范围。后续复核另记结果，保留原检查发生时的事实；任务状态仍在 `TASK.md` 更新。
5. 按 [协作规则](../AGENTS.md) 的维护边界更新受影响的设计、工程文档或整体概览。任务目录保留当次目标与证据，不复制整份系统文档。
6. 按阶段审阅需要保存的变更，再根据任务授权暂存、提交和推送。尚未完成的任务也可以保存阶段提交，不用等全部完成才纳入 Git。

## 保存、提交与上传的区别

| 操作 | 发生什么 |
| --- | --- |
| 保存文件 | 修改本地工作目录中的内容 |
| `git add` | 选择当前文件内容放入暂存区，准备下一次提交 |
| `git commit` | 将暂存内容记录为本地版本 |
| `git push` | 将本地提交发送到选定的远端分支 |

“可纳入 Git”表示文件适合版本管理；是否已经暂存、提交或上传，需要分别检查。未被忽略的文件不会因为保存就自动上传 GitHub。

例如，在仓库根目录选择某个任务的记录并查看暂存内容：

```powershell
git status --short
git add -- work/001-project-documentation/TASK.md work/001-project-documentation/RESULT.md
git diff --cached
```

其他改动也按需要选择具体文件，检查后再执行提交与推送；这段示例仅演示任务记录的选择过程。暂存后继续编辑的内容，需要再次 `git add` 才会进入下一次提交。命令含义见 [Git 官方教程](https://git-scm.com/docs/gittutorial)。

## 本项目的忽略规则

根 [.gitignore](../.gitignore) 保留模板原有全部规则，只在末尾增加：

```gitignore
### Local working material ###
/.local/
```

开头的 `/` 将规则限定在仓库根目录，末尾的 `/` 表示目录，因此它覆盖根 `.local/` 及其中的 `.local/work/`。`work/`、`docs/` 和 `AGENTS.md` 均未被整体忽略，不需要额外添加反向包含规则。

该规则在本地保存后即参与忽略判断；把 `.gitignore` 提交并推送后，其他克隆才能获得同样的规则。本项目选择共享这条约定，因此将它放进 `.gitignore`；只属于某个人且不需要共享的额外规则，可以放在 `.git/info/exclude`。

可用以下只读命令检查规则，不必先创建示例文件：

```powershell
git check-ignore -v --no-index -- .local/work/001-project-documentation/notes.txt
git check-ignore -v --no-index -- work/001-project-documentation/TASK.md
```

第一条应显示 `/.local/` 的匹配来源；第二条在当前规则下没有匹配输出。`.gitignore` 影响未跟踪文件，已跟踪的文件不会仅因新规则自动退出版本管理，遇到这种情况应单独确认和处理。规则语义见 [Git 官方 gitignore 说明](https://git-scm.com/docs/gitignore)。

`.local/work/` 按需创建，本项目不通过 Git 备份其中的临时资料；公开文档必须包含理解与复现结论所需的信息。
