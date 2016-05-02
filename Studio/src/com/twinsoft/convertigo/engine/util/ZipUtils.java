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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.engine.Engine;

public class ZipUtils {
	
	public static void makeZip(String archiveFileName, String sDir, String sRelativeDir) throws Exception {
		FileOutputStream fos = new FileOutputStream(archiveFileName);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		int nbZipEntries = ZipUtils.putEntries(zos, sDir, sRelativeDir, Collections.<File>emptyList());
		if (nbZipEntries > 0) zos.close();
	}
    
	public static void makeZip(String archiveFileName, String sDir, String sRelativeDir, List<File> excludedFiles) throws Exception {
		FileOutputStream fos = new FileOutputStream(archiveFileName);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		int nbZipEntries = ZipUtils.putEntries(zos, sDir, sRelativeDir, excludedFiles);
		if (nbZipEntries > 0) zos.close();
	}
    
	private static int putEntries(ZipOutputStream zos, String sDir, String sRelativeDir, final List<File> excludedFiles) throws Exception {
		Engine.logEngine.trace("==========================================================");
		Engine.logEngine.trace("sDir=" + sDir);
		Engine.logEngine.trace("sRelativeDir=" + sRelativeDir);
		Engine.logEngine.trace("excludedFiles=" + excludedFiles);
    	
		File dir = new File(sDir);
		String[] files = dir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				File file = new File(dir, name);
				return (!excludedFiles.contains(file));
			}
		});

		Engine.logEngine.trace("files=" + files);

		int nbe = 0;
		for (String file : files) {
			String sDirEntry = sDir + "/" + file;
			String sRelativeDirEntry = sRelativeDir != null ? (sRelativeDir + "/" + file) :  file;

			File f = new File(sDirEntry);
			if (!f.isDirectory()) {
				Engine.logEngine.trace("+ " + sDirEntry);
				InputStream fi = new FileInputStream(f);
				
				try {
					ZipEntry entry = new ZipEntry(sRelativeDirEntry);
					entry.setTime(f.lastModified());
					zos.putNextEntry(entry);
					IOUtils.copy(fi, zos);
					nbe++;
				}
				finally {
					fi.close();
				}
			}
			else {
				nbe += putEntries(zos, sDirEntry, sRelativeDirEntry, excludedFiles);
			}
		}
		return nbe;
	}
    
	public static void expandZip(String zipFileName, String rootDir) throws FileNotFoundException, IOException {
		expandZip(zipFileName, rootDir, null);
	}
	
    public static void expandZip(String zipFileName, String rootDir, String prefixDir) throws FileNotFoundException, IOException {
		Engine.logEngine.debug("Expanding the zip file " + zipFileName);

		// Creating the root directory
		File ftmp = new File(rootDir);
		if (!ftmp.exists()) {
			ftmp.mkdirs();
			Engine.logEngine.debug("Root directory created");
		}
		
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFileName)));
		
		try {
			ZipEntry entry;
			int prefixSize = prefixDir != null ? prefixDir.length() : 0;
			
			while ((entry = zis.getNextEntry()) != null) {
				// Ignoring directories
				if (entry.isDirectory()) {
					
				} else {
					
					String entryName = entry.getName();
					Engine.logEngine.debug("+ Analyzing the entry: " + entryName);	            
					
					try {
						// Ignore entry if does not belong to the project directory
						if ((prefixDir == null) || entryName.startsWith(prefixDir)) {
							
							// Ignore entry from _data or _private directory
							if ((entryName.indexOf("/_data/") != prefixSize ) && (entryName.indexOf("/_private/") != prefixSize)) {
								Engine.logEngine.debug("  The entry is accepted");
								String s1 = rootDir + "/" + entryName;
								String dir = s1.substring(0, s1.lastIndexOf('/'));
								
								// Creating the directory if needed
								ftmp = new File(dir);
								if (!ftmp.exists()) {
									ftmp.mkdirs();
									Engine.logEngine.debug("  Directory created");
								}
								
								// Writing the files to the disk
								File file = new File(rootDir + "/" + entryName);
								FileOutputStream fos = new FileOutputStream(file);
								try {
									IOUtils.copy(zis, fos);
								} finally {
									fos.close();
								}
								file.setLastModified(entry.getTime());
								Engine.logEngine.debug("  File written to: " + rootDir + "/" + entryName);
							}
						}
					} catch(IOException e) {
						Engine.logEngine.error("Unable to expand the ZIP entry \"" + entryName + "\": " + e.getMessage(), e);
					}
				}
			}
		} finally {
			zis.close();
		}
	}
    
    
 	/*** Added by julienda - 08/09/2012
 	 * Return the project name by reading the first directory into the archive (.car)
 	 * @param path: the archive path
 	 * @return filename: the project name
 	 * @throws IOException */
     public static String getProjectName(String path) throws IOException {
 		Engine.logEngine.trace("PATH: "+path);
 		
 		ZipInputStream zis = new ZipInputStream(new FileInputStream(path));
 	    ZipEntry ze = null;	  
 	    String fileName = null;
 	    try {
 	        if((ze = zis.getNextEntry()) != null){
 	        	fileName = ze.getName().replaceAll("/.*","");
 	        	Engine.logEngine.trace("ZipUtils.getProjectName() - fileName: "+fileName);
 	        }
 	    }
 	    finally {
 	        zis.close();
 	    }
 		return fileName;
 	}
 	
 	/*** Added by julienda - 10/09/2012
 	 * Return the archive name corresponding of the project name
 	 * @param supposedProject: the (supposed) project
 	 * @return the path of the real archive name
 	 * @throws IOException */
 	public static String getArchiveName(String supposedProject) throws IOException {
 		
 		//Get the project directory path
 		String pathProjects = new File (supposedProject).getParent();
 		//Get the supposed project name without ".car"
 		String supposedProjectName = new File (supposedProject).getName();
 		supposedProjectName = supposedProjectName.substring(0, supposedProjectName.length()-4);
 		
 		File projectPath = new File(pathProjects); 	String realArchiveFile =  null;
 		
 		Engine.logEngine.trace("ZipUtils.getArchiveName() - projectPath: "+projectPath);
 				
 		int i = 0;
 		
 		//Create a list with the (path) files of the project directory 
 		String [] listfiles = projectPath.list(); 
 		
 		while((i < listfiles.length)){ 
 			
 			//If the file is an archive
 			if(listfiles[i].endsWith(".car") == true){
 				Engine.logEngine.trace("ZipUtils.getArchiveName() - listfiles["+i+"]: "+listfiles[i]);
 				Engine.logEngine.trace("ZipUtils.getArchiveName() - supposedProjectName: "+supposedProjectName);
 				Engine.logEngine.trace("ZipUtils.getArchiveName() - listfiles PATH: "+new File(projectPath, listfiles[i]));
 				
 				//If the project name is equals to the (supposed) project name
 				if(getProjectName(new File(projectPath, listfiles[i]).getPath()).equals(supposedProjectName)){
 					return new File(projectPath, listfiles[i]).getName();
 				}
 				Engine.logEngine.trace("ZipUtils.getArchiveName() - realArchiveFile: "+realArchiveFile);
 			}
 			i++;
 		}
 		
 		throw new FileNotFoundException("Supposed project "+supposedProject+" not found!");
 	}
}
