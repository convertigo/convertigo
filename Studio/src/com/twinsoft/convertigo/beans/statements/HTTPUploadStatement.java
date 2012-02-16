package com.twinsoft.convertigo.beans.statements;

import java.io.File;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class HTTPUploadStatement extends HTTPStatement {
	private static final long serialVersionUID = 3095426964527946257L;
	
	private String filename =  "\"filename\"";
	
	transient File fileupload;

	public HTTPUploadStatement() {
		super();
		
		setHttpVerb(HttpTransaction.HTTP_VERB_POST);
	}
	
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
	
    @Override
	public boolean execute(org.mozilla.javascript.Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			fileupload = null;
			evaluate(javascriptContext, scope, filename, "filename", true);
			
			if (evaluated != null) {
				String filepath = Engine.theApp.filePropertyManager.getFilepathFromProperty(evaluated.toString(), getProject().getName());
				fileupload = new File(filepath);
				if (!fileupload.exists()) {
					throw new EngineException("(HTTPUploadStatement) The file '" + fileupload.getAbsolutePath() + "' doesn't exist.");
				}
				if (!fileupload.isFile()) {
					throw new EngineException("(HTTPUploadStatement) The file '" + fileupload.getAbsolutePath() + "' isn't a file.");
				}
			} else {
				throw new EngineException("(HTTPUploadStatement) The filename expresion must return the file path in string.");
			}
			
			return super.execute(javascriptContext, scope);
		}
		return false;
	}
	
	public void handleUpload(HttpMethod method, Context context) throws EngineException {
		if (method instanceof PostMethod) {
			try {
				PostMethod uploadMethod = (PostMethod) method;
				
				Part[] parts = {
					new FilePart(fileupload.getName(), fileupload)
				};
				uploadMethod.setRequestEntity(new MultipartRequestEntity(parts, uploadMethod.getParams()));
			} catch (Exception e) {
				throw new EngineException("(HTTPUploadStatement) failed to handleUpload", e);
			}
		}
	}
}