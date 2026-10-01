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

import edu.scut.nqueens.common.BitVectorBoardState;
import edu.scut.nqueens.common.BitVectorPruner;
import edu.scut.nqueens.common.PartialSolution;

/**
 * 对角线冲突检查知识源（KS_DiagCheck）。
 *
 * <p>本实现中它承担"<b>死路检测</b>"职责：判断栈顶那条路径在下一行是否还有
 * 任何一个不冲突的位置；若一个都没有，说明这条路径已被约束堵死，
 * 把它从黑板上取走——它放不满 N 行，不可能参与任何解。
 *
 * <p>它与 KS_ColCheck 之间<b>没有</b>任何代码或调用关系：各自独立地读黑板、
 * 各自独立地把结论写回黑板，谁先执行、是否执行过都由控制器决定，彼此互不知情。
 * 这正是"知识源之间绝对无直接通信"的含义。
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

    /**
     * 触发条件：栈顶这条路径<b>已经无路可走</b>——它的下一行不论选哪一列，
     * 都会被列或对角线约束拒绝。
     *
     * <p>这条规则只读黑板：它看的是"这条路径还能不能继续"，
     * 与"谁把这条路径放上黑板的""KS_ColCheck 是否跑过"完全无关。
     */
    @Override
    public boolean canHandle(BlackboardState blackboard) {
        PartialSolution top = blackboard.peekCandidate();
        return top != null && !top.isComplete() && !hasAnyContinuation(top);
    }

    /**
     * 把这条被约束堵死的路径从黑板上取走（丢弃）。
     * 它放不满 N 行，因此不可能参与任何一个解——丢掉不会漏解。
     */
    @Override
    public void execute(BlackboardState blackboard) {
        blackboard.takeCandidate();
    }

    /**
     * 判断一条路径在下一行是否还存在至少一个不冲突的放置位置。
     *
     * <p>注意这里用的是路径<b>自己携带</b>的位向量状态（{@link PartialSolution#state()}），
     * 而不是黑板上的 {@code boardState}——因为要判断的正是这条路径的可行性，
     * 而不是"当前正在扩展的那条路径"的可行性。
     */
    private boolean hasAnyContinuation(PartialSolution path) {
        // row == 0 的起点还没建过位向量状态，先补一个空棋盘（N 列全空，必然有出路）
        BitVectorBoardState state =
                path.state() != null ? path.state() : pruner.initial(path.n());

        int row = path.row();
        for (int col = 0; col < path.n(); col++) {
            if (pruner.canPlace(state, row, col)) {
                return true;
            }
        }
        return false;
    }
}
