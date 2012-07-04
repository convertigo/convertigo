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

package com.twinsoft.convertigo.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.event.EventListenerList;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLActionStep;
import com.twinsoft.convertigo.beans.steps.XMLGenerateDatesStep;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.engine.migration.Migration001;
import com.twinsoft.convertigo.engine.migration.Migration3_0_0;
import com.twinsoft.convertigo.engine.migration.Migration5_0_0;
import com.twinsoft.convertigo.engine.migration.Migration5_0_4;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.ZipUtils;

/**
 * This class is responsible for serializing objects to the Convertigo database
 * repository and restoring them from the Convertigo database repository.
 */
public class DatabaseObjectsManager implements AbstractManager {
	private Map<String, Project> projects;

	/**
	 * The symbols repository for compiling text properties.
	 */
	private Map<String, String> symbolsMap;

	public String getSymbolValue(String symbolName) {
		return symbolsMap.get(symbolName);
	}

	// private static String XSL_NAMESPACE_URI =
	// "http://www.w3.org/1999/XSL/Transform";

	public DatabaseObjectsManager() {
	}

	public void init() throws EngineException {
		projects = new HashMap<String, Project>();
		symbolsMap = new HashMap<String, String>(128);
		symbolsMapInit();
	}

	private void symbolsMapInit() {
		String filePath = System.getProperty("convertigo_global_symbols");
		try {
			if (filePath != null) {
				Properties prop = new Properties();
				prop.load(new FileInputStream(filePath));
				// Enumeration of the properties
				Enumeration<?> propsEnum = prop.propertyNames();
				String propertyName, propertyValue;

				while (propsEnum.hasMoreElements()) {
					propertyName = (String) propsEnum.nextElement();
					propertyValue = prop.getProperty(propertyName, "");
					symbolsMap.put(propertyName, propertyValue);
				}
				Engine.logEngine.info("Symbols file \"" + filePath + "\" loaded!");
			}
		} catch (FileNotFoundException e) {
			Engine.logDatabaseObjectManager.error("The symbols file specified in JVM argument as \""
					+ filePath + "\" does not exist! Symbols won't be calculated.");
		} catch (IOException e) {
			Engine.logDatabaseObjectManager.error(
					"Error while reading symbols file specified in JVM argument as \"" + filePath
							+ "\"; symbols won't be calculated.", e);
		}
	}

	public void destroy() throws EngineException {
		projects = null;
		symbolsMap = null;
	}

	private EventListenerList databaseObjectListeners = new EventListenerList();

	public void addDatabaseObjectListener(DatabaseObjectListener databaseObjectListener) {
		databaseObjectListeners.add(DatabaseObjectListener.class, databaseObjectListener);
	}

	public void removeDatabaseObjectListener(DatabaseObjectListener databaseObjectListener) {
		databaseObjectListeners.remove(DatabaseObjectListener.class, databaseObjectListener);
	}

	public void fireDatabaseObjectLoaded(DatabaseObjectLoadedEvent event) {
		// Guaranteed to return a non-null array
		Object[] listeners = databaseObjectListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == DatabaseObjectListener.class) {
				((DatabaseObjectListener) listeners[i + 1]).databaseObjectLoaded(event);
			}
		}
	}

	public void fireDatabaseObjectImported(DatabaseObjectImportedEvent event) {
		// Guaranteed to return a non-null array
		Object[] listeners = databaseObjectListeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == DatabaseObjectListener.class) {
				((DatabaseObjectListener) listeners[i + 1]).databaseObjectImported(event);
			}
		}
	}

	@Deprecated
	public Vector<String> getAllProjectNames() throws EngineException {
		return new Vector<String>(getAllProjectNamesList());
	}

	public List<String> getAllProjectNamesList() throws EngineException {
		try {
			Engine.logDatabaseObjectManager.trace("Retrieving all project names from \""
					+ Engine.PROJECTS_PATH + "\"");
			File projectsDir = new File(Engine.PROJECTS_PATH);
			SortedSet<String> projectNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			
			for (File projectDir : projectsDir.listFiles()) {
				if (projectDir.isDirectory() && new File(projectDir, projectDir.getName() + ".xml").exists()) {
					projectNames.add(projectDir.getName());
				}
			}
			
			Engine.logDatabaseObjectManager.trace("Project names found: " + projectNames.toString());
			return new ArrayList<String>(projectNames);
		} catch (Exception e) {
			throw new EngineException("Unable to retrieve the project names list.", e);
		}
	}

	public String[] getAllProjectNamesArray() {
		try {
			Collection<String> c = getAllProjectNamesList();
			return c.toArray(new String[c.size()]);
		} catch (EngineException e) {
			return new String[0];
		}
	}

	protected void checkForEngineMigrationProcess(String projectName) throws ProjectInMigrationProcessException {
		if (!(Thread.currentThread() instanceof MigrationJob)) {
			if (!MigrationManager.isProjectMigrated(projectName)) {
				throw new ProjectInMigrationProcessException();
			}
		}
	}

	// Thread reference to currently loaded project for log needs
	public static class ProjectLoadingData {
		public ProjectLoadingData() {
		}

		public String projectName;
		public String databaseObjectName;
		public String compilablePropertyFailure;
	}

	private static ThreadLocal<ProjectLoadingData> projectLoadingDataThreadLocal = new ThreadLocal<ProjectLoadingData>() {
		@Override
		protected ProjectLoadingData initialValue() {
			return new ProjectLoadingData();
		}
	};

	public static ProjectLoadingData getProjectLoadingData() {
		return projectLoadingDataThreadLocal.get();
	}
	
	public Project getOriginalProjectByName(String projectName) throws EngineException {
		Engine.logDatabaseObjectManager.trace("Requiring loading of project \"" + projectName + "\"");
		
		Project project;
		
		synchronized (projects) {
			project = projects.get(projectName);
		}
		
		if (project == null) {		
			long t0 = Calendar.getInstance().getTime().getTime();

			projectLoadingDataThreadLocal.remove();
			getProjectLoadingData().projectName = projectName;

			try {
				checkForEngineMigrationProcess(projectName);
				project = importProject(Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".xml");
			} catch (ClassCastException e) {
				throw new EngineException("The requested object \"" + projectName + "\" is not a project!", e);
			} catch (ProjectInMigrationProcessException e) {
				throw new EngineException("Unable to load the project \"" + projectName
						+ "\": the project is in migration process.", e);
			} finally {
				long t1 = Calendar.getInstance().getTime().getTime();
				Engine.logDatabaseObjectManager.trace("Project loaded in " + (t1 - t0) + " ms");
			}
		} else {
			Engine.logDatabaseObjectManager.trace("Retrieve from cache project \"" + projectName + "\"");
		}
		
		return project;
	}

	public Project getProjectByName(String projectName) throws EngineException {
		try {
			return getOriginalProjectByName(projectName).clone();
		} catch (CloneNotSupportedException e) {
			throw new EngineException("Exception on getProjectByName", e);
		}
	}
	
	public void clearCache(Project project) {
		String name = project.getName();
		synchronized (projects) {
			if (projects.get(name) == project) {
				projects.remove(name);
			}
		}
	}

	public void buildCar(String projectName) {
		try {
			CarUtils.makeArchive(projectName);
		} catch (EngineException e) {
			Engine.logDatabaseObjectManager.error("Build car failed!", e);
		}
	}

	/**
	 * Reloads a database object from the projects database repository.
	 * 
	 * @param databaseObject
	 *            the object to reload.
	 * 
	 * @throw DatabaseObjectNotFoundException if the corresponding database
	 *        object does not exist.
	 * @throw EngineException if the corresponding database object can not be
	 *        returned, or does not exist.
	 */
	public void reloadDatabaseObject(DatabaseObject databaseObject) throws EngineException,
			DatabaseObjectNotFoundException {
		String databaseObjectQName = databaseObject.getQName();
		Engine.logDatabaseObjectManager.trace("(DatabaseObjectsManager) Reloading object: "
				+ databaseObjectQName);
		cacheRemoveObject(databaseObjectQName);

		try {
			String fileName = Engine.PROJECTS_PATH + databaseObjectQName;

			FileReader fr = new FileReader(fileName);
			char[] buffer = new char[4096];
			String serializationData = "";
			int nbReadChars = 0;
			while ((nbReadChars = fr.read(buffer)) != -1) {
				serializationData += new String(buffer, 0, nbReadChars);
			}
			fr.close();

			databaseObject.reload(serializationData);
			cacheUpdateObject(databaseObject, databaseObjectQName);
			Engine.logDatabaseObjectManager.debug(databaseObject.getDatabaseType() + " '"
					+ databaseObject.getName() + "' has been successfully deserialized.");
		} catch (FileNotFoundException e) {
			throw new DatabaseObjectNotFoundException(databaseObjectQName);
		} catch (IOException e) {
			throw new EngineException("Unable to read the object file \"" + databaseObjectQName + "\".", e);
		}
	}

	public boolean existsProject(String projectName) {
		File file = new File(Engine.PROJECTS_PATH + "/" + projectName);
		return file.exists();
	}

	public void delete(DatabaseObject databaseObject) throws EngineException {
		String databaseObjectQName = databaseObject.getQName();
		Engine.logDatabaseObjectManager
					.info("Deleting the object \"" + databaseObjectQName + "\"");


			databaseObject.delete();
			cacheRemoveObject(databaseObjectQName);
	}

	public void deleteProject(String projectName) throws EngineException {
		try {
			deleteProject(projectName, true);
		} catch (Exception e) {
			throw new EngineException("Unable to delete the project \"" + projectName + "\".", e);
		}
	}

	public void deleteProject(String projectName, boolean bCreateBackup) throws EngineException {
		try {
			deleteProject(projectName, true, false);
		} catch (Exception e) {
			throw new EngineException("Unable to delete the project \"" + projectName + "\".", e);
		}
	}

	public void deleteProject(String projectName, boolean bCreateBackup, boolean bDataOnly)
			throws EngineException {
		try {
			if (bCreateBackup) {
				Engine.logDatabaseObjectManager.info("Making backup of project \"" + projectName + "\"");
				makeProjectBackup(projectName);
			}

			if (bDataOnly) {
				Engine.logDatabaseObjectManager.info("Deleting __datas for  project \"" + projectName + "\"");
				String dataDir = Engine.PROJECTS_PATH + "/" + projectName + "/_data";
				deleteDir(new File(dataDir));

				Engine.logDatabaseObjectManager
						.info("Deleting __private for  project \"" + projectName + "\"");
				String privateDir = Engine.PROJECTS_PATH + "/" + projectName + "/_private";
				deleteDir(new File(privateDir));
			} else {
				Engine.logDatabaseObjectManager.info("Deleting  project \"" + projectName + "\"");
				String projectDir = Engine.PROJECTS_PATH + "/" + projectName;
				deleteDir(new File(projectDir));
			}

			// Remove all pooled related contexts in server mode
			if (Engine.isEngineMode()) {
				// Bugfix #1659: do not call getProjectByName() if the migration
				// process is ongoing!
				if (!(Thread.currentThread() instanceof MigrationJob)) {
					Project projectToDelete = Engine.theApp.databaseObjectsManager
							.getProjectByName(projectName);
					for (Connector connector : projectToDelete.getConnectorsList()) {
						Engine.theApp.contextManager.removeDevicePool(connector.getQName());
					}
					Engine.theApp.contextManager.removeAll("/" + projectName);
				}
			}

			cacheRemoveObjects("/" + projectName);
		} catch (Exception e) {
			throw new EngineException("Unable to delete" + (bDataOnly ? " datas for" : "") + " project \""
					+ projectName + "\".", e);
		}
	}

	public void deleteProjectAndCar(String projectName) throws EngineException {
		try {
			deleteProject(projectName);

			String projectArchive = Engine.PROJECTS_PATH + "/" + projectName + ".car";
			deleteDir(new File(projectArchive));
		} catch (Exception e) {
			throw new EngineException("Unable to delete the project \"" + projectName + "\".", e);
		}
	}

	public static void deleteDir(File dir) throws IOException {
		Engine.logDatabaseObjectManager.trace("Deleting the directory \"" + dir.getAbsolutePath() + "\"");
		if (dir.exists()) {
			if (dir.isDirectory()) {
				String[] children = dir.list();
				File subdir;
				for (int i = 0; i < children.length; i++) {
					subdir = new File(dir, children[i]);
					deleteDir(subdir);
				}
				if (!dir.delete())
					throw new IOException("Unable to delete the directory \"" + dir.getAbsolutePath() + "\".");
			} else {
				Engine.logDatabaseObjectManager.trace("Deleting the file \"" + dir.getAbsolutePath() + "\"");
				if (!dir.delete())
					throw new IOException("Unable to delete the file \"" + dir.getAbsolutePath() + "\".");
			}
		}
	}

	public void makeProjectBackup(String projectName) throws EngineException {
		try {
			String projectDir = Engine.PROJECTS_PATH + "/" + projectName;

			Calendar calendar = Calendar.getInstance();
			int iMonth = (calendar.get(Calendar.MONTH) + 1);
			int iDay = calendar.get(Calendar.DAY_OF_MONTH);
			String day = (iDay < 10 ? "0" + iDay : iDay + "");
			String month = (iMonth < 10 ? "0" + iMonth : iMonth + "");
			String stamp = calendar.get(Calendar.YEAR) + "-" + month + "-" + day;
			String projectArchiveFilename = Engine.PROJECTS_PATH + "/" + projectName + "_" + stamp + ".zip";

			File file = new File(projectArchiveFilename);
			int i = 1;
			while (file.exists()) {
				projectArchiveFilename = Engine.PROJECTS_PATH + "/" + projectName + "_" + stamp + "_" + i
						+ ".zip";
				file = new File(projectArchiveFilename);
				i++;
			}

			// Creating backup
			ZipUtils.makeZip(projectArchiveFilename, projectDir, projectName);
		} catch (Exception e) {
			throw new EngineException(
					"Unable to make backup archive for the project \"" + projectName + "\".", e);
		}
	}

	public Project updateProject(String projectFileName) throws EngineException {
		try {
			boolean isArchive = false, needsMigration = false;
			String projectName = null;
			Project project = null;

			File projectFile = new File(projectFileName);
			if (projectFile.exists()) {
				String fName = projectFile.getName();
				if (projectFileName.endsWith(".xml")) {
					projectName = fName.substring(0, fName.indexOf(".xml"));
				} else if (projectFileName.endsWith(".car")) {
					isArchive = true;
					projectName = fName.substring(0, fName.indexOf(".car"));
				}

				if (projectName != null) {
					needsMigration = needsMigration(projectName);

					if (isArchive) {
						// Deploy project (will backup project and perform the
						// migration through import if necessary)
						project = deployProject(projectFileName, needsMigration);
					} else {
						if (needsMigration) {
							Engine.logDatabaseObjectManager.debug("Project '" + projectName
									+ "' needs to be migrated");

							// Delete project's data only (will backup project)
							deleteProject(projectName, true, true);

							// Import project (will perform the migration)
							project = importProject(projectFileName);

							Engine.logDatabaseObjectManager.info("Project '" + projectName
									+ "' has been migrated");
						} else {
							Engine.logDatabaseObjectManager.debug("Project '" + projectName
									+ "' is up to date");
						}
					}
				}
			} else
				throw new EngineException("File \"" + projectFileName + "\" is missing");

			return project;
		} catch (Exception e) {
			throw new EngineException("Unable to update the project from the file \"" + projectFileName
					+ "\".", e);
		}
	}

	public void exportProject(Project project) throws EngineException {
		String projectName = project.getName();

		// Export project
		Engine.logDatabaseObjectManager.debug("Saving project \"" + projectName + "\" to XML file ...");
		String exportedProjectFileName = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".xml";
		CarUtils.exportProject(project, exportedProjectFileName);
		Engine.logDatabaseObjectManager.info("Project \"" + projectName + "\" saved!");
	}

	public Project deployProject(String projectArchiveFilename, boolean bForce) throws EngineException {
		return deployProject(projectArchiveFilename, null, bForce);
	}

	public Project deployProject(String projectArchiveFilename, String targetProjectName, boolean bForce)
			throws EngineException {
		String projectName, archiveProjectName = "<unknown>";
		String deployDirPath, projectDirPath;

		try {
			File f = new File(projectArchiveFilename);
			String fName = f.getName();
			archiveProjectName = fName.substring(0, fName.indexOf(".car"));

			if ((targetProjectName == null) || (archiveProjectName.equals(targetProjectName))) {
				projectName = archiveProjectName;
				deployDirPath = Engine.PROJECTS_PATH;
			} else {
				projectName = targetProjectName;
				File deployDir = new File(Engine.USER_WORKSPACE_PATH + "/temp");
				if (!deployDir.exists())
					deployDir.mkdir();
				deployDirPath = deployDir.getCanonicalPath();
			}

			projectDirPath = deployDirPath + "/" + archiveProjectName;

			Engine.logDatabaseObjectManager.info("Deploying the project \"" + archiveProjectName + "\" ...");
			try {
				if (existsProject(projectName)) {
					if (bForce) {
						// Deleting existing project if any
						deleteProject(projectName);
					} else {
						Engine.logDatabaseObjectManager.info("Project \"" + projectName
								+ "\" has already been deployed.");
						return null;
					}
				}

				f = new File(projectDirPath);
				f.mkdir();
				Engine.logDatabaseObjectManager.debug("Project directory created: " + projectDirPath);
			} catch (Exception e) {
				throw new EngineException(
						"Unable to create the project directory \"" + projectDirPath + "\".", e);
			}

			// Decompressing Convertigo archive
			Engine.logDatabaseObjectManager.debug("Analyzing the archive entries: " + projectArchiveFilename);
			ZipUtils.expandZip(projectArchiveFilename, deployDirPath, archiveProjectName);
		} catch (Exception e) {
			throw new EngineException("Unable to deploy the project from the file \"" + projectArchiveFilename
					+ "\".", e);
		}

		// Check for correct project name
		File pFile = new File(projectDirPath + "/" + archiveProjectName + ".xml");
		if (!pFile.exists()) {
			try {
				File pProject = new File(projectDirPath);
				pProject.delete();
			} catch (Exception e) {
			}
			String message = "Unable to deploy the project from the file \"" + projectArchiveFilename
					+ "\". Inconsistency between the archive and project names.";
			Engine.logDatabaseObjectManager.error(message);
			throw new EngineException(message);
		}

		try {
			// Handle non-normalized project name here (fix ticket #788 : Can
			// not import project 213.car)
			String normalizedProjectName = StringUtils.normalize(projectName);
			if (!projectName.equals(normalizedProjectName))
				projectName = "project_" + normalizedProjectName;

			// Rename project and files if necessary
			if (!projectName.equals(archiveProjectName)) {
				File dir = new File(projectDirPath);
				if (dir.isDirectory()) {
					// rename project directory
					File newdir = new File(Engine.PROJECTS_PATH + "/" + projectName);
					dir.renameTo(newdir);
					Engine.logDatabaseObjectManager.debug("Project directory renamed to: " + newdir);
					// rename project
					ProjectUtils.renameXmlProject(Engine.PROJECTS_PATH, archiveProjectName, projectName);
					Engine.logDatabaseObjectManager.debug("Project renamed from \"" + archiveProjectName
							+ "\" to \"" + projectName + "\"");
					// rename/modify project wsdl&xsd files
					try {
						ProjectUtils.renameXsdFile(Engine.PROJECTS_PATH, archiveProjectName, projectName);
						ProjectUtils.renameWsdlFile(Engine.PROJECTS_PATH, archiveProjectName, projectName);
						Engine.logDatabaseObjectManager.debug("Project wsdl & xsd files modified");
					} catch (Exception e) {
					}
				}
			}

			// Import project (will perform the migration)
			Project project = importProject(Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName
					+ ".xml");

			// Rename connector's directory under traces directory if needed
			// (name should be normalized since 4.6)
			File tracesDir = new File(Engine.PROJECTS_PATH + "/" + projectName + "/Traces");
			if (tracesDir.isDirectory()) {
				File connectorDir;
				String connectorName;
				File[] files = tracesDir.listFiles();
				for (int i = 0; i < files.length; i++) {
					connectorDir = files[i];
					if (connectorDir.isDirectory()) {
						connectorName = connectorDir.getName();
						if (!StringUtils.isNormalized(connectorName)) {
							if (!connectorDir.renameTo(new File(Engine.PROJECTS_PATH + "/" + projectName
									+ "/Traces/" + StringUtils.normalize(connectorName))))
								Engine.logDatabaseObjectManager.warn("Could not rename \"" + connectorName
										+ "\" directory under \"Traces\" directory.");
						}
					}
				}
			}

			Engine.logDatabaseObjectManager.info("Project \"" + projectName + "\" deployed!");
			return project;
		} catch (Exception e) {
			throw new EngineException("Unable to deploy the project from the file \"" + projectArchiveFilename
					+ "\".", e);
		}
	}

	public Project deployProject(String projectArchiveFilename, boolean bForce, boolean bAssembleXsl)
			throws EngineException {
		Project project = deployProject(projectArchiveFilename, bForce);
		String projectName = project.getName();

		if (bAssembleXsl) {
			String projectDir = Engine.PROJECTS_PATH + "/" + projectName;
			String xmlFilePath = projectDir + "/" + projectName + ".xml";
			try {
				Document document = XMLUtils.loadXml(xmlFilePath);
				Element rootElement = document.getDocumentElement();
				NodeList sheets = rootElement.getElementsByTagName("sheet");

				NodeList properties;
				Element prop;
				String sheetUrl;
				Document xslDom;
				NodeList includes;
				// for each sheet, read file and assemble xsl includes
				for (int i = 0; i < sheets.getLength(); i++) {
					// retrieve sheet url
					properties = ((Element) sheets.item(i)).getElementsByTagName("property");
					prop = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "url");
					sheetUrl = projectDir
							+ "/"
							+ ((Element) prop.getElementsByTagName("java.lang.String").item(0))
									.getAttribute("value");
					// read file
					xslDom = XMLUtils.loadXml(sheetUrl);
					if (Engine.logDatabaseObjectManager.isTraceEnabled())
						Engine.logDatabaseObjectManager.trace("XSL file read: " + sheetUrl + "\n"
								+ XMLUtils.prettyPrintDOM(xslDom));
					includes = xslDom.getDocumentElement().getElementsByTagName("xsl:include");
					Engine.logDatabaseObjectManager.trace(includes.getLength()
							+ " \"xsl:include\" tags in the XSL file");
					// for each include element, include the xsl elemnts
					for (int j = 0; j < includes.getLength(); j++) {
						includeXsl(projectDir, (Element) includes.item(j));
						j--;
						// decrement variable j because includeXsl function
						// removes "includes.item(j)" Node from
						// its parent Node, and so it is also removed from the
						// "includes" NodeList
					}
					// save the xsl dom in the xsl file
					if (Engine.logDatabaseObjectManager.isTraceEnabled())
						Engine.logDatabaseObjectManager
								.trace("XSL file saved after including include files: \n"
										+ XMLUtils.prettyPrintDOM(xslDom));
					XMLUtils.saveXml(xslDom, sheetUrl);
				}

			} catch (Exception e) {
				deleteProject(projectName);
				throw new EngineException("Unable to assemble the XSL files from project \"" + projectName
						+ "\".", e);
			}
		}

		return project;
	}

	private void includeXsl(String projectDir, Element includeElem) throws ParserConfigurationException,
			SAXException, IOException {
		Element parentElem = (Element) includeElem.getParentNode();
		Document doc = includeElem.getOwnerDocument();

		String href = includeElem.getAttribute("href");
		String xslFile = href.startsWith("../../xsl/") ? Engine.XSL_PATH
				+ href.substring(href.lastIndexOf("/")) : projectDir + "/" + href;
		Document document = XMLUtils.loadXml(xslFile);
		NodeList xslElements = document.getDocumentElement().getChildNodes();
		Node xslElem, importedXslElem;
		for (int i = 0; i < xslElements.getLength(); i++) {
			xslElem = xslElements.item(i);
			if (xslElem.getNodeType() == Node.ELEMENT_NODE && xslElem.getNodeName().equals("xsl:include")) {
				String fileDir = xslFile.substring(0, xslFile.lastIndexOf("/"));
				includeXsl(fileDir, (Element) xslElem);
				// decrement variable i because includeXsl function removes
				// "xslElem" Node from
				// its parent Node and so it is also removed from the
				// "xslElements" NodeList
				i--;
			} else {
				importedXslElem = doc.importNode(xslElem, true);
				parentElem.appendChild(importedXslElem);
			}
		}
		parentElem.removeChild(includeElem);
	}

	public Project importProject(String importFileName) throws EngineException {
		try {
			return importProject(importFileName, null);
		} catch (Exception e) {
			throw new EngineException("An error occured while importing project", e);
		}
	}

	public Project importProject(Document document) throws EngineException {
		try {
			return importProject(null, document);
		} catch (Exception e) {
			throw new EngineException("An error occured while importing project", e);
		}
	}

	private boolean needsMigration(String projectName) throws EngineException {
		if (projectName != null) {
			String projectFileName = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".xml";
			File projectXmlFile = new File(projectFileName);
			if (projectXmlFile.exists()) {
				try {
					final String[] version = { null };
					try {
						XMLUtils.saxParse(new File(projectFileName), new DefaultHandler() {

							@Override
							public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
								if ("convertigo".equals(qName)) {
									// since 6.0.6 (fix #2804)
									version[0] = attributes.getValue("beans");
								} else if ("project".equals(qName)) {
									String projectVersion = attributes.getValue("version");
									if (projectVersion != null) {
										// before 6.0.6
										version[0] = projectVersion;
									}
									throw new SAXException("find");	
								}
							}

						});
						throw new EngineException("Unable to find the project version");
					} catch (SAXException e) {
						if (!"find".equals(e.getMessage())) {
							throw e;
						}
					}					

					String currentVersion = com.twinsoft.convertigo.beans.Version.version;
					if (VersionUtils.compare(version[0], currentVersion) < 0) {
						Engine.logDatabaseObjectManager.warn("Project '" + projectName + "': migration to " + currentVersion + " beans version is required");
						return true;
					}
				} catch (Exception e) {
					throw new EngineException("Unable to retrieve project's version from \"" + projectFileName + "\".", e);
				}
			}
		}
		return false;
	}

	private Project importProject(String importFileName, Document document) throws EngineException {
		try {
			Engine.logDatabaseObjectManager.info("Importing project ...");

			if (importFileName != null) {
				document = XMLUtils.getDefaultDocumentBuilder().parse(new File(importFileName));
			}

			// Performs necessary XML migration
			Element projectNode = performXmlMigration(document);

			Element rootElement = document.getDocumentElement();
			Element projectElement = (Element) XMLUtils.findChildNode(rootElement, Node.ELEMENT_NODE);

			// Retrieve project version
			String version = rootElement.getAttribute("beans");
			projectElement.setAttribute("version", version);

			// Retrieve project name
			NodeList properties = projectElement.getElementsByTagName("property");
			Element pName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "name");
			String projectName = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(pName, Node.ELEMENT_NODE));

			
			
			// Import will perform necessary beans migration (see
			// deserialisation)
			Project project = (Project) importDatabaseObject(projectNode, null);
			
			synchronized (projects) {
				projects.put(project.getName(), project);
			}

			// Creates xsd/wsdl files (Since 4.6.0)
			performWsMigration(version, projectName);

			// Export the project (Since 4.6.0)
			String currentVersion = com.twinsoft.convertigo.beans.Version.version;
			if (VersionUtils.compare(version, currentVersion) < 0) {

				// Since 4.6 export project to its xml file
				// Only export project for versions older than 4.0.1
				// TODO: Migration to 4.0.1 (parent bean handles children order
				// (priorities))!!
				if (VersionUtils.compare(version, "4.0.1") >= 0) {
					exportProject(project);
				} else {
					Engine.logDatabaseObjectManager
							.error("Project \""
									+ projectName
									+ "\" has been partially migrated. It may not work properly. Please import it trought the Studio and export/upload it again.");
				}
			}

			Engine.logDatabaseObjectManager.info("Project \"" + projectName + "\" imported!");

			return project;
		} catch (Exception e) {
			throw new EngineException("Unable to import the project from \"" + importFileName + "\".", e);
		}
	}

	private Element performXmlMigration(Document document) throws EngineException {
		try {
			Element rootElement = document.getDocumentElement();

			Element projectNode = (Element) XMLUtils.findChildNode(rootElement, Node.ELEMENT_NODE);

			String version = projectNode.getAttribute("version");
			if ("".equals(version)) {
				version = rootElement.getAttribute("version");
			}

			// Migration to version 3.0.0 schema
			if ((version.startsWith("1.")) || (version.startsWith("2."))) {
				Engine.logDatabaseObjectManager.info("XML project's file migration to 3.0.0 schema ...");

				projectNode = Migration3_0_0.migrate(document, projectNode);

				if (Engine.logDatabaseObjectManager.isTraceEnabled())
					Engine.logDatabaseObjectManager.trace("XML migrated to v3.0:\n"
							+ (XMLUtils.prettyPrintDOM(document)));
			}

			// Migration to version 4.6.0 schema for CEMS 5.0.0
			if (VersionUtils.compare(version, "4.6.0") < 0) {
				Engine.logDatabaseObjectManager.info("XML project's file migration to 4.6.0 schema ...");

				projectNode = Migration5_0_0.migrate(document, projectNode);

				if (Engine.logDatabaseObjectManager.isTraceEnabled())
					Engine.logDatabaseObjectManager.trace("XML migrated to v4.6:\n"
							+ (XMLUtils.prettyPrintDOM(document)));

				Engine.logDatabaseObjectManager.info("Project's XML file migrated!");
			}

			// Migration to version 5.0.3 schema for CEMS 5.0.4
			if (VersionUtils.compare(version, "5.0.3") < 0) {
				Engine.logDatabaseObjectManager.info("XML project's file migration to 5.0.3 schema ...");

				projectNode = Migration5_0_4.migrate(document, projectNode);

				if (Engine.logDatabaseObjectManager.isTraceEnabled())
					Engine.logDatabaseObjectManager.trace("XML migrated to v5.0.3:\n"
							+ (XMLUtils.prettyPrintDOM(document)));

				Engine.logDatabaseObjectManager.info("Project's XML file migrated!");
			}

			// Migration to version m001
			if (VersionUtils.compareMigrationVersion(version, ".m001") < 0) {
				Engine.logDatabaseObjectManager.info("XML project's file migration to m001 schema ...");

				projectNode = Migration001.migrate(document, projectNode);

				if (Engine.logDatabaseObjectManager.isTraceEnabled())
					Engine.logDatabaseObjectManager.trace("XML migrated to m001:\n"
							+ (XMLUtils.prettyPrintDOM(document)));

				Engine.logDatabaseObjectManager.info("Project's XML file migrated!");
			}

			return projectNode;
		} catch (Exception e) {
			throw new EngineException("Unable to perform XML migration for project", e);
		}
	}

	private boolean performWsMigration(String version, String projectName) {
		/** Part of 4.6.0 migration : creates and update XSD/WSDL static files **/

		// Creates project's files if don't exist
		boolean xsdCreated = false, wsdlCreated = false;
		try {
			xsdCreated = ProjectUtils.createXsdFile(Engine.PROJECTS_PATH, projectName);
			if (xsdCreated)
				Engine.logDatabaseObjectManager.info("Project's XSD file created");
			wsdlCreated = ProjectUtils.createWsdlFile(Engine.PROJECTS_PATH, projectName);
			if (xsdCreated)
				Engine.logDatabaseObjectManager.info("Project's WSDL file created");
		} catch (Exception e) {
			Engine.logDatabaseObjectManager.error(
					"An error occured while creating XSD/WSDL files for project '" + projectName + "'", e);
			return false;
		}

		// Retrieve backup wsdlTypes and update XSD/WSDL project's files
		if (VersionUtils.compare(version, "4.6.0") < 0) {
			try {
				if (xsdCreated && wsdlCreated) {
					String xsdTypes;

					// Retrieve a !clone! of project to perform update
					Project project = getProjectByName(projectName);

					for (Connector connector : project.getConnectorsList()) {
						for (Transaction transaction : connector.getTransactionsList()) {
							try {
								// Migrates
								xsdTypes = transaction.migrateToXsdTypes();
								ProjectUtils.migrateWsdlTypes(projectName, connector, transaction, xsdTypes,
										false);

								Engine.logDatabaseObjectManager
										.info("XSD/WSDL files sucessfully updated for transaction \""
												+ transaction.getName() + "\"");
							} catch (Exception e) {
								Engine.logDatabaseObjectManager.error(
										"An error occured while updating XSD/WSDL files for project '"
												+ projectName + "'", e);
							}
						}
					}

					for (Sequence sequence : project.getSequencesList()) {
						try {
							// Migrates
							xsdTypes = sequence.migrateToXsdTypes();
							ProjectUtils.migrateWsdlTypes(projectName, project, sequence, xsdTypes, false);

							try {
								// Convertigo studio distribution
								try {
									List<Step> steps = sequence.getSteps();

									// Replace source's xpath
									// replace ./xxx by
									// ./transaction/document/xxx or by
									// ./sequence/document/xxx
									replaceSourceXpath(sequence, steps);

									// Add target project import in xsd
									addStepTargetProjectImports(steps);

									// Regenerate sequence schema from
									// definition
									xsdTypes = sequence.generateXsdTypes(null, false);
									ProjectUtils.updateXSDFile(projectName, project, sequence, xsdTypes,
											false, false);
								} catch (EngineException e) {
									Engine.logDatabaseObjectManager.warn(e);
								}
							} catch (ClassNotFoundException ee) {
								// Convertigo server distribution
								Engine.logDatabaseObjectManager
										.warn("Sequence's schema and xpath of step sources have not been migrated. Project needs to be migrated with studio.");
							}

							Engine.logDatabaseObjectManager
									.info("XSD/WSDL files sucessfully updated for sequence \""
											+ sequence.getName() + "\"");
						} catch (Exception e) {
							Engine.logDatabaseObjectManager.error(
									"An error occured while updating XSD/WSDL files for project '"
											+ projectName + "'", e);
						}
					}

				}

			} catch (Exception e) {
				Engine.logDatabaseObjectManager.error(
						"An error occured while updating XSD/WSDL files for project '" + projectName + "'", e);
				return false;
			}
		}

		return true;
	}

	private void replaceSourceXpath(Sequence sequence, List<Step> stepList) {
		for (Step step : stepList) {
			if (step instanceof IStepSourceContainer) {
				replaceXpath(sequence, ((IStepSourceContainer) step).getSourceDefinition());
			} else if (step instanceof XMLGenerateDatesStep) {
				replaceXpath(sequence, ((XMLGenerateDatesStep) step).getStartDefinition());
				replaceXpath(sequence, ((XMLGenerateDatesStep) step).getStopDefinition());
				replaceXpath(sequence, ((XMLGenerateDatesStep) step).getDaysDefinition());
			} else if (step instanceof XMLActionStep) {
				for (int i = 0; i < ((XMLActionStep) step).getSourcesDefinitionSize(); i++) {
					replaceXpath(sequence, ((XMLActionStep) step).getSourceDefinition(i));
				}
			}

			// recurse on children steps
			if (step instanceof StepWithExpressions) {
				replaceSourceXpath(sequence, ((StepWithExpressions) step).getSteps());
			}
			// recurse on children variables
			else if (step instanceof RequestableStep) {
				for (StepVariable variable : ((RequestableStep) step).getVariables()) {
					replaceXpath(sequence, variable.getSourceDefinition());
				}
			}
		}
	}

	private void replaceXpath(Sequence sequence, List<String> definition) {
		if (definition.size() > 0) {
			String xpath = definition.get(1);
			if (xpath.startsWith("./")) {
				Long key = new Long(definition.get(0));
				Step sourceStep = sequence.loadedSteps.get(key);
				if ((sourceStep != null) && (sourceStep instanceof RequestableStep)) {
					String replace = (sourceStep instanceof TransactionStep) ? "transaction" : "sequence";
					xpath = xpath.replaceFirst("./", "./" + replace + "/document/");
					definition.set(1, xpath);
				}
			}
		}
	}

	private void addStepTargetProjectImports(List<Step> stepList) throws EngineException {
		for (Step step : stepList) {
			if (step instanceof SequenceStep) {
				SequenceStep sequenceStep = (SequenceStep) step;
				String projectName = sequenceStep.getProject().getName();
				String targetProjectName = sequenceStep.getProjectName();
				if (!projectName.equals(targetProjectName)) {
					File projectFile = new File(Engine.PROJECTS_PATH + "/" + targetProjectName + "/"
							+ targetProjectName + ".xsd");
					if (projectFile.exists()) {
						try {
							ProjectUtils.addXSDFileImport(projectName, targetProjectName, false);
							Engine.logDatabaseObjectManager.info("Sucessfully added xsd import of \""
									+ targetProjectName + "\" project for SequenceStep \""
									+ sequenceStep.getName() + "\"");
						} catch (Exception e) {
							String message = "Unable to add xsd import of \"" + targetProjectName
									+ "\" project for SequenceStep \"" + sequenceStep.getName() + "\"";
							throw new EngineException(message, e);
						}
					} else {
						String message = "Unable to add xsd import of \"" + targetProjectName
								+ "\" project for SequenceStep \"" + sequenceStep.getName() + "\". "
								+ targetProjectName + ".xsd file is missing.";
						throw new EngineException(message);
					}
				}
			} else if (step instanceof TransactionStep) {
				TransactionStep transactionStep = (TransactionStep) step;
				String projectName = transactionStep.getProject().getName();
				String targetProjectName = transactionStep.getProjectName();
				if (!projectName.equals(targetProjectName)) {
					File projectFile = new File(Engine.PROJECTS_PATH + "/" + targetProjectName + "/"
							+ targetProjectName + ".xsd");
					if (projectFile.exists()) {
						try {
							ProjectUtils.addXSDFileImport(projectName, targetProjectName, false);
							Engine.logDatabaseObjectManager.info("Sucessfully added xsd import of \""
									+ targetProjectName + "\" project for TransactionStep \""
									+ transactionStep.getName() + "\"");
						} catch (Exception e) {
							String message = "Unable to add xsd import of \"" + targetProjectName
									+ "\" project for TransactionStep \"" + transactionStep.getName() + "\"";
							throw new EngineException(message, e);
						}
					} else {
						String message = "Unable to add xsd import of \"" + targetProjectName
								+ "\" project for TransactionStep \"" + transactionStep.getName() + "\". "
								+ targetProjectName + ".xsd file is missing.";
						throw new EngineException(message);
					}
				}
			}

			if (step instanceof StepWithExpressions) {
				addStepTargetProjectImports(((StepWithExpressions) step).getSteps());
			}
		}
	}

	private DatabaseObject importDatabaseObject(Node node, DatabaseObject parentDatabaseObject)
			throws EngineException {
		try {
			DatabaseObject databaseObject = DatabaseObject.read(node);
			if (parentDatabaseObject != null)
				parentDatabaseObject.add(databaseObject);
			databaseObject.isImporting = true;
			
			cacheUpdateObject(databaseObject); // put a clone of databaseObject
												// into object's cache

			NodeList childNodes = node.getChildNodes();
			int len = childNodes.getLength();

			Node childNode;
			String childNodeName;

			for (int i = 0; i < len; i++) {
				childNode = childNodes.item(i);

				if (childNode.getNodeType() != Node.ELEMENT_NODE)
					continue;

				childNodeName = childNode.getNodeName();

				if ((!childNodeName.equalsIgnoreCase("property"))
						&& (!childNodeName.equalsIgnoreCase("handlers"))
						&& (!childNodeName.equalsIgnoreCase("wsdltype"))) {
					importDatabaseObject(childNode, databaseObject);
				}
			}

			databaseObject.isImporting = false;
			databaseObject.isSubLoaded = true;

			fireDatabaseObjectImported(new DatabaseObjectImportedEvent(databaseObject));
			return databaseObject;
		} catch (Exception e) {
			if (e instanceof EngineException
					&& ((EngineException) e).getCause() instanceof ClassNotFoundException) {
				Engine.logBeans.error("Maybe a database object doesn't exist anymore, drop it", e);
				return null;
			} else
				throw new EngineException("Unable to import the object from the XML node \""
						+ node.getNodeName() + "\".", e);
		}
	}

	public void cacheUpdateObject(DatabaseObject databaseObject) throws EngineException {
//		synchronized (objects) {
//			cacheUpdateObject(databaseObject, databaseObject.getQName());
//		}
	}

	public void cacheUpdateObject(DatabaseObject databaseObject, String databaseObjectQName)
			throws EngineException {
//		objects.put(databaseObjectQName, databaseObject);
//		synchronized (objects) {
//			try {
//				DatabaseObject clonedDatabaseObject = (DatabaseObject) databaseObject.clone();
//				Engine.logDatabaseObjectManager.trace("cacheUpdateObject(): " + databaseObjectQName);
//				objects.put(databaseObjectQName, clonedDatabaseObject);
//			} catch (CloneNotSupportedException e) {
//				throw new EngineException("CloneNotSupportedException on object \"" + databaseObjectQName
//						+ "\": " + e.getMessage());
//			}
//		}
	}

	public void cacheRemoveObject(String databaseObjectQName) {
//		synchronized (objects) {
//			Engine.logDatabaseObjectManager.trace("cacheRemoveObject(): " + databaseObjectQName);
//			objects.remove(databaseObjectQName);
//		}
	}

	public void cacheRemoveObjects(String databaseObjectQNamePrefix) {
//		synchronized (objects) {
//
//			Engine.logDatabaseObjectManager.trace("cacheRemoveObjects(): " + databaseObjectQNamePrefix);
//
//			// Nathalieh:
//			// This code throws a ConcurrentModificationException after the
//			// first key is removed
//			/*
//			 * for (String key : objects.keySet()) { if
//			 * (key.startsWith(databaseObjectQNamePrefix)) {
//			 * Engine.logDatabaseObjectManager.trace("   >>> removing " + key);
//			 * objects.remove(key); } }
//			 */
//
//			// Nathalieh:
//			// Until someone writes the right code...
//			boolean bContinue = true;
//			while (bContinue) {
//				try {
//					for (String key : objects.keySet()) {
//						if (key.startsWith(databaseObjectQNamePrefix)) {
//							Engine.logDatabaseObjectManager.trace("   >>> removing " + key);
//							objects.remove(key);
//						}
//					}
//					bContinue = false;
//				} catch (ConcurrentModificationException e) {
//					;// ignore
//				}
//			}
//		}
	}
}