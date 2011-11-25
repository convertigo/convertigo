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
					boolean directoryCreated = false;
					
					String destinationFilePath = getAbsoluteFilePath(evaluateDestinationPath(javascriptContext, scope));
					File destinationFile = new File(destinationFilePath);
					if (destinationFile.exists() && destinationFile.isFile()) {
						throw new EngineException("Destination already exists and is a file: " + destinationFilePath);
					}
					
					if (isCreateNonExistentParentDirectories()) {
						directoryCreated = destinationFile.mkdirs();

						if (!destinationFile.exists()) {
							throw new EngineException("Unable to create the directory or one of its parents: " + destinationFilePath);
						}

						if (directoryCreated) Engine.logBeans.info("Directory \"" + destinationFilePath + "\" has been created with parent directories.");
						else Engine.logBeans.info("Directory \"" + destinationFilePath + "\" already exists.");
					}
					else {
						directoryCreated = destinationFile.mkdir();
						
						if (!destinationFile.exists()) {
							throw new EngineException("Unable to create the directory: " + destinationFilePath);
						}

						if (directoryCreated) Engine.logBeans.info("Directory \"" + destinationFilePath + "\" has been created.");
						else Engine.logBeans.info("Directory \"" + destinationFilePath + "\" already exists.");
					}

					return true;
				} catch (EngineException e) {
					setErrorStatus(true);
					throw e;
				}
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
