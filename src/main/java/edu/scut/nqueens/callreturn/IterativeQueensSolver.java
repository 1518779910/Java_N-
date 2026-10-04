/* ============================================================
 * 文件：IterativeQueensSolver.java
 * 本文件实现的架构风格：调用/返回（Call and Return）—— 迭代版
 * 本文件承担的核心组件/连接器：
 *   - 组件：求解层（迭代版求解器）
 *   - 连接器：方法调用（被 CallReturnMain 调用，内部调用 common 包的 place/isComplete）
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本文件不持有任何跨调用的共享可变状态（pruner 无状态、PartialSolution 不可变）：______
 * [ ] 本文件使用显式栈（ArrayDeque）而非递归，控制流由循环驱动：______
 * [ ] 剪枝逻辑全部委托给 common 包的 BitVectorPruner，本文件不另写剪枝：______
 * [ ] 本文件不依赖其它架构风格的包：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.callreturn;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;

import edu.scut.nqueens.common.BitVectorPruner;
import edu.scut.nqueens.common.PartialSolution;
import edu.scut.nqueens.common.Solution;

/**
 * 迭代版求解器：用显式栈（{@link ArrayDeque}）模拟递归，逐行枚举皇后位置。
 *
 * <p><b>为什么用显式栈而不是递归：</b>调用/返回架构允许两种控制流——
 * 一种是语言原生的递归调用栈（回溯版），另一种是手写的显式栈（本类）。
 * 显式栈的好处是把"调用栈"从 JVM 托管变成数据结构，便于观察、调试、
 * 也避免在极端规模下栈溢出（虽然 N≤12 远用不到）。
 *
 * <p><b>搜索过程（与回溯版等价，仅控制流不同）：</b>
 * <ol>
 *   <li>栈初始化为空解 {@code new PartialSolution(n)}；</li>
 *   <li>弹出栈顶部分解，对当前行的每一列尝试 {@code place}：
 *     <ul>
 *       <li>返回 null → 冲突，跳过；</li>
 *       <li>非 null 且 {@code isComplete()} → 收集成解；</li>
 *       <li>非 null 且未完成 → 压栈，继续扩展。</li>
 *     </ul>
 *   </li>
 *   <li>栈空则搜索结束。</li>
 * </ol>
 *
 * <p><b>解的输出顺序：</b>为与回溯版（col 从 0 到 n-1 递归）保持一致，
 * 压栈时按列号<b>逆序</b>（{@code col = n-1 → 0}），这样弹出时先处理 col=0 的分支，
 * 第一个解与递归版相同。
 */
public final class IterativeQueensSolver {

    private final BitVectorPruner pruner;
    private final PrintStream out;

    /**
     * @param pruner 全组统一的位向量剪枝实现（无状态）
     * @param out    结果输出目标
     */
    public IterativeQueensSolver(BitVectorPruner pruner, PrintStream out) {
        this.pruner = pruner;
        this.out = out;
    }

    /**
     * 迭代求解 N 皇后。
     *
     * @param n       棋盘规模
     * @param findAll true = 求全部解；false = 求第一个解后停止
     * @return 解的数量
     */
    public long solve(int n, boolean findAll) {
        // 显式栈：用 ArrayDeque 模拟递归调用栈。ArrayDeque 是 Java 推荐的栈实现，
        // 比 Stack 快且无同步开销。
        Deque<PartialSolution> stack = new ArrayDeque<>();
        stack.push(new PartialSolution(n));

        long count = 0;
        int index = 1;
        // 棋盘图只在 N<=8 时打印（92 个解尚可阅读，N=12 的 14200 个会淹没日志）
        boolean printBoard = n <= 8;

        while (!stack.isEmpty()) {
            PartialSolution current = stack.pop();

            // 逆序压栈：保证弹出时 col 从小到大，与回溯版输出顺序一致
            for (int col = n - 1; col >= 0; col--) {
                PartialSolution next = current.place(col, pruner);
                if (next == null) {
                    continue;   // 冲突，剪枝
                }
                if (next.isComplete()) {
                    // 放满 N 行 → 收集成完整解并输出
                    Solution solution = next.toSolution();
                    out.println("#" + index++ + " " + solution);
                    if (printBoard) {
                        printBoard(solution);
                    }
                    count++;
                    if (!findAll) {
                        return count;   // 只求第一个解：立即返回
                    }
                } else {
                    // 未放满 → 压栈，继续扩展
                    stack.push(next);
                }
            }
        }
        return count;
    }

    /**
     * 打印棋盘图：{@code Q} 表示皇后，{@code .} 表示空位。
     * 与管道-过滤器架构的 OutputFilter 保持相同格式，便于跨架构对照。
     */
    private void printBoard(Solution solution) {
        int n = solution.n();
        for (int row = 0; row < n; row++) {
            StringBuilder line = new StringBuilder(2 * n);
            for (int col = 0; col < n; col++) {
                line.append(solution.columnAt(row) == col ? 'Q' : '.');
                if (col < n - 1) {
                    line.append(' ');
                }
            }
            out.println("    " + line);
        }
        out.println();
    }
}
