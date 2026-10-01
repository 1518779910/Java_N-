/* ============================================================
 * 文件：KS_ColCheck.java
 * 本文件实现的架构风格：黑板架构（Blackboard）
 * 本文件承担的核心组件/连接器：
 *   - 组件：列冲突检查知识源（KS_ColCheck）——三要素之二的实例之一
 *     对应 C&C 图中的"KS_ColCheck 列冲突检查知识源"
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

import edu.scut.nqueens.common.BitVectorPruner;
import edu.scut.nqueens.common.PartialSolution;

/**
 * 列冲突检查知识源（KS_ColCheck）。
 *
 * <p>本实现中它承担"<b>扩展</b>"职责：弹出栈顶那条路径，对下一行的每一列调用
 * 全组统一的 {@link BitVectorPruner} 判定并生成子路径，通过者压回黑板工作区。
 * 它是最容易被满足的一条规则，因此作为搜索推进的入口。
 *
 * <p>它只读黑板、只写黑板，不持有也不调用任何其它知识源。
 */
public final class KS_ColCheck implements KnowledgeSource {

    private final BitVectorPruner pruner;

    /** @param pruner 全组统一的位向量剪枝实现（无状态，可被多个知识源共享） */
    public KS_ColCheck(BitVectorPruner pruner) {
        this.pruner = pruner;
    }

    public BitVectorPruner pruner() {
        return pruner;
    }

    @Override
    public String name() {
        return "KS_ColCheck";
    }

    /**
     * 触发条件：黑板上还有未放满的候选路径可以继续扩展。
     * 只读黑板，不修改任何东西——因此用 {@code peekCandidate} 而不是 {@code takeCandidate}。
     */
    @Override
    public boolean canHandle(BlackboardState blackboard) {
        PartialSolution top = blackboard.peekCandidate();
        return top != null && !top.isComplete();
    }

    /**
     * 取出栈顶这条路径，把它的下一行展开成 N 个候选：
     * 每一列都交给全组统一的 {@link BitVectorPruner} 做剪枝（列 + 两条对角线一次判定），
     * 通过的子路径压回工作区，冲突的返回 {@code null} 直接丢弃。
     */
    @Override
    public void execute(BlackboardState blackboard) {
        PartialSolution current = blackboard.takeCandidate();
        if (current == null || current.isComplete()) {
            return;   // canHandle 已保证不会走到这里，防御性返回
        }

        // 把"当前正在扩展的路径"同步到黑板的棋盘位向量与搜索深度上，
        // 让黑板内容与 C&C 图的标注保持一致（这一步是黑板内容的变更，会影响 revision）
        blackboard.updateBoardState(
                current.state() != null ? current.state() : pruner.initial(current.n()));
        blackboard.setDepth(current.row());

        // 展开下一行：等价于递归回溯里的 for (int col = 0; col < n; col++)
        int row = current.row();
        for (int col = 0; col < current.n(); col++) {
            PartialSolution child = current.place(col, pruner);
            if (child != null) {
                blackboard.publishCandidate(child);
            }
        }
    }
}
