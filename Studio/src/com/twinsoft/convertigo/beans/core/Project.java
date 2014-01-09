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

package com.twinsoft.convertigo.beans.core;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;

/**
 * This class manages a Convertigo Project.
 */
public class Project extends DatabaseObject implements ITagsProperty, IInfoProperty {

	private static final long serialVersionUID = -7523308164370975102L;

	public static final int MAX_OBJECT_NAME_LENGTH = 64;
	
	public static final String WSDL_STYLE_ALL = "ALL";
	public static final String WSDL_STYLE_DOC = "DOC/LITERAL";
	public static final String WSDL_STYLE_RPC = "RPC";
	
	public static final String XSD_FORM_QUALIFIED = "qualified";
	public static final String XSD_FORM_UNQUALIFIED = "unqualified";
	
	public static final String XSD_FOLDER_NAME = "xsd";
	public static final String XSD_INTERNAL_FOLDER_NAME = "internal";
	public static final String WSDL_FOLDER_NAME = "wsdl";
	
	public final static String CONVERTIGO_PROJECTS_NAMESPACEURI = "http://www.convertigo.com/convertigo/projects/";
	
    /**
     * The HTTP session timeout.
     */
    private int httpSessionTimeout = 300;
    
    /** Holds value of property browserTypes. */
    private XMLVector<XMLVector<String>> browserDefinitions = new XMLVector<XMLVector<String>>();
    
    /**
     * The WSDL style (Doc/Literal, RPC or Both).
     */
	private String wsdlStyle = WSDL_STYLE_DOC;
	
	/**
	 * WSDL with inline schema or not
	 */
	private boolean schemaInline = true;
	
	/**
	 * Holds value if we have an undefined global symbol
	 */
	public boolean undefinedGlobalSymbols = false;
	
	/** 
	 * The namespace URI
	 */
	private String namespaceUri = "";
	
	/**
	 * The schema element form
	 */
	private String schemaElementForm = XSD_FORM_UNQUALIFIED;
	
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
	
	public static String getProjectTargetNamespace(String projectName) {
		try {
			Project p = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
			return p.getTargetNamespace();
		} catch (EngineException e) {
			return CONVERTIGO_PROJECTS_NAMESPACEURI + projectName;
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
		oldName = ((oldName == null) ? name:this.getName());
		super.setName(name);
	}
    
	public static void checkName(String name) throws EngineException {
		if (name.length() > MAX_OBJECT_NAME_LENGTH) throw new EngineException("The project name is too long (length > " + MAX_OBJECT_NAME_LENGTH + ")!");
		else if (name.indexOf(' ') == 0) throw new EngineException("The project name cannot begin with the space character!");
		else if (name.length() == name.lastIndexOf(' ') + 1) throw new EngineException("The project name cannot end with the space character!");
		else if ((name.indexOf('\\') != -1) || (name.indexOf('/') != -1) || (name.indexOf(':') != -1) || (name.indexOf('*') != -1) ||
		(name.indexOf('?') != -1) || (name.indexOf('"') != -1) || (name.indexOf('<') != -1) || (name.indexOf('>') != -1) || (name.indexOf('|') != -1))
			throw new EngineException("The project name (\"" + name + "\") cannot contain one of the following character(s):\n\\/:*?\"<>|");
	}

	public String getQName() {
		return getName();
	}
	
    public int getHttpSessionTimeout() {
        return httpSessionTimeout;
    }
    
    public void setHttpSessionTimeout(int httpSessionTimeout) {
        this.httpSessionTimeout = httpSessionTimeout;
    }
    
    /** Getter for property browserDefinitions.
     * @return Value of property browserDefinitions.
     */
    public XMLVector<XMLVector<String>> getBrowserDefinitions() {
        return this.browserDefinitions;
    }
    
    /** Setter for property browserDefinitions.
     * @param browserTypes New value of property browserDefinitions.
     */
    public void setBrowserDefinitions(XMLVector<XMLVector<String>> browserDefinitions) {
        this.browserDefinitions = browserDefinitions;
    }
    
    public String getWsdlStyle() {
		return wsdlStyle;
	}

	public void setWsdlStyle(String wsdlStyle) {
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
	public String getSchemaElementForm() {
		return schemaElementForm;
	}

	/**
	 * @param schemaElementForm the schemaElementForm to set
	 */
	public void setSchemaElementForm(String schemaElementForm) {
		this.schemaElementForm = XSD_FORM_UNQUALIFIED; // schemaElementForm
	}
	
	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Connector) {
			addConnector((Connector) databaseObject);
		} else if (databaseObject instanceof Sequence) {
			addSequence((Sequence) databaseObject);
		} else if (databaseObject instanceof MobileApplication) {
			addMobileApplication((MobileApplication) databaseObject);
		} else if (databaseObject instanceof Reference) {
			addReference((Reference) databaseObject);
		} else {
			throw new EngineException("You cannot add to a project a database object of type " + databaseObject.getClass().getName());
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
		} else if (databaseObject instanceof Reference) {
			removeReference((Reference) databaseObject);
		} else {
			throw new EngineException("You cannot remove from a project a database object of type " + databaseObject.getClass().getName());
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
		if (connector.isDefault) setDefaultConnector(connector);
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
			if (connector.getName().equalsIgnoreCase(connectorName)) return connector;
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
			if (sequence.getName().equalsIgnoreCase(sequenceName)) return sequence;
		throw new EngineException("There is no sequence named \"" + sequenceName + "\" found into this project.");
	}

	/**
	 * Adds a reference.
	 */
	protected void addReference(Reference reference) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vReferences, reference.getName(), reference.bNew);
		reference.setName(newDatabaseObjectName);
		vReferences.add(reference);
		super.add(reference);
	}

	public void removeReference(Reference device) throws EngineException {
		checkSubLoaded();
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
			}
			else {
				//In Studio mode we must be able to set a default connector!
			}
		}
		
		return defaultConnector;
	}

	/**
	 * Sets the default connector.
	 */
	public synchronized void setDefaultConnector(Connector connector) throws EngineException {
		if (connector == null)
			throw new IllegalArgumentException("The value of argument 'transaction' is null");
		checkSubLoaded();
		if (vConnectors.contains(connector)) {
			if (defaultConnector == null) getDefaultConnector();
			if (defaultConnector != null) defaultConnector.isDefault = false;
			connector.isDefault = true;
			defaultConnector = connector;
		} else throw new IllegalArgumentException("The value of argument 'connector' is invalid: the connector does not belong to the project");
	}
	
	@Override
	public Project clone() throws CloneNotSupportedException {
		Project clonedObject = (Project) super.clone();
		clonedObject.defaultConnector = null;
		clonedObject.vReferences = new LinkedList<Reference>();
		clonedObject.vConnectors = new LinkedList<Connector>();
		clonedObject.vSequences = new LinkedList<Sequence>();
		clonedObject.mobileApplication = null;
		return clonedObject;
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.ITagsProperty#getTagsForProperty(java.lang.String)
	 */
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("wsdlStyle")) {
			return new String[]{WSDL_STYLE_ALL, WSDL_STYLE_DOC, WSDL_STYLE_RPC};
		}
		else if (propertyName.equals("schemaElementForm")) {
			return new String[]{XSD_FORM_QUALIFIED, XSD_FORM_UNQUALIFIED};
		}
		return new String[0];
	}
	
    @Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
		String version = element.getAttribute("version");
		
		 if (version!= null && VersionUtils.compareMigrationVersion(version, ".m002") < 0) {
	        	Engine.logDatabaseObjectManager.info("Project's file migration to m002 index.html ...");
	        	String projectRoot = Engine.PROJECTS_PATH+'/'+getName();
	        	File indexPage = new File(projectRoot+"/index.html");
	        	if(indexPage.exists()){
	        		Engine.logDatabaseObjectManager.info("index.html found, rename it to index_old.html");
	        		indexPage.renameTo(new File(projectRoot+"/index_old.html"));
	        	}
	        	ProjectUtils.copyIndexFile(getName());
	        	Engine.logDatabaseObjectManager.info("Basic index.html copied");
		 }
    }
    
    private transient MobileApplication mobileApplication = null;
    
    public MobileApplication getMobileApplication() {
    	return mobileApplication;
    }
    
    public void addMobileApplication(MobileApplication mobileApplication) throws EngineException {
    	if (this.mobileApplication != null) {
    		throw new EngineException("The project \"" + getName() + "\" already contains a mobile application! Please delete it first.");
    	}
    	this.mobileApplication = mobileApplication;
		super.add(mobileApplication);
    }
    
    public void removeMobileApplication(MobileApplication mobileApplication) {
    	this.mobileApplication = mobileApplication;
    }

	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		rep.addAll(getConnectorsList());
		rep.addAll(getSequencesList());
		rep.addAll(getReferenceList());
		if (mobileApplication != null) rep.add(mobileApplication);
		return rep;
	}

	public String getOldName() {
		return oldName;
	}
	
	public String getXsdDirPath() {
		return Engine.PROJECTS_PATH + "/" + getName() + "/" + XSD_FOLDER_NAME;
	}

	public String getXsdInternalDirPath() {
		return getXsdDirPath() + "/" + XSD_INTERNAL_FOLDER_NAME;
	}
	
	public String getWsdlDirPath() {
		return Engine.PROJECTS_PATH + "/" + getName() + "/" + WSDL_FOLDER_NAME;
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
	private long exported = 0L;

	public void setExportTime(long exported) {
		this.exported = exported;
	}

	public long getExportTime() {
		return exported;
	}

	public String getInfoForProperty(String propertyName, DateFormat df, Locale locale) {
		if ("exported".equals(propertyName)) {
			try {
				return df.format(new Date(getExportTime()));
			}
			catch (Exception e) {}
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
	
}
