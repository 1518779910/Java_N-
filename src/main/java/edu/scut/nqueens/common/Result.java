/* ============================================================
 * 文件：Result.java
 * 所在包：edu.scut.nqueens.common（全组共用，不属于任何单一架构风格）
 *
 * 本文件实现的架构风格：不适用（全组共用的数据表示层）
 * 本文件承担的核心组件/连接器：
 *   - 组件：结果条目（不可变值对象）
 *     对应管道-过滤器 C&C 图中 Pipe 3 上流动的 Result
 *   - 连接器：无
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本类为不可变对象（record），字段不可修改：______
 * [ ] 本类不依赖任何一种架构风格的包：______
 * [ ] 本类只承载数据，不含任何求解或输出逻辑：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.common;

import java.util.Objects;

/**
 * 结果条目：一个完整解 + 它在本次运行中的输出序号。
 *
 * <p><b>为什么要单独有这个类型（而不是直接把 Solution 送进输出过滤器）：</b>
 * "第几个解"这种记账属于<b>收集</b>的职责，由解集收集过滤器（CollectorFilter）负责；
 * 输出过滤器（OutputFilter）只负责把拿到的东西格式化，不需要自己维护计数器。
 * 两种职责分开之后，OutputFilter 变成纯粹的无状态组件——这正是管道-过滤器
 * "每个过滤器只做一件事"的体现，也方便把它换成"写文件""写 CSV"等其它实现。
 *
 * <p>{@code record}：Java 16+ 的不可变数据载体，编译器自动生成
 * 构造器、访问器、equals/hashCode/toString，天然满足架构约束。
 *
 * @param index    解在本次运行中的序号（从 1 开始）
 * @param solution 完整解
 */
public record Result(int index, Solution solution) {

    public Result {
        Objects.requireNonNull(solution, "solution 不能为 null");
        if (index < 1) {
            throw new IllegalArgumentException("解序号从 1 开始，实际为 " + index);
        }
    }

    /** 棋盘规模 n。 */
    public int n() {
        return solution.n();
    }

    @Override
    public String toString() {
        return "#" + index + " [" + solution + "]";
    }
}
