# Guide to install and configure a development Convertigo Studio

## Install Open JDK 15

[Go to this link.](https://adoptopenjdk.net/releases.html?variant=openjdk15&jvmVariant=hotspot)

Select your **Operating System** and the **x64** Architecture, then install the JDK somewhere.

## Install Eclipse 2020-06

[Go to this link.](https://www.eclipse.org/downloads/packages/release/2020-09/r/eclipse-ide-eclipse-committers)

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

In the Gradle Tasks view, execute convertigo > convertigo > setupStudioDev (Eclipse configuration, manifests and admin web build).

Refresh the convertigo project.

Afterwards, for instance after a dependency update, the External Tools menu offers **setupStudioDev (macOS, Linux)** and **setupStudioDev (Windows)**: they run the same task with the JDK of the JavaSE-17 execution environment, then refresh the convertigo project.

The providers (couchdb, sap, plugins) are used as jars. To debug one from its local checkout, see `c8o.localProviders` in gradle.properties.

When the dialog for ModuleCoreNature comes again, click the **Show Solution** and Install Eclipse Java Enterprise Developer Tools.

At the wizard with checkboxes, check only:
* Eclipse Java EE Developer Tools
* Eclipse Java Web Developer Tools
* JST Server UI
* JST Server Adapters
* JST Server Adapters Extensions
* (Eclipse XML Editors and Tools)
* (Docker Tooling)

After installation, reboot and Eclipse is ready.

## Generate templates

Run from the **Gradle Tasks** view the task **convertigo/buildTemplates**.

## Launch the studio

Open the menu Run > Run Configurations...

Then create an **Eclipse application** entry, named **Convertigo Studio** and choose the product **com.twinsoft.convertigo.studio.product.ConvertigoProduct** in the combo box.

The Studio is ready to be launched.
