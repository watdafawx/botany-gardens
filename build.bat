@echo off
REM Builds the mod with a JDK 21 unpacked into toolchain\ (this project's own, or the shared one
REM from the sibling foxs-tweaks project) so nothing has to be installed system-wide. If you
REM install a JDK 21 yourself, you can ignore this script and just run "gradlew build".
setlocal
for /d %%D in ("%~dp0toolchain\jdk-*") do set "JAVA_HOME=%%D"
if not defined JAVA_HOME (
    for /d %%D in ("%~dp0..\foxs-tweaks\toolchain\jdk-*") do set "JAVA_HOME=%%D"
)
if not defined JAVA_HOME (
    echo No JDK found in toolchain\. Set JAVA_HOME to a JDK 21 and run gradlew build.
    exit /b 1
)
echo Using JDK: %JAVA_HOME%
call "%~dp0gradlew.bat" %*
