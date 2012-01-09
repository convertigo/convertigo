package com.twinsoft.convertigo.beans.steps;

import java.io.File;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class IfFileExistStep extends BlockStep {

	private static final long serialVersionUID = 1615428447261700976L;

	protected XMLVector<XMLVector<String>> testDefinition = new XMLVector<XMLVector<String>>();
	
	public IfFileExistStep() {
		super();
	}
	
	public IfFileExistStep(String sourcePath) {
		super(sourcePath);
	}
	
    public Object clone() throws CloneNotSupportedException {
    	IfFileExistStep clonedObject = (IfFileExistStep) super.clone();
        return clonedObject;
    }
	
	public Object copy() throws CloneNotSupportedException {
		IfFileExistStep copiedObject = (IfFileExistStep)super.copy();
		return copiedObject;
	}

	public String toString() {
		String text = this.getComment();
		return "ifFileExists("+ (condition.equals("")?"??":condition) +")" + (!text.equals("") ? " // "+text:"");
	}
	
	public String toJsString() {
		String code = "";
		if (!condition.equals("")) {
			code += " ifFileExists ("+ condition +") {\n";
			code += super.toString();
			code += " \n}\n";
		}
		return code;
	}

	protected boolean workOnSource() {
		return false;
	}
	
	protected StepSource getSource() {
		return null;
	}
	
	protected boolean hasToEvaluateBeforeNextStep() throws EngineException {
		return true;
	}
	
	private String evaluateSourcePath(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, condition, "sourcePath", false);
	}
	
	protected boolean executeNextStep(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			boolean test = evaluateStep(javascriptContext, scope);
			return super.executeNextStep(test, javascriptContext, scope);
		}
		return false;
	}
	
	@Override
	protected boolean evaluateStep(Context javascriptContext, Scriptable scope) throws EngineException {
		try {	
			String sourceFilePath = getAbsoluteFilePath(evaluateSourcePath(javascriptContext, scope));
			File sourcefile = new File(sourceFilePath);
			
			if (sourcefile.exists()) {
				Engine.logBeans.info("File '" + sourceFilePath + "' exists");
				return true;
			}
			else {
				Engine.logBeans.info("File '" + sourceFilePath + "' does not exist");
				return false;
			}
		} catch (Exception e) {
			setErrorStatus(true);
            Engine.logBeans.error("An error occured while testing if the file or directory exists.", e);
		}		       
		return false;
	}

	protected String getAbsoluteFilePath(String entry) throws EngineException {
		if (entry.equals(""))
			throw new EngineException("Please fill the Source property field.");
				
		return Engine.theApp.filePropertyManager.getFilepathFromProperty(entry, getProject().getName());
	}
}
