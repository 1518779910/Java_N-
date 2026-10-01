/* ============================================================
 * 文件：ArchitectureParityTest.java
 * 所在包：edu.scut.nqueens（跨架构一致性测试，不属于任何单一架构风格）
 *
 * 本文件实现的架构风格：不适用（跨架构一致性测试）
 * 本文件承担的核心组件/连接器：
 *   - 组件：单元测试（验证不同架构风格对同一问题给出相同结果）
 *   - 连接器：无
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本文件只调用各架构的对外装配入口（run），不触碰其内部实现：______
 * [ ] 本文件不修改任何架构的内部状态，只比较它们返回的解数：______
 * [ ] 测试用例与实验性能数据无关（本文件只验证正确性，不产生耗时结论）：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import edu.scut.nqueens.blackboard.BlackboardMain;
import edu.scut.nqueens.pipesfilter.PipeFilterMain;

/**
 * 跨架构一致性测试：不同架构风格求解同一个问题时，必须给出相同的解数。
 *
 * <p><b>为什么单独建一个测试类，而不是放进 {@code common/BitVectorPrunerTest}：</b>
 * 那个类的自查表写着"只测试 common 包的剪枝实现，不依赖任何架构风格"。
 * 跨架构比较必然要 import 各架构的入口，放进去会让那条自查项填不了"是"。
 * 职责不同，测试类就应该分开。
 *
 * <p><b>这个测试为什么重要：</b>阶段3 要做五种架构的耗时对比，
 * 但"结果都不对，比性能是没有意义的"。本测试是那份性能对比成立的前提——
 * 它证明两种架构解的是同一个问题，而且解数一致。
 *
 * <p>注意：本测试只断言<b>解的数量</b>一致，不比较解的具体内容。
 * 逐解比对见后续可扩展的方向（{@code Solution} 已经实现了 equals/hashCode）。
 */
class ArchitectureParityTest {

    /** 丢弃输出：本测试只关心解数，不需要把上万个解的棋盘图刷到测试日志里。 */
    private static final PrintStream DISCARD = new PrintStream(OutputStream.nullOutputStream());

    @Test
    @DisplayName("N=8：管道-过滤器与黑板都给出 92 个解")
    void bothArchitecturesAgreeOnEightQueens() {
        assertEquals(92L, PipeFilterMain.run(8, true, DISCARD), "管道-过滤器架构 N=8 的解数");
        assertEquals(92, BlackboardMain.run(8, true, DISCARD), "黑板架构 N=8 的解数");
    }

    @Test
    @DisplayName("N=10 与 N=12：两种架构的解数一致（724 / 14200）")
    void bothArchitecturesAgreeOnLargerBoards() {
        assertEquals(724L, PipeFilterMain.run(10, true, DISCARD), "管道-过滤器架构 N=10 的解数");
        assertEquals(724, BlackboardMain.run(10, true, DISCARD), "黑板架构 N=10 的解数");

        assertEquals(14200L, PipeFilterMain.run(12, true, DISCARD), "管道-过滤器架构 N=12 的解数");
        assertEquals(14200, BlackboardMain.run(12, true, DISCARD), "黑板架构 N=12 的解数");
    }
}
