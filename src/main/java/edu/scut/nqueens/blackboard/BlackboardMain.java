/* ============================================================
 * 文件：BlackboardMain.java
 * 本文件实现的架构风格：黑板架构（Blackboard）
 * 本文件承担的核心组件/连接器：
 *   - 组件：装配器（创建黑板存储区、三个知识源与控制器并串联起来）
 *   - 连接器：把知识源列表交给控制器（监控与调度信号），知识源与黑板共用一个 BlackboardState
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本文件只负责"接线"，不参与任何求解/剪枝逻辑：______
 * [ ] 知识源之间没有互相持有引用，只共享黑板对象：______
 * [ ] 三个知识源注入的是同一个无状态剪枝实现：______
 * [ ] 本文件是唯一知道"一共有哪些知识源"的地方（除控制器外）：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.blackboard;

import java.io.PrintStream;
import java.util.List;

import edu.scut.nqueens.common.BitVectorPruner;
import edu.scut.nqueens.common.BitVectorPrunerImpl;
import edu.scut.nqueens.common.PartialSolution;
import edu.scut.nqueens.common.Solution;

/**
 * 黑板架构的装配与启动入口。
 *
 * <p><b>装配清单（与阶段1 的 C&amp;C 图一一对应）：</b>
 * <pre>
 *                        +---------------------------+
 *                        |  BlackboardState          |
 *                        |  - 棋盘位向量              |
 *                        |  - 当前搜索深度            |
 *                        |  - 候选解 / 完整解         |
 *                        +---------------------------+
 *                          ^         ^          ^
 *             Data Access  |         |          |   Data Access
 *                          |         |          |
 *                    [KS_ColCheck] [KS_DiagCheck] [KS_SolutionCheck]
 *                          ^         ^          ^
 *                          |  监控与调度信号     |
 *                    +-----------------------------+
 *                    |  BlackboardController       |
 *                    +-----------------------------+
 * </pre>
 *
 * <p><b>装配步骤（TODO）：</b>
 * <ol>
 *   <li>创建黑板：{@code new BlackboardState(n)}；</li>
 *   <li>创建全组统一的剪枝实现（{@code common} 包下 {@link BitVectorPruner} 的唯一实现类）；</li>
 *   <li>创建三个知识源：KS_ColCheck、KS_DiagCheck、KS_SolutionCheck，
 *       注意<b>知识源只拿到黑板所必需的信息（剪枝实现），彼此之间没有任何引用</b>；</li>
 *   <li>创建控制器并注入知识源列表，调用 {@link BlackboardController#solve()}；</li>
 *   <li>收尾：读取 {@code blackboard.solutionCount()} 与墙钟耗时，
 *       按阶段3 的日志格式打印汇总行（架构名、N、解数、耗时毫秒、命令行）。</li>
 * </ol>
 *
 * <p><b>需要你们决定的问题：</b>第一步"初始候选"由谁放进黑板？
 * 常见做法是在装配器里直接 {@code blackboard.publishCandidate(new PartialSolution(n))}
 * 把空解作为搜索起点——这也是黑板架构"启动时先往黑板上放一个待求解的问题"的体现。
 * 请在 README 中写明你们的做法。
 */
public final class BlackboardMain {

    private BlackboardMain() {
    }

    /**
     * 运行黑板架构求解 N 皇后问题。
     *
     * @param n       棋盘规模（8 / 10 / 12）
     * @param findAll true = 求全部解；false = 求第一个解后停止
     * @param out     结果输出目标（控制台或日志文件）
     * @return 解的数量
     */
    public static int run(int n, boolean findAll, PrintStream out) {
        // 1. 黑板存储区：全系统唯一的共享状态
        BlackboardState blackboard = new BlackboardState(n);

        // 2. 全组统一的剪枝实现。三个知识源注入的是同一个实例——
        //    它无状态，所以被多个知识源同时持有不会产生共享可变状态。
        BitVectorPruner pruner = new BitVectorPrunerImpl();

        // 3. 三个知识源。注意它们之间没有任何引用，各自只拿到黑板所必需的信息：
        //    两个检查知识源需要 pruner，解判定知识源连 pruner 都不需要。
        List<KnowledgeSource> sources = List.of(
                new KS_ColCheck(pruner),
                new KS_DiagCheck(pruner),
                new KS_SolutionCheck());

        // 4. 启动时先往黑板上放一个"待求解的问题"：空解（一行都还没放，搜索起点）
        blackboard.publishCandidate(new PartialSolution(n));

        // 5. 控制器是唯一持有知识源集合的组件，由它调度直到黑板到达稳定态
        BlackboardController controller = new BlackboardController(blackboard, sources);

        long startMillis = System.currentTimeMillis();
        int count = controller.solve(findAll);
        long elapsed = System.currentTimeMillis() - startMillis;

        // 逐个输出解。格式与管道-过滤器架构保持一致（#序号 + 逗号分隔的列号序列），
        // 这样两种架构的输出可以直接对照，老师不必记两套格式。
        int index = 1;
        for (Solution solution : blackboard.solutions()) {
            out.println("#" + index++ + " " + solution);
        }

        // 汇总行：架构名 / N / 解数 / 耗时
        out.printf("[blackboard] N=%d 解数=%d 耗时=%d ms%n", n, count, elapsed);
        return count;
    }

    /** 便于单架构调试：java -cp target/classes edu.scut.nqueens.blackboard.BlackboardMain --n=8 */
    public static void main(String[] args) {
        edu.scut.nqueens.Main.main(prependArch(args));
    }

    private static String[] prependArch(String[] args) {
        String[] result = new String[args.length + 1];
        result[0] = "--arch=blackboard";
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }
}
