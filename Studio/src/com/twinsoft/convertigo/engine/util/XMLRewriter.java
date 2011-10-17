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

package com.twinsoft.convertigo.engine.util;

import java.io.File; 
import java.io.FileFilter;
import java.io.FileInputStream; 
import java.io.FileOutputStream; 

import java.io.Reader; 
import java.io.BufferedReader; 

import java.io.Writer; 
import java.io.BufferedWriter; 

import java.io.InputStreamReader; 
import java.io.OutputStreamWriter; 

import java.io.IOException;

public class XMLRewriter {
	
	public XMLRewriter() {
		
	}

	public class XsdFilter implements FileFilter {
		public boolean accept(File pathname) {
			return pathname.getName().endsWith(".xsd");
		}
	}
	
	public static void encodeUTF8ToISO8859(String sourcePath, String targetPath) throws IOException {
		encode(sourcePath, "UTF-8", targetPath, "ISO-8859-1");
	}
	
	public static void encode(String sourcePath, String sourceCharset, String targetPath, String targetCharset) throws IOException { 
	    // Source 
	    File sourceFile = null; 
	    FileInputStream is = null; 
	    Reader reader = null; 
	    BufferedReader sourceReader = null; 
	
	    // Target 
	    File targetFile = null; 
	    FileOutputStream os = null; 
	    Writer writer = null; 
	    BufferedWriter targetWriter = null; 
	    try {
	    	
	        sourceFile = new File(sourcePath); 
	        is = new FileInputStream(sourceFile); 
	        reader = new InputStreamReader(is,sourceCharset); 
	        sourceReader = new BufferedReader(reader);
	        
	        targetFile = new File(targetPath); 
	        os = new FileOutputStream(targetFile); 
	        writer = new OutputStreamWriter(os,targetCharset); 
	        targetWriter = new BufferedWriter(writer); 
	
	        String line;
	        int i=0;
	        while ((line = sourceReader.readLine()) != null) {
	        	if(i == 0) line = line.replaceFirst("encoding=\""+ sourceCharset +"\"", "encoding=\""+targetCharset+"\"");
	        	targetWriter.write(line + "\n");
	        	i++;
	        }
	        targetWriter.flush(); 

	    } finally { 
	        // source 
	        if(sourceReader != null) { 
	        	sourceReader.close(); 
	        } 
	        if(reader != null) { 
	            reader.close(); 
	        } 
	        if(is != null) { 
	            is.close(); 
	        } 
	        // target 
	        if(targetWriter != null) { 
	            targetWriter.close(); 
	        } 
	        if(writer != null) { 
	        	writer.close(); 
	        } 
	        if(os != null) { 
	            os.close(); 
	        } 
	    } 
	}

	public static void main(String[] args) {
		XMLRewriter xmlR = new XMLRewriter();
		
		String source = "C:/Development/Documentation/projects/BnppInfogreffe/xsd/v3/Infogreffe";
		String target = "C:/Development/Documentation/projects/BnppInfogreffe/xsd/v3/Convertigo_encoded_ISO-8859-1";
		
		File sourceDir = new File(source);
		File targetDir = new File(target);
		if (sourceDir.isDirectory() && targetDir.isDirectory()) {
			String sourcePath, targetPath;
			File[] files = sourceDir.listFiles(xmlR.new XsdFilter());
			for (int i=0; i<files.length; i++) {
				sourcePath = files[i].getAbsolutePath();
				targetPath = targetDir.getAbsolutePath() + "/" + files[i].getName();
				try {
					encodeUTF8ToISO8859(sourcePath, targetPath);
					System.out.println("'"+ sourcePath + "' " + "rewrite done.");
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
		}
	}
}
