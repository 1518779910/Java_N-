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
     * 调度主循环：反复触发可用的知识源，直到黑板到达稳定态。
     *
     * <p>TODO：实现调度循环——
     * <pre>
     *   while (!blackboard.isFinished()) {
     *       long before = blackboard.revision();
     *       for (KnowledgeSource ks : sources) {
     *           if (ks.canHandle(blackboard)) { ks.execute(blackboard); }
     *       }
     *       if (blackboard.revision() == before) {   // 一轮下来没有任何进展
     *           blackboard.markFinished();
     *       }
     *   }
     * </pre>
     * 上面只是<b>固定顺序轮询</b>的骨架示意，请结合你们选定的黑板求解形态补全；
     * 若只求第一个解，可在找到解后直接跳出循环。
     *
     * @return 找到的解的数量
     */
    public int solve() {
        throw new UnsupportedOperationException(
                "TODO: 调度 " + sources.size() + " 个知识源，直到黑板到达稳定态");
    }
}
