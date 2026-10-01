/* ============================================================
 * 文件：BitVectorPrunerTest.java
 * 所在包：edu.scut.nqueens.common（全组共用）
 *
 * 本文件实现的架构风格：不适用（统一剪枝实现的单元测试）
 * 本文件承担的核心组件/连接器：
 *   - 组件：单元测试（验证五种架构共用同一份剪枝实现的行为正确）
 *   - 连接器：无
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本文件只测试 common 包的剪枝实现，不依赖任何架构风格：______
 * [ ] 测试用例与实验结果无关（不是"跑出来的数据"，不用于报告中的性能结论）：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 统一剪枝实现的单元测试。
 *
 * <p>这些用例现在都是 {@code @Disabled} 的（剪枝尚未实现，启用会失败）。
 * 实现 {@link BitVectorPrunerImpl} 之后请逐个去掉 {@code @Disabled}，
 * 用 {@code mvn test} 验证——这一步同时也是阶段2 代码质量分的依据之一。
 */
class BitVectorPrunerTest {

    private final BitVectorPruner pruner = new BitVectorPrunerImpl();

    @Test
    @Disabled("TODO: 待 BitVectorPrunerImpl 实现后启用")
    @DisplayName("初始状态没有任何占用，深度为 0")
    void initialStateIsEmpty() {
        BitVectorBoardState state = pruner.initial(8);
        assertEquals(0, state.columnMask(), "初始列占用应为 0");
        assertEquals(0, state.diagDownMask(), "初始 ↘ 对角线占用应为 0");
        assertEquals(0, state.diagUpMask(), "初始 ↙ 对角线占用应为 0");
        assertEquals(0, state.depth(), "初始搜索深度应为 0");
    }

    @Test
    @Disabled("TODO: 待 BitVectorPrunerImpl 实现后启用")
    @DisplayName("同列冲突被拒绝")
    void rejectsSameColumn() {
        BitVectorBoardState state = pruner.place(pruner.initial(8), 0, 3);
        assertFalse(pruner.canPlace(state, 1, 3), "第 1 行第 3 列与第 0 行同列，应被拒绝");
        assertTrue(pruner.canPlace(state, 1, 4), "第 1 行第 4 列不冲突，应被允许");
    }

    @Test
    @Disabled("TODO: 待 BitVectorPrunerImpl 实现后启用")
    @DisplayName("两条对角线冲突都被拒绝")
    void rejectsBothDiagonals() {
        BitVectorBoardState state = pruner.place(pruner.initial(8), 0, 3);
        // (1,2) 与 (0,3) 在同一条 ↙ 对角线上；(1,4) 与 (0,3) 在同一条 ↘ 对角线上
        assertFalse(pruner.canPlace(state, 1, 2), "(1,2) 与 (0,3) 同对角线，应被拒绝");
        assertFalse(pruner.canPlace(state, 1, 4), "(1,4) 与 (0,3) 同对角线，应被拒绝");
    }

    @Test
    @Disabled("TODO: 待 BitVectorPrunerImpl 实现后启用")
    @DisplayName("place 不修改传入状态（不可变语义）")
    void placeDoesNotMutateInput() {
        BitVectorBoardState initial = pruner.initial(8);
        pruner.place(initial, 0, 3);
        assertEquals(0, initial.depth(), "place 不得修改传入的状态对象");
        assertEquals(0, initial.columnMask(), "place 不得修改传入的状态对象");
    }

    @Test
    @Disabled("TODO: 待 BitVectorPrunerImpl 实现后启用；该用例直接支撑报告中“剪枝策略等价”的论断")
    @DisplayName("与 O(n^2) 逐格扫描的参考实现判断结果一致")
    void matchesBruteForceReference() {
        // TODO: 对 n=8 随机生成若干 (state, row, col)，同时用
        //   (1) 本实现的 canPlace
        //   (2) 逐格扫描的朴素实现（遍历所有已放置皇后比较列与两条对角线）
        // 断言两者结论完全一致——这是"位向量剪枝没有算错"最直接的证据。
        throw new UnsupportedOperationException("TODO: 与朴素参考实现对照");
    }

    @Test
    @Disabled("TODO: 待各架构求解器实现后启用")
    @DisplayName("N=8 的解数量为 92（五种架构应给出相同结果）")
    void eightQueensSolutionCount() {
        // TODO: 调用某一种（或多于一种）架构的求解入口，断言解数 == 92。
        // 五种架构都通过后，跨架构性能对比才有意义——结果都不对，比性能是没有意义的。
        assertThrows(UnsupportedOperationException.class, () -> {
            throw new UnsupportedOperationException("TODO");
        });
    }
}
