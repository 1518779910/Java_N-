/* ============================================================
 * 文件：PartialSolution.java
 * 所在包：edu.scut.nqueens.common（全组共用，不属于任何单一架构风格）
 *
 * 本文件实现的架构风格：不适用（全组共用的数据表示层）
 * 本文件承担的核心组件/连接器：
 *   - 组件：部分解（不可变值对象），在管道中流动 / 存放在黑板工作区
 *   - 连接器：无
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本类为不可变对象，对外不暴露内部数组引用：______
 * [ ] 本类不依赖任何一种架构风格的包：______
 * [ ] 本类不持有 BitVectorPruner 等可变依赖：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.common;

import java.util.Arrays;

/**
 * 部分解：前 {@code row} 行已经放置完毕的棋盘状态。
 *
 * <p>它同时携带两种表示，服务于五种架构共用的剪枝策略：
 * <ul>
 *   <li>{@code columns}：逐行记录皇后所在的列，用于最终输出与解的比较；</li>
 *   <li>{@code state}：位向量剪枝状态，用于 O(1) 的冲突判断。</li>
 * </ul>
 *
 * <p><b>本类不可变</b>：{@link #place(int, BitVectorPruner)} 返回新对象而不修改原对象。
 * 管道-过滤器架构中，同一个部分解对象会被写进管道传给下游过滤器；
 * 黑板架构中，同一个部分解对象可能同时存在于工作区与某个知识源的局部变量里。
 * 不可变性是"过滤器之间不共享可变状态""知识源之间不直接通信"两条约束的共同前提。
 */
public final class PartialSolution {

    private final int[] columns;
    private final int row;
    private final BitVectorBoardState state;

    /** 构造空解：一行都还没放，用于搜索的起点。 */
    public PartialSolution(int n) {
        this(new int[n], 0, null);
    }

    /**
     * @param columns columns[i] = 第 i 行皇后所在的列，长度即棋盘规模 n
     * @param row     已放置的行数（= 下一个待放置的行号）
     * @param state   对应的位向量剪枝状态；row == 0 时可以为 null
     */
    public PartialSolution(int[] columns, int row, BitVectorBoardState state) {
        this.columns = columns.clone();
        this.row = row;
        this.state = state;
    }

    /** 棋盘规模 n。 */
    public int n() {
        return columns.length;
    }

    /** 已放置的行数，也就是下一个待放置的行号。 */
    public int row() {
        return row;
    }

    /** 第 i 行皇后所在的列；i >= row 时无意义。 */
    public int columnAt(int i) {
        return columns[i];
    }

    /** 复制一份列号数组（防御性拷贝，保证不可变性）。 */
    public int[] columns() {
        return columns.clone();
    }

    /** 剪枝状态；尚未放置任何皇后时为 null，由各架构在首次放置时初始化。 */
    public BitVectorBoardState state() {
        return state;
    }

    /** 是否已放满 N 行（构成一个完整解）。 */
    public boolean isComplete() {
        return row == columns.length;
    }

    /**
     * 在第 {@code row} 行的 {@code col} 列放置一个皇后，生成<b>新的</b>部分解。
     *
     * <p>TODO（各架构的求解层负责实现，但必须调用同一个 BitVectorPruner 实现）：
     * <ol>
     *   <li>先用 {@link BitVectorPruner#canPlace} 判断，冲突则返回 null 或抛出异常（由调用方约定）；</li>
     *   <li>用 {@link BitVectorPruner#place} 得到新状态；</li>
     *   <li>拷贝并更新列号数组，返回新的 PartialSolution。</li>
     * </ol>
     *
     * @param col    列号（0-based）
     * @param pruner 统一剪枝实现，由调用方注入（本类不持有它，避免引入共享状态）
     * @return 放置后的新部分解
     */
    public PartialSolution place(int col, BitVectorPruner pruner) {
        // 起点（row == 0）时 state 尚未初始化，由第一次放置负责建出空棋盘
        BitVectorBoardState current = (state != null) ? state : pruner.initial(columns.length);

        // 冲突则返回 null，由调用方（过滤器 / 知识源）决定如何跳过这个分支
        if (!pruner.canPlace(current, row, col)) {
            return null;
        }

        // 推进位向量状态：返回新对象，current 保持不变
        BitVectorBoardState next = pruner.place(current, row, col);

        // 拷贝列号数组并写入本行列号——不碰本对象的 columns，保持不可变语义
        int[] nextColumns = columns.clone();
        nextColumns[row] = col;

        return new PartialSolution(nextColumns, row + 1, next);
    }

    /** 若已放满 N 行，转换为解对象。 */
    public Solution toSolution() {
        if (!isComplete()) {
            throw new IllegalStateException("尚未放满 " + columns.length + " 行，不能转换为解：" + this);
        }
        return new Solution(columns);
    }

    @Override
    public String toString() {
        return Arrays.toString(Arrays.copyOf(columns, row)) + " (row=" + row + "/" + columns.length + ")";
    }
}
