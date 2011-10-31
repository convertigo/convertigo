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

public class DeleteStep extends Step {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2873578590344942963L;

	private String sourcePath = "";		
	
	public DeleteStep() {
		super();
	}
	
	private String evaluateSourcePath(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, sourcePath, "sourcePath", false);
	}

	@Override
	protected boolean stepExcecute(Context javascriptContext, Scriptable scope) throws EngineException {
		// TODO Auto-generated method stub
		if (isEnable) {
			if (super.stepExcecute(javascriptContext, scope)) {
				try {
					sourcePath = sourcePath.replaceAll("\\\\", "/");
					
					String sourceFilePath = getAbsoluteFilePath(evaluateSourcePath(javascriptContext, scope));
					File sourceFile = new File(sourceFilePath);
					
					if (sourceFile.exists()) {
						if (sourceFile.isDirectory()) {
							FileUtils.deleteDirectory(sourceFile);
						}
						else if (sourceFile.isFile()) {
							sourceFile.delete();
						}						
					}
					else {
						throw new Exception("Source file or directory does not exist.");
					}
		        } catch (IOException e) {
		        	setErrorStatus(true);
		            Engine.logBeans.error("An error occured while deleting the file or directory.", e);
				} catch (NullPointerException e) {
					setErrorStatus(true);
		            Engine.logBeans.error("An error occured while deleting the file.", e);
				} catch (Exception e) {
					setErrorStatus(true);
		            Engine.logBeans.error("An error occured while deleting the file or directory.", e);
				}		  
				Engine.logBeans.info("File " + sourcePath + " has been deleted.");
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
