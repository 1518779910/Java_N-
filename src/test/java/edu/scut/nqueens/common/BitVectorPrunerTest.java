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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test    @DisplayName("初始状态没有任何占用，深度为 0")
    void initialStateIsEmpty() {
        BitVectorBoardState state = pruner.initial(8);
        assertEquals(0, state.columnMask(), "初始列占用应为 0");
        assertEquals(0, state.diagDownMask(), "初始 ↘ 对角线占用应为 0");
        assertEquals(0, state.diagUpMask(), "初始 ↙ 对角线占用应为 0");
        assertEquals(0, state.depth(), "初始搜索深度应为 0");
    }

    @Test    @DisplayName("同列冲突被拒绝")
    void rejectsSameColumn() {
        BitVectorBoardState state = pruner.place(pruner.initial(8), 0, 3);
        assertFalse(pruner.canPlace(state, 1, 3), "第 1 行第 3 列与第 0 行同列，应被拒绝");
        // 注意不能用第 4 列做对照：(0,3) 与 (1,4) 行距 1、列距 1，同在一条 ↘ 对角线上，
        // 那是 rejectsBothDiagonals 覆盖的情形。第 5 列才是真正无冲突的。
        assertTrue(pruner.canPlace(state, 1, 5), "第 1 行第 5 列不冲突，应被允许");
    }

    @Test    @DisplayName("两条对角线冲突都被拒绝")
    void rejectsBothDiagonals() {
        BitVectorBoardState state = pruner.place(pruner.initial(8), 0, 3);
        // (1,2) 与 (0,3) 在同一条 ↙ 对角线上；(1,4) 与 (0,3) 在同一条 ↘ 对角线上
        assertFalse(pruner.canPlace(state, 1, 2), "(1,2) 与 (0,3) 同对角线，应被拒绝");
        assertFalse(pruner.canPlace(state, 1, 4), "(1,4) 与 (0,3) 同对角线，应被拒绝");
    }

    @Test    @DisplayName("place 不修改传入状态（不可变语义）")
    void placeDoesNotMutateInput() {
        BitVectorBoardState initial = pruner.initial(8);
        pruner.place(initial, 0, 3);
        assertEquals(0, initial.depth(), "place 不得修改传入的状态对象");
        assertEquals(0, initial.columnMask(), "place 不得修改传入的状态对象");
    }

    @Test
    @DisplayName("与 O(n^2) 逐格扫描的参考实现判断结果一致")
    void matchesBruteForceReference() {
        // 随机走若干条路径：每一步同时用「位向量」和「朴素逐格扫描」两条路判断，
        // 断言两者结论永远一致——这是"位向量剪枝没有算错"最直接的证据，
        // 也是报告中"剪枝策略等价"论断的支撑材料。
        // 种子固定，失败时可复现。
        final int n = 8;
        final Random random = new Random(20261001L);

        for (int trial = 0; trial < 500; trial++) {
            BitVectorBoardState state = pruner.initial(n);
            List<int[]> placed = new ArrayList<>();   // 朴素参考的输入：已放置皇后的坐标

            for (int row = 0; row < n; row++) {
                int col = random.nextInt(n);

                boolean viaBitVector = pruner.canPlace(state, row, col);
                boolean viaScan = canPlaceByScan(placed, row, col);

                assertEquals(viaScan, viaBitVector,
                        "第 " + trial + " 轮 row=" + row + " col=" + col
                                + " 两种实现结论不一致：朴素=" + viaScan + " 位向量=" + viaBitVector);

                if (viaBitVector) {
                    state = pruner.place(state, row, col);
                    placed.add(new int[]{row, col});
                }
            }
        }
    }

    /**
     * 朴素参考实现：遍历所有已放置的皇后，逐条比较列与两条对角线，O(n²)。
     * 故意用最直白的写法，让"位向量版本有没有算错"一眼可查。
     */
    private static boolean canPlaceByScan(List<int[]> placed, int row, int col) {
        for (int[] queen : placed) {
            if (queen[1] == col) {
                return false;                                       // 同列
            }
            if (Math.abs(queen[0] - row) == Math.abs(queen[1] - col)) {
                return false;                                       // 同对角线（行距 == 列距）
            }
        }
        return true;
    }
}
