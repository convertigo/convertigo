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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import com.twinsoft.convertigo.engine.CLI;

public class GenerateMobileBuilder extends ConvertigoTask {
	
	public GenerateMobileBuilder() {
		Project project = getProject();
		
		project.afterEvaluate(p -> {
			Matcher filter = Pattern.compile("\\.gradle|\\.svn|\\.git|build|_private").matcher("");
			getInputs().files((Object[]) project.getProjectDir().listFiles((f, s) -> !filter.reset(s).matches()));
			
//			File yaml = project.file("c8oProject.yaml");
//			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(yaml), "UTF-8"))) {
//				br.readLine();
//				Matcher m = Pattern.compile("↓(.*) \\[core\\.Project\\]:").matcher(br.readLine());
//				if (m.find()) {
//					String projectName = m.group(1);
//					getOutputs().file(new File(destinationDir, projectName + ".car"));
//				}
//			} catch (Exception e) {
//			}
		});
	}
	
	@TaskAction
	void taskAction() throws Exception {
		CLI cli = plugin.getCLI();
		cli.generateMobileBuilder(plugin.load.getConvertigoProject());
	}
}
