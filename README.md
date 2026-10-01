# N 皇后问题：五种软件体系结构风格实现

《软件体系结构》作业1 · **阶段2**：管道-过滤器 + 黑板（先提交这两种架构的可运行代码与 README，待教师审核架构约束通过后再实现其余三种）

---

## 一、本阶段交付内容

| 项 | 内容 | 状态 |
|---|---|---|
| 管道-过滤器架构 | 可运行代码 + [README-管道-过滤器.md](README-管道-过滤器.md) | 骨架已就绪，求解逻辑待实现 |
| 黑板架构 | 可运行代码 + [README-黑板.md](README-黑板.md) | 骨架已就绪，求解逻辑待实现 |
| 全组共用剪枝模板 | `common` 包，保证五种架构剪枝策略等价 | 接口已定，实现待补 |
| 组件-连接器图 | 阶段1 所画，已复制到 [docs/](docs) 与代码同仓 | 已完成 |

---

## 二、环境要求

| 项 | 要求 | 本机 |
|---|---|---|
| JDK | **21**（编译目标 `maven.compiler.release=21`；JDK 25 亦可编译） | JDK 25 |
| Maven | 3.9+ | 3.9.16 |
| 编码 | 源文件 UTF-8（已在 pom 中显式声明） | — |

---

## 三、快速开始

```bash
# 编译
mvn compile

# 跑单元测试（骨架阶段全部为 @Disabled，BUILD SUCCESS 属正常）
mvn test

# 运行某种架构（五种架构命令格式统一，便于跨架构实验对比）
mvn exec:java "-Dexec.args=--arch=pipes --n=8"
mvn exec:java "-Dexec.args=--arch=blackboard --n=8"

# 只求第一个解
mvn exec:java "-Dexec.args=--arch=pipes --n=12 --first"
```

在 IDEA 中：`File → Open` 选择本目录（含 `pom.xml` 的这一层），Maven 自动导入；
直接运行 `edu.scut.nqueens.Main`，或在 Run Configuration 里填 Program arguments `--arch=pipes --n=8`。

---

## 四、目录结构

```
.
├── pom.xml
├── README.md                     本文件（总览）
├── README-管道-过滤器.md          成员A 负责的架构：说明 / 约束自查 / TODO / 实验记录
├── README-黑板.md                成员B 负责的架构：说明 / 约束自查 / TODO / 实验记录
├── docs/
│   ├── cnc-pipes-filter.png      阶段1 组件-连接器图（管道-过滤器）
│   └── cnc-blackboard.png        阶段1 组件-连接器图（黑板）
└── src
    ├── main/java/edu/scut/nqueens
    │   ├── Main.java             统一命令行入口（--arch / --n / --first）
    │   ├── common/               全组共用，不属于任何单一架构风格
    │   │   ├── BitVectorPruner.java      统一剪枝接口模板（成员A 制定）
    │   │   ├── BitVectorPrunerImpl.java  统一剪枝实现（五种架构共用这一份）
    │   │   ├── BitVectorBoardState.java  位向量棋盘状态（不可变）
    │   │   ├── PartialSolution.java      部分解（不可变）
    │   │   ├── Solution.java             完整解（不可变）
    │   │   └── Result.java               结果条目（解 + 输出序号）
    │   ├── pipesfilter/          架构一：管道-过滤器（成员A）
    │   └── blackboard/           架构二：黑板（成员B）
    └── test/java/edu/scut/nqueens/common
        └── BitVectorPrunerTest.java      剪枝实现的单元测试
```

---

## 五、五种架构风格进度

| 编号 | 架构风格 | 包 | 责任人 | 阶段2 | 计划 |
|---|---|---|---|---|---|
| 1 | 管道-过滤器 | `pipesfilter` | 成员A（组长） | **本次提交** | — |
| 2 | 黑板 | `blackboard` | 成员B | **本次提交** | — |
| 3 | 调用/返回（迭代） | `callreturn` | 成员C | 未建 | 阶段4 |
| 4 | 调用/返回（回溯） | `callreturn` | 成员C | 未建 | 阶段4 |
| 5 | Map-Reduce（单机模拟） | `mapreduce` | 成员D | 未建 | 阶段4 |

> 阶段2 只建这两种架构的包，是为了让教师能专注审核架构约束；
> 其余三种在阶段4 前补齐，届时在 `README.md` 与 `pom.xml` 中同步更新。

---

## 六、三条全组统一的设计约定

**1. 剪枝策略只有一份实现。**
作业硬性要求"五种方案的剪枝策略必须等价"。本工程把它落到 `common/BitVectorPrunerImpl`
一个类上：五种架构的求解层都注入同一个实例，等价性由"共用同一份实现"在结构上保证，
而不是靠五份代码互相校对。答辩被问到时，这是最有力的回答。

**2. 数据表示层统一用不可变对象。**
`BitVectorBoardState` / `PartialSolution` / `Solution` / `Result` 全部不可变。
管道-过滤器"过滤器之间不共享可变状态"、黑板"知识源之间不直接通信"，
两条约束的共同前提都是"共享的数据不会被人偷偷改掉"。

**3. 类型名与 C&C 图一一对应。**
阶段1 的 C&C 图上写的是什么类型，代码里就是什么类型：

| C&C 图上的标注 | 代码 |
|---|---|
| Pipe 1：`BlockingQueue<PartialSolution>` | `Pipe<PartialSolution>` |
| Pipe 2：`BlockingQueue<ValidSolution>` | `Pipe<ValidSolution>` |
| Pipe 3：`BlockingQueue<Result>` | `Pipe<Result>` |
| 数据表示层：BitVectorBoardState | `common/BitVectorBoardState.java` |
| 黑板存储区 / 控制器 / KS_ColCheck … | `blackboard/BlackboardState`、`BlackboardController`、`KS_ColCheck` … |

报告要求"图与代码一致"，这条约定让一致性可以被逐项核对，而不是靠印象。

---

## 七、实验与日志（为阶段3 做准备）

阶段3 要提交 N=8/10/12 每次运行的**完整命令、终端输出、运行环境**，原始日志不得修改。

- 五种架构统一走 `edu.scut.nqueens.Main` 入口，命令行格式一致，横向对比才成立。
- **控制台中文编码**：Java 21 会按 Windows 控制台代码页（中文系统为 GBK）输出，在 cmd 与 IDEA 中显示正常；若要写入日志文件交给老师查看，建议用 UTF-8 的 `PrintStream`（构造 `OutputFilter` 时注入），避免对方用 UTF-8 编辑器打开时乱码。
- 运行环境信息建议固定记录一次并复用：OS 版本、CPU 型号与核数、内存、JDK 版本（`java -version`）、Maven 版本、编译参数、是否预热 JVM、每种配置的重复次数。

---

## 八、提交清单（阶段2）

- [ ] `pipesfilter` 包代码完成，`mvn exec:java "-Dexec.args=--arch=pipes --n=8"` 能跑出 92 个解
- [ ] `blackboard` 包代码完成，同样能跑出 92 个解
- [ ] 两份架构 README 中的"约束自查表"与"待办清单"全部填写完毕
- [ ] 每个源文件头部的《架构自查注释块》逐项填 是/否 并签名
- [ ] `mvn test` 通过（启用 `BitVectorPrunerTest` 中的用例）
- [ ] 附录中按表4 格式声明 AI 使用情况（见下）

---

## 九、成员与分工

详见阶段1 提交的《组内分工表》（锁定后不得随意变更）：

| 成员 | 负责架构 |
|---|---|
| 成员A（组长） | 管道-过滤器；阶段1/3/4 打包提交；统一剪枝模板 |
| 成员B | 黑板架构 |
| 成员C | 调用/返回（迭代 + 回溯） |
| 成员D | Map-Reduce（单机模拟）+ 实验日志与性能对比 |

> 答辩时每人只被问到自己负责的那种架构，请各自把自己那份 README 讲得出来。

---

## 十、AI 使用声明（须在报告附录按此格式逐条填写）

| 标注项 | 填写要求 | 本次填写 |
|---|---|---|
| 使用工具 | 具体产品名称与版本 | |
| 用于任务 | 该次 AI 辅助解决的具体小问题 | |
| 输入提示词 | 原样粘贴完整提示词 | |
| AI 输出内容 | AI 给出的关键回答或代码片段（可摘录） | |
| 本人修改 | 做了哪些理解、改写、删除、重写；哪些最终没有采用 | |

> 任务书规定：标注不完整、与代码/报告实际情况不符者，按隐瞒 AI 使用处理。
> 本工程的代码骨架由 AI 辅助生成，**求解与剪枝逻辑须由本人实现**，请在附录中如实说明。
