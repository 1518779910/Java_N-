#!/usr/bin/env bash
# ============================================================
#  N 皇后问题 · 阶段2 一键验证脚本
#
#  用法：
#     bash verify.sh              # 在含 pom.xml 的目录下运行
#     bash verify.sh --verbose    # 额外打印每次运行的完整输出
#
#  做四件事：
#     1. 打印运行环境（JDK / Maven 版本）
#     2. 编译工程
#     3. 跑单元测试（含跨架构解数一致性断言）
#     4. 逐个运行两种架构 × N=8/10/12，自动核对解数是否符合预期值
#
#  退出码：0 = 全部通过；1 = 有失败项
# ============================================================
set -u
cd "$(dirname "$0")" || exit 1

VERBOSE=0
[ "${1:-}" = "--verbose" ] && VERBOSE=1

PASS=0
FAIL=0
ok()  { PASS=$((PASS + 1)); printf '  [PASS] %s\n' "$1"; }
bad() { FAIL=$((FAIL + 1)); printf '  [FAIL] %s\n' "$1"; }

# java 优先取 JAVA_HOME：某些机器 PATH 上的 java 是旧版本，
# 直接用它跑 release 21 的 class 会报 UnsupportedClassVersionError。
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=java
    printf '  [提示] 未找到 JAVA_HOME，回退到 PATH 上的 java\n'
fi

# timeout 不是所有平台都有（macOS 需 gtimeout），有就用，没有就不加
if command -v timeout >/dev/null 2>&1; then
    TIMEOUT="timeout 120"
elif command -v gtimeout >/dev/null 2>&1; then
    TIMEOUT="gtimeout 120"
else
    TIMEOUT=""
fi

echo "=============================================================="
echo " N 皇后问题 · 阶段2 验证"
echo "=============================================================="

# ---------------------------------------------------------- 1. 环境
echo
echo "--- 1/4 运行环境 ---"
"$JAVA" -version 2>&1 | head -1 | sed 's/^/  JAVA   : /'
mvn -v 2>&1 | head -1 | sed 's/^/  Maven  : /'
printf '  JAVA   : %s\n' "$JAVA"

# ---------------------------------------------------------- 2. 编译
echo
echo "--- 2/4 编译 ---"
if mvn -q compile >/tmp/nq-build.log 2>&1; then
    ok "mvn compile 通过"
else
    bad "mvn compile 失败（详见 /tmp/nq-build.log）"
    tail -20 /tmp/nq-build.log | sed 's/^/    /'
    echo
    echo "编译都不过，后面的验证没有意义，提前结束。"
    exit 1
fi

# ---------------------------------------------------------- 3. 测试
echo
echo "--- 3/4 单元测试 ---"
if mvn -q test >/tmp/nq-test.log 2>&1; then
    ok "mvn test 通过（含跨架构解数一致性断言）"
    grep -E "Tests run:" /tmp/nq-test.log | tail -1 | sed 's/^/    /'
else
    bad "mvn test 失败（详见 /tmp/nq-test.log）"
    grep -E "Tests run:|ERROR" /tmp/nq-test.log | head -10 | sed 's/^/    /'
fi

# ---------------------------------------------------------- 4. 求解
echo
echo "--- 4/4 求解验证 ---"
echo "  预期解数：N=8 → 92 ；N=10 → 724 ；N=12 → 14200"
echo

# 跑一次并核对解数。$1=架构 $2=N $3=预期解数 [$4=--first]
check() {
    local arch=$1 n=$2 expect=$3 extra=${4:-} label out got
    label="$arch N=$n${extra:+ $extra}"

    if [ -n "$extra" ]; then
        out=$($TIMEOUT "$JAVA" -cp target/classes edu.scut.nqueens.Main \
                --arch="$arch" --n="$n" "$extra" 2>&1)
    else
        out=$($TIMEOUT "$JAVA" -cp target/classes edu.scut.nqueens.Main \
                --arch="$arch" --n="$n" 2>&1)
    fi

    # 超时：进程没在 120 秒内退出，多半是过滤器线程没被 EOS 唤醒
    if [ $? -eq 124 ]; then
        bad "$label 超时未退出（疑似线程未结束 / 管道死锁）"
        return
    fi

    # 解数 = 最后一个解的编号。输出过滤器给每个解打 "#序号"，从 1 开始连续编号，
    # 所以最后一行 "#N" 的 N 就是解数。
    #
    # 为什么不用 "解数=NN" 那行：Java 在 Windows 上按控制台代码页（中文系统为 GBK）
    # 输出，而本脚本里的中文是 UTF-8，直接匹配中文会匹配不上。
    # "#N" 是纯 ASCII，不受编码影响——这是刻意选的匹配方式。
    got=$(printf '%s\n' "$out" | grep -a -o '^#[0-9][0-9]*' | tail -1 | tr -d '#')

    if [ "$got" = "$expect" ]; then
        ok "$label 解数=$got"
    else
        bad "$label 解数=${got:-未输出任何解}（预期 $expect）"
        printf '%s\n' "$out" | tail -5 | sed 's/^/    /'
    fi

    if [ "$VERBOSE" = 1 ]; then
        printf '%s\n' "$out" | sed 's/^/    | /'
    fi
}

for arch in pipes blackboard mapreduce; do
    for n in 8 10 12; do
        case $n in
            8)  check "$arch" 8 92 ;;
            10) check "$arch" 10 724 ;;
            12) check "$arch" 12 14200 ;;
        esac
    done
done

# --first：只求第一个解，三种架构都应报告 1 个解并迅速返回
echo
check pipes      12 1 --first
check blackboard 12 1 --first
check mapreduce  12 1 --first

# ---------------------------------------------------------- 汇总
echo
echo "=============================================================="
if [ "$FAIL" -eq 0 ]; then
    printf ' 全部通过：%d 项\n' "$PASS"
    echo " 三种架构在 N=8/10/12 上解数一致，与已知值相符。"
    echo "=============================================================="
    exit 0
else
    printf ' 结果：通过 %d 项，失败 %d 项\n' "$PASS" "$FAIL"
    echo "=============================================================="
    exit 1
fi
