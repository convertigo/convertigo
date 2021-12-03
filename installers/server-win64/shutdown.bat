@echo off
set JAVA_HOME=%~dp0%jdk
set CATALINA_HOME=%~dp0%tomcat
%CATALINA_HOME%\bin\shutdown.bat