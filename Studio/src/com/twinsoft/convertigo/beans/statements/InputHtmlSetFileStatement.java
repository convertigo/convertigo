/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.statements;

import java.io.File;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.parsers.events.AbstractEvent;
import com.twinsoft.convertigo.engine.parsers.events.InputFileEvent;

public class InputHtmlSetFileStatement extends AbstractComplexeEventStatement {
	private static final long serialVersionUID = 5711783044654871254L;
	
	private String filename = "\"filename\"";
	transient File fileupload = null;
	
	public InputHtmlSetFileStatement() {
		super();
	}

	@Override
	public String toString() {
		return "set value of " + filename + super.toString();
	}
	
	@Override
	public AbstractEvent getEvent(Context javascriptContext, Scriptable scope) throws EngineException {
		evaluate(javascriptContext, scope, filename, "filename", true);
		
		if (evaluated != null) {
			String filepath = Engine.theApp.filePropertyManager.getFilepathFromProperty(evaluated.toString(), getProject().getName());
			File fileupload = new File(filepath);
			if (!fileupload.exists()) {
				throw new EngineException("(HTTPUploadStatement) The file '" + fileupload.getAbsolutePath() + "' doesn't exist.");
			}
			if (!fileupload.isFile()) {
				throw new EngineException("(HTTPUploadStatement) The file '" + fileupload.getAbsolutePath() + "' isn't a file.");
			}
			
			return new InputFileEvent(xpath, uiEvent, fileupload);
		} else {
			throw new EngineException("(HTTPUploadStatement) The filename expresion must return the file path in string.");
		}
	}
	
	@Override
	public String toJsString() {
		return filename;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
}