@echo off
set JAVA_HOME=%~dp0%jdk
set CATALINA_HOME=%~dp0%tomcat
set JAVA_OPTS=-Dconvertigo.cems.user_workspace_path=%~dp0%workspace
%CATALINA_HOME%\bin\startup.bat