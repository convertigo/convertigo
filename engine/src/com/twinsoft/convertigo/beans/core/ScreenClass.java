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

package com.twinsoft.convertigo.beans.core;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.util.GenericUtils;

/**
 * This class defines a screen class.
 */
@DboCategoryInfo(
		getCategoryId = "ScreenClass",
		getCategoryName = "Screen class",
		getIconClassCSS = "convertigo-action-newScreenclass"
	)
public class ScreenClass extends DatabaseObject implements ISheetContainer, IContainerOrdered {

	private static final long serialVersionUID = 6886891146000360258L;
	
    /**
     * The vector of ordered Criterias objects which have to be verified by the ScreenClass.
     */
	private XMLVector<XMLVector<Long>> orderedCriterias = null;
	
    /**
     * The vector of ordered ExtractionRules objects which can be applied on the ScreenClass.
     */
	private XMLVector<XMLVector<Long>> orderedExtractionRules = null;
    
    /**
     * The array of inherited ScreenClass objects of the ScreenClass.
     */
    transient private List<ScreenClass> vInheritedScreenClasses = new LinkedList<ScreenClass>();
    
    /**
     * The array of Criteria objects which have to be verified by the ScreenClass.
     */
    transient private List<Criteria> vCriterias = new LinkedList<Criteria>();
    
    transient private List<Criteria> vAllCriterias = null;
    
    /**
     * The array of ExtractionRule objects which can be applied on the ScreenClass.
     */
    transient private List<ExtractionRule> vExtractionRules = new LinkedList<ExtractionRule>();
    
    transient private List<ExtractionRule> vAllExtractionRules = null;

    /**
     * The array of Sheet objects which can be applied on the ScreenClass.
     */
    transient private List<Sheet> vSheets = new LinkedList<Sheet>();
    
	/**
     * Constructs a new ScreenClass object.
     */
    public ScreenClass() {
        super();
        databaseType = "ScreenClass";
        
        orderedCriterias = new XMLVector<XMLVector<Long>>();
        orderedCriterias.add(new XMLVector<Long>());

        orderedExtractionRules = new XMLVector<XMLVector<Long>>();
        orderedExtractionRules.add(new XMLVector<Long>());
    }

    public int getDepth() {
    	if (parent instanceof ScreenClass) {
    		return ((ScreenClass) parent).getDepth() + 1;
    	}
    	else {
			return 0;
    	}
    }
    
	@Override
	public void add(DatabaseObject databaseObject, Long after) throws EngineException {
        if (databaseObject instanceof Criteria) {
            addCriteria((Criteria) databaseObject, after);
        }
        else if (databaseObject instanceof ExtractionRule) {
            addExtractionRule((ExtractionRule) databaseObject, after);
        }
        else if (databaseObject instanceof Sheet) {
            addSheet((Sheet) databaseObject);
        }
        else if (databaseObject instanceof ScreenClass) {
            addInheritedScreenClass((ScreenClass) databaseObject);
        }
        else if (databaseObject instanceof BlockFactory) {
        	// do nothing as this is done by the JavelinScreenClass that inherits this class
        } else {
        	throw new EngineException("You cannot add to a screen class a database object of type " + databaseObject.getClass().getName());
        }		
	}
    
    @Override
    public void add(DatabaseObject databaseObject) throws EngineException {
    	add(databaseObject, null);
    }

    @Override
    public void remove(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof Criteria)
            removeCriteria((Criteria) databaseObject);
        else if (databaseObject instanceof ExtractionRule)
            removeExtractionRule((ExtractionRule) databaseObject);
        else if (databaseObject instanceof Sheet)
            removeSheet((Sheet) databaseObject);
        else if (databaseObject instanceof ScreenClass)
            removeInheritedScreenClass((ScreenClass) databaseObject);
        else throw new EngineException("You cannot remove from a screen class a database object of type " + databaseObject.getClass().getName());
		super.remove(databaseObject);
    }

    /**
     * Adds a new criteria to the screen class definition.
     *
     * @param criteria the criteria to add.
     */
    public void addCriteria(Criteria criteria, Long after) throws EngineException {
    	checkSubLoaded();
    	
		String newDatabaseObjectName = getChildBeanName(vCriterias, criteria.getName(), criteria.bNew);
		criteria.setName(newDatabaseObjectName);
        
        vCriterias.add(criteria);
        
        super.add(criteria);
        
        insertOrderedCriteria(criteria, after);
    }
    
    public void addCriteria(Criteria criteria) throws EngineException {
    	addCriteria(criteria, null);
    }
    
    public void insertOrderedCriteria(Criteria criteria, Long after) {
    	List<Long> ordered = orderedCriterias.get(0);
    	int size = ordered.size();
    	
    	long value = criteria.priority;
    	
    	if (ordered.contains(value))
    		return;
    	
    	if (after == null) {
    		after = (long)0;
    		if (size > 0)
    			if (criteria.parent.equals(this))
    				after = ordered.get(ordered.size()-1);
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, value);
    	hasChanged = true;

        for (ScreenClass inheritedScreenClass : getInheritedScreenClasses())
        	inheritedScreenClass.insertOrderedCriteria(criteria,after);
    }
    
    public List<Criteria> getLocalCriterias() {
    	checkSubLoaded();
        return sort(vCriterias);
    }
    
    public List<Criteria> getUnsortedCriterias() {
    	checkSubLoaded();
        List<Criteria> v = GenericUtils.clone(vCriterias);
        if (parent instanceof ScreenClass) {
        	Collection<Criteria> vInheritedCriterias = ((ScreenClass) parent).getUnsortedCriterias();
            v.addAll(vInheritedCriterias);
        }
        return v;
    }
    
    public List<Criteria> getCriterias(boolean reset) {
    	if (reset)
	    	vAllCriterias = null;
    	return getCriterias();
    }
    
    public List<Criteria> getCriterias() {
    	checkSubLoaded();
    	if ((vAllCriterias == null) || hasChanged)
    		vAllCriterias = getAllCriterias();
    	return vAllCriterias;
    }
    
    private List<Criteria> getAllCriterias() {
        debugCriterias();
    	return sort(getUnsortedCriterias());
    }
    
    /**
     * Get representation of order for quick sort of a given database object.
     */
    @Override
    public Object getOrder(Object object) throws EngineException	{
        if (object instanceof Criteria) {
        	List<Long> ordered = orderedCriterias.get(0);
        	long time = ((Criteria)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted criterias for screenclass \""+ getName() +"\". Criteria \""+ ((Criteria)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        } else if (object instanceof ExtractionRule) {
        	List<Long> ordered = orderedExtractionRules.get(0);
        	long time = ((ExtractionRule)object).priority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted extraction rules for screenclass \""+ getName() +"\". Extraction rule \""+ ((ExtractionRule)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        } else return super.getOrder(object);
    }
    
    public void removeCriteria(Criteria criteria) {
    	checkSubLoaded();
        vCriterias.remove(criteria);
        long value = criteria.priority;
        removeOrderedCriteria(value);
    }

    public void removeOrderedCriteria(Long value) {
        Collection<Long> ordered = orderedCriterias.get(0);
        ordered.remove(value);
        hasChanged = true;
        for (ScreenClass inheritedScreenClass : getInheritedScreenClasses())
        	inheritedScreenClass.removeOrderedCriteria(value);
    }
    
    /**
     * Adds a new extraction rule to the screen class definition.
     *
     * @param extractionRule the ExtractionRule to add.
     */
    public void addExtractionRule(ExtractionRule extractionRule, Long after) throws EngineException {
    	checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vExtractionRules, extractionRule.getName(), extractionRule.bNew);
		extractionRule.setName(newDatabaseObjectName);
        vExtractionRules.add(extractionRule);
        super.add(extractionRule);
        insertOrderedExtractionRule(extractionRule, after);
    }
    
    public void addExtractionRule(ExtractionRule extractionRule) throws EngineException {
    	addExtractionRule(extractionRule, null);
    }

    public void insertOrderedExtractionRule(ExtractionRule extractionrule, Long after) {
    	List<Long> ordered = orderedExtractionRules.get(0);
    	int size = ordered.size();
    	
    	long value = extractionrule.priority;
    	
    	if (ordered.contains(value))
    		return;
    	
    	if (after == null) {
    		after = 0L;
    		if (size > 0)
    			if (extractionrule.parent.equals(this))
    				after = ordered.get(ordered.size()-1);
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, value);
    	hasChanged = true;

        for (ScreenClass inheritedScreenClass : getInheritedScreenClasses())
        	inheritedScreenClass.insertOrderedExtractionRule(extractionrule,after);
    }
    
    public List<ExtractionRule> getLocalExtractionRules() {
    	checkSubLoaded();
        return sort(vExtractionRules);
    }
    
    public List<ExtractionRule> getUnsortedExtractionRules() {
    	checkSubLoaded();
    	List<ExtractionRule> v = GenericUtils.clone(vExtractionRules);
        if (parent instanceof ScreenClass) {
        	Collection<ExtractionRule> vInheritedExtractionRules = ((ScreenClass) parent).getUnsortedExtractionRules();
            v.addAll(vInheritedExtractionRules);
        }
        return v;
    }

    public List<ExtractionRule> getExtractionRules(boolean reset) {
    	if (reset)
    		vAllExtractionRules = null;
    	return getExtractionRules();
    }
    
    public List<ExtractionRule> getExtractionRules() {
    	checkSubLoaded();
    	if ((vAllExtractionRules == null) || hasChanged)
    		vAllExtractionRules = getAllExtractionRules();
    	return vAllExtractionRules;
    }
    
    private List<ExtractionRule> getAllExtractionRules() {
        debugExtractionRules();
    	return sort(getUnsortedExtractionRules());
    }
    
    public void removeExtractionRule(ExtractionRule extractionrule) {
    	checkSubLoaded();
        vExtractionRules.remove(extractionrule);
        long value = extractionrule.priority;
        removeOrderedExtractionRule(value);
    }
    
    public void removeOrderedExtractionRule(Long value) {
    	Collection<Long> ordered = orderedExtractionRules.get(0);
        ordered.remove(value);
        hasChanged = true;
        
        for (ScreenClass inheritedScreenClass : getInheritedScreenClasses())
        	inheritedScreenClass.removeOrderedExtractionRule(value);
    }

    /**
     * Adds a new XSL sheet to the screen class definition.
     *
     * @param sheet the XSL Sheet to add.
     */
    public void addSheet(Sheet sheet) throws EngineException {
        if (sheet == null)
            throw new IllegalArgumentException("The value of argument 'sheet' is invalid");
        checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vSheets, sheet.getName(), sheet.bNew);
		sheet.setName(newDatabaseObjectName);
        // Check for sheet with the same browser
        String requestedBrowser = sheet.getBrowser();
        for (Sheet sh : vSheets)
            if (sh.getBrowser().equals(requestedBrowser))
                throw new EngineException("Cannot add the sheet because a sheet is already defined for the browser \"" + requestedBrowser + "\" in the screen class \"" + getName() + "\".");
        vSheets.add(sheet);
        super.add(sheet);
    }
    
    public Sheet getSheet(String browser) {
    	checkSubLoaded();    	
        String sheetBrowser;
        for (Sheet sheet : vSheets) {
            sheetBrowser = sheet.getBrowser();
            if (sheetBrowser.equals(browser)) return sheet;
        }
        // If no sheet has been found, search for the universal sheet
        for (Sheet sheet : vSheets) {
            sheetBrowser = sheet.getBrowser();
            if (sheetBrowser.equals("*")) return sheet;
        }
        // If no sheet has been found, search in the parent screen class
        // for inherited sheet
        if (parent instanceof ScreenClass) {
            Sheet sheet = ((ScreenClass) parent).getSheet(browser);
            if (sheet != null) return sheet;
        }
        return null;
    }
    
    public Sheet getLocalSheet(String browser) {
    	checkSubLoaded();
        for (Sheet sheet : vSheets) {
           String  sheetBrowser = sheet.getBrowser();
            if (sheetBrowser.equals(browser)) return sheet;
        }
        return null;
    }
    
    public List<Sheet> getLocalSheets() {
    	checkSubLoaded();
    	return sort(vSheets);
    }
    
    public List<Sheet> getSheets() {
    	checkSubLoaded();
    	List<Sheet> v = GenericUtils.clone(vSheets);
        if (parent instanceof ScreenClass) {
            for (Sheet parentSheet  : ((ScreenClass) parent).getSheets()) {
                String parentSheetBrowser = parentSheet.getBrowser();
                if (getLocalSheet(parentSheetBrowser) == null)
                    v.add(parentSheet);
            }
        }
        return sort(v);
    }
    
    public void removeSheet(Sheet sheet) {
    	checkSubLoaded();
        vSheets.remove(sheet);
    }
    
    /**
     * Adds a new ScreenClass to the screen class definition.
     *
     * @param screenclass the ScreenClass to add.
     */
    public void addInheritedScreenClass(ScreenClass screenClass) throws EngineException {
    	checkSubLoaded();   	
		String newDatabaseObjectName = screenClass.getName();
		// cannot have 2 screenClass with the same name (#60)
		if(screenClass.bNew){
        	IScreenClassContainer<? extends ScreenClass> scc = GenericUtils.cast(getConnector());
    		if(scc.getScreenClassByName(newDatabaseObjectName)!=null){
    			int i=1;
    			for(i=1;scc.getScreenClassByName(newDatabaseObjectName+i)!=null;i++);
    			newDatabaseObjectName+=i;
    		}
        }
		getChildBeanName(vInheritedScreenClasses, newDatabaseObjectName, screenClass.bNew);
		screenClass.setName(newDatabaseObjectName);
        vInheritedScreenClasses.add(screenClass);
        super.add(screenClass);
    	Long  after = null;
    	for (Criteria criteria : getCriterias()) {
    		screenClass.insertOrderedCriteria(criteria,after);
    		after = criteria.priority;
    	}
    	after = null;
    	for (ExtractionRule extractionrule : getExtractionRules()) {
    		screenClass.insertOrderedExtractionRule(extractionrule,after);
    		after = extractionrule.priority;
    	}
    }
    
    public List<ScreenClass> getInheritedScreenClasses() {
    	checkSubLoaded();
    	return sort(vInheritedScreenClasses);
    }
    
    public void removeInheritedScreenClass(ScreenClass screenClass) {
    	checkSubLoaded();
        vInheritedScreenClasses.remove(screenClass);    	
    	for (Criteria criteria : getCriterias()) {
    		long value = criteria.priority;
    		screenClass.removeOrderedCriteria(value);
    	}
    	for (ExtractionRule extractionrule : getExtractionRules()) {
    		long value = extractionrule.priority;
    		screenClass.removeOrderedExtractionRule(value);
    	}
    }

    @Override
    public ScreenClass clone() throws CloneNotSupportedException {
        ScreenClass clonedObject = (ScreenClass) super.clone();
        clonedObject.vCriterias = new LinkedList<Criteria>();
        clonedObject.vAllCriterias = null;
        clonedObject.vExtractionRules = new LinkedList<ExtractionRule>();
        clonedObject.vAllExtractionRules = null;
        clonedObject.vInheritedScreenClasses = new LinkedList<ScreenClass>();
        clonedObject.vSheets = new LinkedList<Sheet>();
        return clonedObject;
    }
    
	public int getNumberOfLocalCriterias() {
		checkSubLoaded();
		
		return vCriterias.size();
	}

	/**
	 * @return the orderedCriterias
	 */
	public XMLVector<XMLVector<Long>> getOrderedCriterias() {
		return orderedCriterias;
	}

	/**
	 * @param orderedCriterias the orderedCriterias to set
	 */
	public void setOrderedCriterias(XMLVector<XMLVector<Long>> orderedCriterias) {
		this.orderedCriterias = orderedCriterias;
	}

	private void debugCriterias() {
		String criterias = "";
		if (orderedCriterias.size() > 0) {
			Collection<Long> ordered = orderedCriterias.get(0);
			criterias = ordered.toString();
		}
		Engine.logBeans.trace("["+ getName() +"] Ordered Criterias ["+ criterias + "]");
	}
	
	/**
	 * @return the orderedExtractionRules
	 */
	public XMLVector<XMLVector<Long>> getOrderedExtractionRules() {
		return orderedExtractionRules;
	}

	/**
	 * @param orderedExtractionRules the orderedExtractionRules to set
	 */
	public void setOrderedExtractionRules(XMLVector<XMLVector<Long>> orderedExtractionRules) {
		this.orderedExtractionRules = orderedExtractionRules;
	}
	
	private void debugExtractionRules() {
		String extractionrules = "";
		if (orderedExtractionRules.size() > 0) {
			Collection<Long> ordered = orderedExtractionRules.get(0);
			extractionrules = ordered.toString();
		}
		Engine.logBeans.trace("["+ getName() +"] Ordered ExtractionRules ["+ extractionrules + "]");
	}
	
	public void increasePriority(DatabaseObject databaseObject) throws EngineException {
		if ((databaseObject instanceof Criteria) || (databaseObject instanceof ExtractionRule))
			increaseOrder(databaseObject,null);
	}

	public void decreasePriority(DatabaseObject databaseObject) throws EngineException {
		if ((databaseObject instanceof Criteria) || (databaseObject instanceof ExtractionRule))
			decreaseOrder(databaseObject,null);
	}
	
    private void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	List<Long> ordered = null;
    	long value = databaseObject.priority;
    	
    	if (databaseObject instanceof Criteria)
    		ordered = orderedCriterias.get(0);
    	if (databaseObject instanceof ExtractionRule)
    		ordered = orderedExtractionRules.get(0);
    	
    	if (!ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos == 0)
    		return;
    	
    	if (before == null)
    		before = ordered.get(pos-1);
    	int pos1 = ordered.indexOf(before);
    	
    	ordered.add(pos1, value);
    	ordered.remove(pos+1);
    	hasChanged = true;
    	
    	for (ScreenClass screenClass : getInheritedScreenClasses())
    		screenClass.increaseOrder(databaseObject, before);
    }
    
    private void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
    	List<Long> ordered = null;
    	Long value = databaseObject.priority;
    	
    	if (databaseObject instanceof Criteria)
    		ordered = orderedCriterias.get(0);
    	if (databaseObject instanceof ExtractionRule)
    		ordered = orderedExtractionRules.get(0);
    	
    	if (!ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos+1 == ordered.size())
    		return;
    	
    	if (after == null)
    		after = ordered.get(pos+1);
    	int pos1 = ordered.indexOf(after);
    	
    	ordered.add(pos1+1, value);
    	ordered.remove(pos);
    	hasChanged = true;
    	
    	List<ScreenClass> vScreenClasses = getInheritedScreenClasses();
    	for (ScreenClass screenClass : vScreenClasses)
    		screenClass.decreaseOrder(databaseObject, after);
    }

	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#write(java.lang.String)
	 */
    @Override
	public void write(String databaseObjectQName) throws EngineException {		
		if (hasChanged && !isImporting) {
			getCriterias();
			getExtractionRules();
		}
		
		super.write(databaseObjectQName);
	}
    
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep=super.getAllChildren();
		List<Criteria> criterias=getLocalCriterias();		
		for(Criteria criteria:criterias){
			rep.add(criteria);
		}
		List<ExtractionRule> extractionRules=getLocalExtractionRules();	
		for(ExtractionRule extractionRule:extractionRules){
			rep.add(extractionRule);
		}		
		List<Sheet> sheets=getLocalSheets();	
		for(Sheet sheet:sheets){
			rep.add(sheet);
		}	
		List<ScreenClass> screenClasses=getInheritedScreenClasses();	
		for(ScreenClass screenClass:screenClasses){
			rep.add(screenClass);
		}	
		
		return rep;
	}

	@Override
	public boolean testAttribute(String name, String value) {
		if (name.equals("isDefault")) {
			ScreenClass dsc = ((IScreenClassContainer<?>) getConnector()).getDefaultScreenClass();
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(this.equals(dsc)));
		}
		return super.testAttribute(name, value);
	}

	@Override
	public FolderType getFolderType() {
		return (parent != null && parent instanceof ScreenClass) ? FolderType.INHERITED_SCREEN_CLASS : FolderType.SCREEN_CLASS;
	}
}
