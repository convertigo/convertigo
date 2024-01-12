/*
 * Copyright (c) 2001-2024 Convertigo SA.
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
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
import com.twinsoft.convertigo.engine.EngineException;

public class ProjectContinuousIntegrationGradle extends MyAbstractAction {
	final static private String BASE_URL = "https://github.com/convertigo/convertigo-common-resources/raw/8.3.0/";

	private Set<String> backupFiles = new TreeSet<String>();
	private File dest;
	private String suffix;

	public ProjectContinuousIntegrationGradle() {
		super();
	}

	private void downloadFiles(String url) throws Exception {
		JSONObject json = getJSON(url);
		if (json.has("imports")) {
			JSONArray array = json.getJSONArray("imports");
			for (int i = 0; i < array.length(); i++) {
				String file = array.getString(i);
				downloadFiles(BASE_URL + file);
			}
		}
		if (json.has("files")) {
			JSONArray array = json.getJSONArray("files");
			for (int i = 0; i < array.length(); i++) {
				JSONObject file = array.getJSONObject(i);
				boolean backup = false;
				try {backup = file.getBoolean("backup");} catch (Exception e) {};
				download(BASE_URL + file.getString("from"), file.getString("to"), backup);
			}
		}
	}

	private JSONObject getJSON(String url) throws Exception {
		HttpGet get = new HttpGet(url);
		try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(get)) {
			int code = response.getStatusLine().getStatusCode();
			if (code != 200) {
				throw new EngineException("Code " + code + " for " + url);
			}
			String sContent = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
			return new JSONObject(sContent);
		}
	}

	private void download(String url, String file, boolean backup) throws IOException {
		HttpGet get = new HttpGet(url);
		File target = File.createTempFile("convertigoGradle", ".tmp");
		File dest = new File(this.dest, file);
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
					String sTarget = FileUtils.readFileToString(target, StandardCharsets.UTF_8).replaceAll("[\\s\\n\\r]+", "");
					String sDest = FileUtils.readFileToString(dest, StandardCharsets.UTF_8).replaceAll("[\\s\\n\\r]+", "");
					if (!sTarget.equals(sDest)) {
						File bak = new File(dest.getParentFile(), dest.getName() + suffix);
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
					dest = project.getDirFile();
					suffix = "." + new SimpleDateFormat("yy-MM-dd_HH-mm-ss").format(new Date()) + ".bak";
					int code = ConvertigoPlugin.questionMessageBox(shell, "This will put configuration files in your project.\nIf files already exist, your version will be renamed as a '" + suffix + "' file.");
					if (code == SWT.NO) {
						return;
					}
					String id = action.getId();
					Matcher matcher = Pattern.compile(".*\\.(.*)").matcher(id);
					if (!matcher.matches()) {
						return;
					}
					String type = matcher.group(1);

					IProject iproject = projectTreeObject.getIProject();
					Job.create("Update CI resources of " + projectTreeObject.getName(), (monitor) -> {
						try {
							backupFiles.clear();
							downloadFiles(BASE_URL + type + ".json");
							iproject.refreshLocal(1, monitor);
							if (!backupFiles.isEmpty()) {
								ConvertigoPlugin.infoMessageBox("Backup done in " + backupFiles);
							}
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
