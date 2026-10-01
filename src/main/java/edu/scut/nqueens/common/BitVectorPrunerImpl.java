/* ============================================================
 * 文件：BitVectorPrunerImpl.java
 * 所在包：edu.scut.nqueens.common（全组共用，不属于任何单一架构风格）
 *
 * 本文件实现的架构风格：不适用（全组共用的剪枝实现）
 * 本文件承担的核心组件/连接器：
 *   - 组件：统一的位向量剪枝实现（成员A 负责实现与维护）
 *     五种架构风格（管道-过滤器 / 调用返回-迭代 / 调用返回-回溯 / 黑板 / Map-Reduce）
 *     的求解层全部复用本类，不得各自另写一份剪枝代码
 *   - 连接器：无
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本类无任何可变状态（无实例字段，或无状态设计），可被多线程共享：______
 * [ ] canPlace / place 为纯函数，不修改传入的 BitVectorBoardState：______
 * [ ] 本类不 import 任何一种架构风格的包：______
 * [ ] 五种架构的剪枝行为完全一致（同一份实现）：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.common;

/**
 * 全组唯一的位向量剪枝实现，即作业要求中"五种方案的剪枝策略必须等价"的落地点。
 *
 * <p><b>无状态设计：</b>本类不保存任何实例字段，所有信息都通过参数传入、通过返回值传出。
 * 因此它可以被五种架构、多个线程、多个知识源同时共享，而不产生共享可变状态。
 * 如果你们为了方便想在这里加缓存（例如"已算过的状态"），请先想清楚：
 * 那份缓存会成为所有架构共享的可变状态，答辩时很难解释，也容易被判为不符合约束。
 */
public final class BitVectorPrunerImpl implements BitVectorPruner {

    @Override
    public BitVectorBoardState initial(int n) {

        // 返回 N=n 的初始状态（三个位向量全为 0、深度为 0）
        return new BitVectorBoardState(n,0,0,0,0);

    }

    @Override
    public boolean canPlace(BitVectorBoardState state, int row, int col) {

        // 检查 (row, col) 是否与已放置的皇后冲突
        return (state.columnMask() & (1 << col)) == 0 && (state.diagDownMask() & (1 << (row - col + state.n() - 1))) == 0
                && (state.diagUpMask() & (1 << (row + col))) == 0;
    }

    @Override
    public BitVectorBoardState place(BitVectorBoardState state, int row, int col) {
        // 不可变语义：BitVectorBoardState 的字段全为 private final 且无 setter，
        // 唯一出路是构造一个新对象返回，原 state 保持不变（多线程可安全共享）。
        return new BitVectorBoardState(
                state.n(),                                                  // 棋盘规模不变
                state.columnMask() | (1 << col),                            // 点亮第 col 列
                state.diagDownMask() | (1 << (row - col + state.n() - 1)),  // 点亮 ↘ 对角线
                state.diagUpMask() | (1 << (row + col)),                    // 点亮 ↙ 对角线
                state.depth() + 1                                           // 已放皇后数 +1
        );
    }
}
