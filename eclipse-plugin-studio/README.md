# Guide to install and configure a development Convertigo Studio

## Install Open JDK 11

[Go to this link.](https://adoptopenjdk.net/releases.html?variant=openjdk11&jvmVariant=hotspot)

Select your **Operating System** and the **x64** Architecture, then install the JDK somewhere.

## Install Eclipse 2020-03

[Go to this link.](https://www.eclipse.org/downloads/packages/release/2020-03/r/eclipse-ide-eclipse-committers)

Download the 64 bit **Eclipse Committers** archive.

Extract it where you want.

Edit the **eclipse.ini**:
* after **--launcher.appendVmargs**
* add **-vm path/to/the/jdk/bin/**

## Configure Eclipse

Click menu Window, Show View, select Gradle Tasks.

In the view, import gradle project.

In the wizard, select your git/convertigo folder.

The wizard will automatically import projects, configure a new working set named convertigo.

It may propose you to install org.eclipse.swt.common.modulecore.ModuleCoreNature : CANCEL all stacked dialogs !

In the Gradle Tasks view, execute convertigo > convertigo > generateEclipseConfigurationWithManifest.

Refresh the convertigo project.

When the dialog for ModuleCoreNature comes again, click the **Show Solution** and Install Eclipse Java Enterprise Developer Tools.

At the wizard with checkboxes, check only:
* Eclipse Java EE Developer Tools
* Eclipse Java Web Developer Tools
* JST Server UI
* JST Server Adapters
* JST Server Adapters Extensions
* (Eclipse Web Developer Tools)
* (Eclipse XML Editors and Tools)
* (Docker Tooling)
