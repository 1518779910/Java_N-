/* ============================================================
 * 文件：KS_SolutionCheck.java
 * 本文件实现的架构风格：黑板架构（Blackboard）
 * 本文件承担的核心组件/连接器：
 *   - 组件：解完整性判定知识源（KS_SolutionCheck）——三要素之二的实例之一
 *     对应 C&C 图中的"KS_SolutionCheck 解完整性判定知识源"
 *   - 连接器：与黑板存储区之间的 Data Access（读写状态）；
 *             与控制器之间的"监控与调度信号"（由控制器持有本对象，本类不反向持有控制器）
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本文件未持有、也未调用任何其它知识源：______
 * [ ] 本文件的全部输入来自黑板，全部输出写回黑板：______
 * [ ] 本文件不含黑板之外的共享可变状态：______
 * [ ] 本文件可被单独替换而不影响其它知识源：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.blackboard;

import edu.scut.nqueens.common.PartialSolution;

/**
 * 解完整性判定知识源（KS_SolutionCheck）：判断黑板上当前的状态是否已经构成一个完整解，
 * 若是则把解写入结果区，并推动搜索继续（或回退）。
 *
 * <p><b>本知识源是"多条规则共同收敛"的体现：</b>它不负责校验列或对角线
 * （那是 KS_ColCheck 与 KS_DiagCheck 的事），它只关心"深度是否已达 N"。
 * 如果前面的检查没通过，候选根本不会走到这一步——但这一点不是靠"调用关系"保证的，
 * 而是靠黑板上的状态约定保证的：<b>只有通过检查的候选才会被推进深度</b>。
 * 把这句话讲清楚，就回答了答辩里"知识源不通信，怎么保证顺序正确"的追问。
 *
 * <p>TODO（两个方法都要实现）：
 * <ul>
 *   <li>{@link #canHandle}：只看黑板当前状态判断"当前深度是否已达 N 且有未收集的完整路径"；</li>
 *   <li>{@link #execute}：构造 {@code Solution} 写入结果区，并把黑板状态推进到下一个候选
 *       （推进 / 回退的具体做法取决于你们在 README 里定下的黑板求解形态）。</li>
 * </ul>
 */
public final class KS_SolutionCheck implements KnowledgeSource {

    public KS_SolutionCheck() {
    }

    @Override
    public String name() {
        return "KS_SolutionCheck";
    }

    /**
     * 触发条件：栈顶这条路径已经放满 N 行，构成一个完整解。
     *
     * <p>它<b>不</b>检查列与对角线——那是另外两个知识源的规则。
     * 之所以走到这里的一定是合法解，不是因为它调用过谁，而是因为
     * <b>黑板上只可能存在已通过剪枝的路径</b>（不合法的路径根本不会被放上来）。
     * 这就是"知识源互不通信，却仍能保证结果正确"的答案。
     */
    @Override
    public boolean canHandle(BlackboardState blackboard) {
        PartialSolution top = blackboard.peekCandidate();
        return top != null && top.isComplete();
    }

    /**
     * 取出这条完整路径，转成 {@link edu.scut.nqueens.common.Solution} 写入结果区。
     * 取出即代表这条路径已处理完毕，工作区里不会再有它——
     * 后续搜索自然从栈里剩下的回溯点继续。
     */
    @Override
    public void execute(BlackboardState blackboard) {
        PartialSolution complete = blackboard.takeCandidate();
        if (complete == null || !complete.isComplete()) {
            return;   // canHandle 已保证不会走到这里，防御性返回
        }
        blackboard.publishSolution(complete.toSolution());
    }
}
