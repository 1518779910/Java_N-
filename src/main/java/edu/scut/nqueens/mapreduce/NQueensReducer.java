/* ============================================================
 * 文件：NQueensReducer.java
 * 本文件实现的架构风格：Map-Reduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：N 皇后 Reducer 实现——Reduce 阶段的具体聚合逻辑
 *     对应 C&C 图中的"Reducer ×M：按 firstRowCol 聚合，输出排序后的解"
 *   - 连接器：经 Emitter（BiConsumer）把最终解发给 Driver
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [是] 本文件不持有 Mapper / Shuffle 的任何引用：无任何字段
 * [是] 全部输入来自 Shuffle 分组，全部输出经 Emitter 传出：reduce 方法签名
 * [是] Reducer 之间不互相调用、不共享可变状态：本类无状态
 * [是] 本文件不依赖其它架构风格的包：仅依赖 common 包的 Solution
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：无；用于任务：无；
 * 自行修改内容：本人独立实现；声明人（手写签名）：成员D
 * ============================================================ */
package edu.scut.nqueens.mapreduce;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

import edu.scut.nqueens.common.Solution;

/**
 * N 皇后 Reducer：Reduce 阶段的具体实现。
 *
 * <p><b>输入：</b>Shuffle 分组后的 {@code (firstRowColumn, [Solution1, Solution2, ...])}。
 * 同一个 key（第 0 行列号）下的所有解都在同一个分组里。
 *
 * <p><b>职责：</b>
 * <ol>
 *   <li>把同一组内的解按<b>字典序</b>排序——
 *       保证不同 Reducer 的输出拼接后，整体顺序与其它四种架构一致；</li>
 *   <li>逐个 emit 给 Driver。</li>
 * </ol>
 *
 * <p><b>为什么要在 Reduce 里排序而不是在 Driver 里统一排：</b>
 * "按 key 分组后组内排序"是 Map-Reduce 模型里 Reducer 的典型职责
 * （分布式 Map-Reduce 中 Reducer 输出通常写到分区文件，组内有序是常见约定）。
 * 在 Driver 里再统一按 key 排一次组间顺序即可，组内顺序由 Reducer 保证。
 *
 * <p><b>无状态设计：</b>本类不持有任何字段，可以被多个 Reducer 线程共享。
 */
public final class NQueensReducer implements Reducer<Integer, Solution, Solution> {

    /** N 皇后解的字典序比较器：从第 0 行开始逐行比较列号。 */
    private static final Comparator<Solution> LEXICOGRAPHIC =
            Comparator.nullsFirst((a, b) -> {
                int n = a.n();
                for (int i = 0; i < n; i++) {
                    int diff = Integer.compare(a.columnAt(i), b.columnAt(i));
                    if (diff != 0) {
                        return diff;
                    }
                }
                return 0;
            });

    @Override
    public void reduce(Integer key, List<Solution> values, BiConsumer<Integer, Solution> emitter) {
        // 组内按字典序排序：保证输出顺序与其它四种架构一致
        List<Solution> sorted = new ArrayList<>(values);
        sorted.sort(LEXICOGRAPHIC);
        for (Solution solution : sorted) {
            emitter.accept(key, solution);
        }
    }
}
