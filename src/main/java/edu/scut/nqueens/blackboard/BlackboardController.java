/* ============================================================
 * 文件：BlackboardController.java
 * 本文件实现的架构风格：黑板架构（Blackboard）
 * 本文件承担的核心组件/连接器：
 *   - 组件：控制器（BlackboardController）——三要素之三
 *     对应 C&C 图中的"组件：控制器 / 调度器"
 *   - 连接器：指向各知识源的"监控与调度信号"（控制器单方向持有知识源列表，
 *             知识源不持有控制器，因此调度关系是单向的）
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 控制器是唯一持有知识源集合的组件：______
 * [ ] 知识源之间没有直接的引用或调用（只经黑板间接交互）：______
 * [ ] 控制器本身不含任何 N 皇后求解规则（规则属于知识源）：______
 * [ ] 控制器的调度顺序在 README 中有明确说明，且与代码一致：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.blackboard;

import java.util.List;

/**
 * 黑板控制器：按一定策略触发知识源，直到黑板到达稳定态。
 *
 * <p><b>控制器为什么必须存在：</b>知识源之间互不知情，谁也不知道"下一步该谁动手"。
 * 这个全局决策必须有人做，而做决策的人不能写进知识源里（否则知识源之间就产生了依赖），
 * 于是有了控制器这个独立组件——它看黑板、决定触发谁、看结果、再决定下一步。
 *
 * <p><b>调度策略的三种常见做法（选一种并在 README 中写明理由）：</b>
 * <ol>
 *   <li><b>固定顺序轮询</b>：每一轮按固定次序询问每个知识源 "canHandle"，能触发就触发。
 *       实现最简单，行为可复现，便于实验对比；</li>
 *   <li><b>优先级调度</b>：给知识源排优先级（例如先做检查、后做收集），
 *       避免"无效触发"，调度更高效但顺序耦合变强；</li>
 *   <li><b>机会式调度</b>：根据黑板当前状态动态选择最合适的知识源（真实黑板系统的做法），
 *       灵活但行为不易复现，实验数据波动大，报告里不好解释。</li>
 * </ol>
 *
 * <p><b>停机条件（TODO 中要实现）：</b>所有知识源都不可触发，且黑板版本号
 * {@link BlackboardState#revision()} 相比上一轮没有增长——说明系统到达稳定态。
 * 只判断"知识源都不可触发"是不够的：必须结合版本号，否则可能过早停机。
 */
public final class BlackboardController {

    private final BlackboardState blackboard;
    private final List<KnowledgeSource> sources;

    /**
     * @param blackboard 黑板存储区（共享状态）
     * @param sources    知识源列表（控制器是唯一持有它们的地方）
     */
    public BlackboardController(BlackboardState blackboard, List<KnowledgeSource> sources) {
        this.blackboard = blackboard;
        this.sources = List.copyOf(sources);
    }

    public BlackboardState blackboard() {
        return blackboard;
    }

    public List<KnowledgeSource> sources() {
        return sources;
    }

    /**
     * 调度主循环：按固定顺序反复触发可用的知识源，直到黑板到达稳定态。
     *
     * <p><b>采用的是三种策略中最简单的一种——固定顺序轮询</b>
     * （见类注释第 1 条）：每轮按 {@code sources} 的顺序逐个询问 {@code canHandle}，
     * 能触发就 {@code execute}。顺序固定 ⇒ 行为完全可复现 ⇒
     * 阶段3 做跨架构耗时对比时数据才稳定。
     *
     * <p><b>停机判定同时看两件事</b>：一轮下来没有任何知识源可触发，
     * <b>并且</b>黑板版本号 {@code revision} 相比本轮开始时没有增长。
     * 只看前者是不够的——某条死路被取出后没有产生任何子候选时，
     * 必须靠版本号才能确认"确实没有进展"，否则会过早停机导致漏解。
     */
    public int solve() {
        return solve(true);
    }

    /**
     * 同上，但可以只求第一个解。
     *
     * @param findAll {@code true} = 求全部解；{@code false} = 找到第一个解就停
     * @return 找到的解的数量
     */
    public int solve(boolean findAll) {
        while (!blackboard.isFinished()) {
            long before = blackboard.revision();

            for (KnowledgeSource ks : sources) {
                if (ks.canHandle(blackboard)) {
                    ks.execute(blackboard);
                }
            }

            if (!findAll && blackboard.solutionCount() > 0) {
                break;   // 只求第一个解：黑板上出现解就收工
            }

            if (blackboard.revision() == before) {
                blackboard.markFinished();   // 一轮下来毫无进展 ⇒ 到达稳定态
            }
        }
        return blackboard.solutionCount();
    }
}
