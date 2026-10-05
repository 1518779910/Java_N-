/* ============================================================
 * 文件：KeyValuePair.java
 * 本文件实现的架构风格：Map-Reduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：中间数据 <Key, Value> 对（不可变值对象）
 *     对应 Map-Reduce 模型中 Map 输出 / Shuffle 分组 / Reduce 输入的键值对
 *   - 连接器：无（由 Shuffle 阶段的分组结构承载）
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [是] 本类为不可变对象（record），字段不可修改：所有字段 final，无 setter
 * [是] 本类不依赖任何架构风格包（pipesfilter / blackboard / callreturn）：仅依赖 JDK
 * [是] 本类只承载数据，不含任何求解或剪枝逻辑：纯数据载体
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：无；用于任务：无；
 * 自行修改内容：本人独立实现；声明人（手写签名）：成员D
 * ============================================================ */
package edu.scut.nqueens.mapreduce;

import java.util.Objects;

/**
 * Map-Reduce 模型中的键值对（KeyValuePair）。
 *
 * <p>Map 阶段产出的每一个 {@code (key, value)} 都用它来包装；
 * Shuffle 阶段按 {@code key} 分组时把它作为分组依据；
 * Reduce 阶段消费 {@code (key, [value1, value2, ...])} 的列表。
 *
 * <p>使用 {@code record}：不可变、自动生成构造器/访问器/equals/hashCode/toString，
 * 天然满足"Map 与 Reduce 之间不共享可变状态"的约束——
 * 同一个 KeyValuePair 可以被多个线程安全读取。
 *
 * @param key   键（Map 输出键，Shuffle 分组键，Reduce 输入键）
 * @param value 值（Map 输出值，Reduce 输入值列表的一个元素）
 */
public record KeyValuePair<K, V>(K key, V value) {

    public KeyValuePair {
        Objects.requireNonNull(key, "key 不能为 null");
        Objects.requireNonNull(value, "value 不能为 null");
    }
}
