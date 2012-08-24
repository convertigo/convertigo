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

public class CopyStep extends Step {

	private static final long serialVersionUID = -2873578590344942963L;

	private String sourcePath = "";
	private String destinationPath = "";
	private boolean overwrite = false;

	public CopyStep() {
		super();
	}

	@Override
	public CopyStep clone() throws CloneNotSupportedException {
		CopyStep clonedObject = (CopyStep) super.clone();
		return clonedObject;
	}

	@Override
	public CopyStep copy() throws CloneNotSupportedException {
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
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.stepExecute(javascriptContext, scope)) {
				try {
					// Check source
					String sSourcePath = getAbsoluteFilePath(evaluateSourcePath(javascriptContext, scope));
					File sourceFile = new File(sSourcePath);

					String sDestinationPath = getAbsoluteFilePath(evaluateDestinationPath(javascriptContext,
							scope));
					
					Engine.logBeans.info("File I/O step: " + getAction() + " file or directory \"" + sSourcePath
							+ "\" to \"" + sDestinationPath + "\"...");

					if (!sourceFile.exists()) {
						throw new EngineException("Source file or directory does not exist: " + sSourcePath);
					}

					// Check destination
					File destinationPath = new File(sDestinationPath);
					if (!destinationPath.exists()) {
						throw new EngineException("Destination directory does not exist: " + sDestinationPath);
					} else {
						// The destination path must be a directory
						if (!destinationPath.isDirectory()) {
							throw new EngineException("Destination path is not a directory: "
									+ sDestinationPath);
						}
					}

					destinationPath = new File(destinationPath, sourceFile.getName());

					// Check the existence of the destination element to be
					// created
					if (destinationPath.exists()) {
						if (overwrite) {
							destinationPath.delete();
						} else {
							throw new EngineException(
									"The destination \""
											+ destinationPath.getAbsolutePath()
											+ "\" already exists.\n"
											+ " Please set the \"Overwrite\" property to true if you want to overwrite it.");
						}
					}

					try {
						if (sourceFile.isDirectory()) {
							doActionForSourceDirectory(sourceFile, destinationPath);
							Engine.logBeans.info("Directory copied from \"" + sSourcePath + "\" to \""
									+ sDestinationPath + "\".");
						} else if (sourceFile.isFile()) {
							doActionForSourceFile(sourceFile, destinationPath);
							Engine.logBeans.info("File copied from \"" + sSourcePath + "\" to \""
									+ sDestinationPath + "\".");
						}
					} catch (IOException e) {
						throw new EngineException("Unable to " + getAction() + "\"" + sSourcePath + "\" to \""
								+ sDestinationPath + "\"", e);
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

	protected String getAction() {
		return "copy";
	}

	protected void doActionForSourceFile(File sourceFile, File destinationFile) throws IOException {
		FileUtils.copyFile(sourceFile, destinationFile);
	}

	protected void doActionForSourceDirectory(File sourceFile, File destinationFile) throws IOException {
		FileUtils.copyDirectory(sourceFile, destinationFile);
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

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public boolean isOverwrite() {
		return overwrite;
	}
}
