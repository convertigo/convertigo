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

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.twinsoft.convertigo.engine.*;

public class ZipUtils {
    
	public static boolean checkFilesInZip(String path, List<String> filesNames, String zipFileNamesReplacmentRegex) throws IOException{
		ZipInputStream zis = new ZipInputStream(
	            new BufferedInputStream(new FileInputStream(path)));
	    ZipEntry ze = null;	  
	    String fileName = null;
	    int nbFilesFound = 0;
	    try {
	        while(((ze = zis.getNextEntry()) != null)){
	        	fileName = ze.getName().replaceAll(".*/","");  
	        	
	        	fileName = fileName.replaceAll(zipFileNamesReplacmentRegex,"");   
	          	
	            if (ze.isDirectory()) {
	                continue;
	            }
	            
	            for (String js : filesNames) {
	            	if (js.equalsIgnoreCase(fileName)) {	
	            		nbFilesFound++;
	            	} 	
	            }
	        }
	    }
	    finally {
	        zis.close();
	    }
		return (nbFilesFound == filesNames.size());
	}
	
	public static boolean checkFilesInZip(String path, List<String> filesNames) throws IOException{
		return checkFilesInZip(path, filesNames, "");
	}	
	
	public static void makeZip(String archiveFileName, String sDir, String sRelativeDir) throws Exception {
		FileOutputStream fos = new FileOutputStream(archiveFileName);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		int nbZipEntries = ZipUtils.putEntries(zos, sDir, sRelativeDir, new Vector<File>());
		if (nbZipEntries > 0) zos.close();
	}
    
	public static void makeZip(String archiveFileName, String sDir, String sRelativeDir, List<File> excludedFiles) throws Exception {
		FileOutputStream fos = new FileOutputStream(archiveFileName);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		int nbZipEntries = ZipUtils.putEntries(zos, sDir, sRelativeDir, excludedFiles);
		if (nbZipEntries > 0) zos.close();
	}
    
	public static void makeZip2(String archiveFileName, String sDir, String sRelativeDir, List<File> includedFiles) throws Exception {
		FileOutputStream fos = new FileOutputStream(archiveFileName);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		int nbZipEntries = ZipUtils.putEntries2(zos, sDir, sRelativeDir, includedFiles);
		if (nbZipEntries > 0) zos.close();
	}
    
	public static void makeZip(String archiveFileName, String sDir, String sRelativeDir, String[][] files) throws Exception {
		FileOutputStream fos = new FileOutputStream(archiveFileName);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		int nbZipEntries = ZipUtils.putEntries(zos, sDir, sRelativeDir, new Vector<File>());
		for (int i = 0 ; i < files.length ; i++){
			if(files[i][0].endsWith(".jar"))
				nbZipEntries += ZipUtils.addJarEntries(zos, files[i][0]);
			else
				nbZipEntries += ZipUtils.addFileEntries(zos, files[i][0], files[i][1]);
		}
		if (nbZipEntries > 0) zos.close();
	}
    
	private static int putEntries(ZipOutputStream zos, String sDir, String sRelativeDir, List<File> excludedFiles) throws Exception {
		Engine.logEngine.trace("==========================================================");
		Engine.logEngine.trace("sDir=" + sDir);
		Engine.logEngine.trace("sRelativeDir=" + sRelativeDir);
		Engine.logEngine.trace("excludedFiles=" + excludedFiles);
    	
		final List<File> _excludedFiles = excludedFiles;
		File dir = new File(sDir);
		String[] files = dir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				File file = new File(dir, name);
				return (!_excludedFiles.contains(file));
			}
		});

		Engine.logEngine.trace("files=" + files);

		int len = files.length;
		int nbe = 0, count;
		ZipEntry entry;
		FileInputStream fi;
		BufferedInputStream origin;
		byte data[] = new byte[4096];
		String file;
		File f;
		String sDirEntry, sRelativeDirEntry;
		for (int i = 0 ; i < len ; i++) {
			file = files[i];
			sDirEntry = sDir + "/" + file;
			if(sRelativeDir != null) {
				sRelativeDirEntry = sRelativeDir + "/" + file;
			}
			else {
				sRelativeDirEntry = file;
			}

			f = new File(sDirEntry);
			if (!f.isDirectory()) {
				Engine.logEngine.trace("+ " + sDirEntry);
				fi = new FileInputStream(sDirEntry);
				origin = new BufferedInputStream(fi, 4096);
				
				try {
					entry = new ZipEntry(sRelativeDirEntry);
					zos.putNextEntry(entry);
					while ((count = origin.read(data, 0, 4096)) != -1) {
						zos.write(data, 0, count);
					}
					nbe++;
				}
				finally {
					origin.close();
				}
			}
			else {
				nbe += putEntries(zos, sDirEntry, sRelativeDirEntry, excludedFiles);
			}
		}
		return nbe;
	}

	private static int putEntries2(ZipOutputStream zos, String sDir, String sRelativeDir, List<File> includedFiles) throws Exception {
		Engine.logEngine.trace("==========================================================");
		Engine.logEngine.trace("sDir=" + sDir);
		Engine.logEngine.trace("sRelativeDir=" + sRelativeDir);
		Engine.logEngine.trace("includedFiles=" + includedFiles);
    	
		final List<File> _includedFiles = includedFiles;
		File dir = new File(sDir);
		String[] files = dir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				File file = new File(dir, name);
				return (file.isDirectory() || _includedFiles.contains(file));
			}
		});

		Engine.logEngine.trace("files=" + files);

		int len = files.length;
		int nbe = 0, count;
		ZipEntry entry;
		FileInputStream fi;
		BufferedInputStream origin;
		byte data[] = new byte[4096];
		String file;
		File f;
		String sDirEntry, sRelativeDirEntry;
		for (int i = 0 ; i < len ; i++) {
			file = files[i];
			sDirEntry = sDir + "/" + file;
			if (sRelativeDir != null) {
				sRelativeDirEntry = sRelativeDir + "/" + file;
			}
			else {
				sRelativeDirEntry = file;
			}

			f = new File(sDirEntry);
			if (!f.isDirectory()) {
				Engine.logEngine.trace("+ " + sDirEntry);
				fi = new FileInputStream(sDirEntry);
				origin = new BufferedInputStream(fi, 4096);

				try {
					entry = new ZipEntry(sRelativeDirEntry);
					zos.putNextEntry(entry);
					while ((count = origin.read(data, 0, 4096)) != -1) {
						zos.write(data, 0, count);
					}
					nbe++;
				}
				finally {
					origin.close();
				}
			}
			else {
				nbe += putEntries2(zos, sDirEntry, sRelativeDirEntry, includedFiles);
			}
		}
		return nbe;
	}

    public static int addJarEntries(ZipOutputStream zos, String classesToAdd) throws Exception {        
        FileInputStream fis = new FileInputStream(classesToAdd);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));

        int nbe = 0;
        try {
            ZipEntry entry;
            int count;
            byte data[];
            while((entry = zis.getNextEntry()) != null) {
                // Ignoring directories
                if (entry.isDirectory()) continue;
                // adding just .class
                if (entry.getName().toLowerCase().indexOf("meta-inf") != -1) continue;
                
                // Writing the files to the zip
                data = new byte[512];
                zos.putNextEntry(entry);
                while ((count = zis.read(data, 0, data.length)) != -1) {
                    zos.write(data, 0, count);
                }
                nbe++;
            }
        }
        finally {
        	zis.close();
        }
        return nbe;
    }
    
    public static int addFileEntries(ZipOutputStream zos, String fileToAdd, String entryName) throws Exception {        
        FileInputStream fis = new FileInputStream(fileToAdd);
        BufferedInputStream origin = new BufferedInputStream(fis, 4096);

        int nbe = 0;
        try {
            ZipEntry entry;
            int count;
            byte data[] = new byte[4096];
            entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            while ((count = origin.read(data, 0, 4096)) != -1) {
                zos.write(data, 0, count);
            }
            nbe++;
        }
        finally {
            origin.close();
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

		FileInputStream fis = new FileInputStream(zipFileName);
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
		
		try {
			BufferedOutputStream dest = null;
			ZipEntry entry;
			int count,prefixSize;
			byte data[];
			String entryName;
			if(prefixDir!=null){
				prefixSize=prefixDir.length();
			}else{
				prefixSize=0;
			}
			
			while((entry = zis.getNextEntry()) != null) {
				// Ignoring directories
				if (entry.isDirectory()) continue;

				entryName = entry.getName();
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
							data = new byte[512];
							FileOutputStream fos = new FileOutputStream(rootDir + "/" + entryName);
							dest = new BufferedOutputStream(fos, data.length);
							try {
								while ((count = zis.read(data, 0, data.length)) != -1) {
									dest.write(data, 0, count);
								}
							}
							finally {
								dest.flush();
								dest.close();
							}
							Engine.logEngine.debug("  File written to: " + rootDir + "/" + entryName);
						}
					}
				}
				catch(IOException e) {
					Engine.logEngine.error("Unable to expand the ZIP entry \"" + entryName + "\": " + e.getMessage(), e);
				}
			}
		}
		finally {
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
