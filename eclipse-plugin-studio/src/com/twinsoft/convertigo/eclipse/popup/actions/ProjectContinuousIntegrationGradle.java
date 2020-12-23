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

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;

public class ProjectContinuousIntegrationGradle extends MyAbstractAction {	
	final static private String BASE_URL = "https://github.com/convertigo/convertigo-common-resources/raw/7.9.0/";
	
	private Set<String> backupFiles = new TreeSet<String>();

	public ProjectContinuousIntegrationGradle() {
		super();
	}
	
	private void download(String url, File dest, String file, boolean backup) throws IOException {
		HttpGet get = new HttpGet(url);
		File target = File.createTempFile("convertigoGradle", ".tmp");
		dest = new File(dest, file);
		try {
			target.deleteOnExit();
			try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(get)) {
				long length = response.getEntity().getContentLength();
				if (length < 1) {
					length = Integer.MAX_VALUE;
				}
				try (FileOutputStream fos = new FileOutputStream(target)) {
					InputStream is = response.getEntity().getContent();
					byte[] buf = new byte[1024 * 1024];
					int n;
					long t = 0;
					while (t < length && (n = is.read(buf, 0, (int) Math.min(length - t, buf.length))) > -1) {
						fos.write(buf, 0, n);
						t += n;
					}
				}
			}
			if (target.exists()) {
				if (dest.exists() && backup) {
					if (!FileUtils.readFileToString(target, StandardCharsets.UTF_8).equals(FileUtils.readFileToString(dest, StandardCharsets.UTF_8))) {
						File bak = new File(dest.getParentFile(), dest.getName() + ".bak");
						FileUtils.deleteQuietly(bak);
						FileUtils.moveFile(dest, bak);
						backupFiles.add(file);
					}
				}
				dest.getParentFile().mkdirs();
				FileUtils.copyFile(target, dest);
			}
		} finally {
			target.delete();
		}
	}
	
	private void updateGradle(File dir) throws IOException {
		for (String file: new String[] {
			"gradlew",
			"gradlew.bat",
			"gradle/wrapper/gradle-wrapper.jar",
			"gradle/wrapper/gradle-wrapper.properties"
		}) {
			download(BASE_URL + "gradle/" + file, dir, file, false);
		}
		for (String file: new String[] {
			"build.gradle",
			"settings.gradle"
		}) {
			download(BASE_URL + "gradle/" + file, dir, file, true);
		}
	}
	
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);

		Shell shell = getParentShell();
		shell.setCursor(waitCursor);

		try {
			ProjectExplorerView explorerView = getProjectExplorerView();
			if (explorerView != null) {
				TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
				if (treeObject != null && treeObject instanceof ProjectTreeObject) {
					ProjectTreeObject projectTreeObject = (ProjectTreeObject) treeObject;
					Project project = projectTreeObject.getObject();
					File dir = project.getDirFile();
					int code = ConvertigoPlugin.questionMessageBox(shell, "This will put Continuous Integration template + gradle files in your project.\nIf files already exist, your version will be renamed as a '.bak' file.");
					if (code == SWT.NO) {
						return;
					}
					String id = action.getId();
					IProject iproject = projectTreeObject.getIProject();
					Job.create("Update CI resources of " + projectTreeObject.getName(), (monitor) -> {
						try {
							backupFiles.clear();
							updateGradle(dir);
							if (id.endsWith(".circleci")) {
								download(BASE_URL + "github-actions/main.yml", dir, ".github/workflows/main.yml", true);
							} else if (id.endsWith(".githubactions")) {
								download(BASE_URL + "github-actions/main.yml", dir, ".github/workflows/main.yml", true);
							}
							iproject.refreshLocal(1, monitor);
						} catch (Exception e) {
							Engine.logStudio.error("failed to update gradle resources", e);
						}
					}).schedule();
				}
			}
		} finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}
}
