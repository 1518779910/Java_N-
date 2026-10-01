/* ============================================================
 * 文件：Solution.java
 * 所在包：edu.scut.nqueens.common（全组共用，不属于任何单一架构风格）
 *
 * 本文件实现的架构风格：不适用（全组共用的数据表示层）
 * 本文件承担的核心组件/连接器：
 *   - 组件：完整解（不可变值对象），五种架构风格求解结果的统一表示
 *   - 连接器：无
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本类为不可变对象，对外不暴露内部数组引用：______
 * [ ] 本类不依赖任何一种架构风格的包：______
 * [ ] 本类不包含任何求解或剪枝逻辑：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.common;

import java.util.Arrays;

/**
 * 一个完整的 N 皇后解：{@code columns[i]} 表示第 i 行的皇后位于第 {@code columns[i]} 列。
 *
 * <p>不可变，可安全地在管道中传递、被多个知识源同时读取。
 *
 * <p>{@link #toString()} 输出逗号分隔的列号（如 N=8 的第一个解 {@code 0,4,7,5,2,6,1,3}），
 * 阶段3 的原始实验日志与解的正确性校验可直接使用该格式。
 */
public final class Solution {

    private final int[] columns;

    public Solution(int[] columns) {
        this.columns = columns.clone();
    }

    /** 棋盘规模 n。 */
    public int n() {
        return columns.length;
    }

    /** 第 i 行皇后所在的列。 */
    public int columnAt(int i) {
        return columns[i];
    }

    /** 复制一份列号数组（防御性拷贝）。 */
    public int[] columns() {
        return columns.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Solution other)) {
            return false;
        }
        return Arrays.equals(columns, other.columns);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(columns);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(columns.length * 3);
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(columns[i]);
        }
        return sb.toString();
    }
}
