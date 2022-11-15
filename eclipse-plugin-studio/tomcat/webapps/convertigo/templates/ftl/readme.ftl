<#-- This FTL template helps generating the readme.md file of your project -->
<#-- see FTL language documentation : https://freemarker.apache.org/docs/index.html -->

<#-- GLOBALS -->
<#global lineBreak = settings.lineBreak />
<#global locale = "US" />
<#global dictionnary = {
		"installation":	{"US": "Installation"			, "FR": "Installation"},
		"more.info": 	{"US": "For more technical informations"	, "FR": "Pour plus d'informations techniques"},
		"connectors": 	{"US": "Connectors"				, "FR": "Connecteurs"},
		"transactions": {"US": "Transactions"			, "FR": "Transactions"},
		"sequences": 	{"US": "Sequences"				, "FR": "Séquences"},
		"references": 	{"US": "References"				, "FR": "Références"},
		"urlmapper": 	{"US": "Rest Web Service"		, "FR": "Service Web REST"},
		"mappings": 	{"US": "Mappings"				, "FR": "Mappages"},
		"operations": 	{"US": "Operations"				, "FR": "Operations"},
		"parameters": 	{"US": "Parameters"				, "FR": "Paramètres"},
		"mobileapp": 	{"US": "Mobile Application"		, "FR": "Application Mobile"},
		"mobilelib": 	{"US": "Mobile Library"			, "FR": "Librairie Mobile"},
		"pages": 		{"US": "Pages"					, "FR": "Pages"},
		"actions": 		{"US": "Shared Actions"			, "FR": "Actions partagées"},
		"components": 	{"US": "Shared Components"		, "FR": "Composants partagés"},
		"variables": 	{"US": "variables"				, "FR": "variables"},
		"events": 		{"US": "events"					, "FR": "évènements"}
	}
/>
<#-- please modify the global show values as needed -->
<#global show = {
	"toc"			: true,
	"installation"	: true,
	
	"connectors"	: false,
	"transactions"	: true,
	"sequences"		: !has(project, "urlmapper") && !has(project, "mobileapp"),
	
	"references"	: false,
	
	"urlmapper"		: true,
	"mappings"		: true,
	"operations"	: true,
	"parameters"	: true,
	
	"mobileapp"		: true,
	"pages"			: !project.name?starts_with("lib_"),
	"actions"		: true,
	"components"	: true,
	
	"variables"		: true,
	"events"		: true
	} 
/>

<#-- FUNCTIONS -->
<#-- on: returns the show flag for the given key -->
<#function on key>
  <#return show[key]?? && show[key]>
</#function>

<#-- on: test if given dbo has the given key with non empty size -->
<#function has dbo key>
  <#return dbo[key]?? && (dbo[key]?size > 0) >
</#function>

<#-- anchor: generates an anchor link for the given text -->
<#function anchor anchors text>
  <#assign a = ""+ text?lower_case?replace(" ", "-")?replace("/", "")>
  <#if anchors?seq_contains(a)>
  	<#assign f = anchors?filter(s -> s?matches(""+ a + "-(\\d+)"))>
  	<#assign a = ""+ a + "-" + (f?size+1)>
  </#if>
  <#assign anchors += [""+a]>
  <#return a>
</#function>

<#-- on: returns the dictionnary value for the given key -->
<#function help key>
  <#if has(dictionnary, key)>
    <#return dictionnary[key][locale]!key>
  </#if>
  <#return key>
</#function>

<#-- MACROS -->
<#-- header: generates a header with given text as heading and add it to TOC with its anchor link -->
<#macro header toc anchors heading text>
${heading} ${text}${lineBreak}
<#assign a = anchor(anchors, text)>
<#if (heading?keep_before_last("#")?length > 0)>
<#assign toc += "" + heading?keep_before_last("##")?replace("#","    ") + "-" + " ["+text+"](#"+ a +")" + lineBreak>
</#if>
</#macro>

<#-- comment: add given text -->
<#macro comment text>
<#if (text?length > 0) >
${text}${lineBreak}
</#if>
</#macro>

<#-- table: generates a table with given headers and rows -->
<#macro table title headers rows>
<#if (rows?size > 0)>
${title}${lineBreak}
<table>
<tr>
<#list headers as header><th>${header}</th></#list>
</tr>
<#list rows as i>
<tr>
<#list headers as header><td>${i[header]}</td></#list>
</tr>
</#list>
</table>${lineBreak}
</#if>
</#macro>

<#-- installation : add project installation instructions if any -->
<#macro installation>
<#if locale == "US">
1. In your Convertigo Studio use `File->Import->Convertigo->Convertigo Project` and hit the `Next` button
2. In the dialog `Project remote URL` field, paste the text below:
   <table>
     <tr><td>Usage</td><td>Click the copy button</td></tr>
     <tr><td>To contribute</td><td>${lineBreak}
     ```
     ${project.contributeUrl}
     ```
     </td></tr>
     <tr><td>To simply use</td><td>${lineBreak}
     ```
     ${project.usageUrl}
     ```
     </td></tr>
    </table>
3. Click the `Finish` button. This will automatically import the __${project.name}__ project
</#if>
<#if locale == "FR">
1. Dans votre Studio Convertigo, utilisez `File->Import->Convertigo->Convertigo Project` et appuyez sur le bouton `Next`
2. Dans le champ `Project remote URL` de la boîte de dialogue, collez le texte ci-dessous:
   <table>
     <tr><td>Usage</td><td>Cliquez sur le bouton de copie</td></tr>
     <tr><td>Pour contribuer</td><td>${lineBreak}
     ```
     ${lineBreak}${project.contributeUrl}
     ```
     </td></tr>
     <tr><td>Pour simplement utiliser</td><td>${lineBreak}
     ```
     ${lineBreak}${project.usageUrl}
     ```
     </td></tr>
    </table>
3. Cliquez sur le bouton `Finish`. Cela importera automatiquement le projet __${project.name}__
</#if>
${lineBreak}
</#macro>

<#-- DEFAULT PROJECT TEMPLATE -->

<#-- anchors variable for TOC : do not modify -->
<#assign anchors = [""]>
<#-- toc variable : do not modify -->
<#assign toc = "">

<#-- Please modify below templates as needed -->

<#-- intro variable : add project header and comment -->
<#assign intro>
	<@header toc=toc anchors=anchors heading="#" text=project.label />
	<@comment text=project.comment />
	<#-- you can add your text or own macro call here to add something -->
	<#--
	This is text i want to add after the project comment
	<@my_own_macro my_var='xxxx xxxxx xxxxx'>
	-->
</#assign>

<#-- content variable : add project sub-beans header and comment -->
<#-- you can add your text or own macro call anywhere -->
<#assign content>
<#if on("installation") && (project.url?length > 0) && (project.url != project.name)>
	<@header toc=toc anchors=anchors heading="##" text=help("installation") />
	<@installation />
</#if>
<#if on("references") && has(project,"references")>
  	<@header toc=toc anchors=anchors heading="##" text=help("references") />
  	<#list project.references as reference>
    	<@header toc=toc anchors=anchors heading="###" text=reference.label />
    	<@comment text=reference.comment />
  	</#list>
</#if>
<#if on("sequences") && has(project,"sequences")>
  	<@header toc=toc anchors=anchors heading="##" text=help("sequences") />
  	<#list project.sequences as sequence>
    	<@header toc=toc anchors=anchors heading="###" text=sequence.label />
    	<@comment text=sequence.comment />
    	<#if on("variables") && has(sequence,"variables")>
      		<@table title="**"+help("variables")+"**" headers=["name","comment"] rows=sequence.variables />
    	</#if>
  </#list>
</#if>
<#if on("connectors") && has(project,"connectors")>
  	<@header toc=toc anchors=anchors heading="##" text=help("connectors") />
  	<#list project.connectors as connector>
    	<@header toc=toc anchors=anchors heading="###" text=connector.label />
    	<@comment text=connector.comment />
    	<#if on("transactions") && has(connector,"transactions")>
      		<@header toc=toc anchors=anchors heading="####" text=help("transactions") />
      		<#list connector.transactions as transaction>
        		<@header toc=toc anchors=anchors heading="#####" text=transaction.label />
        		<@comment text=transaction.comment />
        		<#if on("variables") && has(transaction,"variables")>
          			<@table title="**"+help("variables")+"**" headers=["name","comment"] rows=transaction.variables />
        		</#if>
      		</#list>
    	</#if>
  	</#list>
</#if>
<#if on("urlmapper") && has(project,"urlmapper")>
  	<@header toc=toc anchors=anchors heading="##" text=help("urlmapper") />
  	<@comment text=project.urlmapper.comment />
  	<#if on("mappings") && has(project.urlmapper,"mappings")>
	  	<@header toc=toc anchors=anchors heading="###" text=help("mappings") />
	  	<#list project.urlmapper.mappings as mapping>
	    	<@header toc=toc anchors=anchors heading="####" text=mapping.label />
	    	<@comment text=mapping.comment />
	    	<#if on("operations") && has(mapping,"operations")>
	      		<@header toc=toc anchors=anchors heading="#####" text=help("operations") />
	      		<#list mapping.operations as operation>
	        		<@header toc=toc anchors=anchors heading="######" text=operation.label />
	        		<@comment text=operation.comment />
	        		<#if on("parameters") && has(operation,"parameters")>
	          			<@table title="**"+help("parameters")+"**" headers=["name","comment"] rows=operation.parameters />
	        		</#if>
	      		</#list>
	    	</#if>
	  </#list>
	</#if>
</#if>
<#if on("mobileapp") && has(project,"mobileapp")>
	<#assign appname = (project.mobileapp.applicationName?length > 0)
			?string(project.mobileapp.applicationName, (project.name?starts_with("lib_"))?string(help("mobilelib"),help("mobileapp"))) />
  	<@header toc=toc anchors=anchors heading="##" text=appname />
  	<@comment text=project.mobileapp.comment />
  	<#if on("pages") && has(project.mobileapp,"pages")>
	  	<@header toc=toc anchors=anchors heading="###" text=help("pages") />
	  	<#list project.mobileapp.pages as page>
	    	<@header toc=toc anchors=anchors heading="####" text=page.label />
	    	<@comment text=page.comment />
 	  </#list>
	</#if>
  	<#if on("actions") && has(project.mobileapp,"actions")>
	  	<@header toc=toc anchors=anchors heading="###" text=help("actions") />
	  	<#list project.mobileapp.actions as action>
	    	<@header toc=toc anchors=anchors heading="####" text=action.label />
	    	<@comment text=action.comment />
    		<#if on("variables") && has(action,"variables")>
      			<@table title="**"+help("variables")+"**" headers=["name","comment"] rows=action.variables />
    		</#if>
	  </#list>
	</#if>
  	<#if on("components") && has(project.mobileapp,"components")>
	  	<@header toc=toc anchors=anchors heading="###" text=help("components") />
	  	<#list project.mobileapp.components as component>
	    	<@header toc=toc anchors=anchors heading="####" text=component.label />
	    	<@comment text=component.comment />
    		<#if on("variables") && has(component,"variables")>
      			<@table title="**"+help("variables")+"**" headers=["name","comment"] rows=component.variables />
    		</#if>
    		<#if on("events") && has(component,"events")>
      			<@table title="**"+help("events")+"**" headers=["name","comment"] rows=component.events />
    		</#if>
	  </#list>
	</#if>
</#if>
</#assign>


<#-- output project name and comment -->
${intro}
<#-- output project.md link -->
${help("more.info")} : [documentation](./project.md)

<#-- output table of content -->
<#if on("toc")>${toc}</#if>

<#-- output project content -->
${content}

