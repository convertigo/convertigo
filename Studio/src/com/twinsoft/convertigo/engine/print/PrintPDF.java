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

package com.twinsoft.convertigo.engine.print;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.fop.apps.MimeConstants;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.engine.print.ConvertigoPrint;
import com.twinsoft.convertigo.engine.print.PrintStatus;
import com.twinsoft.convertigo.engine.EngineException;

public class PrintPDF extends ConvertigoPrint{
	
	public PrintPDF(String projectName){
		super(projectName);
		init();		
	}
	
	public PrintPDF(String projectName,PrintStatus status){
		super(projectName,status);
		init();		
	}
	
	private void init(){
		outputMime=MimeConstants.MIME_PDF;
		extension=".pdf";
	}
	
	@Override
	public String print(String location) throws IOException, EngineException, SAXException, TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException{
		String outputLocation=super.print(location);
		updateStatus("Printing finished",100);
		return outputLocation;
	}
	
	
}
