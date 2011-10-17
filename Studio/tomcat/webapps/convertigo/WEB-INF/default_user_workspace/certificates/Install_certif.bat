
if [%1] ==[] echo Certificat file name not specified
if [%1] equ [] echo Certificat file name not specified

if [%2] ==[] echo Password of certificat file name not specified
if [%2] equ [] echo Password of certificat file name not specified

@echo File name %1
@echo Password of certificat file %2

%JAVA_HOME%\bin\java -classpath ..\WEB-INF\lib\engine.jar;..\WEB-INF\lib\util.jar com.twinsoft.convertigo.engine.InstallCerts %1 %2


