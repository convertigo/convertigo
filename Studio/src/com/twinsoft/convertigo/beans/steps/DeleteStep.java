package com.twinsoft.convertigo.beans.steps;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class DeleteStep extends Step {

	private static final long serialVersionUID = -2873578590344942963L;

	private String sourcePath = "";

	public DeleteStep() {
		super();
	}

	@Override
	public DeleteStep clone() throws CloneNotSupportedException {
		DeleteStep clonedObject = (DeleteStep) super.clone();
		return clonedObject;
	}

	@Override
	public DeleteStep copy() throws CloneNotSupportedException {
		DeleteStep copiedObject = (DeleteStep) super.copy();
		return copiedObject;
	}

	private String evaluateSourcePath(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, sourcePath, "sourcePath", false);
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.stepExecute(javascriptContext, scope)) {
				try {
					String sourceFilePath = getAbsoluteFilePath(evaluateSourcePath(javascriptContext, scope));
					
					Engine.logBeans.info("Deleting file or directory \"" + sourceFilePath + "\"");

					File sourceFile = new File(sourceFilePath);
					if (!sourceFile.exists()) {
						throw new EngineException("Source file or directory does not exist: " + sourceFilePath);
					}

					if (sourceFile.isDirectory()) {
						try {
							FileUtils.deleteDirectory(sourceFile);
						} catch (IOException e) {
							throw new EngineException("Unable to delete \"" + sourceFilePath + "\"");
						}
						Engine.logBeans.info("Directory \"" + sourceFilePath + "\" has been deleted.");
					} else if (sourceFile.isFile()) {
						sourceFile.delete();
						Engine.logBeans.info("File \"" + sourceFilePath + "\" has been deleted.");
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
}
