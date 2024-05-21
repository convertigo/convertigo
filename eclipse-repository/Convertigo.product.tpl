<?xml version="1.0" encoding="UTF-8"?>
<?pde version="3.5"?>

<product name="Convertigo Studio" uid="com.convertigo.studio" id="com.twinsoft.convertigo.studio.product.ConvertigoProduct" application="org.eclipse.ui.ide.workbench" version="@convertigoVersion@.qualifier" useFeatures="true" includeLaunchers="true">

   <aboutInfo>
      <image path="images/about.png"/>
      <text>@aboutText@</text>
   </aboutInfo>

   <configIni use="default">
   </configIni>

   <launcherArgs>
      <programArgsLin>
      </programArgsLin>
      <programArgsMac>-vm
./../jre/bin/java
      </programArgsMac>
      <vmArgs>-Dfile.encoding=UTF-8
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED
--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
         <argsX86>-Xms256m -Xmx1024m</argsX86>
         <argsX86_64>-Xms256m -Xmx2048m</argsX86_64>
      </vmArgs>
      <vmArgsMac>-Djxbrowser.ipc.external=true
      </vmArgsMac>
   </launcherArgs>

   <windowImages i16="images/convertigo_16x16_32.png" i32="images/convertigo_32x32_32.png" i48="images/convertigo_48x48_32.png" i64="images/convertigo_64x64_32.png" i128="images/convertigo_128x128_32.png"/>

   <splash
      location="com.twinsoft.convertigo.studio.product"
      startupProgressRect="0,316,500,16"
      startupMessageRect="40,265,220,35"
      startupForegroundColor="FFFFFF" />
   <launcher name="ConvertigoStudio">
      <linux icon="images/convertigo_256x256_8.xpm"/>
      <macosx icon="images/convertigo.icns"/>
      <win useIco="true">
         <ico path="images/product.ico"/>
         <bmp/>
      </win>
   </launcher>

   <intro introId="com.twinsoft.convertigo.studio.product.intro"/>

   <vm>
   </vm>

   <license>
        <url>https://c8ocloud.convertigo.net/convertigo/licenses/index.html</url>
        <text>@licenseText@</text>
   </license>

   <plugins>
   </plugins>

   <features>
      <feature id="com.twinsoft.convertigo.studio.feature" version="@convertigoVersion@.qualifier"/>
      <feature id="org.eclipse.e4.rcp"/>
      <feature id="org.eclipse.ecf.core.feature"/>
      <feature id="org.eclipse.ecf.core.ssl.feature"/>
      <feature id="org.eclipse.ecf.filetransfer.feature"/>
      <feature id="org.eclipse.help"/>
      <feature id="org.eclipse.jgit"/>
      <feature id="org.eclipse.jgit.ssh.apache"/>
      <feature id="org.eclipse.ecf.filetransfer.ssl.feature"/>
      <feature id="org.eclipse.egit"/>
      <feature id="org.eclipse.egit.gitflow.feature"/>
      <feature id="org.eclipse.emf.common"/>
      <feature id="org.eclipse.emf.ecore"/>
      <feature id="org.eclipse.equinox.p2.core.feature"/>
      <feature id="org.eclipse.equinox.p2.extras.feature"/>
      <feature id="org.eclipse.equinox.p2.rcp.feature"/>
      <feature id="org.eclipse.equinox.p2.user.ui"/>
      <feature id="org.eclipse.platform"/>
      <feature id="org.eclipse.rcp"/>
      <feature id="org.eclipse.wst.common_core.feature"/>
      <feature id="org.eclipse.wst.common_ui.feature"/>
      <feature id="org.eclipse.wst.xml_core.feature"/>
      <feature id="org.eclipse.wst.xml_ui.feature"/>
      <feature id="org.eclipse.wst.xml_userdoc.feature"/>
      <feature id="org.eclipse.epp.mpc"/>
   </features>

   <configurations>
      <plugin id="org.apache.felix.scr" autoStart="true" startLevel="2" />
      <plugin id="org.eclipse.core.runtime" autoStart="true" startLevel="0" />
      <plugin id="org.eclipse.equinox.common" autoStart="true" startLevel="2" />
      <plugin id="org.eclipse.equinox.ds" autoStart="true" startLevel="2" />
      <plugin id="org.eclipse.equinox.event" autoStart="true" startLevel="2" />
      <plugin id="org.eclipse.equinox.simpleconfigurator" autoStart="true" startLevel="1" />
      <plugin id="org.eclipse.update.configurator" autoStart="true" startLevel="4" />
      <property name="org.eclipse.update.reconcile" value="false" />
      <property name="osgi.instance.area.default" value="@user.home/ConvertigoWorkspace" os="macosx" />
   </configurations>

   <repositories>
      <repository location="https://convertigo-update-site.s3.eu-west-3.amazonaws.com/@convertigoMode@/@convertigoMajor@" enabled="true" />
      <repository location="https://download.eclipse.org/releases/@eclipseVersion@" enabled="true" />
   </repositories>

   <preferencesInfo>
      <targetfile overwrite="false"/>
   </preferencesInfo>

   <cssInfo>
   </cssInfo>

</product>
