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

package com.twinsoft.convertigo.beans.core;

import java.io.File;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.RestApiManager;
import com.twinsoft.convertigo.engine.enums.JsonOutput;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.XPathEngine;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest;
import com.twinsoft.convertigo.engine.requesters.InternalRequester;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.DirClassLoader;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * This class manages a Convertigo Project.
 */
public class Project extends DatabaseObject implements IInfoProperty {

	private static final long serialVersionUID = -7523308164370975102L;

	public static final int MAX_OBJECT_NAME_LENGTH = 64;

	public enum WsdlStyle {
		all("ALL"), docLiteral("DOC/LITERAL"), rpc("RPC");

		private final String label;

		private WsdlStyle(String label) {
			this.label = label;
		}

		public String toString() {
			return label;
		}
	}

	public enum XsdForm {
		qualified, unqualified
	}

	public static final String XSD_FOLDER_NAME = "xsd";
	public static final String XSD_INTERNAL_FOLDER_NAME = "internal";
	public static final String WSDL_FOLDER_NAME = "wsdl";

	public static final String CONVERTIGO_PROJECTS_NAMESPACEURI = "http://www.convertigo.com/convertigo/projects/";

	private final Object mutexClassLoader = new Object();

	/**
	 * The Context timeout in seconds.
	 */
	private int contextTimeout = 300;

	/**
	 * The HTTP session timeout in seconds.
	 */
	private int httpSessionTimeout = 1800;

	/** Holds value of property browserTypes. */
	private XMLVector<XMLVector<String>> browserDefinitions = new XMLVector<XMLVector<String>>();

	/**
	 * The WSDL style (Doc/Literal, RPC or Both).
	 */
	private WsdlStyle wsdlStyle = WsdlStyle.docLiteral;

	/**
	 * WSDL with inline schema or not
	 */
	private boolean schemaInline = true;

	/**
	 * Holds value if we have an undefined global symbol
	 */
	public boolean undefinedGlobalSymbols = false;

	public Set<Pair<String, String>> defaultSymbols;

	/**
	 * The namespace URI
	 */
	private String namespaceUri = "";

	/**
	 * .json and .jsonp requester should use "type" attribute to make output
	 */
	private JsonOutput jsonOutput = JsonOutput.useType;

	private JsonRoot jsonRoot = JsonRoot.docChildNodes;

	/**
	 * The schema element form
	 */
	private XsdForm schemaElementForm = XsdForm.unqualified;

	private boolean bStrictMode = true;

	private String corsOrigin = "=Global";

	private XPathEngine xpathEngine = XPathEngine.JXPath;

	/**
	 * The default connector for this project.
	 */
	transient private Connector defaultConnector = null;

	/**
	 * The list of available connectors for this project.
	 */
	transient private List<Connector> vConnectors = new LinkedList<Connector>();

	/**
	 * The list of available sequences for this project.
	 */
	transient private List<Sequence> vSequences = new LinkedList<Sequence>();

	/**
	 * The list of available references for this project.
	 */
	transient private List<Reference> vReferences = new LinkedList<Reference>();

	transient private String oldName;

	transient private long lastChange = 0L;

	transient private DirClassLoader loader;

	transient private String minVersion;

	public static String getProjectTargetNamespace(String projectName) {
		try {
			Project p = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
			if (p != null) {
				return p.getTargetNamespace();
			}
		} catch (EngineException e) {
		}
		return CONVERTIGO_PROJECTS_NAMESPACEURI + projectName;
	}

	public static void executeAutoStartSequences(final String projectName) {
		try {
			final Project p = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
			if (p != null) {
				for (final Sequence sequence : p.getSequencesList()) {
					if (sequence.isAutoStart()) {
						Engine.execute(() -> {
							try {
								Map<String, String[]> parameters = new HashMap<String, String[]>();
								parameters.put(Parameter.Project.getName(), new String[] { projectName });
								parameters.put(Parameter.Sequence.getName(), new String[] { sequence.getName() });
								InternalHttpServletRequest request = new InternalHttpServletRequest();
								InternalRequester requester = new InternalRequester(
										GenericUtils.<Map<String, Object>>cast(parameters), request);
								requester.processRequest();
							} catch (Exception e) {
								Engine.logEngine.error(
										"Failed to execute the auto start sequence \"" + sequence.getQName() + "\"", e);
							}
						});
					}
				}
			}
		} catch (Exception e) {
			Engine.logEngine.error("Failed to execute auto start sequences for project \"" + projectName + "\"", e);
		}
	}

	/**
	 * Construct a new Project object.
	 */
	public Project() {
		super();
		databaseType = "Project";
	}

	/**
	 * Retrieves the parent Object object.
	 */
	@Override
	public DatabaseObject getParent() {
		return null;
	}

	/**
	 * Sets the object name.
	 *
	 * @param name the object name.
	 */
	@Override
	public void setName(String name) throws EngineException {
		name = name.trim();
		Project.checkName(name);
		oldName = ((oldName == null) ? name : this.getName());
		super.setName(name);
	}

	public static void checkName(String name) throws EngineException {
		if (name.length() > MAX_OBJECT_NAME_LENGTH)
			throw new EngineException("The project name is too long (length > " + MAX_OBJECT_NAME_LENGTH + ")!");
		else if (name.indexOf(' ') == 0)
			throw new EngineException("The project name cannot begin with the space character!");
		else if (name.length() == name.lastIndexOf(' ') + 1)
			throw new EngineException("The project name cannot end with the space character!");
		else if ((name.indexOf('\\') != -1) || (name.indexOf('/') != -1) || (name.indexOf(':') != -1)
				|| (name.indexOf('*') != -1) || (name.indexOf('?') != -1) || (name.indexOf('"') != -1)
				|| (name.indexOf('<') != -1) || (name.indexOf('>') != -1) || (name.indexOf('|') != -1))
			throw new EngineException("The project name (\"" + name
					+ "\") cannot contain one of the following character(s):\n\\/:*?\"<>|");
	}

	public String getQName() {
		return getName();
	}

	public int getContextTimeout() {
		return contextTimeout;
	}

	public void setContextTimeout(int contextTimeout) {
		this.contextTimeout = contextTimeout;
	}

	public int getHttpSessionTimeout() {
		return httpSessionTimeout;
	}

	public void setHttpSessionTimeout(int httpSessionTimeout) {
		this.httpSessionTimeout = httpSessionTimeout;
	}

	/**
	 * Getter for property browserDefinitions.
	 * 
	 * @return Value of property browserDefinitions.
	 */
	public XMLVector<XMLVector<String>> getBrowserDefinitions() {
		return this.browserDefinitions;
	}

	/**
	 * Setter for property browserDefinitions.
	 * 
	 * @param browserTypes New value of property browserDefinitions.
	 */
	public void setBrowserDefinitions(XMLVector<XMLVector<String>> browserDefinitions) {
		this.browserDefinitions = browserDefinitions;
	}

	public WsdlStyle getWsdlStyle() {
		return wsdlStyle;
	}

	public void setWsdlStyle(WsdlStyle wsdlStyle) {
		this.wsdlStyle = wsdlStyle;
	}

	public boolean isSchemaInline() {
		return schemaInline;
	}

	public void setSchemaInline(boolean schemaInline) {
		this.schemaInline = schemaInline;
	}

	/**
	 * @return the namespaceUri
	 */
	public String getNamespaceUri() {
		return namespaceUri;
	}

	/**
	 * @param namespaceUri the namespaceUri to set
	 */
	public void setNamespaceUri(String namespaceUri) {
		this.namespaceUri = namespaceUri;
	}

	public String getTargetNamespace() {
		String targetNamespace = getNamespaceUri();
		if (targetNamespace.equals(""))
			targetNamespace = CONVERTIGO_PROJECTS_NAMESPACEURI + getName();
		return targetNamespace;
	}

	/**
	 * @return the schemaElementForm
	 */
	public XsdForm getSchemaElementForm() {
		return schemaElementForm;
	}

	/**
	 * @param schemaElementForm the schemaElementForm to set
	 */
	public void setSchemaElementForm(XsdForm schemaElementForm) {
		this.schemaElementForm = XsdForm.unqualified; // schemaElementForm
	}

	public boolean isStrictMode() {
		return bStrictMode;
	}

	public void setStrictMode(boolean strictMode) {
		bStrictMode = strictMode;
	}

	public JsonOutput getJsonOutput() {
		return jsonOutput;
	}

	public void setJsonOutput(JsonOutput jsonOutput) {
		this.jsonOutput = jsonOutput;
	}

	public JsonRoot getJsonRoot() {
		return jsonRoot;
	}

	public void setJsonRoot(JsonRoot jsonRoot) {
		this.jsonRoot = jsonRoot;
	}

	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Connector) {
			addConnector((Connector) databaseObject);
		} else if (databaseObject instanceof Sequence) {
			addSequence((Sequence) databaseObject);
		} else if (databaseObject instanceof MobileApplication) {
			addMobileApplication((MobileApplication) databaseObject);
		} else if (databaseObject instanceof UrlMapper) {
			addUrlMapper((UrlMapper) databaseObject);
		} else if (databaseObject instanceof Reference) {
			addReference((Reference) databaseObject);
		} else {
			throw new EngineException(
					"You cannot add to a project a database object of type " + databaseObject.getClass().getName());
		}
	}

	@Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Connector) {
			removeConnector((Connector) databaseObject);
		} else if (databaseObject instanceof Sequence) {
			removeSequence((Sequence) databaseObject);
		} else if (databaseObject instanceof MobileApplication) {
			removeMobileApplication((MobileApplication) databaseObject);
		} else if (databaseObject instanceof UrlMapper) {
			removeUrlMapper((UrlMapper) databaseObject);
		} else if (databaseObject instanceof Reference) {
			removeReference((Reference) databaseObject);
		} else {
			throw new EngineException("You cannot remove from a project a database object of type "
					+ databaseObject.getClass().getName());
		}
		super.remove(databaseObject);
	}

	/**
	 * Adds a connector.
	 */
	protected void addConnector(Connector connector) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vConnectors, connector.getName(), connector.bNew);
		connector.setName(newDatabaseObjectName);
		vConnectors.add(connector);
		if (connector.isDefault)
			setDefaultConnector(connector);
		super.add(connector);
	}

	public void removeConnector(Connector connector) throws EngineException {
		checkSubLoaded();
		vConnectors.remove(connector);
	}

	public List<Connector> getConnectorsList() {
		checkSubLoaded();
		return sort(vConnectors);
	}

	public Connector getConnectorByName(String connectorName) throws EngineException {
		checkSubLoaded();
		for (Connector connector : vConnectors)
			if (connector.getName().equalsIgnoreCase(connectorName))
				return connector;
		throw new EngineException("There is no connector named \"" + connectorName + "\" found into this project.");
	}

	/**
	 * Adds a sequence.
	 */
	protected void addSequence(Sequence sequence) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vSequences, sequence.getName(), sequence.bNew);
		sequence.setName(newDatabaseObjectName);
		vSequences.add(sequence);
		super.add(sequence);
	}

	public void removeSequence(Sequence sequence) throws EngineException {
		checkSubLoaded();
		vSequences.remove(sequence);
	}

	public List<Sequence> getSequencesList() {
		checkSubLoaded();
		return sort(vSequences);
	}

	public Sequence getSequenceByName(String sequenceName) throws EngineException {
		checkSubLoaded();
		for (Sequence sequence : vSequences)
			if (sequence.getName().equalsIgnoreCase(sequenceName))
				return sequence;
		throw new EngineException(
				"There is no sequence named \"" + sequenceName + "\" found into the project \"" + getName() + "\".");
	}

	/**
	 * Adds a reference.
	 */
	protected void addReference(Reference reference) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vReferences, reference.getName(), reference.bNew);
		reference.setName(newDatabaseObjectName);
		vReferences.add(reference);
		loader = null;
		super.add(reference);
	}

	public void removeReference(Reference device) throws EngineException {
		checkSubLoaded();
		loader = null;
		vReferences.remove(device);
	}

	public List<Reference> getReferenceList() {
		checkSubLoaded();
		return sort(vReferences);
	}

	/**
	 * Retrieves the default connector.
	 */
	public Connector getDefaultConnector() throws EngineException {
		if (defaultConnector == null) {
			checkSubLoaded();
			for (Connector connector : vConnectors)
				if (connector.isDefault) {
					defaultConnector = connector;
					break;
				}
		}

		// Report from 4.5: fix #401
		if (defaultConnector == null) {
			// Fire exception in Engine mode only!
			if (Engine.isEngineMode()) {
				throw new EngineException("There is no default connector defined for project \"" + getName() + "\".");
			} else {
				// In Studio mode we must be able to set a default connector!
			}
		}

		return defaultConnector;
	}

	/**
	 * Sets the default connector.
	 */
	public void setDefaultConnector(Connector connector) throws EngineException {
		synchronized (mutex) {
			if (connector == null)
				throw new IllegalArgumentException("The value of argument 'transaction' is null");
			checkSubLoaded();
			if (vConnectors.contains(connector)) {
				if (defaultConnector == null)
					getDefaultConnector();
				if (defaultConnector != null)
					defaultConnector.isDefault = false;
				connector.isDefault = true;
				defaultConnector = connector;
			} else
				throw new IllegalArgumentException(
						"The value of argument 'connector' is invalid: the connector does not belong to the project");
		}
	}

	@Override
	public Project clone() throws CloneNotSupportedException {
		Project clonedObject = (Project) super.clone();
		clonedObject.defaultConnector = null;
		clonedObject.vReferences = new LinkedList<Reference>();
		clonedObject.vConnectors = new LinkedList<Connector>();
		clonedObject.vSequences = new LinkedList<Sequence>();
		clonedObject.mobileApplication = null;
		clonedObject.urlMapper = null;
		clonedObject.mobileBuilder = null;
		return clonedObject;
	}

	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);

		String version = element.getAttribute("version");
		minVersion = version;

		if (VersionUtils.compare(version, "7.5.0") < 0) {
			NodeList properties = element.getElementsByTagName("property");

			Element propName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "name");
			String objectName = (String) XMLUtils
					.readObjectFromXml((Element) XMLUtils.findChildNode(propName, Node.ELEMENT_NODE));

			Element propVarDef1 = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "contextTimeout");
			Element propVarDef2 = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "httpSessionTimeout");
			if (propVarDef1 == null && propVarDef2 != null) {
				propVarDef2.setAttribute("name", "contextTimeout");
				hasChanged = true;
				Engine.logBeans.warn("[Project] The object \"" + objectName
						+ "\" has been updated to version 7.5.0 (property \"httpSessionTimeout\" changed to \"contextTimeout\")");
			}
		}
	}

	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);

		String version = element.getAttribute("version");

		if (version != null) {
			if (VersionUtils.compareMigrationVersion(version, ".m002") < 0) {
				Engine.logDatabaseObjectManager.info("Project's file migration to m002 index.html ...");
				String projectRoot = Engine.projectDir(getName());
				File indexPage = new File(projectRoot + "/index.html");
				if (indexPage.exists()) {
					Engine.logDatabaseObjectManager.info("index.html found, rename it to index_old.html");
					indexPage.renameTo(new File(projectRoot + "/index_old.html"));
				}
				ProjectUtils.copyIndexFile(getName());
				Engine.logDatabaseObjectManager.info("Basic index.html copied");
			}

			if (VersionUtils.compareMigrationVersion(version, ".m006") < 0) {
				Engine.logDatabaseObjectManager
						.info("Project's file migration to m006: set 'jsonOutput' value to 'verbose' (old behavior)");
				jsonOutput = JsonOutput.verbose;
				hasChanged = true;
			}

			if (VersionUtils.compare(version, "7.3.0") < 0) {
				NodeList properties = element.getElementsByTagName("property");

				Element propVarDom = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "bStrictMode");
				if (propVarDom == null) {
					bStrictMode = false;
					hasChanged = true;
					Engine.logBeans.warn("[Project] Successfully set 'bStrictMode' property for project \"" + getName()
							+ "\" (v 7.3.0)");
				}
			}

			if (VersionUtils.compare(version, "7.4.0") < 0) {
				NodeList properties = element.getElementsByTagName("property");
				Element propVarDom = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "jsonRoot");
				if (propVarDom == null) {
					Engine.logDatabaseObjectManager.info(
							"Project's file migration to 7.4.0: set 'jsonRoot' value to 'docNode' (old behavior)");
					jsonRoot = JsonRoot.docNode;
					hasChanged = true;
				}
			}
		}
	}

	private transient MobileBuilder mobileBuilder = null;

	public MobileBuilder getMobileBuilder() {
		if (mobileBuilder == null) {
			try {
				synchronized (this) {
					mobileBuilder = MobileBuilder.getInstance(this);
				}
			} catch (EngineException e) {
				Engine.logEngine.warn(e.getMessage());
			}
		}
		return mobileBuilder;
	}

	public boolean isMobileBuilderInitialized() {
		if (mobileBuilder != null) {
			return mobileBuilder.isInitialized();
		}
		return false;
	}

	private transient MobileApplication mobileApplication = null;

	public MobileApplication getMobileApplication() {
		return mobileApplication;
	}

	public void addMobileApplication(MobileApplication mobileApplication) throws EngineException {
		if (this.mobileApplication != null) {
			throw new EngineException(
					"The project \"" + getName() + "\" already contains a mobile application! Please delete it first.");
		}
		this.mobileApplication = mobileApplication;
		super.add(mobileApplication);
	}

	public void removeMobileApplication(MobileApplication mobileApplication) {
		if (mobileApplication != null && mobileApplication.equals(this.mobileApplication)) {
			this.mobileApplication = null;
		}
	}

	private transient UrlMapper urlMapper = null;

	public UrlMapper getUrlMapper() {
		return ((Project) getOriginal()).urlMapper;
	}

	public void addUrlMapper(UrlMapper urlMapper) throws EngineException {
		if (!isOriginal()) {
			return;
		}
		;
		if (this.urlMapper != null) {
			throw new EngineException(
					"The project \"" + getName() + "\" already contains an URL mapper! Please delete it first.");
		}
		this.urlMapper = urlMapper;
		super.add(urlMapper);
		RestApiManager.getInstance().putUrlMapper(this);
	}

	public void removeUrlMapper(UrlMapper urlMapper) {
		if (!isOriginal()) {
			return;
		}
		;
		if (urlMapper != null && urlMapper.equals(this.urlMapper)) {
			this.urlMapper = null;
			RestApiManager.getInstance().removeUrlMapper(getName());
		}
	}

	@Override
	public List<DatabaseObject> getAllChildren() {
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getConnectorsList());
		rep.addAll(getSequencesList());
		rep.addAll(getReferenceList());
		if (mobileApplication != null)
			rep.add(mobileApplication);
		if (urlMapper != null)
			rep.add(urlMapper);
		return rep;
	}

	public String getOldName() {
		return oldName;
	}

	public String getDirPath() {
		return Engine.projectDir(getName());
	}

	public File getDirFile() {
		return Engine.projectFile(getName()).getParentFile();
	}

	public String getXsdDirPath() {
		return getDirPath() + "/" + XSD_FOLDER_NAME;
	}

	public String getXsdInternalDirPath() {
		return getXsdDirPath() + "/" + XSD_INTERNAL_FOLDER_NAME;
	}

	public String getWsdlDirPath() {
		return getDirPath() + "/" + getName() + "/" + WSDL_FOLDER_NAME;
	}

	@Override
	public void changed() {
		lastChange = System.currentTimeMillis();
		super.changed();
	}

	public long getLastChange() {
		return lastChange;
	}

	/*
	 * The user custom project's version
	 */
	private String version = "";

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	/*
	 * The time of last project export
	 */
	public long getExportTime() {
		try {
			File f = Engine.projectFile(getName());
			return f.lastModified();
		} catch (Throwable t) {
		}
		return 0L;
	}

	public String getInfoForProperty(String propertyName, DateFormat df, Locale locale) {
		if ("exported".equals(propertyName)) {
			try {
				return df.format(new Date(getExportTime()));
			} catch (Exception e) {
			}
		}
		return "";
	}

	public String getInfoForProperty(String propertyName, Locale locale) {
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, locale);
		return getInfoForProperty(propertyName, df, locale);
	}

	public String getInfoForProperty(String propertyName) {
		return getInfoForProperty(propertyName, Locale.getDefault());
	}

	public Map<String, Boolean> getNeededProjects() {
		Map<String, Boolean> neededProjects = new HashMap<>();

		// needed projects by mapping operations
		getNeededProjects(neededProjects, getUrlMapper());

		// needed projects by references
		for (Reference reference : getReferenceList()) {
			getNeededProjects(neededProjects, reference);
		}

		// needed projects by sequences
		for (Sequence sequence : getSequencesList()) {
			getNeededProjects(neededProjects, sequence);
		}

		// needed projects for the mobile application
		try {
			IApplicationComponent app = mobileApplication.getApplicationComponent();

			String tplProject = app.getTplProjectName();
			if (!neededProjects.containsKey(tplProject)) {
				neededProjects.put(tplProject, false);
			}

			for (DatabaseObject child : app.getAllChildren()) {
				getNeededProjects(neededProjects, child);
			}

		} catch (Exception e) {
		}

		return neededProjects;
	}

	private void getNeededProjects(Map<String, Boolean> neededProjects, DatabaseObject dbo) {
		if (dbo instanceof UrlMapper) {
			UrlMapper urlMapper = (UrlMapper) dbo;
			if (urlMapper != null) {
				for (UrlMapping mapping : urlMapper.getMappingList()) {
					for (UrlMappingOperation operation : mapping.getOperationList()) {
						String targetRequestableQName = operation.getTargetRequestable();
						if (!targetRequestableQName.isEmpty()) {
							int index = targetRequestableQName.indexOf(".");
							if (index != -1) {
								String targetProjectName = targetRequestableQName.substring(0, index);
								neededProjects.put(targetProjectName, true);
							}
						}
					}
				}
			}
		} else if (dbo instanceof ProjectSchemaReference) {
			String targetProjectName = ((ProjectSchemaReference) dbo).getParser().getProjectName();
			if (!targetProjectName.isBlank()) {
				neededProjects.put(targetProjectName, true);
			}
		} else if (dbo instanceof Sequence) {
			getNeededProjects(neededProjects, ((Sequence) dbo).getSteps());
		} else if (dbo instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent
				|| dbo instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
			try {
				for (java.beans.PropertyDescriptor pd : CachedIntrospector.getBeanInfo(dbo).getPropertyDescriptors()) {
					if (pd.getPropertyEditorClass() != null) {
						if (pd.getPropertyEditorClass().getSimpleName().equals("NamedSourceSelectorEditor")) {
							Object args[] = {};
							Method getter = pd.getReadMethod();
							String qname = (String) getter.invoke(dbo, args);
							if (!qname.isEmpty() && !qname.startsWith(dbo.getProject().getName() + ".")) {
								int index = qname.indexOf(".");
								if (index != -1) {
									String targetProjectName = qname.substring(0, index);
									neededProjects.put(targetProjectName, true);
								}
							}
						}
					}
				}
				if (dbo instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicElement) {
					com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean ionBean = ((com.twinsoft.convertigo.beans.mobile.components.UIDynamicElement) dbo)
							.getIonBean();
					if (ionBean != null) {
						for (com.twinsoft.convertigo.beans.mobile.components.dynamic.IonProperty property : ionBean
								.getProperties().values()) {
							String editor = property.getEditor();
							if (editor.equals("NamedSourceSelectorEditor")) {
								String qname = (String) property.getValue();
								if (!qname.isEmpty() && !qname.startsWith(dbo.getProject().getName() + ".")) {
									int index = qname.indexOf(".");
									if (index != -1) {
										String targetProjectName = qname.substring(0, index);
										neededProjects.put(targetProjectName, true);
									}
								}
							}
						}
					}
				}
				if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement) {
					com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean ionBean = ((com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement) dbo)
							.getIonBean();
					if (ionBean != null) {
						for (com.twinsoft.convertigo.beans.ngx.components.dynamic.IonProperty property : ionBean
								.getProperties().values()) {
							String editor = property.getEditor();
							if (editor.equals("NamedSourceSelectorEditor")) {
								String qname = (String) property.getValue();
								if (!qname.isEmpty() && !qname.startsWith(dbo.getProject().getName() + ".")) {
									int index = qname.indexOf(".");
									if (index != -1) {
										String targetProjectName = qname.substring(0, index);
										neededProjects.put(targetProjectName, true);
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			for (DatabaseObject child : dbo.getAllChildren()) {
				getNeededProjects(neededProjects, child);
			}
		}
	}

	private void getNeededProjects(Map<String, Boolean> projectList, List<Step> steps) {
		for (Step step : steps) {
			if (step instanceof StepWithExpressions) {
				getNeededProjects(projectList, ((StepWithExpressions) step).getSteps());
			} else if (step instanceof RequestableStep) {
				RequestableStep transactionStep = (RequestableStep) step;
				String targetProjectName = transactionStep.getProjectName();
				if (!targetProjectName.equals(getName())) {
					projectList.put(targetProjectName, true);
				}
			}
		}
	}

	public Map<String, Boolean> getMissingProjectReferences() {
		Map<String, Boolean> missingProjects = getNeededProjects();
		for (String project : new HashSet<>(missingProjects.keySet())) {
			if (ProjectUtils.existProjectSchemaReference(this, project)) {
				missingProjects.remove(project);
			}
		}

		try {
			missingProjects.remove(getName());
			missingProjects.remove(mobileApplication.getApplicationComponent().getTplProjectName());
		} catch (Exception e) {
		}

		return missingProjects;
	}

	public Map<String, Boolean> getMissingProjects() {
		Map<String, Boolean> missingProjects = getNeededProjects();
		for (Entry<String, Boolean> project : new HashMap<>(missingProjects).entrySet()) {
			try {
				if (Engine.theApp.databaseObjectsManager.getOriginalProjectByName(project.getKey(),
						project.getValue()) != null) {
					missingProjects.remove(project.getKey());
				}
			} catch (Exception e) {
			}
		}
		missingProjects.remove(getName());
		return missingProjects;
	}

	public String getCorsOrigin() {
		return corsOrigin;
	}

	public void setCorsOrigin(String corsOrigin) {
		this.corsOrigin = corsOrigin;
	}

	public XPathEngine getXpathEngine() {
		return xpathEngine;
	}

	public void setXpathEngine(XPathEngine xpathEngine) {
		this.xpathEngine = xpathEngine;
	}

	private List<File> addClassPathDirs(List<File> dirs) {
		File dir = new File(getDirPath(), "libs");
		if (!dirs.contains(dir)) {
			dirs.add(dir);
			Map<String, Boolean> missingProjects = getNeededProjects();
			for (Entry<String, Boolean> project : new HashMap<>(missingProjects).entrySet()) {
				try {
					Project prj = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(project.getKey(),
							project.getValue());
					if (prj != null) {
						prj.addClassPathDirs(dirs);
					}
				} catch (Exception e) {
				}
			}
		}
		return dirs;
	}

	public ClassLoader getProjectClassLoader() {
		Object original = getOriginal();
		if (original != this) {
			return ((Project) original).getProjectClassLoader();
		}
		synchronized (mutexClassLoader) {
			if (loader != null && loader.isContentChanged()) {
				loader = null;
			}
			if (loader == null) {
				List<File> dirs = addClassPathDirs(new LinkedList<>());
				File toCopy = new File(Engine.USER_WORKSPACE_PATH + "/libs/" + getName());
				loader = new DirClassLoader(dirs, Engine.getEngineClassLoader(), toCopy);
			}
		}
		return loader;
	}

	public void set(String key, Object value) {
		Engine.theApp.getShareProjectMap(this).set(key, value);
	}

	public Object get(String key) {
		return Engine.theApp.getShareProjectMap(this).get(key);
	}

	public Object getMinVersion() {
		return minVersion;
	}

	public void setMinVersion(String minVersion) {
		this.minVersion = minVersion;
	}

	@Override
	public boolean testAttribute(String name, String value) {
		if ("isApplicationProject".equals(name)) {
			IApplicationComponent app = mobileApplication != null ? mobileApplication.getApplicationComponent() : null;
			return app != null;
		}
		if ("isMobileApplicationProject".equals(name)) {
			IApplicationComponent app = mobileApplication != null ? mobileApplication.getApplicationComponent() : null;
			return app != null && app instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
		}
		return super.testAttribute(name, value);
	}

	static public String formatNameWithHash(Project project) {
		return project != null ? project.getName() + " (" + project.hashCode() + ")" : "unknow";
	}
}
