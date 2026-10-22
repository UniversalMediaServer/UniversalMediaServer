@echo off
setlocal
cd /d "%~dp0" || exit /b 1

rem Prefer the packaged runtime; otherwise use JAVA_HOME or Java from PATH.
set "UMS_JAVA="
if exist "%~dp0jre25\bin\java.exe" (
    set "UMS_JAVA=%~dp0jre25\bin\java.exe"
) else if defined JAVA_HOME (
    set "UMS_JAVA=%JAVA_HOME%\bin\java.exe"
) else (
    for /f "delims=" %%J in ('where java.exe 2^>nul') do if not defined UMS_JAVA set "UMS_JAVA=%%J"
)
if not defined UMS_JAVA goto wrong_java

set "UMS_JAVA_VERSION="
for /f "tokens=3" %%V in ('""%UMS_JAVA%" -XshowSettings:properties -version 2^>^&1 ^| findstr /C:"java.specification.version =""') do set "UMS_JAVA_VERSION=%%V"
if not "%UMS_JAVA_VERSION%"=="25" goto wrong_java

if not defined UMS_MAX_MEMORY set "UMS_MAX_MEMORY=2048M"
rem Keep the console open for diagnostic output when bypassing UMS.exe.
"%UMS_JAVA%" %JAVA_OPTS% -Xmx%UMS_MAX_MEMORY% -Xss2048k --add-modules=jdk.incubator.vector --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Djna.nosys=true -classpath "update.jar;ums.jar" net.pms.PMS %*
exit /b %errorlevel%

:wrong_java
echo UMS requires Java 25. Selected executable: "%UMS_JAVA%". 1>&2
echo Install JDK 25 and set JAVA_HOME, or use the bundled jre25 runtime. 1>&2
exit /b 1
