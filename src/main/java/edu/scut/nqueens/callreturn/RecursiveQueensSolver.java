/* ============================================================
 * 文件：RecursiveQueensSolver.java
 * 本文件实现的架构风格：调用/返回（Call and Return）—— 回溯版
 * 本文件承担的核心组件/连接器：
 *   - 组件：求解层（回溯版求解器）
 *   - 连接器：方法调用（被 CallReturnMain 调用，递归调用自身，内部调用 common 包）
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本文件不持有任何跨调用的共享可变状态（pruner 无状态、PartialSolution 不可变）：______
 * [ ] 回溯是求解层内部的算法（递归逐行放置），不是独立的架构风格：______
 * [ ] 剪枝逻辑全部委托给 common 包的 BitVectorPruner，本文件不另写剪枝：______
 * [ ] 本文件不依赖其它架构风格的包：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.callreturn;

import java.io.PrintStream;

import edu.scut.nqueens.common.BitVectorPruner;
import edu.scut.nqueens.common.PartialSolution;
import edu.scut.nqueens.common.Solution;

/**
 * 回溯版求解器：递归逐行放置皇后，遇到冲突时回溯（返回上一层尝试下一列）。
 *
 * <p><b>回溯在调用/返回架构中的位置：</b>回溯不是一种独立的架构风格，
 * 而是<b>求解层内部的一种控制流</b>——它利用语言原生的递归调用栈来管理搜索状态。
 * 在调用/返回架构的分层中，本类位于求解层，对上接受装配器的调用、
 * 对下调用 common 包的数据表示与剪枝接口。
 *
 * <p><b>递归过程：</b>
 * <pre>
 *   backtrack(PartialSolution ps):
 *     if ps.isComplete():  收集解；return
 *     for col in 0..n-1:
 *       next = ps.place(col, pruner)
 *       if next != null:   // 不冲突
 *         backtrack(next)  // 递归深入下一行
 * </pre>
 *
 * <p><b>只求第一个解的停止机制：</b>递归方法返回 boolean，
 * 找到第一个解后向上层返回 {@code true}，各层收到后立即停止循环、继续向上传递，
 * 直到整个搜索树被"快速回卷"。这比"设一个全局 flag 每层检查"更干净——
 * 停止信号通过返回值传递，没有额外的共享可变状态。
 */
public final class RecursiveQueensSolver {

    private final BitVectorPruner pruner;
    private final PrintStream out;
    private final boolean printBoard;

    /** 解的输出序号（从 1 开始，与 Result.index 的约定一致）。 */
    private int index = 1;
    /** 已找到的解数量。 */
    private long count = 0;

    /**
     * @param pruner 全组统一的位向量剪枝实现（无状态）
     * @param out    结果输出目标
     * @param n      棋盘规模（用于决定是否打印棋盘图）
     */
    public RecursiveQueensSolver(BitVectorPruner pruner, PrintStream out, int n) {
        this.pruner = pruner;
        this.out = out;
        this.printBoard = n <= 8;
    }

    /**
     * 回溯求解 N 皇后。
     *
     * @param n       棋盘规模
     * @param findAll true = 求全部解；false = 求第一个解后停止
     * @return 解的数量
     */
    public long solve(int n, boolean findAll) {
        this.index = 1;
        this.count = 0;
        backtrack(new PartialSolution(n), findAll);
        return count;
    }

    /**
     * 递归回溯：在 {@code ps} 的基础上尝试放置下一行的皇后。
     *
     * @param ps      当前部分解
     * @param findAll true = 求全部解；false = 求第一个解后停止
     * @return true 表示已找到第一个解，应停止搜索（仅 findAll=false 时有意义）
     */
    private boolean backtrack(PartialSolution ps, boolean findAll) {
        if (ps.isComplete()) {
            // 放满 N 行 → 收集成完整解并输出
            Solution solution = ps.toSolution();
            out.println("#" + index++ + " " + solution);
            if (printBoard) {
                printBoard(solution);
            }
            count++;
            return !findAll;   // findAll=false 时返回 true 向上传递"停止"信号
        }

        for (int col = 0; col < ps.n(); col++) {
            PartialSolution next = ps.place(col, pruner);
            if (next == null) {
                continue;   // 冲突，剪枝，尝试下一列
            }
            if (backtrack(next, findAll)) {
                return true;   // 只求第一个解：向上传递停止信号
            }
        }
        return false;   // 当前行所有列都试过了，回溯到上一行
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
