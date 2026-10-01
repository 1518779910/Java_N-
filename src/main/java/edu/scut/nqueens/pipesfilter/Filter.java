/* ============================================================
 * 文件：Filter.java
 * 本文件实现的架构风格：管道-过滤器（Pipes and Filters）
 * 本文件承担的核心组件/连接器：
 *   - 组件：所有过滤器的抽象基类（定义过滤器的统一形状）
 *   - 连接器：持有管道（Pipe）的读端 in 与写端 out
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

/**
 * 过滤器（组件）的统一基类。
 *
 * <p><b>架构约束（本基类的形状就是为了让违规写不出来）：</b>
 * <ol>
 *   <li>过滤器的全部状态只有 in / out 两个管道引用与自己的私有字段，
 *       <b>不持有其它过滤器</b>，因此不存在"过滤器 A 直接调用过滤器 B"的可能；</li>
 *   <li>过滤器之间只能通过管道（{@link Pipe}）通信，数据一律"从 in 读、往 out 写"；</li>
 *   <li>{@link #run()} 是 {@code final} 的：子类只能实现 {@link #process}，
 *       无法绕开管道读写去直接搬运数据；</li>
 *   <li>源过滤器（{@link GeneratorFilter}）的 {@code in} 为 {@code null}，
 *       汇过滤器（{@link OutputFilter}）的 {@code out} 为 {@code null}。</li>
 * </ol>
 *
 * <p><b>数据流的终止（管道-过滤器的经典问题）：</b>过滤器无法预知上游何时不再产出数据，
 * 因此必须显式传递"流结束标记"（EOS）。约定：主循环读到 EOS 后，先把它转发给下游管道，
 * 再结束自己的线程——这样 EOS 会沿着管道链一路传播，最后一个过滤器收到后即可收尾。
 * 一个过滤器可能有多个上游时（本项目不会出现），需要额外计数；本工程的管道链是线性的。
 *
 * @param <I> 输入数据类型
 * @param <O> 输出数据类型
 */
public abstract class Filter<I, O> implements Runnable {

    private final String name;
    private final Pipe<I> in;
    private final Pipe<O> out;

    protected Filter(String name, Pipe<I> in, Pipe<O> out) {
        this.name = name;
        this.in = in;
        this.out = out;
    }

    /** 过滤器名称，仅用于日志输出，不参与任何逻辑判断。 */
    public final String name() {
        return name;
    }

    /** 上游管道（读端）；源过滤器为 null。 */
    protected final Pipe<I> in() {
        return in;
    }

    /** 下游管道（写端）；汇过滤器为 null。 */
    protected final Pipe<O> out() {
        return out;
    }

    /**
     * 处理一个输入数据项，向下游写出 0..n 个数据项（扇出是允许的，
     * 例如校验过滤器把 1 个部分解扩展成多个合法的子部分解）。
     *
     * <p><b>实现中禁止：</b>调用其它过滤器的方法、访问其它过滤器的字段、使用静态可变变量。
     *
     * @param item 从 in 读到的一个数据项；源过滤器传入 null
     */
    protected abstract void process(I item) throws InterruptedException;

    /**
     * 过滤器主循环（由基类固定，子类不得覆写）。
     *
     * <p>TODO（组员按自己负责的过滤器逐个实现 process 即可，主循环只需实现一次）：
     * <pre>
     *   若 in == null：                 // 源过滤器
     *       调用一次 process(null)，结束
     *   否则：
     *       循环 { item = in.take(); 若 item 是 EOS → 转发给 out 并跳出循环;
     *             否则 process(item); }
     * </pre>
     */
    @Override
    public final void run() {
        throw new UnsupportedOperationException("TODO: 实现过滤器主循环：" + name);
    }
}
