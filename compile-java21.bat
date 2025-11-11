@echo off
cd /d "%~dp0"
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=C:\Program Files\Java\jdk-21\bin;%PATH%
echo Using Java:
java -version
echo.
echo Compiling with Maven...
call "%~dp0mvnw.cmd" clean compile
