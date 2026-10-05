/* ============================================================
 * 文件：MapReduceMain.java
 * 本文件实现的架构风格：Map-Reduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：装配器（Driver）——创建剪枝器、构造输入分片、启动 Map/Reduce、收集输出
 *   - 连接器：MapReduceFramework（线程池模拟多 Worker）
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [是] 本文件只负责"接线"，不参与任何求解/剪枝逻辑：求解在 Mapper，剪枝在 common
 * [是] 数据经不可变对象（PartialSolution / Solution）传递，不依赖共享可变状态：record + final
 * [是] 本文件不向 Mapper/Reducer 传递任何可变共享状态：pruner 无状态
 * [是] Mapper 与 Reducer 之间不互相持有引用，只经框架的 Shuffle 传递：框架持有二者
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：无；用于任务：无；
 * 自行修改内容：本人独立实现；声明人（手写签名）：成员D
 * ============================================================ */
package edu.scut.nqueens.mapreduce;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import edu.scut.nqueens.common.BitVectorPruner;
import edu.scut.nqueens.common.BitVectorPrunerImpl;
import edu.scut.nqueens.common.PartialSolution;
import edu.scut.nqueens.common.Solution;

/**
 * Map-Reduce 架构的装配与启动入口（Driver 角色）。
 *
 * <p><b>Map-Reduce 架构的核心特征——分而治之：</b>
 * <pre>
 *   [Driver]  ← 本文件：装配、提交分片、收集输出
 *      │
 *      │ 1. 把第 0 行 N 个列号各做成一个输入分片（PartialSolution）
 *      ▼
 *   [Mapper ×N]   ← NQueensMapper：每个分片递归搜索剩余 N-1 行
 *      │             emit (firstRowCol, Solution)
 *      ▼
 *   [Shuffle]     ← 框架自动按 firstRowCol 分组
 *      │
 *      ▼
 *   [Reducer ×M]  ← NQueensReducer：组内按字典序排序后逐个 emit
 *      │
 *      ▼
 *   [Driver]      ← 收集 List&lt;Solution&gt;，按 key 排序后输出
 * </pre>
 *
 * <p><b>搜索空间切分：</b>
 * N 皇后的解空间按"第 0 行皇后的列号"切分成 N 个互不相交的子空间。
 * 第 0 行不存在冲突（棋盘上只有它一个皇后），因此每个子空间独立可解。
 * 这正是 Map-Reduce "把大问题切成小问题并行求解"的天然划分点。
 *
 * <p><b>剪枝策略等价性：</b>本架构注入的是全组唯一的 {@link BitVectorPrunerImpl}，
 * Mapper 的递归展开调用 {@link PartialSolution#place}，
 * 后者内部调用 {@link BitVectorPruner#canPlace} 与 {@link BitVectorPruner#place}——
 * 与其它四种架构调用的是同一份实现，剪枝等价性由"共用同一份实现"在结构上保证。
 *
 * <p><b>输出顺序：</b>Reducer 已保证组内字典序；Driver 再按 key（第 0 行列号）
 * 排序组间顺序，最终输出顺序与回溯版（第 0 行列号 0..N-1，组内递归展开）一致。
 */
public final class MapReduceMain {

    private MapReduceMain() {
    }

    /**
     * 运行 Map-Reduce 架构求解 N 皇后问题。
     *
     * @param n       棋盘规模（8 / 10 / 12）
     * @param findAll true = 求全部解；false = 求第一个解后停止
     * @param out     结果输出目标（控制台或日志文件）
     * @return 解的数量
     */
    public static long run(int n, boolean findAll, PrintStream out) {
        // 1. 全组统一的剪枝实现。无状态，可被多个 Mapper 线程安全共享。
        BitVectorPruner pruner = new BitVectorPrunerImpl();

        // 2. 构造 N 个输入分片：第 0 行皇后的列号依次取 0..N-1。
        //    第 0 行不存在冲突，place 必返回非 null。
        PartialSolution empty = new PartialSolution(n);
        List<PartialSolution> splits = new ArrayList<>(n);
        for (int col = 0; col < n; col++) {
            splits.add(empty.place(col, pruner));
        }

        // 3. 装配框架：Mapper 与 Reducer 各一份（无状态，可被多线程共享）。
        NQueensMapper mapper = new NQueensMapper(pruner);
        NQueensReducer reducer = new NQueensReducer();
        MapReduceFramework<PartialSolution, Integer, Solution, Solution> framework =
                new MapReduceFramework<>(mapper, reducer);

        // 4. 执行 Map → Shuffle → Reduce
        long startMillis = System.currentTimeMillis();
        List<Solution> solutions = framework.run(splits, findAll);
        long elapsed = System.currentTimeMillis() - startMillis;

        // 5. 按 key（第 0 行列号）排序组间顺序，与其它四种架构输出顺序一致。
        //    组内顺序已由 Reducer 保证（字典序）。
        solutions.sort(Comparator.comparingInt(s -> s.columnAt(0)));

        // 6. 逐个输出解。格式与其它架构保持一致：#序号 + 逗号分隔的列号序列
        int index = 1;
        boolean printBoard = n <= 8;
        for (Solution solution : solutions) {
            out.println("#" + index + " " + solution);
            if (printBoard) {
                printBoard(out, solution);
            }
            index++;
        }

        long count = solutions.size();

        // 只求第一个解时，框架保证至多产出 1 个解；这里再做一次防御性截断
        long reported = findAll ? count : Math.min(count, 1L);

        // 7. 汇总行（阶段3 的实验日志格式：架构名 / N / 解数 / 耗时毫秒）
        out.printf("[mapreduce] N=%d 解数=%d 耗时=%d ms%n", n, reported, elapsed);
        return reported;
    }

    /**
     * 打印棋盘图：{@code Q} 表示皇后，{@code .} 表示空位。
     * 与其它四种架构保持相同格式，便于跨架构对照。
     */
    private static void printBoard(PrintStream out, Solution solution) {
        int n = solution.n();
        for (int row = 0; row < n; row++) {
            StringBuilder line = new StringBuilder(2 * n);
            for (int col = 0; col < n; col++) {
                line.append(solution.columnAt(row) == col ? 'Q' : '.');
                if (col < n - 1) {
                    line.append(' ');
                }
            }
            out.println("    " + line);
        }
        out.println();
    }

    /** 便于单架构调试：java -cp target/classes edu.scut.nqueens.mapreduce.MapReduceMain --n=8 */
    public static void main(String[] args) {
        edu.scut.nqueens.Main.main(prependArch(args));
    }

    private static String[] prependArch(String[] args) {
        String[] result = new String[args.length + 1];
        result[0] = "--arch=mapreduce";
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }
}
