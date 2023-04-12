/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.event.EventListenerList;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.twinsoft.convertigo.beans.BeansDefaultValues;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.steps.ReadFileStep;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLActionStep;
import com.twinsoft.convertigo.beans.steps.XMLGenerateDatesStep;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboExplorerManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;
import com.twinsoft.convertigo.engine.dbo_explorer.DboParent;
import com.twinsoft.convertigo.engine.dbo_explorer.DboUtils;
import com.twinsoft.convertigo.engine.enums.DeleteProjectOption;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.migration.Migration001;
import com.twinsoft.convertigo.engine.migration.Migration3_0_0;
import com.twinsoft.convertigo.engine.migration.Migration5_0_0;
import com.twinsoft.convertigo.engine.migration.Migration5_0_4;
import com.twinsoft.convertigo.engine.migration.Migration7_0_0;
import com.twinsoft.convertigo.engine.migration.Migration7_4_0;
import com.twinsoft.convertigo.engine.migration.Migration8_0_0;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.YamlConverter;
import com.twinsoft.convertigo.engine.util.ZipUtils;

/**
 * This class is responsible for serializing objects to the Convertigo database
 * repository and restoring them from the Convertigo database repository.
 */
public class DatabaseObjectsManager implements AbstractManager {
	private static final Pattern pValidSymbolName = Pattern.compile("[\\{=}\\r\\n]");
	private static final Pattern pFindSymbol = Pattern.compile("\\$\\{([^\\{\\r\\n]*?)(?:=(.*?(?<!\\\\)))?}");
	private static final Pattern pFindEnv = Pattern.compile("\\%([^\\r\\n]*?)(?:=(.*?(?<!\\\\)))?\\%");
	private static final Pattern pYamlProjectVersion = Pattern.compile("↑(?:(convertigo)|.*?): (.*)");
	private static final Pattern pYamlProjectName = Pattern.compile("(?:↑.*?:.*)|(?:↓(.*?) \\[core\\.Project\\]: )");
	private static final Pattern pProjectName = Pattern.compile("(.*)\\.(?:xml|car)");

	public static interface StudioProjects {
		default public void declareProject(String projectName, File projectFile) {
		}

		default public boolean canOpen(String projectName) {
			return true;
		}

		default public Map<String, File> getProjects(boolean checkOpenable) {
			return Collections.emptyMap();
		}

		default public void projectLoaded(Project project) {
		}

		public File getProject(String projectName);
	}

	public static StudioProjects studioProjects = new StudioProjects() {
		Map<String, File> projectsDir = new HashMap<String, File>();

		@Override
		public void declareProject(String projectName, File projectFile) {
			if (projectFile != null && projectFile.exists()) {
				projectsDir.put(projectName, projectFile);
			}
		}

		public File getProject(String projectName) {
			File file = projectsDir.get(projectName);
			if (file == null || !file.exists()) {
				file = new File(Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".xml");
			}
			return file.exists() ? file : null;
		}
	};

	private Map<String, Project> projects;
	private Map<String, Object> importLocks;

	private String globalSymbolsFilePath = null;
	/**
	 * The symbols repository for compiling text properties.
	 */
	protected Properties symbolsProperties;
	private Object symbolsMutex = new Object();

	// private static String XSL_NAMESPACE_URI =
	// "http://www.w3.org/1999/XSL/Transform";

	public DatabaseObjectsManager() {
	}

	public void init() throws EngineException {
		projects = new HashMap<String, Project>();
		importLocks = new HashMap<String, Object>();
		symbolsInit();
	}

	public void destroy() throws EngineException {
		projects = null;
		symbolsProperties = null;
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

	public List<String> getAllProjectNamesList() {
		return getAllProjectNamesList(true);
	}

	public List<String> getAllProjectNamesList(boolean checkOpenable) {
		Engine.logDatabaseObjectManager.trace("Retrieving all project names from \"" + Engine.PROJECTS_PATH + "\"");

		File projectsDir = new File(Engine.PROJECTS_PATH);
		SortedSet<String> projectNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		projectNames.addAll(projects.keySet());
		projectNames.addAll(getStudioProjects().getProjects(checkOpenable).keySet());

		File[] list = projectsDir.listFiles();
		if (list == null) {
			projectsDir.mkdirs();
			list = projectsDir.listFiles();
		}

		if (list != null) {
			for (File projectDir : projectsDir.listFiles()) {
				String projectName = projectDir.getName();

				if (!projectNames.contains(projectName)) {
					if (projectDir.isFile() && projectDir.length() < 4096) {
						try {
							projectDir = new File(FileUtils.readFileToString(projectDir, StandardCharsets.UTF_8));
						} catch (IOException e) {
						}
					}
					if (projectDir.isDirectory()) {
						File pFile = new File(projectDir, projectName + ".xml");
						if (!pFile.exists()) {
							pFile = new File(projectDir, "c8oProject.yaml");
						}
						try {
							if (projectName.equals(DatabaseObjectsManager.getProjectName(pFile))) {
								if (!checkOpenable || canOpenProject(projectName)) {
									projectNames.add(projectName);
								} else {
									clearCache(projectName);
								}
							}
						} catch (EngineException e) {
						}
					}
				}
			}
		}

		Engine.logDatabaseObjectManager.trace("Project names found: " + projectNames.toString());
		return new ArrayList<String>(projectNames);
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
		public ProjectLoadingData() {}

		public String projectName;
		public boolean undefinedGlobalSymbol = false;
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
		return getOriginalProjectByName(projectName, true);
	}

	public Project getOriginalProjectByName(String projectName, boolean checkOpenable) throws EngineException {
		Engine.logDatabaseObjectManager.trace("Requiring loading of project \"" + projectName + "\"");
		
		if (checkOpenable && !canOpenProject(projectName)) {
			Engine.logDatabaseObjectManager.trace("The project \"" + projectName + "\" cannot be open");
			clearCache(projectName);
			return null;
		}

		Project project;

		synchronized (projects) {
			project = projects.get(projectName);
		}

		if (project == null) {
			long t0 = Calendar.getInstance().getTime().getTime();
			try {
				checkForEngineMigrationProcess(projectName);
				File projectPath = getStudioProjects().getProjects(checkOpenable).get(projectName);
				if (projectPath == null) {
					projectPath = Engine.projectYamlFile(projectName);
					if (projectPath == null || !projectPath.exists()) {
						projectPath = Engine.projectFile(projectName);
					}
				}
				project = importProject(projectPath, false);
			} catch (ClassCastException e) {
				throw new EngineException("The requested object \"" + projectName + "\" is not a project!", e);
			} catch (ProjectInMigrationProcessException e) {
				throw new EngineException("Unable to load the project \"" + projectName
						+ "\": the project is in migration process.", e);
			} catch (VersionException e) {
				throw e;
			} catch (Exception e) {
				throw new EngineException("Unable to load the project \"" + projectName + "\"", e);
			} finally {
				long t1 = Calendar.getInstance().getTime().getTime();
				Engine.logDatabaseObjectManager.info("Project \"" + projectName + "\" loaded in " + (t1 - t0) + " ms");
			}
		} else if (!project.getDirFile().exists()) {
			Engine.logDatabaseObjectManager.warn("Retrieve from cache project \"" + projectName + "\" but removing it because its folder missing: " + project.getDirFile());
			clearCache(project);
			project = null;
		} else {
			Engine.logDatabaseObjectManager.trace("Retrieve from cache project \"" + projectName + "\"");
		}

		return project;
	}

	public static boolean checkParent(Class<? extends DatabaseObject> parentObjectClass, DboBean bean) throws ClassNotFoundException {
		Collection<DboParent> parents = bean.getParents();
		for (DboParent possibleParent : parents) {
			// Check if parent allow inheritance
			if (Class.forName(possibleParent.getClassName()).equals(parentObjectClass) ||
					possibleParent.allowInheritance() && Class.forName(possibleParent.getClassName()).isAssignableFrom(parentObjectClass)) {
				return true;
			}
		}
		return false;
	}

	public static boolean acceptDatabaseObjects(DatabaseObject parentObject, DatabaseObject object ) {
		try {
			Class<? extends DatabaseObject> parentObjectClass = parentObject.getClass();
			Class<? extends DatabaseObject> objectClass = object.getClass();

			DboExplorerManager manager = Engine.theApp.getDboExplorerManager();
			List<DboGroup> groups = manager.getGroups();
			for (DboGroup group : groups) {
				List<DboCategory> categories = group.getCategories();
				for (DboCategory category : categories) {
					List<DboBeans> beansCategories	= category.getBeans();
					for (DboBeans beansCategory : beansCategories) {
						List<DboBean> beans = beansCategory.getBeans();
						for (DboBean bean : beans) {
							String className = bean.getClassName();
							Class<DatabaseObject> beanClass = GenericUtils.cast(Class.forName(className));

							// The bean should derived from DatabaseObject...
							boolean isDatabaseObject = (DatabaseObject.class.isAssignableFrom(beanClass));

							if (isDatabaseObject) {
								// ... and should derived from the specified class
								boolean isFromSpecifiedClass = ((objectClass == null) ||
										((objectClass != null) && (objectClass.isAssignableFrom(beanClass))));
								if (isFromSpecifiedClass) {
									// Check parent
									boolean bFound = checkParent(parentObjectClass, bean);
									if (bFound) {
										// Check technology if needed
										String technology = DboUtils.getTechnology(parentObject, objectClass);
										if (technology != null) {
											Collection<String> acceptedTechnologies = bean.getEmulatorTechnologies();
											if (!acceptedTechnologies.isEmpty() && !acceptedTechnologies.contains(technology)) {
												continue;
											}
										}
										return true;
									}
								}
							}
						}
					}
				}
			}
			return false;
		} catch (Exception e) {
			Engine.logDatabaseObjectManager.error("Unable to load database objects properties.", e);
			return false;
		}
	}

	public static boolean acceptDatabaseObjects(DatabaseObject parentObject, Class<? extends DatabaseObject> objectClass, Class<? extends DatabaseObject> folderBeanClass) {
		try {
			Class<? extends DatabaseObject> parentObjectClass = parentObject.getClass();
			DboExplorerManager manager = Engine.theApp.getDboExplorerManager();
			List<DboGroup> groups = manager.getGroups();
			for (DboGroup group : groups) {
				List<DboCategory> categories = group.getCategories();
				for (DboCategory category : categories) {
					List<DboBeans> beansCategories  = category.getBeans();
					for (DboBeans beansCategory : beansCategories) {
						List<DboBean> beans = beansCategory.getBeans();
						for (DboBean bean : beans) {
							String className = bean.getClassName();
							Class<DatabaseObject> beanClass = GenericUtils.cast(Class.forName(className));

							if (beanClass.equals(objectClass)) {
								// The bean should derived from DatabaseObject...
								boolean isDatabaseObject = (DatabaseObject.class.isAssignableFrom(beanClass));

								if (isDatabaseObject) {
									// ... and should derived from the specified class
									boolean isFromSpecifiedClass = ((folderBeanClass == null) ||
											((folderBeanClass != null) && (folderBeanClass.isAssignableFrom(beanClass))));
									if (isFromSpecifiedClass) {
										// Check parent
										boolean bFound = checkParent(parentObjectClass, bean);
										if (bFound) {
											// Check technology if needed
											String technology = DboUtils.getTechnology(parentObject, objectClass);
											if (technology != null) {
												Collection<String> acceptedTechnologies = bean.getEmulatorTechnologies();
												if (!acceptedTechnologies.isEmpty() && !acceptedTechnologies.contains(technology)) {
													continue;
												}
											}
											return true;
										}
									}
								}
							}
						}
					}
				}
			}
			return false;
		} catch (Exception e) {
			Engine.logDatabaseObjectManager.error("Unable to load database objects properties.", e);
			return false;
		}
	}

	public Project getProjectByName(String projectName) throws EngineException {
		try {
			Project project = getOriginalProjectByName(projectName);
			return project != null ? project.clone() : null;
		} catch (CloneNotSupportedException e) {
			throw new EngineException("Exception on getProjectByName", e);
		}
	}

	public void clearCache(Project project) {
		String name = project.getName();
		clearCache(name);
	}

	public void clearCache(String projectName) {
		Object lock;
		synchronized (importLocks) {
			lock = importLocks.get(projectName);
			if (lock == null) {
				importLocks.put(projectName, lock = new Object());
			}
		}
		Project project = null;
		synchronized (lock) {
			synchronized (projects) {
				project = projects.remove(projectName);
				if (project != null) {
					Engine.logDatabaseObjectManager.info("[clearCache] project removed from cache: "+ Project.formatNameWithHash(project));
				}
			}
		}
		if (project != null) {
			Engine.logDatabaseObjectManager.info("[clearCache] start releasing for "+ Project.formatNameWithHash(project));
			RestApiManager.getInstance().removeUrlMapper(projectName);
			MobileBuilder.releaseBuilder(project);
			Engine.logDatabaseObjectManager.info("[clearCache] end releasing for "+ Project.formatNameWithHash(project));
		}
	}

	public void clearCacheIfSymbolError(String projectName) throws Exception {
		Object lock;
		synchronized (importLocks) {
			lock = importLocks.get(projectName);
			if (lock == null) {
				importLocks.put(projectName, lock = new Object());
			}
		}
		Project project = null;
		synchronized (lock) {
			synchronized (projects) {
				if (projects.containsKey(projectName) && symbolsProjectCheckUndefined(projectName)) {
					project = projects.remove(projectName);
					if (project != null) {
						Engine.logDatabaseObjectManager.info("[clearCacheIfSymbolError] project removed from cache: "+ Project.formatNameWithHash(project));
					}
				}
			}
		}
		if (project != null) {
			Engine.logDatabaseObjectManager.info("[clearCacheIfSymbolError] start releasing for "+ Project.formatNameWithHash(project));
			RestApiManager.getInstance().removeUrlMapper(projectName);
			MobileBuilder.releaseBuilder(project);
			Engine.logDatabaseObjectManager.info("[clearCacheIfSymbolError] end releasing for "+ Project.formatNameWithHash(project));
		}
	}

	public Project getCachedProject(String projectName) {
		synchronized (projects) {
			return projects.get(projectName);
		}
	}

	public boolean existsProject(String projectName) {
		File file = getStudioProjects().getProject(projectName);
		if (file == null) {
			file = new File(Engine.PROJECTS_PATH + "/" + projectName);
		}
		return file.exists();
	}

	public void deleteProject(String projectName, boolean bCreateBackup, boolean bDataOnly)
			throws EngineException {
		deleteProject(projectName,
				bCreateBackup ? DeleteProjectOption.createBackup : null,
						bDataOnly ? DeleteProjectOption.dataOnly : null);
	}

	public void deleteProject(String projectName, DeleteProjectOption... options) throws EngineException {
		boolean bCreateBackup = DeleteProjectOption.createBackup.as(options);
		boolean bDataOnly = DeleteProjectOption.dataOnly.as(options);
		boolean bPreserveEclipe = DeleteProjectOption.preserveEclipse.as(options);
		boolean bPreserveVCS = DeleteProjectOption.preserveVCS.as(options);
		boolean bUnloadOnly = DeleteProjectOption.unloadOnly.as(options);
		try {
			// Remove all pooled related contexts in server mode
			if (Engine.isEngineMode() && !Engine.isCliMode()) {
				// Bugfix #1659: do not call getProjectByName() if the migration
				// process is ongoing!
				if (!(Thread.currentThread() instanceof MigrationJob)) {
					Project projectToDelete = Engine.theApp.databaseObjectsManager.getCachedProject(projectName);
					if (projectToDelete != null) {
						for (Connector connector : projectToDelete.getConnectorsList()) {
							Engine.theApp.contextManager.removeDevicePool(connector.getQName());
						}
					}
					Engine.theApp.contextManager.removeAll("/" + projectName);
				}
			}

			if (bUnloadOnly) {
				clearCache(projectName);
				return;
			}

			File projectDir = new File(Engine.projectDir(projectName));
			File removeDir = projectDir;

			if (!bDataOnly && !bPreserveEclipe && !bPreserveVCS) {
				StringBuilder sb = new StringBuilder("_remove_" + projectName);
				while ((removeDir = new File(projectDir.getParentFile(), sb.toString())).exists()) {
					sb.append('_');
				}
				if (!projectDir.renameTo(removeDir)) {
					throw new EngineException("Unable to rename project's directory. It may be locked.");
				}
			}

			if (bCreateBackup && EnginePropertiesManager.getPropertyAsBoolean(PropertyName.ZIP_BACKUP_OLD_PROJECT) && !Engine.isCliMode()) {
				Engine.logDatabaseObjectManager.info("Making backup of project \"" + projectName + "\"");
				makeProjectBackup(projectName, removeDir);
			}

			if (bDataOnly) {
				Engine.logDatabaseObjectManager.info("Deleting _data for project \"" + projectName + "\"");
				File dataDir = new File(removeDir, "_data");
				deleteDir(dataDir);

				Engine.logDatabaseObjectManager.info("Deleting _private for project \"" + projectName + "\"");
				File privateDir = new File(removeDir, "/_private");
				deleteDir(privateDir);
			} else {
				Engine.logDatabaseObjectManager.info("Deleting  project \"" + projectName + "\"");
				if (!bPreserveEclipe && !bPreserveVCS) {
					deleteDir(removeDir);
					File f = new File(Engine.PROJECTS_PATH, projectName);
					if (f.exists() && f.isFile()) {
						f.delete();
					}
				} else {
					for (File f: removeDir.listFiles((dir, name) -> {
						if (bPreserveEclipe && (name.equals(".project") || name.equals(".settings"))) {
							return false;
						}
						if (bPreserveVCS && (name.equals(".git") || name.equals(".svn"))) {
							return false;
						}
						return true;
					})) {
						deleteDir(f);
					};
				}
			}

			clearCache(projectName);
		} catch (Exception e) {
			throw new EngineException("Unable to delete" + (bDataOnly ? " datas for" : "") + " project \""
					+ projectName + "\".", e);
		}
	}

	public void deleteProjectAndCar(String projectName, DeleteProjectOption... options) throws EngineException {
		try {
			deleteProject(projectName, options);

			String projectArchive = Engine.projectDir(projectName) + ".car";
			deleteDir(new File(projectArchive));
		} catch (Exception e) {
			throw new EngineException("Unable to delete the project \"" + projectName + "\".", e);
		}
	}

	public static void deleteDir(File dir) throws IOException {
		Engine.logDatabaseObjectManager.trace("Deleting the directory \"" + dir.getAbsolutePath() + "\"");
		if (dir.exists()) {
			if (dir.isDirectory()) {
				boolean deleted = FileUtils.deleteQuietly(dir);
				if (deleted) {
					Engine.logDatabaseObjectManager.debug("Deleting the file \"" + dir.getAbsolutePath() + "\" by a native command.");
					return;
				}

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

	public void makeProjectBackup(String projectName, File projectDir) throws EngineException {
		try {

			if (projectDir.exists()) {
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
				ZipUtils.makeZip(projectArchiveFilename, projectDir.getPath(), projectName, new HashSet<File>(Arrays.asList(
						new File(projectDir, "_private"),
						new File(projectDir, ".git"),
						new File(projectDir, ".svn"))
						));
			} else {
				Engine.logDatabaseObjectManager.warn("Cannot make project archive, the folder '" + projectDir + "' doesn't exist.");
			}
		} catch (Exception e) {
			throw new EngineException(
					"Unable to make backup archive for the project \"" + projectName + "\".", e);
		}
	}

	public Project updateProject(File projectFile) throws EngineException {
		return updateProject(projectFile.getAbsolutePath());
	}

	public Project updateProject(String projectFileName) throws EngineException {
		try {
			boolean isArchive = false, needsMigration = false;
			Project project = null;

			Engine.logDatabaseObjectManager.trace("DatabaseObjectsManager.updateProject() - projectFileName  :  " + projectFileName);
			File projectFile = new File(projectFileName);
			Engine.logDatabaseObjectManager.trace("DatabaseObjectsManager.updateProject() - projectFile.exists()  :  " + projectFile.exists());

			if (projectFile.exists()) {
				String projectName = getProjectName(projectFile);
				if (projectFileName.endsWith(".car")) {
					isArchive = true;
				}

				if (projectName != null) {
					if (isArchive) {
						// Deploy project (will backup project and perform the
						// migration through import if necessary)
						project = deployProject(projectFileName, needsMigration);
					} else {
						project = importProject(projectName, false);
					}
				}
			} else {
				//Added by julienda - 10/09/2012
				Engine.logDatabaseObjectManager.trace("DatabaseObjectsManager.updateProject() - projectFileName :  " + projectFileName);
				//Get the correct archive file (path)
				String archiveFileProject =  ZipUtils.getArchiveName(projectFileName);

				if (archiveFileProject == null) {
					throw new EngineException("File \"" + projectFileName + "\" is missing");
				} else {
					//Call method with the correct archive (path)
					updateProject(new File(new File (projectFileName).getParent(), archiveFileProject).getPath());
				}

				Engine.logDatabaseObjectManager.trace("DatabaseObjectsManager.updateProject() - archiveFileProject  :  " + archiveFileProject);
			}
			return project;
		} catch (Exception e) {
			throw new EngineException("Unable to update the project from the file \"" + projectFileName
					+ "\".", e);
		}
	}

	public void exportProject(Project project) throws EngineException {
		String projectName = project.getName();

		// Export project
		String exportedProjectFileName = Engine.projectFile(projectName).getAbsolutePath();
		Engine.logDatabaseObjectManager.info("Saving project \"" + projectName + "\" to: " + exportedProjectFileName);
		CarUtils.exportProject(project, exportedProjectFileName);
		if (exportedProjectFileName.endsWith(".xml")) {
			File yaml = new File(new File(exportedProjectFileName).getParentFile(), "c8oProject.yaml");
			Engine.logDatabaseObjectManager.info("Declaring project project \"" + projectName + "\" to: " + yaml.getAbsolutePath());
			getStudioProjects().declareProject(projectName, yaml);
		}
		RestApiManager.getInstance().putUrlMapper(project);
		Engine.logDatabaseObjectManager.info("Project \"" + projectName + "\" saved!");
	}

	public Project deployProject(String projectArchiveFilename, boolean bForce) throws EngineException {
		return deployProject(projectArchiveFilename, null, bForce);
	}

	public Project deployProject(String projectArchiveFilename, String targetProjectName, boolean bForce)
			throws EngineException {
		return deployProject(projectArchiveFilename, targetProjectName, bForce, false);
	}

	public Project deployProject(URL projectUrl, String targetProjectName, boolean bForce, boolean keepOldReferences) throws Exception {
		HttpGet get = new HttpGet(projectUrl.toURI());
		File archive = File.createTempFile("convertigoImportFromHttp", ".car");
		archive.deleteOnExit();
		try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(get)) {
			FileUtils.deleteQuietly(archive);
			archive.getParentFile().mkdirs();
			long length = response.getEntity().getContentLength();
			String sl = Long.toString(length);
			if (length < 1) {
				length = Integer.MAX_VALUE;
				sl = "??";
			}
			try (FileOutputStream fos = new FileOutputStream(archive)) {
				InputStream is = response.getEntity().getContent();
				byte[] buf = new byte[1024 * 1024];
				int n;
				long t = 0, now, ts = 0;
				while (t < length && (n = is.read(buf, 0, (int) Math.min(length - t, buf.length))) > -1) {
					fos.write(buf, 0, n);
					t += n;
					now = System.currentTimeMillis();
					if (now > ts) {
						Engine.logEngine.debug("Download project from " + projectUrl.toString() + " : " + t + " / " + sl);
						ts = now + 2000;
					}
				}
				Engine.logEngine.debug("Download project from " + projectUrl.toString() + " : " + t + " / " + sl);
			}
			Engine.logEngine.info("Downloaded project " + projectUrl.toString() + " to " + archive.toString());
			return deployProject(archive.getAbsolutePath(), targetProjectName, bForce, keepOldReferences);
		} finally {
			archive.delete();
		}
	}

	public Project deployProject(String projectArchiveFilename, String targetProjectName, boolean bForce, boolean keepOldReferences)
			throws EngineException {
		if (projectArchiveFilename.matches("https?://.+")) {
			try {
				return deployProject(new URL(projectArchiveFilename), targetProjectName, bForce, keepOldReferences);
			} catch (Exception e) {
				Engine.logDatabaseObjectManager.warn("Failed to load project from '" + projectArchiveFilename + "', try again\nBecause of [" + e.getClass().getSimpleName() + "] " + e.getMessage());
				try {
					return deployProject(new URL(projectArchiveFilename), targetProjectName, bForce, keepOldReferences);
				} catch (Exception e2) {
					throw new EngineException("Failed to load project from '" + projectArchiveFilename + "' because of [" + e2.getClass().getSimpleName() + "] " + e2.getMessage(), e2);
				}
			}
		}
		String archiveProjectName, projectDirPath;
		try {
			archiveProjectName = ZipUtils.getProjectName(projectArchiveFilename);
			if (archiveProjectName == null) {
				String message = "Unable to deploy the project from the file \"" + projectArchiveFilename
						+ "\". Inconsistency between the archive and project names.";
				Engine.logDatabaseObjectManager.error(message);
				throw new EngineException(message);
			}

			Engine.logDatabaseObjectManager.trace("Deploying project from \"" + projectArchiveFilename + "\"");
			Engine.logDatabaseObjectManager.trace("- archiveProjectName: " + archiveProjectName);
			Engine.logDatabaseObjectManager.trace("- targetProjectName: " + targetProjectName);

			if (targetProjectName == null && projectArchiveFilename != null) {
				targetProjectName = archiveProjectName;
			}

			// Handle non-normalized project name here (fix ticket #788 : Can
			// not import project 213.car)
			String normalizedProjectName = StringUtils.normalize(targetProjectName);
			if (!targetProjectName.equals(normalizedProjectName)) {
				targetProjectName = "project_" + normalizedProjectName;
			}

			File existingProject = Engine.projectFile(targetProjectName);
			projectDirPath = existingProject.getParent();

			Engine.logDatabaseObjectManager.info("Deploying the project \"" + archiveProjectName + "\" to \"" + projectDirPath + "\"");
			try {
				if (existingProject.exists()) {
					if (bForce) {
						// Deleting existing project if any
						deleteProject(targetProjectName,
								DeleteProjectOption.createBackup,
								DeleteProjectOption.preserveEclipse,
								DeleteProjectOption.preserveVCS);
					} else {
						Engine.logDatabaseObjectManager.info("Project \"" + targetProjectName + "\" has already been deployed.");
						return null;
					}
				}

				new File(projectDirPath).mkdir();
				Engine.logDatabaseObjectManager.debug("Project directory created: " + projectDirPath);
			} catch (Exception e) {
				throw new EngineException("Unable to create the project directory \"" + projectDirPath + "\".", e);
			}

			// Decompressing Convertigo archive
			Engine.logDatabaseObjectManager.debug("Analyzing the archive entries: " + projectArchiveFilename);
			ZipUtils.expandZip(projectArchiveFilename, projectDirPath, archiveProjectName);
		} catch (Exception e) {
			throw new EngineException("Unable to deploy the project from the file \"" + projectArchiveFilename + "\".", e);
		}

		try {
			File xmlFile = new File(projectDirPath + "/" + archiveProjectName + ".xml");
			// Rename project and files if necessary
			if (!targetProjectName.equals(archiveProjectName)) {
				xmlFile = ProjectUtils.renameProjectFile(xmlFile, targetProjectName, keepOldReferences);
			}

			if (getProjectLoadingData().projectName == null) {
				getProjectLoadingData().projectName = targetProjectName;
			}

			// Import project (will perform the migration)
			Project project = importProject(xmlFile, true);

			// Rename connector's directory under traces directory if needed
			// (name should be normalized since 4.6)
			File tracesDir = new File(projectDirPath + "/Traces");
			if (tracesDir.isDirectory()) {
				File connectorDir;
				String connectorName;
				File[] files = tracesDir.listFiles();
				for (int i = 0; i < files.length; i++) {
					connectorDir = files[i];
					if (connectorDir.isDirectory()) {
						connectorName = connectorDir.getName();
						if (!StringUtils.isNormalized(connectorName)) {
							if (!connectorDir.renameTo(new File(Engine.PROJECTS_PATH + "/" + targetProjectName
									+ "/Traces/" + StringUtils.normalize(connectorName))))
								Engine.logDatabaseObjectManager.warn("Could not rename \"" + connectorName
										+ "\" directory under \"Traces\" directory.");
						}
					}
				}
			}

			Engine.logDatabaseObjectManager.info("Project \"" + targetProjectName + "\" deployed!");
			return project;
		} catch (VersionException e) {
			throw e;
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
			String projectDir = Engine.projectDir(projectName);
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

	static private Map<String ,Pair<String, Long>> projectNameCache = new HashMap<>();
	static public String getProjectName(File projectFile) throws EngineException {
		String projectName = null;
		if (projectFile != null) {
			String path = projectFile.getAbsolutePath();
			if (projectFile.exists()) {
				Pair<String, Long> cache;
				long lastModified = projectFile.lastModified();
				if ((cache = projectNameCache.get(path)) != null
						&& cache.getRight() == lastModified) {
					projectName = cache.getLeft();
				} else {
					String filename = projectFile.getName();
					if (filename.equals("c8oProject.yaml")) {
						try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(projectFile), StandardCharsets.UTF_8))) {
							String line = br.readLine();
							Matcher m = pYamlProjectName.matcher("");
							while (line != null && projectName == null) {
								m.reset(line);
								if (m.matches()) {
									projectName = m.group(1);
									line = br.readLine();
								} else {
									line = null;
								}
							}
						} catch (Exception e) {
							throw new EngineException("Unable to parse the yaml: " + path, e);
						}
					} else if (filename.endsWith(".car") || filename.endsWith(".zip")) {
						try {
							projectName = ZipUtils.getProjectName(path);
						} catch (IOException e) {
						}
					}
					if (projectName == null) {
						Matcher m = pProjectName.matcher(filename);
						if (m.matches()) {
							projectName = m.group(1);
						}
					}
					projectNameCache.put(path, Pair.of(projectName, lastModified));
				}
			} else {
				projectNameCache.remove(path);
			}
		}
		return projectName;
	}

	static public String getProjectVersion(File projectFile) throws EngineException {
		final String[] version = { null };
		if (projectFile.exists()) {
			if (projectFile.getName().equals("c8oProject.yaml")) {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(projectFile), StandardCharsets.UTF_8))) {
					String line = br.readLine();
					Matcher m = pYamlProjectVersion.matcher("");
					while (line != null && version[0] == null) {
						m.reset(line);
						if (m.matches()) {
							if (m.group(1) != null) {
								version[0] = m.group(2);
							} else {
								line = br.readLine();
							}
						} else {
							line = null;
						}
					}
				} catch (Exception e) {
					throw new EngineException("Unable to parse the yaml: " + projectFile.getAbsolutePath(), e);
				}
			} else if (projectFile.getName().endsWith(".xml")) {
				try {
					XMLUtils.saxParse(projectFile, new DefaultHandler() {

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
							}
							throw new SAXException("stop");
						}

					});
				} catch (SAXException e) {
					if (!"stop".equals(e.getMessage())) {
						throw new EngineException("Unable to find the project version", e);
					}
				} catch (IOException e) {
					throw new EngineException("Unable to parse the xml: " + projectFile.getAbsolutePath(), e);
				}
			}
		}
		return version[0];
	}

	public Project importProject(String importFileName, boolean override) throws EngineException {
		return importProject(new File(importFileName), override);
	}

	public Project importProject(File importFile, boolean override) throws EngineException {
		String filename = importFile.getName();
		if (filename.endsWith(".xml") && !importFile.exists()) {
			String oldName = filename;
			importFile = new File(importFile.getParentFile(), "c8oProject.yaml");
			Engine.logDatabaseObjectManager.info("Trying to load unexisting: " + oldName + "\nLoading instead: " + importFile);
		}
		String projectName = getProjectName(importFile);
		if (projectName == null) {
			return null;
		}
		Object lock;
		Project project = null;
		try {
			synchronized (importLocks) {
				lock = importLocks.get(projectName);
				if (lock == null) {
					importLocks.put(projectName, lock = new Object());
				}
			}
			String version;
			boolean isMigrating;
			boolean needExport;

			Engine.logDatabaseObjectManager.info("[importProject] Waiting synchronized: " + projectName);
			synchronized (lock) {
				Engine.logDatabaseObjectManager.info("[importProject] Enter synchronized: " + projectName);

				if (!override) {
					synchronized (projects) {
						project = projects.get(projectName);
						if (project != null) {
							Engine.logDatabaseObjectManager.info("[importProject] return project from cache: " + Project.formatNameWithHash(project));
							return project;
						}
					}
				}

				Document document;
				Engine.logDatabaseObjectManager.info("Importing project from: " + importFile);
				if (importFile.getName().equals("c8oProject.yaml")) {
					document = YamlConverter.readYaml(importFile);
					document = BeansDefaultValues.unshrinkProject(document);
				} else {
					document = XMLUtils.loadXml(importFile);
				}

				// Performs necessary XML migration
				Element projectNode = performXmlMigration(document);

				Element rootElement = document.getDocumentElement();
				Element projectElement = (Element) XMLUtils.findChildNode(rootElement, Node.ELEMENT_NODE);

				// Retrieve project version
				version = rootElement.getAttribute("beans");
				projectElement.setAttribute("version", version);

				// Retrieve project name
				NodeList properties = projectElement.getElementsByTagName("property");
				Element pName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "name");
				String xName = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(pName, Node.ELEMENT_NODE));
				if (!projectName.equals(xName)) {
					throw new EngineException("Project name mismatch: " + projectName + " != " + xName);
				}

				needExport = "true".equals(document.getUserData("needExport"));
				isMigrating = "true".equals(document.getUserData("isMigrating"));

				if (isMigrating) {
					Engine.logDatabaseObjectManager.debug("Project '" + projectName + "' needs to be migrated");
					// Delete project's data only (will backup project)
					deleteProject(projectName, true, true);
				}

				studioProjects.declareProject(projectName, importFile);

				projectLoadingDataThreadLocal.remove();
				getProjectLoadingData().projectName = projectName;

				// Import will perform necessary beans migration (see deserialization)
				try {
					project = (Project) importDatabaseObject(projectNode, null);
				} catch (VersionException e) {
					e.setProjectName(projectName);
					e.setProjectPath(importFile.getAbsolutePath());
					throw e;
				} catch (Exception e) {
					if (document != null) {
						Engine.logDatabaseObjectManager.error("Failed to import project \"" + projectName + "\":\n" + XMLUtils.prettyPrintDOM(document));
					}
					throw e;
				}
				project.undefinedGlobalSymbols = getProjectLoadingData().undefinedGlobalSymbol;

				synchronized (projects) {
					projects.put(project.getName(), project);
				}
				Engine.logDatabaseObjectManager.info("[importProject] Put in projects cache: " + Project.formatNameWithHash(project));
				Engine.logDatabaseObjectManager.info("[importProject] Leave synchronized: " + Project.formatNameWithHash(project));
			}


			if (this instanceof SystemDatabaseObjectsManager) {
				return project;
			}

			Engine.logDatabaseObjectManager.info("[importProject] projectLoaded: " + Project.formatNameWithHash(project));
			getStudioProjects().projectLoaded(project);

			Engine.logDatabaseObjectManager.info("[importProject] start initializing: " + Project.formatNameWithHash(project));
			RestApiManager.getInstance().putUrlMapper(project);
			MobileBuilder.initBuilder(project);
			Engine.logDatabaseObjectManager.info("[importProject] end initializing: " + Project.formatNameWithHash(project));

			if (!Engine.isStudioMode()) {
				Engine.theApp.referencedProjectManager.check(project);
			}

			// Creates xsd/wsdl files (Since 4.6.0)
			performWsMigration(version, projectName);

			// Performs POST migration
			performPostMigration(version, projectName);

			// Export the project (Since 4.6.0)
			if (isMigrating || needExport) {

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

			if (project.undefinedGlobalSymbols) {
				Engine.logDatabaseObjectManager.error("Project \"" + projectName + "\" contains undefined global symbols: " + symbolsGetUndefined(projectName));
			}

			if (Engine.isEngineMode() && !Engine.isCliMode()) {
				File prjDir = project.getDirFile();
				File pDir = new File(Engine.PROJECTS_PATH, projectName);
				if (pDir != prjDir && !pDir.exists()) {
					FileUtils.write(pDir, prjDir.getCanonicalPath(), StandardCharsets.UTF_8);
				}
			}

			Engine.logDatabaseObjectManager.info("Project \"" + projectName + "\" imported!");

			if (!Engine.isCliMode()) {
				Project p = project;
				Engine.logDatabaseObjectManager.debug("Syncing FullSync DesignDocument for the projet loaded from: " + importFile);
				Engine.execute(() -> {
					CouchDbManager.syncDocument(p);
				});
			}

			return project;
		} catch (VersionException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to import the project from \"" + importFile + "\".", e);
		}
	}

	private Element performXmlMigration(Document document) throws EngineException {
		try {
			Element rootElement = document.getDocumentElement();

			Element projectNode = (Element) XMLUtils.findChildNode(rootElement, Node.ELEMENT_NODE);

			String version = projectNode.getAttribute("version");
			if ("".equals(version)) {
				version = rootElement.getAttribute("beans");
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

			// Migration to version 7.0.0 (mobile application)
			if (VersionUtils.compare(version, "7.0.0") < 0) {
				Engine.logDatabaseObjectManager.info("XML project's file migration to 7.0.0 schema (mobile application)...");

				projectNode = Migration7_0_0.migrate(document, projectNode);

				if (Engine.logDatabaseObjectManager.isTraceEnabled())
					Engine.logDatabaseObjectManager.trace("XML migrated to v7.0.0:\n"
							+ (XMLUtils.prettyPrintDOM(document)));

				Engine.logDatabaseObjectManager.info("Project's XML file migrated!");
			}

			// Migration to version 8.0.0 (ngx shared component's events)
			if (VersionUtils.compareProductVersion(version, "8.0.0") <= 0) {
				Engine.logDatabaseObjectManager.info("XML project's file migration to 8.0.0 schema (ngx shared component's events)...");

				projectNode = Migration8_0_0.migrate(document, projectNode);

				if (Engine.logDatabaseObjectManager.isTraceEnabled())
					Engine.logDatabaseObjectManager.trace("XML migrated to v8.0.0:\n"
							+ (XMLUtils.prettyPrintDOM(document)));

				Engine.logDatabaseObjectManager.info("Project's XML file migrated!");
			}

			return projectNode;
		} catch (Exception e) {
			throw new EngineException("Unable to perform XML migration for project", e);
		}
	}

	private void performPostMigration(String version, String projectName) {
		if (VersionUtils.compare(version, "6.2.0") < 0) {
			try {
				//Project project = getProjectByName(projectName); //recursivity issue since #4780
				Project project = getOriginalProjectByName(projectName, false);
				for (Sequence sequence : project.getSequencesList()) {
					// Modify source's xpath for steps which have a source on a ReadFileStep
					replaceSourceXpath(version, sequence, sequence.getSteps());
				}

			} catch (Exception e) {
				Engine.logDatabaseObjectManager.error(
						"An error occured while performing 6.2.0 migration for project '" + projectName + "'", e);
			}
		}

		if (VersionUtils.compare(version, "7.0.0") < 0) {
			// !! Studio mode only !!
			// Project must be migrated by hand through: Studio import
			if (Engine.isStudioMode()) {
				// Project's xsd/wsdl no more based on file (Since 7.0.0)
				Migration7_0_0.migrate(projectName);
			}
		}

		if (VersionUtils.compare(version, "7.4.0") < 0) {
			// !! Studio mode only !!
			// Project must be migrated by hand through: Studio import
			if (Engine.isStudioMode()) {
				// Change MobilePlatform config.xml
				Migration7_4_0.migrate(projectName);
			}
		}

	}

	private boolean performWsMigration(String version, String projectName) {
		/** Part of 4.6.0 migration : creates and update XSD/WSDL static files **/

		if (VersionUtils.compare(version, "4.6.0") < 0) {
			try {
				// Retrieve a !clone! of project to perform update
				Project project = getProjectByName(projectName);

				for (Connector connector : project.getConnectorsList()) {
					// Retrieve backup wsdlTypes and store Transaction's schema
					for (Transaction transaction : connector.getTransactionsList()) {
						try {
							String xsdTypes = transaction.migrateToXsdTypes();
							transaction.writeSchemaToFile(xsdTypes);
							Engine.logDatabaseObjectManager
							.info("Internal schema stored for \""
									+ transaction.getName() + "\" transaction");
						} catch (Exception e) {
							Engine.logDatabaseObjectManager.error(
									"An error occured while writing schema to file for \""
											+ transaction.getName() + "\" transaction");
						}
					}
				}

				// Fix sequence's steps sources
				for (Sequence sequence : project.getSequencesList()) {
					try {
						List<Step> steps = sequence.getSteps();

						// Replace source's xpath
						// replace ./xxx by
						// ./transaction/document/xxx or by
						// ./sequence/document/xxx
						replaceSourceXpath(version, sequence, steps);

						Engine.logDatabaseObjectManager
						.info("Step sources updated for sequence \""
								+ sequence.getName() + "\"");
					} catch (Exception e) {
						Engine.logDatabaseObjectManager.error(
								"An error occured while updating step sources for sequence \""
										+ sequence.getName() + "\"");
					}
				}


			} catch (Exception e) {
				Engine.logDatabaseObjectManager.error(
						"An error occured while updating project '" + projectName + "' for XSD", e);
				return false;
			}
		}

		return true;
	}

	private void replaceSourceXpath(String version, Sequence sequence, List<Step> stepList) {
		for (Step step : stepList) {
			if (step instanceof IStepSourceContainer) {
				replaceXpath(version, sequence, ((IStepSourceContainer) step).getSourceDefinition());
			} else if (step instanceof XMLGenerateDatesStep) {
				replaceXpath(version, sequence, ((XMLGenerateDatesStep) step).getStartDefinition());
				replaceXpath(version, sequence, ((XMLGenerateDatesStep) step).getStopDefinition());
				replaceXpath(version, sequence, ((XMLGenerateDatesStep) step).getDaysDefinition());
			} else if (step instanceof XMLActionStep) {
				for (int i = 0; i < ((XMLActionStep) step).getSourcesDefinitionSize(); i++) {
					replaceXpath(version, sequence, ((XMLActionStep) step).getSourceDefinition(i));
				}
			}

			// recurse on children steps
			if (step instanceof StepWithExpressions) {
				replaceSourceXpath(version, sequence, ((StepWithExpressions) step).getSteps());
			}
			// recurse on children variables
			else if (step instanceof RequestableStep) {
				for (StepVariable variable : ((RequestableStep) step).getVariables()) {
					replaceXpath(version, sequence, variable.getSourceDefinition());
				}
			}
		}
	}

	private void replaceXpath(String version, Sequence sequence, List<String> definition) {
		if (definition.size() > 0) {
			String xpath = definition.get(1);
			if (xpath.startsWith("./")) {
				Long key = Long.valueOf(definition.get(0));
				Step sourceStep = sequence.loadedSteps.get(key);
				if (sourceStep != null) {
					if (VersionUtils.compare(version, "4.6.0") < 0) {
						if (sourceStep instanceof RequestableStep) {
							String replace = (sourceStep instanceof TransactionStep) ? "transaction" : "sequence";
							xpath = xpath.replaceFirst("./", "./" + replace + "/document/");
							definition.set(1, xpath);
						}
					}
					else if (VersionUtils.compare(version, "6.2.0") < 0) {
						if (sourceStep instanceof ReadFileStep) {
							xpath = ((ReadFileStep)sourceStep).migrateSourceXpathFor620(xpath);
							definition.set(1, xpath);
						}
					}
				}
			}
		}
	}

	private DatabaseObject importDatabaseObject(Node node, DatabaseObject parentDatabaseObject)
			throws EngineException {
		try {
			DatabaseObject databaseObject = DatabaseObject.read(node);

			if (parentDatabaseObject != null) {
				parentDatabaseObject.add(databaseObject);
			}

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
						&& (!childNodeName.equalsIgnoreCase("wsdltype"))
						&& (!childNodeName.equalsIgnoreCase("docdata"))
						&& (!childNodeName.equalsIgnoreCase("beandata"))) {
					importDatabaseObject(childNode, databaseObject);
				}
			}

			databaseObject.isImporting = false;
			databaseObject.isSubLoaded = true;

			fireDatabaseObjectImported(new DatabaseObjectImportedEvent(databaseObject));
			return databaseObject;
		} catch (VersionException e) {
			throw e;
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

	public void renameProject(Project project, String newName) throws ConvertigoException {
		renameProject(project, newName, false);
	}

	public void renameProject(Project project, String newName, boolean keepOldReferences) throws ConvertigoException {
		String oldName = project.getName();

		if (oldName.equals(newName)) {
			return;
		}
		// Rename dir
		File file = Engine.projectFile(oldName);
		if (!file.exists()) {
			return;
		}

		File dir = file.getParentFile();
		File newDir = new File(dir.getParentFile(), newName);
		if (!dir.renameTo(newDir)) {
			throw new EngineException(
					"Unable to rename the object path \""
							+ dir.getAbsolutePath()
							+ "\" to \""
							+ newDir.getAbsolutePath()
							+ "\".\n This directory already exists or is probably locked by another application.");
		}

		try {
			Engine.logDatabaseObjectManager.info("Renaming project '" + oldName + "' to '" + newName + "' " + (keepOldReferences ? "with" : "without") + " keepOldReferences");
			clearCache(project);
			project.setName(newName);
			project.hasChanged = true;
			exportProject(project);
			file = new File(newDir, file.getName());
			ProjectUtils.renameProjectFile(file, newName, keepOldReferences);
			FileUtils.deleteQuietly(new File(newDir, ".project"));
		} catch (Exception e) {
			throw new ConvertigoException("Failed to rename to project", e);
		}
	}

	public String getCompiledValue(String value) throws UndefinedSymbolsException {
		Matcher mFindSymbol = pFindSymbol.matcher(value);
		if (mFindSymbol.find(0)) {
			int start = 0;
			StringBuffer newValue = new StringBuffer();
			Set<String> undefinedSymbols = null;
			do {
				newValue.append(value.substring(start, mFindSymbol.start()));

				String name = mFindSymbol.group(1);
				String def = mFindSymbol.group(2);

				String symbolValue = symbolsGetValue(name);

				if (symbolValue == null) {
					if (def != null) {
						symbolValue = def.replace("\\}", "}");
					} else {
						if (undefinedSymbols == null) {
							undefinedSymbols = new HashSet<String>();
						}
						undefinedSymbols.add(name);
						symbolValue = "";
					}
				}

				// Handle symbols in this symbol value
				symbolValue = getCompiledValue(symbolValue);

				// Handle environment variables %name%, %name=def%, %name=def\\%%,
				symbolValue = replaceEnvValues(symbolValue);

				newValue.append(symbolValue);

				start = mFindSymbol.end();
			} while (mFindSymbol.find(start));

			newValue.append(value.substring(start));

			if (undefinedSymbols != null) {
				throw new UndefinedSymbolsException(undefinedSymbols, newValue.toString());
			}

			return newValue.toString();
		} else {
			return value;
		}
	}

	private String replaceEnvValues(String symbolValue) {
		Matcher mFindEnv = pFindEnv.matcher(symbolValue);

		// If there is at least an environment variable
		if (mFindEnv.find(0)) {
			int start = 0;
			// The symbol value re-built
			StringBuffer newValue = new StringBuffer();
			do {
				// Append the string between the last occurrence and the next one
				newValue.append(symbolValue.substring(start, mFindEnv.start()));

				// Get the environment variable name and its default value
				String name = mFindEnv.group(1);
				String def = mFindEnv.group(2);

				String envValue = System.getenv(name);

				if (envValue == null) {
					if (def != null) {
						envValue = def.replace("\\%", "%");
					} else {
						// If the environment variable is not defined and there is not default value neither
						Engine.logDatabaseObjectManager.error("The environment variable "  + name + " is undefined.");
						envValue = mFindEnv.group(0);
					}
				}

				// Append the value of the environment variable
				newValue.append(envValue);

				start = mFindEnv.end();
			} while (mFindEnv.find(start)); // While there is an environment variable

			// Append the string between the last occurrence and the end
			newValue.append(symbolValue.substring(start));

			return newValue.toString();

		} else {
			return symbolValue;
		}
	}

	public Object getCompiledValue(Object propertyObjectValue) throws UndefinedSymbolsException {
		if (propertyObjectValue instanceof String) {
			return getCompiledValue((String) propertyObjectValue);
		} else {
			Set<String> undefinedSymbols = null;
			if (propertyObjectValue instanceof XMLVector<?>) {

				XMLVector<Object> xmlv = GenericUtils.<XMLVector<Object>> cast(propertyObjectValue);

				for (int i = 0; i < xmlv.size(); i++) {
					Object ob = xmlv.get(i);
					Object compiled;

					try {
						compiled = getCompiledValue(ob);
					} catch (UndefinedSymbolsException e) {
						compiled = e.incompletValue();
						if (undefinedSymbols == null) {
							undefinedSymbols = new HashSet<String>();
						}
						undefinedSymbols.addAll(e.undefinedSymbols());
					}

					if (ob != compiled) { // symbol case
						if (xmlv == propertyObjectValue) {
							propertyObjectValue = xmlv = new XMLVector<Object>(xmlv);
						}
						xmlv.set(i, compiled);
					}
				}
			} else if (propertyObjectValue instanceof SmartType) {
				SmartType smartType = (SmartType) propertyObjectValue;
				if (smartType.isUseExpression()) {
					String expression = smartType.getExpression();
					String compiled;
					try {
						compiled = getCompiledValue(expression);
					} catch (UndefinedSymbolsException e) {
						compiled = (String) e.incompletValue();
						undefinedSymbols = e.undefinedSymbols();
					}
					if (compiled != expression) {
						propertyObjectValue = smartType = smartType.clone();
						smartType.setExpression(compiled);
					}
				}
			}

			if (undefinedSymbols != null) {
				throw new UndefinedSymbolsException(undefinedSymbols, propertyObjectValue);
			}
		}

		return propertyObjectValue;
	}

	protected void symbolsInit() {
		synchronized (symbolsMutex) {
			symbolsProperties = new Properties();

			if (Engine.isCliMode()) {
				return;
			}

			globalSymbolsFilePath = System.getProperty(Engine.JVM_PROPERTY_GLOBAL_SYMBOLS_FILE_COMPATIBILITY,
					System.getProperty(Engine.JVM_PROPERTY_GLOBAL_SYMBOLS_FILE,
							Engine.CONFIGURATION_PATH + "/global_symbols.properties"));
			Properties prop = new Properties();

			try {
				PropertiesUtils.load(prop, globalSymbolsFilePath);
			} catch (FileNotFoundException e) {
				String msg = "The symbols file specified in JVM argument as \""
						+ globalSymbolsFilePath + "\" does not exist! Creating a new one...";
				System.out.println(msg);
				if (Engine.logDatabaseObjectManager != null) {
					Engine.logDatabaseObjectManager.warn(msg);
				}

				// Create the global_symbols.properties file into the default workspace
				File globalSymbolsProperties = new File(Engine.CONFIGURATION_PATH + "/global_symbols.properties");
				globalSymbolsFilePath = globalSymbolsProperties.getAbsolutePath();

				try {
					PropertiesUtils.store(prop, globalSymbolsProperties, "global symbols");
					msg = "New global symbols file created: " + globalSymbolsProperties.getAbsolutePath();
					System.out.println(msg);
					if (Engine.logDatabaseObjectManager != null) {
						Engine.logDatabaseObjectManager.warn(msg);
					}
				} catch (Exception e1) {
					Engine.logDatabaseObjectManager.error("Error while creating the global_symbols.properties file; symbols won't be calculated.", e1);
					return;
				}
			} catch (IOException e) {
				Engine.logDatabaseObjectManager.error(
						"Error while reading symbols file specified in JVM argument as \"" + globalSymbolsFilePath
						+ "\"; symbols won't be calculated.", e);
				return;
			}

			symbolsLoad(prop);

			Engine.logEngine.info("Symbols file \"" + globalSymbolsFilePath + "\" loaded! [" + symbolsProperties.size() + "]");
		}
	}

	private void symbolsLoad(Properties map) {
		// Enumeration of the properties
		Enumeration<String> propsEnum = GenericUtils.cast(map.propertyNames());
		boolean needUpdate = false;
		while (propsEnum.hasMoreElements()) {
			String propertyName = propsEnum.nextElement();
			try {
				symbolsAdd(propertyName, uncipherSymbol(map, propertyName), false);
				needUpdate = true;
			} catch (Exception e) {
				Engine.logEngine.info("Don't add invalid symbol '" + propertyName + "'", e);
			}
		}
		if (needUpdate) {
			symbolsUpdated();
		}
	}

	public void symbolsUpdate(Properties map, String importAction) {
		synchronized (symbolsMutex) {
			if (importAction == null) {
				importAction = "priority-server";
			}
			File f = new File(globalSymbolsFilePath);

			File oldFile = null;
			if (f.exists()) {
				Date date = new Date();
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

				File parentFile = f.getParentFile();
				oldFile = new File(parentFile, f.getName().replaceAll(".properties", "_" + dateFormat.format(date) + ".properties"));

				int i = 1;
				while (oldFile.exists()) {
					oldFile = new File(parentFile, f.getName().replaceAll(".properties", "_" + dateFormat.format(date) + "_" + i + ".properties"));
					i++;
				}
				f.renameTo(oldFile);
			}
			//Remove all symbols & import symbols from file
			if (importAction.equals("clear-import")) {
				symbolsProperties.clear();
				symbolsLoad(map);
			}
			//Add symbols from imported file and merge with existing symbols from server (priority to server if same key)
			if (importAction.equals("priority-server")) {
				symbolsFileImport(map, true);
			}
			//Add symbols from imported file and merge with existing symbols from server (priority to import symbols if same key)
			if (importAction.equals("priority-import")) {
				symbolsFileImport(map, false);
			}
		}
	}

	static private String uncipherSymbol(Properties props, String name) {
		String value = props.getProperty(name);
		if (value != null && name.endsWith(".secret")) {
			value = Crypto2.decodeFromHexString(value);
		}
		return value;
	}

	private void symbolsFileImport(Properties map, boolean keepServerSymbols) {
		// Enumeration of the properties
		Enumeration<String> propsEnum = GenericUtils.cast(map.propertyNames());
		boolean needUpdate = false;
		while (propsEnum.hasMoreElements()) {
			String propertyName = propsEnum.nextElement();
			try {
				if (keepServerSymbols){
					if (!symbolsProperties.containsKey(propertyName)) {
						symbolsAdd(propertyName, uncipherSymbol(map, propertyName), false);
						needUpdate = true;
					}
				} else {
					if (symbolsProperties.containsKey(propertyName)) {
						symbolsProperties.remove(propertyName);
					}
					symbolsAdd(propertyName, uncipherSymbol(map, propertyName), false);
					needUpdate = true;
				}
			} catch (Exception e) {
				Engine.logEngine.info("Don't add invalid symbol '" + propertyName + "'", e);
			}
		}
		if (needUpdate) {
			symbolsUpdated();
		}
	}

	private void symbolsUpdated() {
		synchronized (symbolsMutex) {
			try {
				Properties symbolsToStore = new Properties();
				Enumeration<String> propsEnum = GenericUtils.cast(symbolsProperties.propertyNames());
				while (propsEnum.hasMoreElements()) {
					String name = propsEnum.nextElement();
					symbolsToStore.put(name, symbolsGetValueStore(name));
				}
				String msg = "symbolsUpdated: " + symbolsToStore + " [" + symbolsToStore.size() + "] to:" + globalSymbolsFilePath;
				if (Engine.logDatabaseObjectManager != null) {
					Engine.logDatabaseObjectManager.warn(msg);
				}
				System.out.println(msg);
				PropertiesUtils.store(symbolsToStore, globalSymbolsFilePath, "global symbols");
			} catch (Exception e) {
				Engine.logEngine.error("Failed to store symbols!", e);
			}

			for (Project project : projects.values()) {
				getProjectLoadingData().undefinedGlobalSymbol = false;
				try {
					new WalkHelper() {

						@Override
						protected void walk(DatabaseObject databaseObject) throws Exception {
							databaseObject.updateSymbols();
							super.walk(databaseObject);
						}

					}.init(project);
				} catch (Exception e) {
					Engine.logDatabaseObjectManager.error("Failed to update symbols of '" + project.getName() + "' project.", e);
				}
				project.undefinedGlobalSymbols = getProjectLoadingData().undefinedGlobalSymbol;
			}
		}
	}

	public void symbolsCreateUndefined(Set<String> symbolsUndefined) throws Exception {
		if (!symbolsUndefined.isEmpty()) {
			for (String symbolUndefined : symbolsUndefined) {
				symbolsAdd(symbolUndefined, "", false);
			}
			symbolsUpdated();
		}
	}

	public String symbolsGetValue(String symbolName) {
		return symbolsProperties.getProperty(symbolName);
	}

	public String symbolsGetValueStore(String symbolName) {
		String value = symbolsGetValue(symbolName);
		if (value != null && symbolName.endsWith(".secret")) {
			value = Crypto2.encodeToHexString(value);
		}
		return value;
	}

	public String symbolsGetValueService(String symbolName) {
		String value = symbolsGetValue(symbolName);
		if (value != null && symbolName.endsWith(".secret")) {
			value = "**********";
		}
		return value;
	}

	public Set<String> symbolsGetNames() {
		return Collections.unmodifiableSet(GenericUtils.<Set<String>>cast(symbolsProperties.keySet()));
	}

	private void symbolsValidName(String symbolName) {
		if (symbolName == null || symbolName.isEmpty()) {
			throw new IllegalArgumentException("The symbol name must not be empty");
		} else if (pValidSymbolName.matcher(symbolName).find()) {
			throw new IllegalArgumentException("The symbol name must not contain the following caracters '{', '=', or '}'");
		}
	}

	private void symbolsAdd(String symbolName, String symbolValue, boolean update) {
		symbolsValidName(symbolName);
		synchronized (symbolsProperties) {
			if (symbolsProperties.containsKey(symbolName)) {
				throw new IllegalArgumentException("The symbol name is already defined");
			} else {
				symbolsProperties.put(symbolName, symbolValue);
			}
			if (update) {
				symbolsUpdated();
			}
		}
	}

	public void symbolsAdd(String symbolName, String symbolValue) {
		symbolsAdd(symbolName, symbolValue, true);
	}

	public void symbolsEdit(String oldSymbolName, String symbolName, String symbolValue) {
		symbolsValidName(symbolName);
		synchronized (symbolsProperties) {
			symbolsProperties.remove(oldSymbolName);
			symbolsProperties.put(symbolName, symbolValue);
		}
		symbolsUpdated();
	}

	public void symbolsDelete(String symbolName) {
		synchronized (symbolsProperties) {
			symbolsProperties.remove(symbolName);
		}
		symbolsUpdated();
	}

	public void symbolsDeleteAll() {
		synchronized (symbolsProperties) {
			symbolsProperties.clear();
		}
		symbolsUpdated();
	}

	public boolean symbolsProjectCheckUndefined(String projectName) throws Exception {
		final Project project = getOriginalProjectByName(projectName, false);
		if (project == null) {
			return false;
		}
		if (project.undefinedGlobalSymbols) {
			project.undefinedGlobalSymbols = false;
			new WalkHelper() {

				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					if (databaseObject.isSymbolError()) {
						project.undefinedGlobalSymbols = true;
					} else {
						super.walk(databaseObject);
					}
				}

			}.init(project);
		}
		return project.undefinedGlobalSymbols;
	}

	public Set<String> symbolsGetUndefined(String projectName) throws Exception {
		Project project = getOriginalProjectByName(projectName, false);
		final Set<String> allUndefinedSymbols = new HashSet<String>();

		if (project.undefinedGlobalSymbols) {
			new WalkHelper() {

				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					if (databaseObject.isSymbolError()) {
						for (Set<String> undefinedSymbols : databaseObject.getSymbolsErrors().values()) {
							allUndefinedSymbols.addAll(undefinedSymbols);
						}
					}
					super.walk(databaseObject);
				}

			}.init(project);
		}

		return Collections.unmodifiableSet(allUndefinedSymbols);
	}

	public void symbolsCreateUndefined(String projectName) throws Exception {
		Set<String> undefinedSymbols = symbolsGetUndefined(projectName);
		symbolsCreateUndefined(undefinedSymbols);
		Engine.logDatabaseObjectManager.info("The undefined global symbols for the project \"" + projectName + "\" are declared: " + undefinedSymbols);
	}

	public Set<String> symbolsSetCheckUndefined(Set<String> value) {
		for (String name : value) {
			if (symbolsProperties.containsKey(name)) {
				value = new HashSet<String>(value);
				value.removeAll(symbolsProperties.keySet());
				return Collections.unmodifiableSet(value);
			}
		}
		return value;
	}

	public void fullsyncUpdate() {
		for (Project project : projects.values()) {
			CouchDbManager.syncDocument(project);
		}
	}

	public boolean canOpenProject(String projectName) {
		return getStudioProjects().canOpen(projectName);
	}

	public DatabaseObject getDatabaseObjectByQName(String qname) throws Exception {
		qname = qname.replaceFirst("\\.\\w+?:$", "");
		String[] name = qname.split("\\.");
		String project = name[0];
		DatabaseObject dbo = getOriginalProjectByName(project);
		for (int i = 1; i < name.length && dbo != null; i++) {
			dbo = dbo.getDatabaseObjectChild(name[i]);
		}
		return dbo;
	}

	public StudioProjects getStudioProjects() {
		return studioProjects;
	}
}