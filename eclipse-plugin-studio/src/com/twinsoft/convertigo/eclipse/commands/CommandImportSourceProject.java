/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.commands;

import java.io.File;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNode;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;

public class CommandImportSourceProject extends AbstractHandler {
	Pattern reSkipDir = Pattern.compile("^\\.|^\\_");
	Pattern reKey = Pattern.compile("(.*?)  →.*");
	
	SortedMap<String, File> projects = new TreeMap<>();
	long nextCheck = 0;
	File lastFile;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			File file = getFile((TreeSelection) HandlerUtil.getCurrentSelection(event));
			projects.clear();
			nextCheck = 0;
			if (!checkFile(file)) {
				checkDir(file, true);
			}
			if (!projects.isEmpty()) {
				if (file.isFile()) {
					Entry<String, File> project = projects.entrySet().iterator().next();
					ConvertigoPlugin.getDefault().getProjectExplorerView().importProject(project.getValue().getAbsolutePath(), project.getKey());
				} else {
					Object[] elements = new String[projects.size()];
					int i = 0;
					int len = file.getAbsolutePath().length() + 1;
					for (Entry<String, File> project: projects.entrySet()) {
						elements[i++] = project.getKey() + "  → " + project.getValue().getAbsolutePath().substring(len);
					}
					ListSelectionDialog dialog = ListSelectionDialog.of(elements).contentProvider(ArrayContentProvider.getInstance()).labelProvider(new LabelProvider()).message("Select Convertigo project to import").create(ConvertigoPlugin.getMainShell());
					dialog.setTitle("Import Convertigo projects");
					dialog.setInitialSelections(elements);
					dialog.open();

					Object [] result = dialog.getResult();
					if (result.length > 0) {
						Matcher matcher = reKey.matcher("");
						for (Object res: result) {
							matcher.reset(res.toString());
							if (matcher.matches()) {
								String projectName = matcher.group(1);
								File projectFile = projects.get(projectName);
								ConvertigoPlugin.getDefault().getProjectExplorerView().importProject(projectFile.getAbsolutePath(), projectName);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		try {
			setBaseEnabled(false);
			File file = getFile((TreeSelection) SelectionUtils.getSelection((IEvaluationContext) evaluationContext));
			long now = System.currentTimeMillis();
			if (file != lastFile || now > nextCheck) {
				projects.clear();
				nextCheck = now + 2000;
				if (!checkFile(file)) {
					checkDir(file, false);
				}
			}
			setBaseEnabled(!projects.isEmpty());
		} catch (Exception e) {
		}
		super.setEnabled(evaluationContext);
	}
	
	private File getFile(TreeSelection selection) {
		RepositoryTreeNode<?> node = (RepositoryTreeNode<?>) selection.getFirstElement();
		Object o = node.getObject();
		return (o instanceof File) ? (File) o : node.getPath().toFile();
	}
	
	private boolean checkFile(File file) {
		try {
			if (file.isFile() && DatabaseObjectsManager.getProjectVersion(file) != null) {
				String projectName = DatabaseObjectsManager.getProjectName(file);
				if (projectName != null) {
					if (file.getName().endsWith(".xml")) {
						File exFile = projects.get(projectName);
						if (exFile.getName().equals("c8oProject.yaml")) {
							return false;
						}
					}
					projects.put(projectName, file);
					return true;
				}
			}
		} catch (Exception e) {
		}
		return false;
	}
	
	private boolean checkDir(File file, boolean all) {
		if (file.isDirectory() && !reSkipDir.matcher(file.getName()).find()) {
			for (File sFile: file.listFiles()) {
				if (checkFile(sFile)) {
					return true;
				}
			}
			for (File sFile: file.listFiles()) {
				if (checkDir(sFile, all) && !all) {
					return true;
				}
			}
		}
		return false;
	}
}
