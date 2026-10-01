/* ============================================================
 * 文件：CollectorFilter.java
 * 本文件实现的架构风格：管道-过滤器（Pipes and Filters）
 * 本文件承担的核心组件/连接器：
 *   - 组件：解集收集过滤器（CollectorFilter）
 *   - 连接器：上游 ValidatorFilter 的 in 管道（Pipe 2：BlockingQueue<ValidSolution>）；
 *             下游 OutputFilter 的 out 管道（Pipe 3：BlockingQueue<Result>）
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本文件未使用任何全局共享可变状态：______
 * [ ] 本文件未直接 import / 调用其他过滤器的内部函数：______
 * [ ] 本文件所有输入均来自管道读端，所有输出均写入管道写端：______
 * [ ] 过滤器之间不共享数据结构、不共享队列句柄以外的状态：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.pipesfilter;

import java.util.concurrent.atomic.AtomicLong;

import edu.scut.nqueens.common.Result;
import edu.scut.nqueens.common.Solution;

/**
 * 解集收集过滤器：判定完整解、计数，并把解对象写向下游输出过滤器。
 *
 * <p><b>关于 {@code collectedCount} 这个可变字段：</b>它是<b>过滤器私有</b>的状态，
 * 只有本过滤器所在的线程会读写它，因此不违反"过滤器之间不共享可变状态"的约束。
 * 判断标准是"这个变量有没有被两个过滤器同时访问"，而不是"是不是可变"。
 * 用 {@link AtomicLong} 是为了让主线程在管道跑完后能安全地读取计数用于实验日志。
 *
 * <p>TODO：实现 process——
 * <ol>
 *   <li>若 {@code item.isComplete()} 为 false，说明上游逻辑异常，直接抛出 IllegalStateException
 *       （不要静默丢弃：静默丢弃会让实验数据悄悄少掉，是比崩溃更难查的错误）；</li>
 *   <li>否则 {@code item.partial().toSolution()} 得到完整解，按
 *       {@code new Result(序号, 解)} 包装后写出下游，并让 collectedCount 自增。</li>
 * </ol>
 */
public final class CollectorFilter extends Filter<ValidSolution, Result> {

    private final AtomicLong collectedCount = new AtomicLong();

    /**
     * @param name 过滤器名称
     * @param in   上游管道（Pipe 2：承载 ValidSolution）
     * @param out  下游管道（Pipe 3：承载 Result）
     */
    public CollectorFilter(String name, Pipe<ValidSolution> in, Pipe<Result> out) {
        super(name, in, out);
    }

    /** 已收集的完整解数量（供主线程在管道结束后读取）。 */
    public long collectedCount() {
        return collectedCount.get();
    }

    @Override
    protected void process(ValidSolution item) throws InterruptedException {
        throw new UnsupportedOperationException(
                "TODO: 判定完整解、按序号包装成 Result 写向下游，同时累计解的数量（当前已收集 "
                        + collectedCount.get() + " 个）");
    }
}
