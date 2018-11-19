/*
 * Copyright (c) 2001-2018 Convertigo SA.
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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;

import com.twinsoft.convertigo.engine.CLI;

public class ConvertigoPlugin implements Plugin<Project> {
	private CLI cli;
	
	ProjectLoad load;
	ProjectCar car;
	
	synchronized CLI getCLI() throws Exception {
		if (cli == null) {
			cli = new CLI();
		}
		return cli;
	}
	
	public void apply(Project project) {
		TaskContainer tasks = project.getTasks();
		
		load = tasks.create("load", ProjectLoad.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
		});
		
		car = tasks.create("car", ProjectCar.class, (task) -> {
			task.plugin = ConvertigoPlugin.this;
			task.setGroup("build");
			task.dependsOn(load);
			System.out.println("declare CAR");
			
			String destinationDir = (String) project.getProperties().get("convertigo.destinationDir");
			System.out.println("destination dir is: " + destinationDir);
			try {
				FileUtils.write(new File("c:/TMP/output.txt"), "destination dir is:  " + destinationDir, "UTF-8");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (destinationDir != null) {
				task.setDestinationDir(new File(destinationDir));
			} else {
				task.setDestinationDir(project.getBuildDir());
			}
		});
	}
}
