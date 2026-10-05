/* ============================================================
 * 文件：NQueensMapper.java
 * 本文件实现的架构风格：Map-Reduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：N 皇后 Mapper 实现——Map 阶段的具体求解逻辑
 *     对应 C&C 图中的"Mapper ×N：按第 0 行列号切分搜索空间"
 *   - 连接器：经 Emitter（BiConsumer）把 (firstRowCol, Solution) 发给 Shuffle
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [是] 本文件不持有 Shuffle / Reduce 的任何引用：只持有无状态 pruner
 * [是] 全部输出经 Emitter 传出，不直接写共享集合：emitter 由框架注入
 * [是] 剪枝逻辑全部委托给 common 包的 BitVectorPruner，本文件不另写剪枝：调用 pruner
 * [是] 本文件不依赖其它架构风格的包（pipesfilter / blackboard / callreturn）：仅依赖 common
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：无；用于任务：无；
 * 自行修改内容：本人独立实现；声明人（手写签名）：成员D
 * ============================================================ */
package edu.scut.nqueens.mapreduce;

import java.util.function.BiConsumer;

import edu.scut.nqueens.common.BitVectorPruner;
import edu.scut.nqueens.common.PartialSolution;
import edu.scut.nqueens.common.Solution;

/**
 * N 皇后 Mapper：Map 阶段的具体实现。
 *
 * <p><b>输入分片：</b>一个<b>已放置第 0 行皇后</b>的 {@link PartialSolution}。
 * Driver（{@link MapReduceMain}）在装配时把第 0 行的 N 个列号各做成一个分片，
 * 因此 Mapper 的总数 = N，每个 Mapper 处理一个起点。
 *
 * <p><b>Map 的职责：</b>从输入分片（第 0 行已放好）出发，递归搜索剩余 N-1 行的
 * 全部合法摆放，每凑成一个完整解就 {@code emit(firstRowColumn, Solution)}。
 *
 * <p><b>为什么不同 Mapper 的解集互不相交：</b>第 0 行的列号互不相同，
 * 而一个完整解的第 0 行列号是唯一的——所以两个不同 Mapper 不可能产出同一个解。
 * 这意味着 Shuffle 之后<b>不需要去重</b>，每个 key 下面的值列表天然无重复。
 *
 * <p><b>剪枝策略等价性：</b>本类的递归展开调用 {@link PartialSolution#place}，
 * 后者内部调用全组统一的 {@link BitVectorPruner#canPlace} 与
 * {@link BitVectorPruner#place}——与其它四种架构调用的是同一份实现，
 * 剪枝策略等价性在结构上得到保证。
 *
 * <p><b>无状态设计：</b>本类仅持有 {@code pruner}（无状态），因此同一个 Mapper
 * 实例可以被多个线程并发调用，不产生共享可变状态。
 */
public final class NQueensMapper implements Mapper<PartialSolution, Integer, Solution> {

    private final BitVectorPruner pruner;

    /**
     * @param pruner 全组统一的位向量剪枝实现（无状态，可被多个 Mapper 共享）
     */
    public NQueensMapper(BitVectorPruner pruner) {
        this.pruner = java.util.Objects.requireNonNull(pruner, "pruner 不能为 null");
    }

    /** 全组统一的剪枝实现。 */
    public BitVectorPruner pruner() {
        return pruner;
    }

    @Override
    public void map(PartialSolution input, BiConsumer<Integer, Solution> emitter) {
        // 输入分片已放好第 0 行，从第 1 行开始递归搜索
        backtrack(input, emitter);
    }

    /**
     * 递归回溯：在 {@code ps} 的基础上尝试放置下一行的皇后。
     *
     * <p>与回溯版（{@code RecursiveQueensSolver}）的算法完全等价，
     * 区别仅在于"收集到解后写回 emitter"而不是"写回上层调用者"。
     *
     * <p>key 用 {@code ps.columnAt(0)}——第 0 行皇后的列号。
     * 一个完整解的第 0 行列号是唯一的，因此不同 Mapper 产出的解的 key 不会相同。
     */
    private void backtrack(PartialSolution ps, BiConsumer<Integer, Solution> emitter) {
        if (ps.isComplete()) {
            // 放满 N 行 → 凑成一个完整解，emit 给 Shuffle
            emitter.accept(ps.columnAt(0), ps.toSolution());
            return;
        }
        for (int col = 0; col < ps.n(); col++) {
            PartialSolution next = ps.place(col, pruner);
            if (next != null) {
                backtrack(next, emitter);
            }
        }
    }
}
