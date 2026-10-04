/* ============================================================
 * 文件：CallReturnMain.java
 * 本文件实现的架构风格：调用/返回（Call and Return）
 * 本文件承担的核心组件/连接器：
 *   - 组件：装配器（创建剪枝器、选择求解器、调用求解器、收集输出）
 *   - 连接器：方法调用（上层调用下层求解器，下层通过返回值交回解数）
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本文件只负责"接线"，不参与任何求解/剪枝逻辑：______
 * [ ] 数据仅通过参数与返回值传递，不依赖共享可变状态：______
 * [ ] 本文件不向求解器传递任何可变共享状态（pruner 无状态、PartialSolution 不可变）：______
 * [ ] 求解器之间没有互相持有引用，只通过本装配器串联：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.callreturn;

import java.io.PrintStream;

import edu.scut.nqueens.common.BitVectorPruner;
import edu.scut.nqueens.common.BitVectorPrunerImpl;

/**
 * 调用/返回架构的装配与启动入口。
 *
 * <p><b>调用/返回架构的核心特征——分层调用：</b>
 * <pre>
 *   [CallReturnMain]  ← 装配器（本文件）
 *        │  创建 pruner、选择求解器、调用 solve()
 *        ▼
 *   [IterativeQueensSolver / RecursiveQueensSolver]  ← 求解层
 *        │  调用 place() / isComplete() / toSolution()
 *        ▼
 *   [PartialSolution / BitVectorPruner]  ← 数据表示层（common 包，全组共用）
 * </pre>
 *
 * <p>上层调用下层，下层通过<b>返回值</b>把结果交回上层；数据通过参数与返回值传递，
 * 不依赖共享可变状态（{@link BitVectorPruner} 无状态、{@code PartialSolution} 不可变）。
 *
 * <p><b>迭代版与回溯版的关系：</b>二者是同一架构风格（调用/返回）下的两种求解控制流——
 * 迭代版用显式栈模拟递归，回溯版用语言原生的递归调用栈。它们共享同一套剪枝实现与数据表示，
 * 区别仅在于搜索的控制流是手写栈还是递归。答辩时要能讲清这一点。
 */
public final class CallReturnMain {

    private CallReturnMain() {
    }

    /** 求解器模式：迭代（显式栈）或回溯（递归）。 */
    public enum SolverMode {
        /** 迭代版：用 ArrayDeque 显式栈 + 循环逐行枚举。 */
        ITERATIVE,
        /** 回溯版：递归逐行放置，冲突时回溯。 */
        RECURSIVE
    }

    /**
     * 运行调用/返回架构求解 N 皇后问题。
     *
     * @param n       棋盘规模（8 / 10 / 12）
     * @param findAll true = 求全部解；false = 求第一个解后停止
     * @param out     结果输出目标（控制台或日志文件）
     * @param mode    求解器模式：ITERATIVE（迭代）或 RECURSIVE（回溯）
     * @return 解的数量
     */
    public static long run(int n, boolean findAll, PrintStream out, SolverMode mode) {
        // 1. 全组统一的剪枝实现。无状态，可被求解器安全持有。
        BitVectorPruner pruner = new BitVectorPrunerImpl();

        long startMillis = System.currentTimeMillis();
        long count;

        // 2. 根据模式选择求解器并调用——这是调用/返回架构"上层调下层"的体现。
        //    求解器只拿到它需要的依赖（pruner、out），通过返回值把解数交回本装配器。
        switch (mode) {
            case ITERATIVE -> {
                IterativeQueensSolver iterative = new IterativeQueensSolver(pruner, out);
                count = iterative.solve(n, findAll);
            }
            case RECURSIVE -> {
                RecursiveQueensSolver recursive = new RecursiveQueensSolver(pruner, out, n);
                count = recursive.solve(n, findAll);
            }
            default -> throw new IllegalArgumentException("未知求解器模式：" + mode);
        }

        long elapsed = System.currentTimeMillis() - startMillis;

        // 3. 汇总行（阶段3 的实验日志格式：架构名 / N / 解数 / 耗时毫秒）
        String label = (mode == SolverMode.ITERATIVE)
                ? "call-return-iterative"
                : "call-return-backtracking";
        out.printf("[%s] N=%d 解数=%d 耗时=%d ms%n", label, n, count, elapsed);
        return count;
    }

    /** 便于单架构调试：java -cp target/classes edu.scut.nqueens.callreturn.CallReturnMain --n=8 */
    public static void main(String[] args) {
        edu.scut.nqueens.Main.main(prependArch(args));
    }

    private static String[] prependArch(String[] args) {
        String[] result = new String[args.length + 1];
        result[0] = "--arch=iterative";
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }
}
