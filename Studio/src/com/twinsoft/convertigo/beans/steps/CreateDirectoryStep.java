package com.twinsoft.convertigo.beans.steps;

import java.io.File;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class CreateDirectoryStep extends Step {

	private static final long serialVersionUID = -2873578590344942963L;
	
	private String destinationPath = "";	
	private boolean createNonExistentParentDirectories = true;

	public CreateDirectoryStep() {
		super();
	}
	
    public Object clone() throws CloneNotSupportedException {
    	CreateDirectoryStep clonedObject = (CreateDirectoryStep) super.clone();
        return clonedObject;
    }
	
	public Object copy() throws CloneNotSupportedException {
		CreateDirectoryStep copiedObject = (CreateDirectoryStep)super.copy();
		return copiedObject;
	}

	private String evaluateDestinationPath(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, destinationPath, "destinationPath", false);
	}

	@Override
	protected boolean stepExcecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			if (super.stepExcecute(javascriptContext, scope)) {
				try {
					String destinationFilePath = getAbsoluteFilePath(evaluateDestinationPath(javascriptContext, scope));
					File destinationFile = new File(destinationFilePath);
					if (destinationFile.exists() && destinationFile.isFile()) {
						throw new Exception("Destination already exists and is a file: " + destinationFilePath);
					}
					
					boolean directoryCreated = false;
					
					if (isCreateNonExistentParentDirectories()) {
						directoryCreated = destinationFile.mkdirs();
						Engine.logBeans.info("Directory \"" + destinationFilePath + "\" has been created with parent directories.");
					}
					else {
						directoryCreated = destinationFile.mkdir();
						Engine.logBeans.info("Directory \"" + destinationFilePath + "\" has been created.");
					}

					if (!directoryCreated) {
						throw new Exception("An error occured while creating the directory.");
					}
		        } catch (Exception e) {
					setErrorStatus(true);
		            Engine.logBeans.error("An error occured while creating the directory.", e);
				}					
		        return true;
			}
		}
		return false;
	}

	protected String getAbsoluteFilePath(String entry) throws EngineException {
		if (entry.equals(""))
			throw new EngineException("Please fill the Destination property field.");
				
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

	public String getDestinationPath() {
		return destinationPath;
	}


	public void setDestinationPath(String destinationPath) {
		this.destinationPath = destinationPath;
	}

	public void setCreateNonExistentParentDirectories(
			boolean createNonExistentParentDirectories) {
		this.createNonExistentParentDirectories = createNonExistentParentDirectories;
	}

	public boolean isCreateNonExistentParentDirectories() {
		return createNonExistentParentDirectories;
	}
}
