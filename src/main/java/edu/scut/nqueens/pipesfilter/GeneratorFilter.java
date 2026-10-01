/* ============================================================
 * 文件：GeneratorFilter.java
 * 本文件实现的架构风格：管道-过滤器（Pipes and Filters）
 * 本文件承担的核心组件/连接器：
 *   - 组件：生成过滤器（GeneratorFilter）——数据源，产生第 0 行的候选部分解
 *   - 连接器：与下游 ValidatorFilter 之间的 out 管道（Pipe 1）
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

import edu.scut.nqueens.common.PartialSolution;

/**
 * 生成过滤器：整条管道链的数据源，向下游写出第 0 行的候选部分解。
 *
 * <p>它是源过滤器，{@code in} 为 null，只在 {@code process} 中被调用一次。
 *
 * <p>TODO：写出 N 个候选（第 0 行的皇后可以落在任意一列）。
 * 第 0 行不存在冲突，因此这里<b>不做剪枝</b>——剪枝是下游 ValidatorFilter 的职责，
 * 这样两种职责不会混在一个过滤器里。
 */
public final class GeneratorFilter extends Filter<Void, PartialSolution> {

    private final int n;

    /**
     * @param name 过滤器名称（对应 C&amp;C 图中的"组件：GeneratorFilter"）
     * @param out  下游管道（对应 C&amp;C 图中的"连接器：Pipe 1"）
     * @param n    棋盘规模
     */
    public GeneratorFilter(String name, Pipe<PartialSolution> out, int n) {
        super(name, null, out);
        this.n = n;
    }

    /** 棋盘规模 n。 */
    public int n() {
        return n;
    }

    @Override
    protected void process(Void ignored) throws InterruptedException {
        throw new UnsupportedOperationException(
                "TODO: 向下游写出 " + n + " 个第 0 行的候选部分解（每个候选一行一列，均不做剪枝）");
    }
}
