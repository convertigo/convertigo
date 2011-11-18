package com.twinsoft.convertigo.beans.steps;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class DeleteStep extends Step {

	private static final long serialVersionUID = -2873578590344942963L;

	private String sourcePath = "";

	public DeleteStep() {
		super();
	}
	
    public Object clone() throws CloneNotSupportedException {
    	DeleteStep clonedObject = (DeleteStep) super.clone();
        return clonedObject;
    }
	
	public Object copy() throws CloneNotSupportedException {
		DeleteStep copiedObject = (DeleteStep)super.copy();
		return copiedObject;
	}
	
	private String evaluateSourcePath(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, sourcePath, "sourcePath", false);
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

					if (sourceFile.isDirectory()) {
						FileUtils.deleteDirectory(sourceFile);
						Engine.logBeans.info("Directory \"" + sourceFilePath + "\" has been deleted.");
					}
					else if (sourceFile.isFile()) {
						sourceFile.delete();
						Engine.logBeans.info("File \"" + sourceFilePath + "\" has been deleted.");
					}						
				} catch (Exception e) {
					setErrorStatus(true);
		            Engine.logBeans.error("An error occured while deleting the file or directory.", e);
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
}
