/* ============================================================
 * 文件：Reducer.java
 * 本文件实现的架构风格：Map-Reduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：Reducer 接口——Reduce 阶段的抽象
 *     对应 Map-Reduce 模型中的"Reduce 阶段：按 key 聚合，输出最终结果"
 *   - 连接器：通过 Emitter（BiConsumer）把最终结果传给 Driver
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [是] 本接口不持有 Mapper / Shuffle 的任何引用：接口只定义 reduce 方法签名
 * [是] 本接口的全部输入来自 Shuffle 分组，全部输出经 Emitter 传出：List<V> 输入 + emitter 输出
 * [是] Reducer 之间不互相调用、不共享可变状态：每个 Reducer 实例只处理一个 key
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：无；用于任务：无；
 * 自行修改内容：本人独立实现；声明人（手写签名）：成员D
 * ============================================================ */
package edu.scut.nqueens.mapreduce;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Reducer：Map-Reduce 模型中 Reduce 阶段的抽象接口。
 *
 * <p><b>Reduce 阶段的职责：</b>接收一个 {@code (key, [v1, v2, ...])} 分组，
 * 对该分组的所有值做聚合处理，通过 {@code emitter} 发出 0..n 个最终结果。
 *
 * <p><b>设计约束：</b>
 * <ul>
 *   <li>Reducer 之间不共享可变状态——本接口不持有 Mapper/Shuffle 的任何引用；</li>
 *   <li>一个 Reducer 调用只处理一个 key 的分组
 *       （多 key 并行靠多个 Reducer 调用，由框架的线程池调度）；</li>
 *   <li>最终结果只经 {@code emitter} 传出，不直接写共享集合。</li>
 * </ul>
 *
 * <p><b>在 N 皇后问题中的角色：</b>
 * Reducer 接收 {@code (firstRowColumn, [Solution1, Solution2, ...])} 分组，
 * 把同一列号下的所有解<b>按字典序排序</b>后逐个 emit。
 * 排序是为了保证输出顺序与其它四种架构一致——
 * 搜索空间按第 0 行列号切分后，每组的解都按列号字典序输出，
 * 整体顺序与回溯版（第 0 行列号从 0 到 N-1，组内按递归展开顺序）一致。
 *
 * @param <K>   分组键类型
 * @param <V>   中间值类型
 * @param <OUT> 最终输出类型
 */
@FunctionalInterface
public interface Reducer<K, V, OUT> {

    /**
     * 处理一个 key 分组，发出 0..n 个最终结果。
     *
     * @param key    分组键
     * @param values 该 key 下的所有中间值（Shuffle 后聚合的列表）
     * @param emitter 最终结果发射器（线程安全，由框架注入）
     */
    void reduce(K key, List<V> values, BiConsumer<K, OUT> emitter);
}
