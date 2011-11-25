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

public class DuplicateStep extends Step {

	private static final long serialVersionUID = 6719621111445726010L;

	private String sourcePath = "";
	private String copyName = "";
	private boolean overwrite = false;

	public DuplicateStep() {
		super();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		DuplicateStep clonedObject = (DuplicateStep) super.clone();
		return clonedObject;
	}

	@Override
	public Object copy() throws CloneNotSupportedException {
		DuplicateStep copiedObject = (DuplicateStep) super.copy();
		return copiedObject;
	}

	private String evaluateSourcePath(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, sourcePath, "sourcePath", false);
	}

	private String evaluateCopyName(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, copyName, "copyName", false);
	}

	@Override
	protected boolean stepExcecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.stepExcecute(javascriptContext, scope)) {
				try {
					String sSourcePath = getAbsoluteFilePath(evaluateSourcePath(javascriptContext, scope));
					File sourceFile = new File(sSourcePath);
					File destinationFile = new File(sourceFile.getParentFile(), evaluateCopyName(javascriptContext, scope));
					String sDestinationFile = destinationFile.getAbsolutePath();
					
					Engine.logBeans.info("Duplicating file or directory \"" + sSourcePath + "\" to \""
							+ sDestinationFile + "\"...");

					if (!sourceFile.exists()) {
						throw new EngineException("Source file or directory does not exist: " + sSourcePath);
					}

					if (destinationFile.exists()) {
						throw new EngineException("Destination directory does not exist: " + sDestinationFile);
					}

					try {
						if (sourceFile.isDirectory()) {
							FileUtils.copyDirectory(sourceFile, destinationFile);
							Engine.logBeans.info("Directory duplicated from \"" + sSourcePath + "\" to \""
									+ sDestinationFile + "\".");
						} else if (sourceFile.isFile()) {
							FileUtils.copyFile(sourceFile, destinationFile);
							Engine.logBeans.info("File duplicated from \"" + sSourcePath + "\" to \""
									+ sDestinationFile + "\".");
						}
					} catch (IOException e) {
						throw new EngineException("Unable to duplicate file or directory from \"" + sSourcePath + "\" to \""
									+ sDestinationFile + "\".", e);
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

	public String getCopyName() {
		return copyName;
	}

	public void setCopyName(String copyName) {
		this.copyName = copyName;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public boolean isOverwrite() {
		return overwrite;
	}
}
