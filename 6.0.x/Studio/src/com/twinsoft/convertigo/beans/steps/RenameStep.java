package com.twinsoft.convertigo.beans.steps;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class RenameStep extends Step {

	private static final long serialVersionUID = -2873578590344942963L;

	private String sourcePath = "";
	private String newName = "";
	private boolean overwrite = false;

	public RenameStep() {
		super();
	}

	public Object clone() throws CloneNotSupportedException {
		RenameStep clonedObject = (RenameStep) super.clone();
		return clonedObject;
	}

	public Object copy() throws CloneNotSupportedException {
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
		if (isEnable) {
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

						if (sourceFile.isDirectory()) {
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
	protected boolean workOnSource() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected StepSource getSource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toJsString() {
		// TODO Auto-generated method stub
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

}
