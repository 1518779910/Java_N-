/* ============================================================
 * 文件：PipeFilterMain.java
 * 本文件实现的架构风格：管道-过滤器（Pipes and Filters）
 * 本文件承担的核心组件/连接器：
 *   - 组件：装配器（把过滤器与管道组装成一条完整的管道链）
 *   - 连接器：创建 Pipe 1 / Pipe 2 / Pipe 3 并注入上下游过滤器
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本文件只负责"接线"，不参与任何求解/剪枝逻辑：______
 * [ ] 过滤器之间没有互相持有引用，只共享管道对象：______
 * [ ] 本文件不向过滤器传递任何可变共享状态：______
 * [ ] 管道链为线性单向，无反馈回路：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.pipesfilter;

import java.io.PrintStream;

import edu.scut.nqueens.common.BitVectorPruner;
import edu.scut.nqueens.common.PartialSolution;
import edu.scut.nqueens.common.Result;

/**
 * 管道-过滤器架构的装配与启动入口。
 *
 * <p><b>装配清单（与阶段1 的 C&amp;C 图一一对应）：</b>
 * <pre>
 *   [GeneratorFilter]        [ValidatorFilter]        [CollectorFilter]        [OutputFilter]
 *   候选行/部分解生成器        位向量约束校验器            解集收集器               结果输出器
 *          |                        ^                       ^                       ^
 *          | 推送 PartialSolution    | 推送 ValidSolution     | 推送 Result            |
 *          v                        |                       |                       |
 *      [Pipe 1]  ──────────────> [Pipe 2] ──────────────> [Pipe 3] ──────────────> 结果输出
 *      BlockingQueue            BlockingQueue           BlockingQueue
 *      <PartialSolution>        <ValidSolution>         <Result>
 * </pre>
 *
 * <p><b>装配步骤（TODO）：</b>
 * <ol>
 *   <li>创建 3 个管道：{@code new Pipe<PartialSolution>("Pipe 1", 64)} …
 *       （Pipe 2 的类型参数是 ValidSolution，Pipe 3 是 Result）</li>
 *   <li>创建统一的剪枝实现（{@code common} 包下 {@link BitVectorPruner} 的唯一实现类），
 *       把它注入 ValidatorFilter；</li>
 *   <li>按上图顺序构造 4 个过滤器，每个过滤器只拿到自己那一侧的管道；</li>
 *   <li>每个过滤器一个线程（源过滤器与汇过滤器也要独立线程，它们同样被管道阻塞驱动）；</li>
 *   <li>启动全部线程，主线程 join 等待；若只求第一个解，可提前中断并清理线程；</li>
 *   <li>收尾：读取 {@link CollectorFilter#collectedCount()} 与墙钟耗时，
 *       按阶段3 的日志格式打印汇总行（架构名、N、解数、耗时毫秒、命令行）。</li>
 * </ol>
 *
 * <p><b>注意：</b>不要用 {@code Thread.stop()} 之类的强制手段终止过滤器；
 * 求第一个解的场景下，用"取消标志 + 关闭管道"让过滤器正常退出。
 */
public final class PipeFilterMain {

    private PipeFilterMain() {
    }

    /**
     * 运行管道-过滤器架构求解 N 皇后问题。
     *
     * @param n       棋盘规模（8 / 10 / 12）
     * @param findAll true = 求全部解；false = 求第一个解后停止
     * @param out     结果输出目标（控制台或日志文件）
     * @return 解的数量
     */
    public static long run(int n, boolean findAll, PrintStream out) {
        throw new UnsupportedOperationException(
                "TODO: 装配管道链并运行（N=" + n + "，" + (findAll ? "全部解" : "首个解") + "）");
    }

    /** 便于单架构调试：java -cp target/classes edu.scut.nqueens.pipesfilter.PipeFilterMain --n=8 */
    public static void main(String[] args) {
        edu.scut.nqueens.Main.main(prependArch(args));
    }

    private static String[] prependArch(String[] args) {
        String[] result = new String[args.length + 1];
        result[0] = "--arch=pipes";
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }
}
