/* ============================================================
 * 文件：Main.java
 * 所在包：edu.scut.nqueens（工程入口，不属于任何单一架构风格）
 *
 * 本文件实现的架构风格：不适用（统一命令行入口）
 * 本文件承担的核心组件/连接器：
 *   - 组件：命令行入口与架构分发器
 *   - 连接器：无（解析参数后转调对应架构的装配器）
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [ ] 本文件不包含任何求解/剪枝逻辑，只做参数解析与分发：______
 * [ ] 本文件不向任何架构内部传递可变共享状态：______
 * [ ] 本文件不依赖任何架构的内部实现细节（只调用各架构的装配入口）：______
 * [ ] 五种架构的运行命令格式统一，便于跨架构实验对比：______
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：______；用于任务：______；
 * 自行修改内容：______；声明人（手写签名）：______
 * ============================================================ */
package edu.scut.nqueens;

import java.io.PrintStream;

import edu.scut.nqueens.blackboard.BlackboardMain;
import edu.scut.nqueens.callreturn.CallReturnMain;
import edu.scut.nqueens.pipesfilter.PipeFilterMain;

/**
 * 统一命令行入口：五种架构风格共用一套命令格式，是跨架构性能对比公平的前提。
 *
 * <pre>
 *   java -cp target/classes edu.scut.nqueens.Main --arch=pipes      --n=8
 *   java -cp target/classes edu.scut.nqueens.Main --arch=blackboard --n=8
 *   mvn exec:java -Dexec.args="--arch=pipes --n=8"
 * </pre>
 *
 * <p>参数：
 * <ul>
 *   <li>{@code --arch=pipes|blackboard|iterative|backtracking|mapreduce}（默认 pipes）；</li>
 *   <li>{@code --n=8|10|12}（默认 8）；</li>
 *   <li>{@code --first}：只求第一个解（默认求全部解）；</li>
 *   <li>{@code --help}：打印帮助。</li>
 * </ul>
 *
 * <p>阶段3 采集原始实验日志时，请固定使用本入口、固定命令行格式，
 * 这样五种架构的日志可以直接横向对比。
 */
public final class Main {

    private static final PrintStream OUT = System.out;

    private Main() {
    }

    public static void main(String[] args) {
        String arch = "pipes";
        int n = 8;
        boolean findAll = true;

        for (String arg : args) {
            if (arg.startsWith("--arch=")) {
                arch = arg.substring("--arch=".length()).trim();
            } else if (arg.startsWith("--n=")) {
                n = Integer.parseInt(arg.substring("--n=".length()).trim());
            } else if ("--first".equals(arg)) {
                findAll = false;
            } else if ("--all".equals(arg)) {
                findAll = true;
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printUsage();
                return;
            } else {
                OUT.println("未知参数：" + arg);
                printUsage();
                return;
            }
        }

        if (n < 1 || n > 31) {
            // 位向量用 int 承载，故 N 上界为 31；N=8/10/12 是作业要求的测试规模
            OUT.println("N 必须在 1..31 之间（当前 " + n + "）。作业测试规模为 8 / 10 / 12。");
            return;
        }

        OUT.printf("=== 架构风格：%s | N=%d | %s ===%n", arch, n, findAll ? "求全部解" : "只求第一个解");
        long startMillis = System.currentTimeMillis();
        try {
            long solutions = switch (arch) {
                case "pipes", "pipe-filter" -> PipeFilterMain.run(n, findAll, OUT);
                case "blackboard" -> BlackboardMain.run(n, findAll, OUT);
                case "iterative", "call-return-iterative" ->
                        CallReturnMain.run(n, findAll, OUT, CallReturnMain.SolverMode.ITERATIVE);
                case "backtracking", "call-return-backtracking" ->
                        CallReturnMain.run(n, findAll, OUT, CallReturnMain.SolverMode.RECURSIVE);
                case "mapreduce" -> throw new UnsupportedOperationException(
                        "该架构风格属于后续阶段实现（Map-Reduce 尚未交付）。");
                default -> throw new IllegalArgumentException("未知架构风格：" + arch);
            };
            long elapsed = System.currentTimeMillis() - startMillis;
            OUT.printf("=== 完成：解数=%d，耗时=%d ms ===%n", solutions, elapsed);
        } catch (UnsupportedOperationException e) {
            printSkeletonNotice(arch, e);
        } catch (IllegalArgumentException e) {
            OUT.println(e.getMessage());
            printUsage();
        }
    }

    /**
     * 骨架阶段的提示输出：把"哪一行还没实现"直接告诉运行者。
     * 实现完对应 TODO 之后，这段提示自然不会出现。
     */
    private static void printSkeletonNotice(String arch, UnsupportedOperationException e) {
        OUT.println();
        OUT.println("================================================================");
        OUT.println(" 【骨架未实现】" + arch);
        OUT.println(" " + e.getMessage());
        OUT.println();
        OUT.println(" 待办清单与约束自查表见：");
        OUT.println("   README-管道-过滤器.md  /  README-黑板.md");
        OUT.println("================================================================");
    }

    private static void printUsage() {
        OUT.println("用法：");
        OUT.println("  --arch=pipes|blackboard|iterative|backtracking|mapreduce");
        OUT.println("  --n=8|10|12          棋盘规模（默认 8）");
        OUT.println("  --first              只求第一个解（默认求全部解）");
        OUT.println("  --help               显示本帮助");
        OUT.println();
        OUT.println("示例：mvn exec:java -Dexec.args=\"--arch=pipes --n=8\"");
    }
}
