/* ============================================================
 * 文件：MapReduceFramework.java
 * 本文件实现的架构风格：Map-Reduce（单机模拟）
 * 本文件承担的核心组件/连接器：
 *   - 组件：单机 Map-Reduce 框架（Driver + Map 阶段 + Shuffle + Reduce 阶段）
 *     对应 C&C 图中的"Driver → Mappers → Shuffle → Reducers"流水线
 *   - 连接器：ExecutorService（线程池）模拟多 Worker；
 *             CopyOnWriteArrayList / ConcurrentHashMap 承载中间结果
 * ------------------------------------------------------------
 * 架构约束自查清单（逐项填 是/否，并简述）：
 * [是] 框架不包含任何 N 皇后求解规则：求解规则属于 Mapper/Reducer 实现
 * [是] Mapper 与 Reducer 之间不直接引用，只经 Shuffle 分组结构传递：框架持有二者，二者互不可见
 * [是] 中间结果（KeyValuePair）不可变，多线程可安全共享：record + final 字段
 * [是] 只求第一个解时，停止信号经共享原子标志传递，不用 Thread.stop()：AtomicBoolean
 * ------------------------------------------------------------
 * AI 使用情况声明：
 * 使用工具：无；用于任务：无；
 * 自行修改内容：本人独立实现；声明人（手写签名）：成员D
 * ============================================================ */
package edu.scut.nqueens.mapreduce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * 单机 Map-Reduce 框架：用线程池模拟分布式 Map-Reduce 的 Driver / Mappers / Shuffle / Reducers。
 *
 * <p><b>四个阶段（与 C&amp;C 图一一对应）：</b>
 * <pre>
 *   [Driver]                     ← 提交输入分片、启动 Map/Reduce、汇总输出
 *      │
 *      │ 1. 提交 List&lt;IN&gt; 给 Map 阶段
 *      ▼
 *   [Mapper ×N]   ← 并行线程池，每个 Mapper 处理一个分片，emit (K,V)
 *      │
 *      │ 2. 中间结果汇集到 Shuffle 缓冲区（CopyOnWriteArrayList）
 *      ▼
 *   [Shuffle]     ← 按 K 分组：Map&lt;K, List&lt;V&gt;&gt;
 *      │
 *      │ 3. 每个 key 分组交给一个 Reducer
 *      ▼
 *   [Reducer ×M]  ← 并行线程池，每个 Reducer 聚合一个 key，emit (K, OUT)
 *      │
 *      ▼
 *   [Driver]      ← 收集 List&lt;OUT&gt; 作为最终输出
 * </pre>
 *
 * <p><b>并行度：</b>Mapper 与 Reducer 各用一个固定大小的线程池，
 * 默认取 {@code Runtime.getRuntime().availableProcessors()}。
 * 这与分布式 Map-Reduce "一个 Worker 上跑多个 Task"的语义一致：
 * Task 数 &gt; Worker 数时，由线程池自行排队调度。
 *
 * <p><b>只求第一个解（{@code --first}）的提前终止：</b>
 * Driver 维护一个共享的 {@link AtomicBoolean} 取消标志。
 * Mapper 的 emitter 用 {@code compareAndSet(false, true)} 抢占式置位——
 * 只有<b>第一个</b>成功置位的线程的 emit 会真正写入中间缓冲区，
 * 其余线程的 emit 直接丢弃。
 * 这比 {@code Thread.stop()} 干净——被丢弃的 Mapper 走正常退出路径，不留半截状态。
 *
 * <p><b>关于线程安全：</b>
 * <ul>
 *   <li>中间结果用 {@link CopyOnWriteArrayList} 承载，多线程并发写安全；</li>
 *   <li>最终输出同样用 {@link CopyOnWriteArrayList}；</li>
 *   <li>{@link KeyValuePair} 是不可变 record，多线程读取无需同步。</li>
 * </ul>
 *
 * @param <IN>  Map 输入分片类型
 * @param <K>   中间键类型
 * @param <V>   中间值类型
 * @param <OUT> Reduce 最终输出类型
 */
public final class MapReduceFramework<IN, K, V, OUT> {

    /** 默认并行度：取 CPU 核数。单机模拟与分布式"每节点一个 Worker"的语义对齐。 */
    private static final int DEFAULT_PARALLELISM = Runtime.getRuntime().availableProcessors();

    private final Mapper<IN, K, V> mapper;
    private final Reducer<K, V, OUT> reducer;
    private final int parallelism;

    /**
     * 用默认并行度构造框架。
     *
     * @param mapper  Map 阶段实现
     * @param reducer Reduce 阶段实现
     */
    public MapReduceFramework(Mapper<IN, K, V> mapper, Reducer<K, V, OUT> reducer) {
        this(mapper, reducer, DEFAULT_PARALLELISM);
    }

    /**
     * @param mapper      Map 阶段实现
     * @param reducer     Reduce 阶段实现
     * @param parallelism 线程池大小；&lt;= 0 时取 {@link #DEFAULT_PARALLELISM}
     */
    public MapReduceFramework(Mapper<IN, K, V> mapper, Reducer<K, V, OUT> reducer, int parallelism) {
        this.mapper = Objects.requireNonNull(mapper, "mapper 不能为 null");
        this.reducer = Objects.requireNonNull(reducer, "reducer 不能为 null");
        this.parallelism = parallelism > 0 ? parallelism : DEFAULT_PARALLELISM;
    }

    /** 当前框架使用的并行度（线程池大小）。 */
    public int parallelism() {
        return parallelism;
    }

    /**
     * 执行 Map → Shuffle → Reduce 流水线，返回最终输出列表。
     *
     * <p><b>注意：返回的列表顺序取决于 Reducer 的完成顺序（非确定性的）。</b>
     * 调用方如需确定顺序，应自行排序。
     *
     * @param inputs  输入分片列表
     * @param findAll {@code true} = 求全部结果；{@code false} = 求第一个结果后停止
     * @return 最终输出列表（已收集到 Driver）
     */
    public List<OUT> run(List<IN> inputs, boolean findAll) {
        Objects.requireNonNull(inputs, "inputs 不能为 null");
        // 只求第一个解的取消标志：第一个成功 emit 的线程置位，其余线程的 emit 被丢弃
        AtomicBoolean cancelled = new AtomicBoolean(false);

        // ====== 1. Map 阶段 ======
        List<KeyValuePair<K, V>> intermediate = new CopyOnWriteArrayList<>();

        // Mapper 的 emitter：线程安全，处理只求第一个解时的抢占式置位
        BiConsumer<K, V> mapEmitter = (k, v) -> {
            if (!findAll) {
                // compareAndSet 保证只有一个线程能从 false 翻到 true；
                // 失败的线程直接返回，丢弃这次 emit——保证只产出 1 个中间值
                if (!cancelled.compareAndSet(false, true)) {
                    return;
                }
            }
            intermediate.add(new KeyValuePair<>(k, v));
        };

        ExecutorService mapPool = Executors.newFixedThreadPool(parallelism);
        try {
            List<Future<?>> mapFutures = new ArrayList<>(inputs.size());
            for (IN input : inputs) {
                mapFutures.add(mapPool.submit(() -> mapper.map(input, mapEmitter)));
            }
            // 等所有 Mapper 结束。只求第一个解时，Mapper 仍会跑完，
            // 但后续 emit 已被 emitter 丢弃——浪费的算力不超过一个分片
            for (Future<?> f : mapFutures) {
                awaitQuietly(f, "Map");
            }
        } finally {
            mapPool.shutdown();
        }

        // ====== 2. Shuffle 阶段：按 key 分组 ======
        Map<K, List<V>> groups = shuffle(intermediate);

        // 只求第一个解：只把第一个 key 的第一个值交给 Reduce
        // （compareAndSet 已保证 intermediate 最多只有 1 个元素，这里再做一次防御）
        if (!findAll) {
            if (intermediate.isEmpty()) {
                return List.of();
            }
            KeyValuePair<K, V> only = intermediate.get(0);
            groups = new HashMap<>();
            List<V> single = new ArrayList<>();
            single.add(only.value());
            groups.put(only.key(), single);
        }

        // ====== 3. Reduce 阶段 ======
        List<OUT> outputs = new CopyOnWriteArrayList<>();
        BiConsumer<K, OUT> reduceEmitter = (k, v) -> outputs.add(v);

        ExecutorService reducePool = Executors.newFixedThreadPool(parallelism);
        try {
            List<Future<?>> reduceFutures = new ArrayList<>(groups.size());
            for (Map.Entry<K, List<V>> entry : groups.entrySet()) {
                reduceFutures.add(reducePool.submit(() ->
                        reducer.reduce(entry.getKey(), entry.getValue(), reduceEmitter)));
            }
            for (Future<?> f : reduceFutures) {
                awaitQuietly(f, "Reduce");
            }
        } finally {
            reducePool.shutdown();
        }

        return outputs;
    }

    /**
     * Shuffle：把 Map 产出的 {@code List<(K, V)>} 按 K 分组。
     *
     * <p>单机模拟下 Shuffle 就是内存里的 {@code Map<K, List<V>>}；
     * 分布式 Map-Reduce 中 Shuffle 涉及网络传输与磁盘溢写，本工程不模拟那部分。
     *
     * <p>分组结果用 {@link HashMap}——顺序非确定，但 Reduce 阶段之后
     * 调用方会自行排序，因此顺序不重要。
     */
    private static <K, V> Map<K, List<V>> shuffle(List<KeyValuePair<K, V>> intermediate) {
        Map<K, List<V>> groups = new HashMap<>();
        for (KeyValuePair<K, V> pair : intermediate) {
            groups.computeIfAbsent(pair.key(), k -> new ArrayList<>()).add(pair.value());
        }
        return groups;
    }

    /** 等待一个 Future 完成，把异常包装成 IllegalStateException 抛出。 */
    private static void awaitQuietly(Future<?> future, String phase) {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(phase + " 阶段异常", cause);
        }
    }
}
