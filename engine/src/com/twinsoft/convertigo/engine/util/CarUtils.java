/*
 * Copyright (c) 2001-2024 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.BeansDefaultValues;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;

public class CarUtils {

	public static File makeArchive(String projectName, Set<ArchiveExportOption> archiveExportOptions) throws EngineException {
		Project project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
		return makeArchive(project, archiveExportOptions);
	}

	public static File makeArchive(Project project, Set<ArchiveExportOption> archiveExportOptions) throws EngineException {
		return makeArchive(Engine.PROJECTS_PATH, project, archiveExportOptions);
	}

	public static File makeArchive(String dir, Project project) throws EngineException {
		return makeArchive(new File(dir, project.getName() + ".car"), project, ArchiveExportOption.all);
	}

	public static File makeArchive(String dir, Project project, Set<ArchiveExportOption> archiveExportOptions) throws EngineException {
		return makeArchive(new File(dir, project.getName() + ".car"), project, archiveExportOptions);
	}

	public static File makeArchive(File file, Project project, Set<ArchiveExportOption> archiveExportOptions) throws EngineException {
		Set<File> undeployedFiles = getUndeployedFiles(project.getName());
		String projectName = project.getName();
		String dirPath = project.getDirPath();
		File projectDir = new File(dirPath);
		File skipTestCase = null;
		try {
			if (!archiveExportOptions.contains(ArchiveExportOption.includeTestCase)) {
				skipTestCase = new File(projectDir, "_private/noTestCase/c8oProject.yaml");
				CarUtils.exportProject(project, skipTestCase.getAbsolutePath(), false);
			}
			for (ArchiveExportOption opt: ArchiveExportOption.values()) {
				if (!archiveExportOptions.contains(opt)) {
					for (File dir: opt.dirs(projectDir)) {
						undeployedFiles.add(dir);
					}
				}
			}
			FileUtils.deleteQuietly(file);
			File f = ZipUtils.makeZip(file.getAbsolutePath(), dirPath, projectName, undeployedFiles);
			
			if (skipTestCase != null) {
				Map<String, String> zip_properties = new HashMap<>();
				zip_properties.put("create", "false");
				zip_properties.put("encoding", "UTF-8");
				URI zip_disk = URI.create("jar:" + f.toURI());
				try (FileSystem zipfs = FileSystems.newFileSystem(zip_disk, zip_properties)) {
					Path addNewFile = skipTestCase.getParentFile().toPath();
					Files.walk(addNewFile).forEach(p -> {
						try {
							String relat = addNewFile.relativize(p).toString();
							if (!relat.isEmpty()) {
								Files.copy(p, zipfs.getPath(projectName, relat));
							}
						} catch (IOException e) {
							Engine.logEngine.debug("(CarUtils) failed to copy project without TestCase: " + e);
						}
					});
				}
			}
			return f;
		} catch(Exception e) {
			throw new EngineException("Unable to make the archive file for the project \"" + projectName + "\".", e);
		} finally {
			if (skipTestCase != null) {
				FileUtils.deleteQuietly(skipTestCase.getParentFile());
			}
		}
	}

	private static Set<File> getUndeployedFiles(String projectName){
		final Set<File> undeployedFiles = new HashSet<File>();
		
		File projectDir = new File(Engine.projectDir(projectName));
		
		// Private - Data directories
		File privateDir = new File(projectDir, "_private");
		undeployedFiles.add(privateDir);
		File dataDir = new File(projectDir, "_data");
		undeployedFiles.add(dataDir);
		File carFile = new File(projectDir, projectName + ".car");
		undeployedFiles.add(carFile);
		File buildDir = new File(projectDir, "build");
		undeployedFiles.add(buildDir);

		// UrlMapper - JsonSchema directories
		File oas2Dir = new File(projectDir, "oas2");
		undeployedFiles.add(oas2Dir);
		File oas3Dir = new File(projectDir, "oas3");
		undeployedFiles.add(oas3Dir);
		
		new FileWalker(){

			@Override
			public void walk(File file) {
				String filename = file.getName(); 
				if (filename.equals(".svn") || filename.equals(".git") || filename.equals(".gradle") || filename.equals("CVS") || filename.equals("node_modules")) {
					undeployedFiles.add(file);
				} else {
					super.walk(file);					
				}
			}
			
		}.walk(projectDir);
		
		return undeployedFiles;
	}

	public static void exportProject(Project project, String fileName) throws EngineException {
		exportProject(project, fileName, true);
	}
	
	public static void exportProject(Project project, String fileName, boolean includeTestCases) throws EngineException {
		Document document = exportProject(project, includeTestCases);
		try {
			exportYAMLProject(project, fileName, document);
		} catch (Exception e) {
			Engine.logEngine.error("(CarUtils) Failed to export the project as YAML to '" + fileName + "', export XML instead.", e);
			exportXMLProject(new File(new File(fileName).getParentFile(), project.getName() + ".xml").getAbsolutePath(), document);
		}
	}
	
	private static void exportYAMLProject(Project project, String fileName, Document document) throws EngineException {
		try {
			Document shrink = BeansDefaultValues.shrinkProject(document);
			try {
				NodeList nl = shrink.getDocumentElement().getElementsByTagName("bean");
				String minVersion = ((Element) nl.item(0)).getTextContent();
				project.setMinVersion(minVersion);
			} catch (Exception e) {}
			File projectDir = new File(fileName).getParentFile();
			YamlConverter.writeYaml(shrink, new File(projectDir, "c8oProject.yaml"), new File(projectDir, "_c8oProject"));
			if (fileName.endsWith(".xml")) {
				new File(fileName).delete();
			}
		} catch (Exception e) {
			Engine.logEngine.error("(CarUtils) exportProject in YAML failed (" + e.getMessage() + ")");
			throw new EngineException("(CarUtils) exportProject in YAML failed", e);
		}
	}
	
	private static void exportXMLProject(String fileName, Document document) throws EngineException {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			
			File file = new File(fileName);
			if (!file.exists()) {
				file.getParentFile().mkdirs();
			}
			
			boolean isCR = FileUtils.isCRLF();
			
			Writer writer = isCR ? new StringWriter() : new FileWriterWithEncoding(fileName, "UTF-8");
			transformer.transform(new DOMSource(document), new StreamResult(writer));
			
			if (isCR) {
				String content = FileUtils.CrlfToLf(writer.toString());
				writer = new FileWriterWithEncoding(fileName, "UTF-8");
				writer.write(content);
			}
			
			writer.close();
		} catch (Exception e) {
			throw new EngineException("(CarUtils) exportProject failed", e);
		}
	}
	
	private static Document exportProject(Project project, final boolean includeTestCases) 
			throws EngineException {
		try {
			final Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
			//            ProcessingInstruction pi = document.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"UTF-8\"");
			//            document.appendChild(pi);
			final Element rootElement = document.createElement("convertigo");
			
			rootElement.setAttribute("version", com.twinsoft.convertigo.engine.Version.fullProductVersion);
			rootElement.setAttribute("engine", com.twinsoft.convertigo.engine.Version.version);
			rootElement.setAttribute("beans", com.twinsoft.convertigo.beans.Version.version);
			String studioVersion = "";
			try {
				Class<?> c = Class.forName("com.twinsoft.convertigo.eclipse.Version");
				studioVersion = (String)c.getDeclaredField("version").get(null);
			} catch (Exception e) {
			} catch (Throwable th) {
			}
			
			rootElement.setAttribute("studio", studioVersion);
			document.appendChild(rootElement);
			
			new WalkHelper() {
				protected Element parentElement = rootElement;
				
				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					Element parentElement = this.parentElement;
					
					Element element = parentElement;
					element = databaseObject.toXml(document);
					String name = " : " + databaseObject.getName();
					try {
						name = CachedIntrospector.getBeanInfo(databaseObject.getClass()).getBeanDescriptor().getDisplayName() + name;
					} catch (IntrospectionException e) {
						name = databaseObject.getClass().getSimpleName() + name;
					}
					Integer depth = (Integer) document.getUserData("depth");
					if (depth == null) {
						depth = 0;
					}
					
					String openpad = StringUtils.repeat("   ", depth);
					String closepad = StringUtils.repeat("   ", depth);
					parentElement.appendChild(document.createTextNode("\n"));
					parentElement.appendChild(document.createComment(StringUtils.rightPad(openpad + "<" + name + ">", 150)));
					
					if (databaseObject instanceof TestCase) {
						if (includeTestCases) {
							parentElement.appendChild(element);
						}
					} else {
						parentElement.appendChild(element);
					}
					
					document.setUserData("depth", depth + 1, null);
					
					this.parentElement = element;
					super.walk(databaseObject);
					
					element.appendChild(document.createTextNode("\n"));
					element.appendChild(document.createComment(StringUtils.rightPad(closepad + "</" + name + ">", 150)));
					document.setUserData("depth", depth, null);
					
					databaseObject.hasChanged = false;
					databaseObject.bNew = false;
					
					this.parentElement = parentElement;
				}				
				
			}.init(project);
			
			return document;
		} catch(Exception e) {
			throw new EngineException("Unable to export the project \"" + project.getName() + "\".", e);
		}
	}
	
	/*
	 * Returns an ArrayList of abstract pathnames denoting the files and directories
	 * in the directory denoted by this abstract pathname
	 * that satisfy the specified suffix
	 */
	public static ArrayList<File> deepListFiles(String sDir, String suffix) {
		final String _suffix = suffix;
		File[] all, files;
		File f, dir;
		
		dir = new File(sDir);
		
		all = dir.listFiles();
		
		files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				File file = new File(dir, name);
				return (file.getName().endsWith(_suffix));
			}
		});
		
		ArrayList<File> list = null, deep = null;
		if (files != null) {
			list = new ArrayList<File>(Arrays.asList(files));
		}
		
		if ((list != null) && (all != null)) {
			for (int i=0; i<all.length; i++) {
				f = all[i];
				if (f.isDirectory() && !list.contains(f)) {
					deep = deepListFiles(f.getAbsolutePath(), suffix);
					if (deep != null) {
						list.addAll(deep);
					}
				}
			}
		}
		
		return list;
	}
}
