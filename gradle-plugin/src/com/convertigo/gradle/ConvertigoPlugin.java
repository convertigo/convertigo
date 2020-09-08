/*
 * Copyright (c) 2001-2020 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.convertigo.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;

import com.twinsoft.convertigo.engine.CLI;

public class ConvertigoPlugin implements Plugin<Project> {	
	ProjectLoad load;
	ProjectExport export;
	GenerateMobileBuilder generateMobileBuilder;
	CompileMobileBuilder compileMobileBuilder;
	ProjectCar car;
	ProjectDeploy deploy;
	NativeBuild nativeBuild;
	NativeBuildLaunch launchNativeBuild;
	NativeBuildDownload downloadNativeBuild;
	LocalBuild localBuild;
	
	CLI getCLI() throws Exception {
		return CLI.instance;
	}
	
	public void apply(Project project) {
		TaskContainer tasks = project.getTasks();
		
		load = tasks.create("load", ProjectLoad.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
			task.setDescription("Load and migrate the project to the current plugin version.");
		});
		
		export = tasks.create("export", ProjectExport.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
			task.dependsOn(load);
			task.setDescription("Save the project at the current plugin version.");
		});
		
		generateMobileBuilder = tasks.create("generateMobileBuilder", GenerateMobileBuilder.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
			task.dependsOn(load);
			task.setDescription("Generate sources of the Ionic application into _private/ionic.");
		});
		
		compileMobileBuilder = tasks.create("compileMobileBuilder", CompileMobileBuilder.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
			task.dependsOn(generateMobileBuilder);
			task.setDescription("Compile the Ionic application with NPM into DisplayObject/mobile.");
		});
		
		car = tasks.create("car", ProjectCar.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
			task.dependsOn(export);
			task.setDescription("Build a <projectName>.car file.");
		});
		
		deploy = tasks.create("deploy", ProjectDeploy.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("publishing");
			task.dependsOn(car);
			task.setDescription("Push the project to a Convertigo server.");
		});
		
		nativeBuild = tasks.create("nativeBuild", NativeBuild.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("configuration");
			task.dependsOn(load);
			task.setDescription("Configurator task for 'launchNativeBuild' and 'downloadNativeBuild'.");
		});
		
		launchNativeBuild = tasks.create("launchNativeBuild", NativeBuildLaunch.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
			task.dependsOn(nativeBuild);
			task.setDescription("Upload the mobile source package to the Convertigo Phonegap Build Gateway.");
		});
		
		downloadNativeBuild = tasks.create("downloadNativeBuild", NativeBuildDownload.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
			task.dependsOn(launchNativeBuild);
			task.setDescription("Wait the remote build to finish, then download the native packages (iOS ipa or Android apk).");
		});
		
		localBuild = tasks.create("localBuild", LocalBuild.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
			task.dependsOn(load);
			task.setDescription("Build native package for selected platforms.");
		});
	}
}
