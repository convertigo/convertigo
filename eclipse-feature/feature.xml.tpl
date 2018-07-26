<?xml version="1.0" encoding="UTF-8"?>
<feature
      id="com.twinsoft.convertigo.studio.feature"
      label="Convertigo Studio"
      version="7.6.0.qualifier"
      provider-name="Convertigo"
      plugin="com.twinsoft.convertigo.studio.product">

   <description url="http://www.convertigo.com">
      Convertigo adds to the eclipse platform a complete Mobile Application Development Platform
to create mobile enterprise applications
   </description>

   <copyright>
      Copyright (c) 2001-@CURRENT_YEAR@ Convertigo.  All rights reserved.
   </copyright>

   <license url="http://www.convertigo.com/en/license.txt">@licenseText@</license>

   <url>
      <update label="Convertigo Update Site" url="http://update.convertigo.com"/>
      <discovery label="Convertigo  discovery site" url="http://discovery.convertigo.com"/>
   </url>

   <requires>
      <import plugin="org.eclipse.core.runtime"/>
      <import plugin="org.eclipse.core.resources"/>
      <import plugin="org.eclipse.jface.text"/>
      <import plugin="org.eclipse.swt"/>
      <import plugin="org.eclipse.ui"/>
      <import plugin="org.eclipse.ui.console"/>
      <import plugin="org.eclipse.ui.ide"/>
      <import plugin="org.eclipse.ui.intro"/>
      <import plugin="org.eclipse.ui.intro.universal"/>
      <import plugin="org.eclipse.ui.views"/>
      <import plugin="org.eclipse.wst.jsdt.ui"/>
      <import plugin="org.eclipse.wst.jsdt.core"/>
      <import plugin="org.eclipse.wst.sse.core"/>
      <import plugin="org.eclipse.wst.sse.ui"/>
      <import plugin="org.eclipse.wst.xml.ui"/>
      <import feature="org.eclipse.egit"/>
      <import feature="org.eclipse.egit.gitflow.feature"/>
      <import feature="org.eclipse.jgit.lfs"/>
      <import feature="org.eclipse.jdt"/>
      <import feature="org.eclipse.pde"/>
      <import feature="org.eclipse.platform"/>
      <import feature="org.eclipse.rcp"/>
      <import feature="org.eclipse.wst.common_core.feature"/>
      <import feature="org.eclipse.wst.jsdt.feature"/>
      <import feature="org.eclipse.wst.web_core.feature"/>
      <import feature="org.eclipse.wst.web_ui.feature"/>
      <import feature="org.eclipse.wst.xml_core.feature"/>
      <import feature="org.eclipse.wst.xml_ui.feature"/>
      <import feature="org.eclipse.wst.xsl.feature"/>
      <import feature="ts-feature"/>
      <import feature="ts-jsdt-feature"/>
      <import feature="ts-json-feature"/>
   </requires>

   <plugin
         id="com.twinsoft.convertigo.studio.product"
         download-size="0"
         install-size="0"
         version="7.6.0.qualifier"
         unpack="false"/>

   <plugin
         id="com.twinsoft.convertigo.studio"
         download-size="0"
         install-size="0"
         version="7.6.0.qualifier"/>

   <plugin
         id="com.twinsoft.convertigo.studio.xulrunner.win32_win32_x86"
         os="win32"
         ws="win32"
         arch="x86"
         download-size="0"
         install-size="0"
         version="7.6.0.qualifier"
         fragment="true"/>

   <plugin
         id="com.twinsoft.convertigo.studio.jxbrowser.win"
         os="win32"
         ws="win32"
         arch="x86,x86_64"
         download-size="0"
         install-size="0"
         version="7.6.0.qualifier"
         fragment="true"/>
         
   <plugin
         id="com.twinsoft.convertigo.studio.jxbrowser.mac"
         os="macosx"
         ws="cocoa"
         arch="x86_64"
         download-size="0"
         install-size="0"
         version="7.6.0.qualifier"
         fragment="true"/>

   <plugin
         id="com.twinsoft.convertigo.studio.jxbrowser.linux64"
         os="linux"
         ws="gtk"
         arch="x86_64"
         download-size="0"
         install-size="0"
         version="7.6.0.qualifier"
         fragment="true"/>
</feature>
