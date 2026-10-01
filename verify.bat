@echo off
rem ============================================================
rem  N 皇后问题 · 阶段2 一键验证脚本（Windows）
rem
rem  用法：双击本文件，或在命令行执行 verify.bat
rem
rem  做四件事：
rem     1. 打印运行环境（JDK / Maven 版本）
rem     2. 编译工程
rem     3. 跑单元测试（含跨架构解数一致性断言）
rem     4. 逐个运行两种架构 x N=8/10/12，自动核对解数
rem
rem  全部通过时退出码为 0，有失败项为 1。
rem
rem  注意：本文件必须保存为 GBK/ANSI 编码。cmd.exe 是按字节解析批处理
rem  文件的，若存成 UTF-8，中文字节会把命令行拆碎导致语法错误，
rem  且 chcp 65001 也解决不了这个问题。
rem ============================================================
setlocal
cd /d "%~dp0"

set PASS=0
set FAIL=0

echo ==============================================================
echo  N 皇后问题 · 阶段2 验证
echo ==============================================================
echo.

rem java 优先取 JAVA_HOME：某些机器 PATH 上的 java 是旧版本，
rem 直接用它跑 release 21 的 class 会报 UnsupportedClassVersionError
set "JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"

echo --- 1/4 运行环境 ---
"%JAVA%" -version 2>&1
echo   ^(实际使用的 java：%JAVA%^)
echo.
call mvn -v

echo.
echo --- 2/4 编译 ---
call mvn -q compile
if errorlevel 1 goto build_failed
echo   [PASS] mvn compile 通过
set /a PASS+=1

echo.
echo --- 3/4 单元测试 ---
call mvn -q test
if errorlevel 1 goto test_failed
echo   [PASS] mvn test 通过（含跨架构解数一致性断言）
set /a PASS+=1

goto solve


rem ------------------------------------------------------------
:solve
echo.
echo --- 4/4 求解验证 ---
echo   预期解数：N=8 = 92 ；N=10 = 724 ；N=12 = 14200
echo.
call :check pipes      8  92
call :check pipes      10 724
call :check pipes      12 14200
call :check blackboard 8  92
call :check blackboard 10 724
call :check blackboard 12 14200
call :check pipes      12 1 --first
call :check blackboard 12 1 --first
goto summary


rem ------------------------------------------------------------
rem  :check 架构 N 预期解数 [额外参数]
rem
rem  核对方式：既确认第 EXPECT 个解存在，也确认第 EXPECT+1 个解不存在。
rem  输出过滤器给每个解打 "#序号"，所以 "#92 " 这种匹配是精确的
rem  （"#92 " 不会误匹配 "#920 "，因为 92 后面必须紧跟空格）。
rem
rem  两个刻意的选择：
rem    1) 匹配 "#N" 而不是 "解数=NN"——Java 按控制台代码页输出，
rem       与批处理文件的编码不一定一致，匹配中文容易失败；"#N" 是纯 ASCII。
rem    2) 用 findstr 而不是 "find /c /v" 数行数——从 Git Bash 之类环境调用
rem       cmd 时，PATH 里的 find 可能被解析成 Unix find，"/c" 会被当成 C 盘
rem       去遍历整个硬盘（真的会扫，非常慢）。
rem ------------------------------------------------------------
:check
setlocal
set "ARCH=%~1"
set "N=%~2"
set "EXPECT=%~3"
set "EXTRA=%~4"
set "LABEL=%ARCH% N=%N% %EXTRA%"
set "TMPF=%TEMP%\nq-check-%ARCH%-%N%%EXTRA%.txt"

"%JAVA%" -cp target\classes edu.scut.nqueens.Main --arch=%ARCH% --n=%N% %EXTRA% > "%TMPF%" 2>&1

set /a NEXT=EXPECT+1
findstr /C:"#%EXPECT% " "%TMPF%" >nul
if errorlevel 1 goto check_fail
findstr /C:"#%NEXT% " "%TMPF%" >nul
if not errorlevel 1 goto check_fail

echo   [PASS] %LABEL% 解数=%EXPECT%
endlocal & set /a PASS+=1
exit /b

:check_fail
echo   [FAIL] %LABEL% 解数不符（预期 %EXPECT%）
echo          完整输出见 %TMPF%
endlocal & set /a FAIL+=1
exit /b


rem ------------------------------------------------------------
:summary
echo.
echo ==============================================================
if not "%FAIL%"=="0" goto failed
echo  全部通过：%PASS% 项
echo  两种架构在 N=8/10/12 上解数一致，与已知值相符。
echo ==============================================================
pause
exit /b 0

:failed
echo  结果：通过 %PASS% 项，失败 %FAIL% 项
echo ==============================================================
pause
exit /b 1


rem ------------------------------------------------------------
:build_failed
echo   [FAIL] mvn compile 失败
echo ==============================================================
pause
exit /b 1

:test_failed
echo   [FAIL] mvn test 失败
set /a FAIL+=1
goto solve
