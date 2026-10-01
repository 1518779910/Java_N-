/* ============================================================
 * 文件：BitVectorBoardState.java
 * 所在包：edu.scut.nqueens.common（全组共用，不属于任何单一架构风格）
 *
 * 本文件实现的架构风格：不适用（全组共用的数据表示层）
 * 本文件承担的核心组件/连接器：
 *   - 组件：位向量棋盘状态（不可变值对象）
 *     对应 C&C 图中的"数据表示层：BitVectorBoardState"
 *   - 连接器：无
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本类为不可变对象，所有字段 final，对外不暴露可变引用：______
 * [ ] 本类不依赖任何一种架构风格（pipesfilter / blackboard / …）的包：______
 * [ ] 本类不包含算法逻辑，仅为位掩码的承载结构：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.common;

/**
 * 位向量棋盘状态：用三个整数的二进制位记录"已被占用"的列与两条对角线。
 *
 * <p>在五种架构风格中扮演的角色：
 * <ul>
 *   <li><b>调用/返回（迭代 / 回溯）</b>：数据表示层（BitVectorBoardState），
 *       由求解层的 IterativeQueensSolver / RecursiveQueensSolver 调用其方法读写棋盘；</li>
 *   <li><b>管道-过滤器</b>：随 PartialSolution 一起在管道中流动的剪枝状态；</li>
 *   <li><b>黑板</b>：黑板存储区中"棋盘位向量"那部分共享状态；</li>
 *   <li><b>Map-Reduce</b>：Map 输出与 Shuffle 分组的依据。</li>
 * </ul>
 *
 * <p>位向量剪枝的核心思想（五种架构共用同一份语义，保证剪枝策略等价）：
 * <ul>
 *   <li>{@code columnMask}：第 c 位为 1 表示第 c 列已被占用；</li>
 *   <li>{@code diagDownMask}：第 (row - col + n - 1) 位为 1 表示该 ↘ 方向对角线已被占用；</li>
 *   <li>{@code diagUpMask}：第 (row + col) 位为 1 表示该 ↙ 方向对角线已被占用。</li>
 * </ul>
 *
 * <p>判断 (row, col) 是否可放置皇后，只需三次按位与：
 * <pre>
 *   (columnMask &amp; (1 &lt;&lt; col)) == 0
 *   &amp;&amp; (diagDownMask &amp; (1 &lt;&lt; (row - col + n - 1))) == 0
 *   &amp;&amp; (diagUpMask   &amp; (1 &lt;&lt; (row + col))) == 0
 * </pre>
 *
 * <p><b>本类是不可变的</b>：{@link BitVectorPruner#place} 返回新对象而不修改原对象，
 * 这样同一个状态可以被多个线程安全共享，符合"过滤器之间不共享可变状态"
 * 以及"知识源之间不直接通信"的架构约束。
 *
 * <p>TODO（成员A 负责）：确认位宽是否满足 N=8/10/12 的要求，并在注释中写明 N 的上界
 * （int 为 32 位，因此列上界为 32；若要求支持更大的 N 需改用 long 或位数组）。
 */
public final class BitVectorBoardState {

    private final int n;
    private final int columnMask;
    private final int diagDownMask;
    private final int diagUpMask;
    private final int placedRows;

    /**
     * @param n           棋盘规模
     * @param columnMask  列占用位向量
     * @param diagDownMask ↘ 对角线占用位向量
     * @param diagUpMask    ↙ 对角线占用位向量
     * @param placedRows   已放置的皇后数量（等于下一个待放置的行号，也就是"当前搜索深度"）
     */
    public BitVectorBoardState(int n, int columnMask, int diagDownMask, int diagUpMask, int placedRows) {
        this.n = n;
        this.columnMask = columnMask;
        this.diagDownMask = diagDownMask;
        this.diagUpMask = diagUpMask;
        this.placedRows = placedRows;
    }

    public int n() {
        return n;
    }

    public int columnMask() {
        return columnMask;
    }

    public int diagDownMask() {
        return diagDownMask;
    }

    public int diagUpMask() {
        return diagUpMask;
    }

    /** 已放置的皇后数量，也是当前搜索深度（下一个待放置的行号）。 */
    public int depth() {
        return placedRows;
    }

    /** 是否已放满 N 行（即构成一个完整解）。 */
    public boolean isComplete() {
        return placedRows == n;
    }

    @Override
    public String toString() {
        return "BitVectorBoardState{n=" + n
                + ", col=" + Integer.toBinaryString(columnMask)
                + ", diagDown=" + Integer.toBinaryString(diagDownMask)
                + ", diagUp=" + Integer.toBinaryString(diagUpMask)
                + ", depth=" + placedRows + '}';
    }
}
