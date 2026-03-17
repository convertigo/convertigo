/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.jgit.api.Git;

import com.twinsoft.convertigo.beans.core.Project;

public final class ProjectGitHelper {

	private ProjectGitHelper() {
	}

	public static void ensureGitRepository(Project project) {
		if (project == null) {
			return;
		}

		String autoCreate = ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_AUTO_CREATE_PROJECT_GIT_REPOSITORY);
		if (!"true".equalsIgnoreCase(autoCreate)) {
			return;
		}

		File projectDir = project.getDirFile();
		if (projectDir == null || new File(projectDir, ".git").exists()) {
			return;
		}

		try (Git git = Git.init().setDirectory(projectDir).call()) {
			git.add().addFilepattern(".").call();
			git.commit().setMessage("Initial commit").call();

			@SuppressWarnings("restriction")
			boolean ok = org.eclipse.egit.core.RepositoryUtil.INSTANCE.addConfiguredRepository(git.getRepository().getDirectory());
			if (ok) {
				ConvertigoPlugin.getDisplay().asyncExec(() -> {
					try {
						IProject iproject = ConvertigoPlugin.getDefault().getProjectPluginResource(project.getName());
						iproject.close(null);
						iproject.open(null);
					} catch (Exception e) {
						ConvertigoPlugin.logException(e, "An error occured while refreshing git state for the project", false);
					}
				});
			}
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "An error occured while create git repository for the project", false);
		}
	}
}
