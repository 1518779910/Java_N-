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
import java.util.ArrayList;
import java.util.List;

import edu.scut.nqueens.common.BitVectorPruner;
import edu.scut.nqueens.common.BitVectorPrunerImpl;
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

    /** 等待过滤器线程结束的上限（毫秒）。N≤12 的正常运行远用不到，只是防死锁的兜底。 */
    private static final long JOIN_TIMEOUT_MILLIS = 60_000;

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
        // 1. 三条管道（连接器）。容量取 64 而不是无界队列：无界队列会让管道退化成
        //    "内存里的数组"，既失去"流"的语义，也会掩盖下游的消费瓶颈。
        Pipe<PartialSolution> pipe1 = new Pipe<>("Pipe 1", 64);
        Pipe<ValidSolution> pipe2 = new Pipe<>("Pipe 2", 64);
        Pipe<Result> pipe3 = new Pipe<>("Pipe 3", 64);

        // 2. 全组统一的剪枝实现。校验过滤器与生成过滤器注入的是同一个无状态实例。
        BitVectorPruner pruner = new BitVectorPrunerImpl();

        // 3. 四个过滤器，严格按 C&C 图的顺序接线。
        //    每个过滤器只拿到自己那一侧的管道，彼此之间没有任何引用——
        //    "过滤器之间只通过管道通信"这条约束在构造阶段就已经成立，不需要靠自觉。
        GeneratorFilter generator = new GeneratorFilter("GeneratorFilter", pipe1, n, pruner);
        ValidatorFilter validator = new ValidatorFilter("ValidatorFilter", pipe1, pipe2, pruner);
        CollectorFilter collector = new CollectorFilter("CollectorFilter", pipe2, pipe3);
        // 棋盘图只在 N<=8 时打印（92 个解尚可阅读，N=12 的 14200 个会淹没日志）；
        // 输出条数上限：求全部解时不限（0），只求第一个解时限 1 条
        OutputFilter output = new OutputFilter("OutputFilter", pipe3, out, n <= 8, findAll ? 0 : 1);

        // 4. 每个过滤器一个线程。源过滤器与汇过滤器同样需要独立线程：
        //    它们也是被管道的阻塞语义驱动的，放进主线程会让整条链失去流水线形态。
        List<Filter<?, ?>> filters = List.of(generator, validator, collector, output);
        List<Thread> threads = new ArrayList<>(filters.size());
        for (Filter<?, ?> filter : filters) {
            Thread thread = new Thread(filter, filter.name());
            thread.setDaemon(true);   // 主线程异常退出时，别让残留线程把 JVM 挂住
            threads.add(thread);
        }

        long startMillis = System.currentTimeMillis();
        threads.forEach(Thread::start);

        if (!findAll) {
            // 只求第一个解：管道-过滤器没有天然的提前终止点，
            // 因为解一旦进入管道，后续数据还在链上流动。
            // 这里由主线程盯着输出过滤器的**打印计数**，打印出来一个就中断整条链。
            //
            // 注意不能盯着收集计数：收集与打印是两个线程，一收到解就打断的话，
            // 解可能还没被打印出来，使用者会看到"解数=1"却没有任何解的输出。
            //
            // 用中断而不是 Thread.stop()：阻塞在管道上的过滤器会被 InterruptedException
            // 唤醒，走正常的退出路径，不会留下半截状态。
            try {
                while (output.printedCount() == 0 && anyAlive(threads)) {
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            threads.forEach(Thread::interrupt);
        }

        for (Thread thread : threads) {
            try {
                thread.join(JOIN_TIMEOUT_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 死锁兜底：某个过滤器异常退出后，它<b>上游</b>的过滤器会永远阻塞在
        // "已经没有下游消费"的管道上，join 就再也不会返回——现象是程序像卡死一样挂着。
        // 因此超时后主动中断全部线程，让它们走正常退出路径。
        if (anyAlive(threads)) {
            threads.forEach(Thread::interrupt);
            for (Thread thread : threads) {
                try {
                    thread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startMillis;

        // 5. 任何一个过滤器异常终止，都必须让整条链失败得响亮
        for (Filter<?, ?> filter : filters) {
            if (filter.failure() != null) {
                throw new IllegalStateException(
                        "过滤器 " + filter.name() + " 异常终止", filter.failure());
            }
        }

        // 6. 汇总行（阶段3 的实验日志格式：架构名 / N / 解数 / 耗时毫秒）
        long count = collector.collectedCount();
        // 只求第一个解时，中断发生在"观察到第一个解"之后，管道里可能还有在途数据被顺带收集。
        // 那种情况下报告 1（"至少存在一个解"），而不是把在途数据的数量报出去。
        long reported = findAll ? count : Math.min(count, 1L);

        out.printf("[pipes] N=%d 解数=%d 耗时=%d ms%n", n, reported, elapsed);
        return reported;
    }

    /** 是否还有过滤器线程存活；用作只求第一个解时的轮询兜底，避免空转死循环。 */
    private static boolean anyAlive(List<Thread> threads) {
        for (Thread thread : threads) {
            if (thread.isAlive()) {
                return true;
            }
        }
        return false;
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
