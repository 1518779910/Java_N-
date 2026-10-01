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
 * 解完整性判定知识源（KS_SolutionCheck）。
 *
 * <p>它不校验列或对角线（那是另外两个知识源的规则），只关心"是否已放满 N 行"；
 * 放满就把它转成 {@code Solution} 写入结果区。
 *
 * <p>之所以走到这里的必定是合法解，不是因为它调用过谁，而是因为
 * <b>黑板上只可能存在已通过剪枝的路径</b>——不合法的路径根本不会被放上来。
 * 这是"知识源互不通信，却仍能保证结果正确"的关键。
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
