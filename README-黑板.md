# 架构二：黑板（Blackboard）

> 责任人：成员B　包路径：`edu.scut.nqueens.blackboard`　对应阶段1 C&C 图：[docs/cnc-blackboard.png](docs/cnc-blackboard.png)

---

## 一、风格说明

黑板的思路是"**共享状态 + 独立规则 + 调度器**"：没有固定的数据流，也没有固定的调用链，
而是把问题求解的全部信息放在一块共享的黑板上，由多个彼此互不知情的知识源
各自读黑板、判断自己能不能出手、出手就把结论写回黑板，直到黑板不再变化为止。

**三要素缺一不可**（作业评分点，每种架构 7 分）：

| 要素 | 在本工程中的体现 | 代码 |
|---|---|---|
| 黑板存储区（共享状态） | 棋盘位向量 + 当前搜索深度 + 候选解/完整解 | `BlackboardState.java` |
| 多个相互独立的知识源 | KS_ColCheck / KS_DiagCheck / KS_SolutionCheck | `KS_*.java` |
| 控制器（调度） | 决定每个知识源何时触发、何时停机 | `BlackboardController.java` |

**知识源之间绝对无直接通信**这条约束不是靠自觉，而是结构上写不出来：
`KnowledgeSource` 接口的两个方法只接受 `BlackboardState`，知识源拿不到彼此的任何引用。

## 二、组件-连接器图（阶段1 所画）

![黑板 组件-连接器图](docs/cnc-blackboard.png)

```mermaid
graph TD
    subgraph BlackboardStorage [组件: 黑板存储区 (Shared State)]
        BB[(BlackboardState\n- 棋盘位向量\n- 当前搜索深度\n- 候选解/完整解)]
    end

    subgraph KnowledgeSources [知识源组件 (互相隔离)]
        KS1[KS_ColCheck\n列冲突检查知识源]
        KS2[KS_DiagCheck\n对角线冲突检查知识源]
        KS3[KS_SolutionCheck\n解完整性判定知识源]
    end

    subgraph BlackboardController [组件: 控制器]
        Ctrl[BlackboardController / 调度器]
    end

    %% 连接器定义
    Ctrl -->|连接器: 监控与调度信号| KS1
    Ctrl -->|连接器: 监控与调度信号| KS2
    Ctrl -->|连接器: 监控与调度信号| KS3

    KS1 <-->|连接器: Data Access 读写状态| BB
    KS2 <-->|连接器: Data Access 读写状态| BB
    KS3 <-->|连接器: Data Access 读写状态| BB

    style BB fill:#bbf,stroke:#333,stroke-width:2px
    style KS1 fill:#dfd,stroke:#333
    style KS2 fill:#dfd,stroke:#333
    style KS3 fill:#dfd,stroke:#333
```

## 三、组件清单（图 ↔ 代码对照）

| 图中组件 | 职责 | 代码 |
|---|---|---|
| 黑板存储区 (Shared State) | 全系统唯一的共享状态；知识源通过它间接交互 | `BlackboardState.java` |
| KS_ColCheck（列冲突检查知识源） | **扩展**：把栈顶路径展开成下一行的候选 | `KS_ColCheck.java` |
| KS_DiagCheck（对角线冲突检查知识源） | **死路检测**：路径已被约束堵死时丢弃 | `KS_DiagCheck.java` |
| KS_SolutionCheck（解完整性判定知识源） | **收集**：放满 N 行则写入结果区 | `KS_SolutionCheck.java` |
| 控制器 / 调度器 | 调度触发顺序、判定停机、不含任何求解规则 | `BlackboardController.java` |

| 图中连接器 | 含义 | 代码体现 |
|---|---|---|
| 监控与调度信号 | 控制器 → 知识源（单向） | 控制器持有 `List<KnowledgeSource>` 并调用其 `canHandle` / `execute` |
| Data Access 读写状态 | 知识源 ↔ 黑板（双向） | `KnowledgeSource` 接口参数为 `BlackboardState` |

## 四、本实现的三个关键决策（答辩核心）

**1. 黑板上存"一条路径"还是"一堆候选"？**
存**候选集合**。`candidates` 当**栈**用，每个元素是一条已放好前 `row` 行的合法路径
（`PartialSolution` 自带列号数组 + 位向量状态）。栈顶即当前正在处理的候选，
因此这是"黑板上的深度优先搜索"。`boardState` / `depth` 是栈顶候选的镜像，
用于与 C&C 图上的"棋盘位向量 / 当前搜索深度"对应。

**2. 谁来负责"扩展下一行"？**
**`KS_ColCheck`**。它是最容易被满足的一条规则，天然适合做搜索推进的入口：
弹出栈顶路径，对每一列调用全组统一的剪枝实现生成子路径并压回工作区。

**3. 停机条件是什么？**
**版本号收敛**：一轮下来所有知识源都不可触发，**并且** `revision()` 相比本轮开始时
没有增长 → 到达稳定态，停机。

只判断"知识源都不可触发"是不够的：某条死路被取出后没有产生任何子候选时，
必须靠版本号才能确认确实没有进展，否则会过早停机导致漏解（N=8 应恰好 92 个解）。

> 因此 `takeCandidate()` 也递增版本号——**取出候选同样算"黑板内容变更"**。

### 附：调度策略与一处设计张力

**调度策略**采用固定顺序轮询：每轮按固定顺序逐个询问 `canHandle`，能触发就 `execute`。
顺序固定 ⇒ 行为完全可复现 ⇒ 后续做跨架构耗时对比时数据才稳定。

**一处需要说明的设计张力**：骨架把检查拆成 `KS_ColCheck` 与 `KS_DiagCheck` 两个知识源，
但全组统一的 `BitVectorPruner.canPlace` 是一次判完列 + 两条对角线的。
本实现的处理是：`KS_ColCheck` 负责扩展（用 pruner 做完整剪枝），
`KS_DiagCheck` 负责"这条路径是否已被约束堵死"这个独立的可行性规则。

## 五、架构约束自查表（提交前逐项确认）

| # | 约束 | 是/否 | 说明 / 证据 |
|---|---|---|---|
| 1 | 具备黑板存储区（共享状态区） | | `BlackboardState` 是唯一共享对象 |
| 2 | 具备多个相互独立的知识源 | | 三个 KS 各自只实现一条规则 |
| 3 | 具备控制器，且负责调度触发顺序 | | `BlackboardController.solve()` |
| 4 | **知识源之间无任何直接通信**（不互相持有引用/调用） | | 接口方法只收 `BlackboardState` |
| 5 | 知识源不通过黑板反向持有彼此 | | `BlackboardState` 不持有 KS 引用 |
| 6 | 控制器是唯一持有知识源集合的组件 | | `List<KnowledgeSource>` 仅存在于控制器与装配器 |
| 7 | 知识源可独立替换而不影响其它知识源 | | 三个 KS 之间零依赖 |
| 8 | 剪枝调用全组统一实现 | | KS_ColCheck / KS_DiagCheck 注入 `common.BitVectorPruner` |
| 9 | 黑板内容变更都递增版本号（供控制器判断进展） | | `publish*` / `updateBoardState` / `setDepth` / `takeCandidate` 内 `revision++` |

## 六、实现状态

阶段2 已全部实现完成，`mvn test` 通过，N=8/10/12 解数 92 / 724 / 14200。

| 文件 | 状态 |
|---|---|
| `common/BitVectorPrunerImpl.java` | ✅ `initial` / `canPlace` / `place` |
| `blackboard/BlackboardState.java` | ✅ 6 个状态变更方法 + `peekCandidate` |
| `blackboard/KS_ColCheck.java` | ✅ `canHandle` / `execute`（扩展） |
| `blackboard/KS_DiagCheck.java` | ✅ `canHandle` / `execute`（死路检测） |
| `blackboard/KS_SolutionCheck.java` | ✅ `canHandle` / `execute`（收集） |
| `blackboard/BlackboardController.java` | ✅ `solve()` 调度循环与停机判定 |
| `blackboard/BlackboardMain.java` | ✅ 装配与汇总 |

## 七、运行与验证

```bash
# 求全部解（N=8 应为 92 个解）
mvn exec:java "-Dexec.args=--arch=blackboard --n=8"

# 只求第一个解
mvn exec:java "-Dexec.args=--arch=blackboard --n=12 --first"
```

验收标准：N=8 输出 92 个解；与管道-过滤器架构的解集合**逐一相同**（可写一个对比测试验证）。

## 八、实验记录（阶段3 采集，原始日志另存）

运行环境（固定记录一次）：OS ______ / CPU ______ / 内存 ______ / JDK ______ / Maven ______

| N | 完整命令 | 解数 | 耗时(ms) | 备注 |
|---|---|---|---|---|
| 8 | | | | |
| 10 | | | | |
| 12 | | | | |

---

## 附：为什么这条架构"看起来简单、写起来容易跑偏"

黑板最容易被写成"伪黑板"的两种方式：
① 把整个求解算法塞进一个知识源，其余知识源只是摆设；
② 让知识源之间互相调用（"检查完列再让对角线检查一下"），变成一条隐式的调用链。
判断标准很简单：**把任意一个知识源删掉，其余知识源应当照常运行**（只是结果不同或漏解），
而不是编译不过或抛异常。请在提交前用这个标准自查一遍。
