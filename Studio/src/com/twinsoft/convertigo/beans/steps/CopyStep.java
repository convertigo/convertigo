package com.twinsoft.convertigo.beans.steps;

import java.io.File;
import java.io.IOException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

import org.apache.commons.io.FileUtils;

public class CopyStep extends Step {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2873578590344942963L;

	private String sourcePath = "";		
	private String destinationPath = "";	
	
	public CopyStep() {
		super();
	}
	
	private String evaluateSourcePath(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, sourcePath, "sourcePath", false);
	}
	
	private String evaluateDestinationPath(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, destinationPath, "destinationPath", false);
	}
	
	@Override
	protected boolean stepExcecute(Context javascriptContext, Scriptable scope) throws EngineException {
		// TODO Auto-generated method stub
		if (isEnable) {
			if (super.stepExcecute(javascriptContext, scope)) {
				try {
					
					String sourceFilePath = getAbsoluteFilePath(evaluateSourcePath(javascriptContext, scope));
					String destinationFilePath = getAbsoluteFilePath(evaluateDestinationPath(javascriptContext, scope));

					File sourcefile = new File(sourceFilePath);
					File destinationFile = new File(destinationFilePath);
					
					if (destinationFile.isDirectory()) {
						if (destinationFile.exists()) {
							if (sourcefile.isFile()) {
								String fileName = "";
								String tmpSourceFilePath = sourceFilePath.replaceAll("\\\\", "/");

								int index = tmpSourceFilePath.lastIndexOf("/");
								if (index != -1) {
									fileName = tmpSourceFilePath.substring(index+1);
								}
								destinationFile = new File(destinationFilePath + "/" + fileName);
							}
						}
						else {
							throw new Exception("Destination directory does not exist.");
						}
					}
					
					if (sourcefile.exists()) {
						if (sourcefile.isDirectory()) {
							FileUtils.copyDirectory(sourcefile, destinationFile);
						}
						else if (sourcefile.isFile()) {
							FileUtils.copyFile(sourcefile, destinationFile);
						}						
					}
					else {
						throw new Exception("Source file or directory does not exist.");
					}
		        } catch (IOException e) {
		        	setErrorStatus(true);
		            Engine.logBeans.error("An error occured while copying the file or directory.", e);
				} catch (NullPointerException e) {
					setErrorStatus(true);
		            Engine.logBeans.error("An error occured while creating the file.", e);
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
