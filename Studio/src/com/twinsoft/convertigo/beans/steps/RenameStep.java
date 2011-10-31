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

	/**
	 * 
	 */
	private static final long serialVersionUID = -2873578590344942963L;

	private String sourcePath = "";		
	private String newName = "";	
	
	public RenameStep() {
		super();
	}
	
	private String evaluateSourcePath(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, sourcePath, "sourcePath", false);
	}
	
	private String evaluateNewName(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, newName, "newName", false);
	}

	@Override
	protected boolean stepExcecute(Context javascriptContext, Scriptable scope) throws EngineException {
		// TODO Auto-generated method stub
		if (isEnable) {
			if (super.stepExcecute(javascriptContext, scope)) {
				try {
					sourcePath = sourcePath.replaceAll("\\\\", "/");
		
					String sourceFilePath = getAbsoluteFilePath(evaluateSourcePath(javascriptContext, scope));				
					String tmpSourceFilePath = sourceFilePath.replaceAll("\\\\", "/");
					String destinationPath = sourceFilePath.substring(0, tmpSourceFilePath.lastIndexOf("/"));				
					File sourceFile = new File(sourceFilePath);					
					String newFileName = evaluateNewName(javascriptContext, scope); 
					
					if (newFileName.length() == 0) {
						throw new Exception("Please fill the New name property field.");
					}
					
					File destinationFile = new File(destinationPath + "/" + newFileName);
					
					if (sourceFile.exists()) {
						if (sourceFile.isDirectory()) {
							FileUtils.moveDirectory(sourceFile, destinationFile);
						}
						else if (sourceFile.isFile()) {
							FileUtils.moveFile(sourceFile, destinationFile);
						}						
					}
					else {
						throw new Exception("Source file or directory does not exist.");
					}
		        } catch (IOException e) {
		        	setErrorStatus(true);
		            Engine.logBeans.error("An error occured while renaming the file or directory.", e);
				} catch (NullPointerException e) {
					setErrorStatus(true);
		            Engine.logBeans.error("An error occured while creating the file.", e);
				} catch (Exception e) {
					setErrorStatus(true);
		            Engine.logBeans.error("An error occured while renaming the file or directory.", e);
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
}
