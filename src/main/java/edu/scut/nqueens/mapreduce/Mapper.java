/* ============================================================
 * 文件：Mapper.java
 * 本文件实现的架构风格：Map-Reduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：Mapper 接口——Map 阶段的抽象
 *     对应 Map-Reduce 模型中的"Map 阶段：把输入分片映射成中间 <K,V> 对"
 *   - 连接器：通过 Emitter（BiConsumer）把中间结果传给 Shuffle
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [是] 本接口不持有 Shuffle / Reduce 的任何引用：接口只定义 map 方法签名
 * [是] 本接口的全部输出经 Emitter 传递，不直接写共享可变集合：emitter 由框架注入
 * [是] Mapper 之间不互相调用、不共享可变状态：Mapper 实现应为无状态或仅持有无状态依赖
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：无；用于任务：无；
 * 自行修改内容：本人独立实现；声明人（手写签名）：成员D
 * ============================================================ */
package edu.scut.nqueens.mapreduce;

import java.util.function.BiConsumer;

/**
 * Mapper：Map-Reduce 模型中 Map 阶段的抽象接口。
 *
 * <p><b>Map 阶段的职责：</b>把一个输入分片（input split）转换成 0..n 个
 * 中间 {@code (key, value)} 对，通过 {@code emitter} 发给 Shuffle 阶段。
 *
 * <p><b>设计约束：</b>
 * <ul>
 *   <li>Mapper 之间不共享可变状态——本接口不持有 Shuffle/Reduce 的任何引用；</li>
 *   <li>中间结果只经 {@code emitter} 传出，不直接写共享集合
 *       （emitter 由框架注入，线程安全由框架保证）；</li>
 *   <li>Mapper 实现可以是同一个对象被多线程调用（无状态），
 *       也可以每任务一个实例，由框架决定——接口不强制。</li>
 * </ul>
 *
 * <p><b>在 N 皇后问题中的角色：</b>
 * 每个 Mapper 接收一个<b>已放置第 0 行皇后</b>的部分解作为输入分片，
 * 递归搜索剩余 N-1 行的全部合法摆放，每凑成一个完整解就
 * {@code emit(firstRowColumn, Solution)}。
 * 由于第 0 行的列号互不相同，不同 Mapper 产出的解集互不相交——
 * 这是"按第 0 行列号切分搜索空间"的天然划分，无需额外去重。
 *
 * @param <IN>  输入分片类型
 * @param <K>   中间键类型（Shuffle 分组依据）
 * @param <V>   中间值类型
 */
@FunctionalInterface
public interface Mapper<IN, K, V> {

    /**
     * 处理一个输入分片，发出 0..n 个 {@code (key, value)} 对。
     *
     * @param input   输入分片
     * @param emitter 中间结果发射器（线程安全，由框架注入）
     */
    void map(IN input, BiConsumer<K, V> emitter);
}
