/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.beans.steps;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class RenameStep extends Step {

	private static final long serialVersionUID = -2873578590344942963L;

	private String sourcePath = "";
	private String newName = "";
	private boolean overwrite = false;

	public RenameStep() {
		super();
	}

	@Override
	public RenameStep clone() throws CloneNotSupportedException {
		RenameStep clonedObject = (RenameStep) super.clone();
		return clonedObject;
	}

	@Override
	public RenameStep copy() throws CloneNotSupportedException {
		RenameStep copiedObject = (RenameStep) super.copy();
		return copiedObject;
	}

	private String evaluateSourcePath(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, sourcePath, "sourcePath", false);
	}

	private String evaluateNewName(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, newName, "newName", false);
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled()) {
			if (super.stepExecute(javascriptContext, scope)) {
				try {
					String sourceFilePath = getAbsoluteFilePath(evaluateSourcePath(javascriptContext, scope));
					String newFileName = evaluateNewName(javascriptContext, scope);
					
					Engine.logBeans.info("Renaming file or directory \"" + sourceFilePath + "\" to \""
							+ newFileName + "\"...");

					File sourceFile = new File(sourceFilePath);
					if (!sourceFile.exists()) {
						throw new EngineException("Source file or directory does not exist: " + sourceFilePath);
					}

					if (newFileName.length() == 0) {
						throw new EngineException("Please fill the New name property field.");
					}

					try {
						File destinationFile = new File(sourceFile.getParentFile().getCanonicalPath() + "/"
								+ newFileName);
						if (sourceFile.equals(destinationFile)) {
							Engine.logBeans.info("The source and the destination are the same: \"" + sourceFilePath + "\".");
						} else if (sourceFile.isDirectory()) {
							if (destinationFile.exists() && !overwrite) {
								throw new EngineException(
										"The destination directory "
												+ destinationFile.getAbsolutePath()
												+ " already exists.\n"
												+ " Please set the \"Overwrite\" property to true if you want to overwrite it.");
							} else if (destinationFile.exists() && overwrite) {
								FileUtils.deleteDirectory(destinationFile);
								FileUtils.moveDirectory(sourceFile, destinationFile);
								Engine.logBeans.info("Directory \"" + sourceFilePath + "\" renamed to \""
										+ newFileName + "\".");
							} else if (!destinationFile.exists()) {
								FileUtils.moveDirectory(sourceFile, destinationFile);
								Engine.logBeans.info("Directory \"" + sourceFilePath + "\" renamed to \""
										+ newFileName + "\".");
							}
						} else if (sourceFile.isFile()) {
							if (destinationFile.exists() && !overwrite) {
								throw new EngineException(
										"The destination file "
												+ destinationFile.getAbsolutePath()
												+ " already exists.\n"
												+ " Please set the \"Overwrite\" property to true if you want to overwrite it.");
							} else if (destinationFile.exists() && overwrite) {
								destinationFile.delete();
								FileUtils.moveFile(sourceFile, destinationFile);
								Engine.logBeans.info("File \"" + sourceFilePath + "\" renamed to \""
										+ newFileName + "\".");
							} else if (!destinationFile.exists()) {
								FileUtils.moveFile(sourceFile, destinationFile);
								Engine.logBeans.info("File \"" + sourceFilePath + "\" renamed to \""
										+ newFileName + "\".");
							}
						}
					} catch (IOException e) {
						throw new EngineException("Unablie to rename the file or directory \""
								+ sourceFilePath + "\" renamed to \"" + newFileName + "\".", e);
					}
				} catch (EngineException e) {
					setErrorStatus(true);
					throw e;
				}
				return true;
			}
		}
		return false;
	}

	protected String getAbsoluteFilePath(String entry) throws EngineException {
		if (entry.equals(""))
			throw new EngineException("Please fill the Source property field.");

		return Engine.theApp.filePropertyManager.getFilepathFromProperty(entry, getProject().getName());
	}

	@Override
	public String toJsString() {
		return null;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}
	
	@Override
	protected void onBeanNameChanged(String oldName, String newName) {
		if (oldName.startsWith(StringUtils.normalize(sourcePath))) {
			sourcePath = newName;
			hasChanged = true;
		}
	}
}
