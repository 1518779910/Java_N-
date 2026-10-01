/* ============================================================
 * 文件：KS_DiagCheck.java
 * 本文件实现的架构风格：黑板架构（Blackboard）
 * 本文件承担的核心组件/连接器：
 *   - 组件：对角线冲突检查知识源（KS_DiagCheck）——三要素之二的实例之一
 *     对应 C&C 图中的"KS_DiagCheck 对角线冲突检查知识源"
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

/**
 * 对角线冲突检查知识源（KS_DiagCheck）：判断待放置的位置是否与已放置的皇后在同一条对角线上。
 *
 * <p>两条对角线（↘ 与 ↙）在位向量中各占一组比特位，检查方式与列检查一样是三次按位与之一，
 * 具体实现委托给全组统一的 {@link BitVectorPruner}。
 *
 * <p><b>与 KS_ColCheck 的关系：</b>二者<b>没有</b>任何代码或调用关系——
 * 它们各自独立地读黑板、各自独立地把结论写回黑板，谁先执行、是否执行过，
 * 都由控制器决定，彼此互不知情。这正是"知识源之间绝对无直接通信"的含义，
 * 也是答辩最可能追问的点，请在 README 中把你的解释写清楚。
 *
 * <p>TODO（两个方法都要实现）：
 * <ul>
 *   <li>{@link #canHandle}：只看黑板当前状态判断"是否有待检查的候选"；</li>
 *   <li>{@link #execute}：对候选执行对角线冲突检查，把结论写回黑板
 *       （写回方式须与 KS_ColCheck 的约定一致，但两个类之间不得互相引用）。</li>
 * </ul>
 */
public final class KS_DiagCheck implements KnowledgeSource {

    private final BitVectorPruner pruner;

    /** @param pruner 全组统一的位向量剪枝实现（无状态，可被多个知识源共享） */
    public KS_DiagCheck(BitVectorPruner pruner) {
        this.pruner = pruner;
    }

    public BitVectorPruner pruner() {
        return pruner;
    }

    @Override
    public String name() {
        return "KS_DiagCheck";
    }

    @Override
    public boolean canHandle(BlackboardState blackboard) {
        throw new UnsupportedOperationException("TODO: 判断黑板当前是否有需要做对角线冲突检查的候选（只看黑板）");
    }

    @Override
    public void execute(BlackboardState blackboard) {
        throw new UnsupportedOperationException("TODO: 用 pruner 检查两条对角线冲突，把结论写回黑板");
    }
}
