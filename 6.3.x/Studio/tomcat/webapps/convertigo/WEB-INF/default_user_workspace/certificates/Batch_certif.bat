@echo off
if [%1] ==[] echo Certificat file name not specified
if [%1] equ [] echo Certificat file name not specified

@echo Certificat file name within password : %1
%JAVA_HOME%\bin\java -classpath ..\WEB-INF\lib\engine.jar;..\WEB-INF\lib\util.jar com.twinsoft.convertigo.engine.InstallCerts -f %1


