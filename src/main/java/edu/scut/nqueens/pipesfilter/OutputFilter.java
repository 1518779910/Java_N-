/* ============================================================
 * 文件：OutputFilter.java
 * 本文件实现的架构风格：管道-过滤器（Pipes and Filters）
 * 本文件承担的核心组件/连接器：
 *   - 组件：结果输出过滤器（OutputFilter）——数据汇
 *   - 连接器：上游 CollectorFilter 的 in 管道（Pipe 3：BlockingQueue<Result>）
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本文件未使用任何全局共享可变状态：______
 * [ ] 本文件未直接 import / 调用其他过滤器的内部函数：______
 * [ ] 本文件所有输入均来自管道读端，所有输出均写入管道写端：______
 * [ ] 过滤器之间不共享数据结构、不共享队列句柄以外的状态：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens.pipesfilter;

import java.io.PrintStream;

import edu.scut.nqueens.common.Result;
import edu.scut.nqueens.common.Solution;

/**
 * 结果输出过滤器：管道链的末端，把解打印到控制台或写入文件。
 *
 * <p>它是汇过滤器，{@code out} 为 null。
 *
 * <p>输出流由构造方注入（{@link System#out} 或指向日志文件的 {@link PrintStream}），
 * 这样同一个过滤器既能用于人工查看，也能用于阶段3 的原始日志采集——
 * 日志格式由本过滤器统一决定，是"同环境、同命令、可复现"的前提之一。
 *
 * <p>TODO：实现 process——
 * <ol>
 *   <li>按约定格式输出一个结果条目：{@code Result.index()}（第几个解）+
 *       {@code Result.solution().toString()}（逗号分隔的列号序列）；</li>
 *   <li>若 printBoard 为 true 再输出棋盘图（Q 表示皇后，. 表示空位）；</li>
 *   <li>不要在这里做 flush 之外的缓冲控制，flush 交给调用方在收尾时统一处理。</li>
 * </ol>
 */
public final class OutputFilter extends Filter<Result, Void> {

    /**
     * 输出目标。注意不能命名为 out——基类的 {@code out()} 是 final 的管道写端访问器，
     * 两个含义不同的东西共用同一个名字会让"数据从哪里来、往哪里去"变得难以阅读。
     */
    private final PrintStream sink;
    private final boolean printBoard;

    /**
     * @param name       过滤器名称
     * @param in         上游管道（Pipe 3：承载 Result）
     * @param sink       输出目标（控制台或日志文件）
     * @param printBoard 是否额外打印棋盘图（N=12 时建议关闭，否则日志过长）
     */
    public OutputFilter(String name, Pipe<Result> in, PrintStream sink, boolean printBoard) {
        super(name, in, null);
        this.sink = sink;
        this.printBoard = printBoard;
    }

    public PrintStream sink() {
        return sink;
    }

    public boolean printBoard() {
        return printBoard;
    }

    /**
     * 输出一个结果条目。
     *
     * <p>本过滤器不维护任何计数器——"第几个解"由上游 {@code CollectorFilter} 编号后
     * 通过 {@link Result#index()} 传进来。所以它是完全无状态的：
     * 换个输出目标（控制台 / 文件 / CSV）不需要动一行逻辑。
     */
    @Override
    protected void process(Result item) {
        sink.println("#" + item.index() + " " + item.solution());
        if (printBoard) {
            printBoard(item.solution());
        }
    }

    /**
     * 打印棋盘图：{@code Q} 表示皇后，{@code .} 表示空位。
     * N=8 有 92 个解、N=12 有 14200 个，全打印出来日志会长到没法看，
     * 因此只在棋盘小的时候启用（由装配器决定，见 {@code PipeFilterMain}）。
     */
    private void printBoard(Solution solution) {
        int n = solution.n();
        for (int row = 0; row < n; row++) {
            StringBuilder line = new StringBuilder(2 * n);
            for (int col = 0; col < n; col++) {
                line.append(solution.columnAt(row) == col ? 'Q' : '.');
                if (col < n - 1) {
                    line.append(' ');
                }
            }
            sink.println("    " + line);
        }
        sink.println();
    }
}
