/* ============================================================
 * 文件：Pipe.java
 * 本文件实现的架构风格：管道-过滤器（Pipes and Filters）
 * 本文件承担的核心组件/连接器：
 *   - 组件：无
 *   - 连接器：管道（Pipe）——过滤器之间唯一的通信通道，内部是阻塞队列
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本文件未使用任何全局共享可变状态：______
 * [ ] 本文件未直接 import / 调用任何过滤器的内部函数：______
 * [ ] 本文件只提供 put / take / close 三个接口，不暴露队列本身：______
 * [ ] 过滤器之间只通过本类传递数据，不共享本类以外的任何状态：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.pipesfilter;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 管道（连接器）：两端分别是上游过滤器的写端与下游过滤器的读端。
 *
 * <p>本工程用 {@link BlockingQueue} 实现管道：
 * <ul>
 *   <li>队列满时上游 {@code put} 阻塞，队列空时下游 {@code take} 阻塞，
 *       天然形成"生产者-消费者"的背压（back pressure），不需要额外的同步代码；</li>
 *   <li>通道容量建议取一个小值（如 64）而不是无界队列：
 *       无界队列会让管道退化成"内存中的数组"，失去"流"的语义，且掩盖下游的消费瓶颈。</li>
 * </ul>
 *
 * <p><b>本类是过滤器之间唯一的共享对象</b>：过滤器持有同一个 Pipe 实例的两个方向
 * （上游写、下游读），但管道内部的数据一旦被 take 就离开了共享区域，
 * 加上 {@code PartialSolution} / {@code Solution} 都是不可变对象，
 * 因此"过滤器之间不共享可变状态"这条约束成立。
 *
 * @param <T> 管道中流动的数据类型
 */
public final class Pipe<T> implements AutoCloseable {

    /**
     * 流结束标记（End Of Stream）。用私有哨兵对象而不是 null，原因有两个：
     * <ul>
     *   <li>{@code LinkedBlockingQueue} 不接受 null 元素；</li>
     *   <li>数据项本身可能是任意类型，只有"引用相等"才能区分"这是 EOS"和"这是恰好等于某个值的数据"。</li>
     * </ul>
     * 泛型在运行期被擦除，因此入队时要经过一次受检的强制转换（见 {@link #close()}）。
     */
    private static final Object EOS = new Object();

    private final String name;
    private final BlockingQueue<T> queue;

    /**
     * @param name     管道名称，用于日志与 C&amp;C 图对照（如 "Pipe 1"）
     * @param capacity 通道容量；<= 0 表示无界（不推荐，见类注释）
     */
    public Pipe(String name, int capacity) {
        this.name = name;
        this.queue = capacity <= 0
                ? new LinkedBlockingQueue<>()
                : new LinkedBlockingQueue<>(capacity);
    }

    public String name() {
        return name;
    }

    /**
     * 把数据写入管道（写端）。
     *
     * <p>队列满时自然阻塞——这就是背压（back pressure）：上游不会无限生产，
     * 生产速度被下游的消费速度自动限制住，不需要任何额外代码。
     */
    public void put(T item) throws InterruptedException {
        queue.put(item);
    }

    /**
     * 从管道读取数据（读端）。
     *
     * <p>队列空时自然阻塞，直到上游写入数据或放入 EOS。
     * 用引用比较 {@code item == EOS} 判断流结束：只有 {@link #close()} 放进去的那个
     * 哨兵对象会命中，数据项本身不可能与之相等。
     *
     * @return 数据项；上游已结束且队列已排空时返回 {@code Optional.empty()}
     */
    public Optional<T> take() throws InterruptedException {
        T item = queue.take();
        if (item == EOS) {
            return Optional.empty();
        }
        return Optional.of(item);
    }

    /**
     * 关闭管道写端：放入流结束标记（EOS），下游读到后会继续向下游转发。
     *
     * <p>注意这是"放在队列末尾"而不是"立刻切断"：队列里排在 EOS 前面的数据
     * 仍会被下游依次读到，EOS 只是告诉下游"这之后不会再有数据了"。
     * 顺序由此天然保证，不需要额外的同步。
     */
    @Override
    @SuppressWarnings("unchecked")
    public void close() {
        try {
            queue.put((T) EOS);
        } catch (InterruptedException e) {
            // close() 是 AutoCloseable 的约定，不能抛出 InterruptedException；
            // 保留中断状态，交给上层察觉。
            Thread.currentThread().interrupt();
        }
    }
}
