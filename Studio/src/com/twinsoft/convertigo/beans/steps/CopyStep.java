package com.twinsoft.convertigo.beans.steps;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class CopyStep extends Step {

	private static final long serialVersionUID = -2873578590344942963L;

	private String sourcePath = "";
	private String destinationPath = "";

	public CopyStep() {
		super();
	}

	public Object clone() throws CloneNotSupportedException {
		CopyStep clonedObject = (CopyStep) super.clone();
		return clonedObject;
	}

	public Object copy() throws CloneNotSupportedException {
		CopyStep copiedObject = (CopyStep) super.copy();
		return copiedObject;
	}

	private String evaluateSourcePath(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, sourcePath, "sourcePath", false);
	}

	private String evaluateDestinationPath(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, destinationPath, "destinationPath", false);
	}

	@Override
	protected boolean stepExcecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.stepExcecute(javascriptContext, scope)) {
				try {
					String sourceFilePath = getAbsoluteFilePath(evaluateSourcePath(javascriptContext, scope));
					File sourceFile = new File(sourceFilePath);
					if (!sourceFile.exists()) {
						throw new Exception("Source file or directory does not exist: " + sourceFilePath);
					}

					String destinationFilePath = getAbsoluteFilePath(evaluateDestinationPath(
							javascriptContext, scope));
					File destinationFile = new File(destinationFilePath);
					if (!destinationFile.exists()) {
						throw new Exception("Destination directory does not exist: " + destinationFilePath);
					}

					if (sourceFile.isDirectory()) {
						FileUtils.copyDirectory(sourceFile, destinationFile);
						Engine.logBeans.info("Directory copied from \"" + sourceFilePath + "\" to \""
								+ destinationFilePath + "\".");
					} else if (sourceFile.isFile()) {
						destinationFile = new File(destinationFilePath + "/" + sourceFile.getName());
						FileUtils.copyFile(sourceFile, destinationFile);
						Engine.logBeans.info("File copied from \"" + sourceFilePath + "\" to \""
								+ destinationFilePath + "\".");
					}
				} catch (Exception e) {
					setErrorStatus(true);
					Engine.logBeans.error("An error occured while copying the file or directory.", e);
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

	public String getDestinationPath() {
		return destinationPath;
	}

	public void setDestinationPath(String destinationPath) {
		this.destinationPath = destinationPath;
	}
}
