/* ============================================================
 * 文件：ValidSolution.java
 * 本文件实现的架构风格：管道-过滤器（Pipes and Filters）
 * 本文件承担的核心组件/连接器：
 *   - 组件：通过校验的部分解（不可变数据项）
 *     对应 C&C 图中 Pipe 2 上流动的 ValidSolution
 *   - 连接器：无（由 Pipe 2 承载）
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本文件未使用任何全局共享可变状态：______
 * [ ] 本文件未直接 import / 调用任何过滤器的内部函数：______
 * [ ] 本文件为不可变对象，可在管道中安全传递：______
 * [ ] 过滤器之间不共享数据结构、不共享队列句柄以外的状态：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.pipesfilter;

import java.util.Objects;

import edu.scut.nqueens.common.BitVectorBoardState;
import edu.scut.nqueens.common.PartialSolution;

/**
 * 通过位向量校验的部分解：管道-过滤器架构中 Pipe 2 上流动的数据项。
 *
 * <p><b>它为什么是独立类型：</b>管道的类型参数就是过滤器的"契约"。
 * {@code Pipe<PartialSolution>}（Pipe 1）表示"尚未校验的候选"，
 * {@code Pipe<ValidSolution>}（Pipe 2）表示"已确认不与已放置皇后冲突的部分解"。
 * 类型一换，下游收集过滤器就不需要再怀疑上游是否漏了校验，
 * 编译期就把"未经校验的数据流到下游"这种错误挡掉了——
 * 这是用类型表达架构约束的常用手法，也便于在 UML 类图上区分两个阶段的数据。
 *
 * <p>它是纯粹的包装，不复制内部数据；被包装的 {@link PartialSolution} 本身不可变。
 *
 * @param partial 已通过校验的部分解（可能已放满 N 行，也可能是中间状态）
 */
public record ValidSolution(PartialSolution partial) {

    public ValidSolution {
        Objects.requireNonNull(partial, "partial 不能为 null");
    }

    /** 已放置的行数（当前搜索深度）。 */
    public int row() {
        return partial.row();
    }

    /** 棋盘规模 n。 */
    public int n() {
        return partial.n();
    }

    /** 是否已放满 N 行（是完整解，交由收集过滤器判定与收集）。 */
    public boolean isComplete() {
        return partial.isComplete();
    }

    /** 当前的位向量棋盘状态。 */
    public BitVectorBoardState state() {
        return partial.state();
    }

    @Override
    public String toString() {
        return "ValidSolution[" + partial + "]";
    }
}
