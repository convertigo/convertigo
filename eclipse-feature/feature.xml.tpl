<?xml version="1.0" encoding="UTF-8"?>
<feature
      id="com.twinsoft.convertigo.studio.feature"
      label="Convertigo Studio"
      version="@convertigoVersion@.qualifier"
      provider-name="Convertigo"
      plugin="com.twinsoft.convertigo.studio.product">

   <description url="https://www.convertigo.com">
      Convertigo is the first open source Enterprise Grade Low Code / No Code platform to accelerate your Enterprise digitalization for reasonable costs.
   </description>

   <copyright>
      Copyright (c) 2001-@CURRENT_YEAR@ Convertigo.  All rights reserved.
   </copyright>

   <license url="https://c8ocloud.convertigo.net/convertigo/licenses/index.html">@licenseText@</license>

   <url>
      <update label="Convertigo Update Site" url="https://convertigo-update-site.s3.eu-west-3.amazonaws.com/release/@convertigoVersionMajor@"/>
      <discovery label="Convertigo  discovery site" url="https://www.convertigo.com/"/>
   </url>

   <requires>
      <import plugin="org.eclipse.core.runtime"/>
      <import plugin="org.eclipse.ui"/>
      <import plugin="org.eclipse.ui.intro.universal"/>
      <import plugin="org.eclipse.ui.intro"/>
      <import plugin="org.eclipse.swt"/>
      <import plugin="org.eclipse.core.resources"/>
      <import plugin="org.eclipse.jface.text"/>
      <import plugin="org.eclipse.ui.console"/>
      <import plugin="org.eclipse.ui.ide"/>
      <import plugin="org.eclipse.ui.views"/>
      <import plugin="org.eclipse.wst.xml.ui"/>
      <import plugin="org.eclipse.wst.sse.ui"/>
      <import plugin="org.eclipse.wst.sse.core"/>
      <import feature="org.eclipse.egit"/>
      <import feature="org.eclipse.egit.gitflow.feature"/>
      <import feature="org.eclipse.platform"/>
      <import feature="org.eclipse.mylyn.wikitext_feature"/>
      <import feature="org.eclipse.mylyn.wikitext.editors_feature"/>
      <import feature="org.eclipse.rcp"/>
      <import plugin="org.eclipse.core.expressions"/>
      <import plugin="org.eclipse.egit.ui"/>
      <import plugin="org.eclipse.jgit"/>
      <import plugin="org.eclipse.ui.editors"/>
      <import plugin="org.eclipse.ui.genericeditor"/>
      <import plugin="org.eclipse.ui.workbench.texteditor"/>
      <import plugin="org.eclipse.wildwebdeveloper"/>
      <import plugin="com.twinsoft.convertigo.studio"/>
   </requires>

   <plugin
         id="com.twinsoft.convertigo.studio.product"
         download-size="0"
         install-size="0"
         version="@convertigoVersion@.qualifier"
         unpack="false"/>

   <plugin
         id="com.twinsoft.convertigo.studio"
         download-size="0"
         install-size="0"
         version="@convertigoVersion@.qualifier"/>

   <plugin
         id="com.twinsoft.convertigo.studio.jxbrowser.win64"
         os="win32"
         ws="win32"
         arch="x86_64"
         download-size="0"
         install-size="0"
         version="@convertigoVersion@.qualifier"
         fragment="true"/>

   <plugin
         id="com.twinsoft.convertigo.studio.jxbrowser.mac"
         os="macosx"
         ws="cocoa"
         arch="x86_64"
         download-size="0"
         install-size="0"
         version="@convertigoVersion@.qualifier"
         fragment="true"/>

   <plugin
         id="com.twinsoft.convertigo.studio.jxbrowser.mac.arm"
         os="macosx"
         ws="cocoa"
         arch="aarch64"
         download-size="0"
         install-size="0"
         version="@convertigoVersion@.qualifier"
         fragment="true"/>

   <plugin
         id="com.twinsoft.convertigo.studio.jxbrowser.linux64"
         os="linux"
         ws="gtk"
         arch="x86_64"
         download-size="0"
         install-size="0"
         version="@convertigoVersion@.qualifier"
         fragment="true"/>
</feature>
