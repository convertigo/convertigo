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
	
	CLI getCLI() throws Exception {
		return CLI.instance;
	}
	
	public void apply(Project project) {
		TaskContainer tasks = project.getTasks();
		
		load = tasks.create("load", ProjectLoad.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
		});
		
		export = tasks.create("export", ProjectExport.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
			task.dependsOn(load);
		});
		
		generateMobileBuilder = tasks.create("generateMobileBuilder", GenerateMobileBuilder.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
			task.dependsOn(load);
		});
		
		compileMobileBuilder = tasks.create("compileMobileBuilder", CompileMobileBuilder.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
			task.dependsOn(generateMobileBuilder);
		});
		
		car = tasks.create("car", ProjectCar.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
			task.dependsOn(export);
		});
	}
}
