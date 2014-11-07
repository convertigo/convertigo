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

package com.twinsoft.convertigo.beans.steps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class MoveFileStep extends Step {
	
	private static final long serialVersionUID = 6828884684092984710L;
	
	private String dataFile = "";		
	private String newFilename = "";	
	
	public MoveFileStep() {
		super();
	}

	@Override
    public MoveFileStep clone() throws CloneNotSupportedException {
    	MoveFileStep clonedObject = (MoveFileStep) super.clone();
        return clonedObject;
    }

	@Override
    public MoveFileStep copy() throws CloneNotSupportedException {
    	MoveFileStep copiedObject = (MoveFileStep) super.copy();
        return copiedObject;
    }
    
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
        String version = element.getAttribute("version");
        
        if (version == null) {
            String s = XMLUtils.prettyPrintDOM(element);
            EngineException ee = new EngineException(
                "Unable to find version number for the database object \"" + getName() + "\".\n" +
                "XML data: " + s
            );
            throw ee;
        }
        
        if (VersionUtils.compareMigrationVersion(version, ".m003") < 0) {
        	if (!dataFile.equals("")) dataFile = "'" + dataFile + "'";
        	if (!newFilename.equals("")) newFilename = "'" + newFilename + "'";
			hasChanged = true;
			Engine.logBeans.warn("[MoveFileStep] The object \"" + getName()+ "\" has been updated to .m003 version");
        }
	}

	public String getDataFile() {
		return dataFile;
	}

	public void setDataFile(String dataFile) {
		this.dataFile = dataFile;
	}

	public String getNewFilename() {
		return newFilename;
	}

	public void setNewFilename(String newFilename) {
		this.newFilename = newFilename;
	}

	public String toJsString() {
		return null;
	}

	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += getLabel();
		} catch (EngineException e) {
		}
		
		return "MoveFile: " + label + (!text.equals("") ? " // "+text:"");
	}
	
	@Override
	protected String getSpecificLabel() throws EngineException {
		String label = getDataFileName();
		return label.equals("") ? "??":label;
	}
	
	private String evaluateDataFileName(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, dataFile, "dataFile", true);
	}
	
	private String evaluateNewFileName(Context javascriptContext, Scriptable scope) throws EngineException {
		return evaluateToString(javascriptContext, scope, newFilename, "newFilename", true);
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.stepExecute(javascriptContext, scope)) {
				try {
					String inputFilePath = getAbsoluteFilePath(evaluateDataFileName(javascriptContext, scope));
					String outputFilePath = getAbsoluteFilePath(evaluateNewFileName(javascriptContext, scope));
					moveFichier(inputFilePath, outputFilePath);
		        } catch (Exception e) {
		        	setErrorStatus(true);
		            Engine.logBeans.error("An error occured while moving file", e);
		        }
		        return true;
			}
		}
		return false;
	}
	
	protected String getAbsoluteFilePath(String entry) throws EngineException {
		if (entry.equals(""))
			throw new EngineException("The file name is empty");
		
		return Engine.theApp.filePropertyManager.getFilepathFromProperty(entry, getProject().getName());
	}
	
	private void moveFichier(String entree,String sortie) throws IOException {
		File inputFile = new File(entree);
		if (inputFile.exists()) {
			copierFichier(entree, sortie);
			inputFile.delete();
		}
	}
	
	private void copierFichier(String entree,String sortie) throws IOException {
	    int bytes_read=0;
	    byte [] buffer=  new byte[512];
	    FileInputStream fin = new FileInputStream(entree);
	    FileOutputStream fout = new FileOutputStream(sortie);
	    while ((bytes_read =fin.read(buffer)) != -1)
	      fout.write(buffer,0,bytes_read);
	    fin.close();
	    fout.close();
    }
	
	protected String getDataFileName() {
		String fileName = dataFile;
		int index = dataFile.lastIndexOf("/");
		if (index != -1) {
			fileName = dataFile.substring(index+1);
		}
		return fileName;
	}
	
}
