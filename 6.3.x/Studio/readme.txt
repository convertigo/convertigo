Convertigo Studio
=================

Convertigo Studio Licence
=========================
Convertigo Studio is based on Affero GPL V3 Licence. Some modules such as the HTML Connector and the
Legacy Connector are included in object code only, and based on the Convertigo Enterprise Licence.
These modules are provided in Convertigo Studio for demo and development purposes only. See the EULA included
in Convertigo Studio installer for Convertigo Enteprise Licence Details. 


Convertigo Studio Compilation
==============================
In order to compile Convertigo Studio project, you must import the whole project from
our SVN repository inside your Eclipse workspace.

Use the Classic 3.4.2 (and higher) eclipse distribution and use JVM 1.5 (and higher).

The following Eclipse components should have been installed:

* org.eclipse.jface.text
* org.eclipse.wst.xml.ui
* org.eclipse.wst.sse.ui
* org.eclipse.wst.sse.core
* org.eclipse.wst.jsdt.ui
* org.eclipse.wst.jsdt.core

You will find these components in following Eclipse plugins:

* Eclipse Web Developer Tools
* JST Server UI
* JST Server Adapters Extensions

Optionaly, you can consider installing following plugins:

* Subversive SVN Team ProviderSource control plugins (and choose the latest SVNkit connector at the first run)
* Eclipse XML Editors and Tools
* Eclipse XSL Developer Tools
* Eclipse Java Web Developer Tools

After plugins installation and reboot, add the SVN repository and extract the Studio folder as CEMS Eclipse project.

The project should compile without error.

Before launch, open "Run configuration...":
 * Eclipse Application > New
 * Set name "CEMS" to the configuration name
 * click "File System..." in "Workspace data" part
 * go to your current user home folder (like "Documents and Setting" or "Users")
 * create a "convertigo" folder if not exist
 * create a "projects" folder inside the previous "convertigo" folder if not exist
 * select the 'projects' folder and validate
 * goto the "Argument" tab
 * edit "VM arguments" part to put "-Xmx1G -XX:MaxPermSize=256m"
 * press "Run" and the Studio should run
 
/!\ Warning /!\

* There is no templates projects for project creation wizard, but you can build them
  thanks to the provided Ant build procedure: build-template.xml
  This Ant build needs ant-contrib add-on in order to be executed. Just download this
  Ant plugin from https://sourceforge.net/projects/ant-contrib/files/, and add the
  ant-contrib JAR into eclipse's Ant classpath: Preferences -> Ant -> Runtime -> Add
  External Jar in Global Entries.

* To use connectors, you have to rename "convertigo/minime/Java/keys_studio.txt" to
  "convertigo/minime/Java/keys.txt" and restart the studio. 