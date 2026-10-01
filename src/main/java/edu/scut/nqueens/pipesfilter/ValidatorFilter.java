/* ============================================================
 * 文件：ValidatorFilter.java
 * 本文件实现的架构风格：管道-过滤器（Pipes and Filters）
 * 本文件承担的核心组件/连接器：
 *   - 组件：校验过滤器（ValidatorFilter）——位向量约束校验
 *   - 连接器：上游 GeneratorFilter 的 in 管道（Pipe 1：BlockingQueue<PartialSolution>）；
 *             下游 CollectorFilter 的 out 管道（Pipe 2：BlockingQueue<ValidSolution>）
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

import edu.scut.nqueens.common.BitVectorPruner;
import edu.scut.nqueens.common.PartialSolution;

/**
 * 校验过滤器：对输入的部分解做"逐行扩展 + 位向量剪枝"，把合法的子部分解写向下游。
 *
 * <p><b>为什么校验与扩展在同一个过滤器里：</b>纯粹的管道-过滤器没有反馈回路，
 * 部分解只能沿着管道单向流动。如果让校验过滤器只负责"丢弃非法解"、另设一个扩展
 * 过滤器负责"往下多放一行"，那么合法部分解在链条上每前进一格只能增长一行，
 * 数据量会成倍膨胀却没有意义。把"枚举下一行的 N 个列 + 位向量剪枝"放在同一个过滤器里，
 * 一次处理就能把 1 个部分解扩展并剪枝成若干个合法的子部分解，扇出一棵搜索树。
 *
 * <p><b>它是整条链上唯一需要剪枝的组件</b>，且必须注入全组统一的
 * {@link BitVectorPruner} 实现，以保证与其它四种架构风格剪枝策略等价。
 *
 * <p>TODO：实现 process——
 * <ol>
 *   <li>若 {@code item.isComplete()}：用 {@code new ValidSolution(item)} 包一层直接写向下游
 *       （它已放满 N 行且逐行都通过了校验，交给收集过滤器判定）；</li>
 *   <li>否则枚举 col = 0..n-1：用 pruner.canPlace 判断，合法则 {@code item.place(col, pruner)}
 *       得到子部分解，包装成 {@code ValidSolution} 写向下游。</li>
 * </ol>
 */
public final class ValidatorFilter extends Filter<PartialSolution, ValidSolution> {

    private final BitVectorPruner pruner;

    /**
     * @param name   过滤器名称
     * @param in     上游管道（Pipe 1：承载 PartialSolution）
     * @param out    下游管道（Pipe 2：承载通过校验的 ValidSolution）
     * @param pruner 全组统一的位向量剪枝实现
     */
    public ValidatorFilter(String name, Pipe<PartialSolution> in, Pipe<ValidSolution> out, BitVectorPruner pruner) {
        super(name, in, out);
        this.pruner = pruner;
    }

    public BitVectorPruner pruner() {
        return pruner;
    }

    /**
     * 处理一个输入的部分解：把这条分支一路展开到底，每凑满 N 行就写一个完整解给下游。
     *
     * <p><b>"校验"和"扩展"为什么必须合并在同一个过滤器里（本架构的关键设计决策）：</b>
     * 纯管道-过滤器没有反馈回路，数据只能单向流动。如果把两者拆成两个过滤器，
     * 扩展出来的子部分解就<b>再也回不到校验过滤器</b>了——它只会顺着 Pipe 2 流到收集器，
     * 而收集器只会判定"这不是完整解"。想在链条上传回去，就必须引入反馈回路，
     * 那就不是管道-过滤器了。
     *
     * <p>所以一次 {@code process} 调用必须把这条分支<b>走到底</b>：
     * 在过滤器内部递归展开（{@link #expand}），只把<b>完整解</b>写向下游。
     * 这样一条输入数据会在管道末端扇出成若干个解——1 个部分解"扇出一棵搜索树"，
     * 而管道依然是单向无环的。答辩追问时按这个理由回答。
     */
    @Override
    protected void process(PartialSolution item) throws InterruptedException {
        expand(item);
    }

    /**
     * 递归展开一条部分解的全部合法续接。
     *
     * <p>换成递归回溯的写法对照：本方法就是
     * {@code if (完成) 收集; else for (col) if (canPlace) { place; 递归; }}，
     * 唯一的区别是"收集"变成了"写向下游管道"。
     *
     * <p>递归深度不超过 N（作业规模 12），不会栈溢出。
     */
    private void expand(PartialSolution partial) throws InterruptedException {
        if (partial.isComplete()) {
            // 已放满 N 行且逐行都通过了剪枝，是一个完整解，交给下游收集过滤器判定与编号
            out().put(new ValidSolution(partial));
            return;
        }

        // 枚举下一行的 N 个列，逐个交给全组统一的 pruner 判定；
        // PartialSolution.place 内部调用 pruner.canPlace，冲突时返回 null
        for (int col = 0; col < partial.n(); col++) {
            PartialSolution child = partial.place(col, pruner);
            if (child != null) {
                expand(child);
            }
        }
    }
}
