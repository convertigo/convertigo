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

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public abstract class StatementWithExpressions extends Statement implements IContainerOrdered {
	private static final long serialVersionUID = -1354987626710764475L;

	/**
     * The vector of ordered statement objects which can be applied on the StatementWithExpressions.
     */
    public XMLVector<XMLVector<Long>> orderedStatements = null;
	
    transient private List<Statement> vStatements = new Vector<Statement>();
    
    transient private List<Statement> vAllStatements = null;
    
	transient protected int currentChildStatement;

    transient public boolean bContinue = true;
    
    transient public boolean handlePriorities = true;
    
	public StatementWithExpressions() {
		super();
		
		orderedStatements = new XMLVector<XMLVector<Long>>();
		orderedStatements.addElement(new XMLVector<Long>());
	}

	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof Statement) {
        	addStatement((Statement) databaseObject);
        }
        else {
            throw new EngineException("You cannot add to a statement a database object of type " + databaseObject.getClass().getName());
        }
    }
	
	@Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof Statement) {
        	removeStatement((Statement) databaseObject);
        }
        else {
            throw new EngineException("You cannot remove from a statement class a database object of type " + databaseObject.getClass().getName());
        }
		super.remove(databaseObject);
	}

	public void addStatement(Statement statement) throws EngineException {
		checkSubLoaded();
		
		String newDatabaseObjectName = getChildBeanName(vStatements, statement.getName(), statement.bNew);
		statement.setName(newDatabaseObjectName);
        
        vStatements.add(statement);
        
        super.add(statement);
        
        if (!statement.bNew && !handlePriorities)
        	initializeOrderedStatements();
        else
        	insertOrderedStatement(statement,null);
    }

    public void insertOrderedStatement(Statement statement, Long after) {
    	XMLVector<Long> ordered = orderedStatements.elementAt(0);
    	int size = ordered.size();
    	
    	Long value = new Long(handlePriorities ? statement.priority : statement.newPriority);
    	
    	if (ordered.contains(value))
    		return;
    	
    	if (after == null) {
    		after = new Long(0);
    		if (size>0)
    			after = (Long)ordered.lastElement();
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.insertElementAt(value, order+1);
    	hasChanged = true;
    }
	
    public void removeStatement(Statement statement) {
    	checkSubLoaded();
    	
    	vStatements.remove(statement);
    	
    	Long value = new Long(handlePriorities ? statement.priority : statement.newPriority);
        removeOrderedStatement(value);
    }
    
    public void removeOrderedStatement(Long value) {
    	XMLVector<Long> ordered = orderedStatements.elementAt(0);
        ordered.removeElement(value);
        hasChanged = true;
    }
    
    public void init() {
    	vAllStatements = null;
    }
    
    public List<Statement> getStatements(boolean reset) {
    	if (reset)
    		vAllStatements = null;
    	return getStatements();
    }
    
    public List<Statement> getStatements() {
    	checkSubLoaded();
    	
    	if ((vAllStatements == null) || hasChanged)
    		vAllStatements = getAllStatements();
    	return vAllStatements;
    }
    
    private List<Statement> getAllStatements() {
    	checkSubLoaded();

        debugStatements();
    	return sort(vStatements);
    }
    
    private void initializeOrderedStatements() {
    	checkSubLoaded();
    	
    	XMLVector<XMLVector<Long>> statements = new XMLVector<XMLVector<Long>>();
    	XMLVector<Long> ordered = new XMLVector<Long>();
    	
    	List<Statement> v = new Vector<Statement>(vStatements);
    	v = GenericUtils.cast(sort((Vector<?>) v, false));
    	String s = "Sorted Statements [";
		for (int i=0;i<v.size();i++) {
			Statement statement = (Statement)v.get(i);
			if (statement.parent.equals(this)) statement.hasChanged = true;
    		s += "("+statement.getName()+":"+statement.priority+" -> "+statement.newPriority+")";
			ordered.addElement(new Long(statement.newPriority));
		}
    	s += "]";
    	Engine.logBeans.debug("["+ name +"] " + s);
    	
    	statements.addElement(ordered);
		setOrderedStatements(statements);
		debugStatements();
		hasChanged = true;
    }
    
    /**
     * Get representation of order for quick sort of a given database object.
     */
    @Override
    public Object getOrder(Object object) throws EngineException	{
        if (object instanceof Statement) {
        	List<Long> ordered = orderedStatements.get(0);
        	long time = handlePriorities ? ((Statement)object).priority : ((Statement)object).newPriority;
        	if (ordered.contains(time))
        		return (long)ordered.indexOf(time);
        	else throw new EngineException("Corrupted statement for StatementWithExpressions \""+ getName() +"\". Statement \""+ ((Statement)object).getName() +"\" with priority \""+ time +"\" isn't referenced anymore.");
        } else return super.getOrder(object);
    }
    
    public boolean hasStatements()
    {
    	checkSubLoaded();
    	
    	return (vStatements.size()>0) ? true: false;
    }
    
    public int numberOfStatements()
    {
    	checkSubLoaded();
    	
    	return vStatements.size();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
    	StatementWithExpressions clonedObject = (StatementWithExpressions) super.clone();
        clonedObject.vStatements = new Vector<Statement>();
        clonedObject.vAllStatements = null;
        clonedObject.bContinue = true;
        clonedObject.handlePriorities = handlePriorities;
        return clonedObject;
    }
    
    /**
	 * @return the orderedStatements
	 */
	public XMLVector<XMLVector<Long>> getOrderedStatements() {
		return orderedStatements;
	}

	/**
	 * @param orderedStatements the orderedStatements to set
	 */
	public void setOrderedStatements(XMLVector<XMLVector<Long>> orderedStatements) {
		this.orderedStatements = orderedStatements;
	}

	public void increasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Statement)
			increaseOrder(databaseObject,null);
	}
	
	public void decreasePriority(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Statement)
			decreaseOrder(databaseObject,null);
	}
	
    private void increaseOrder(DatabaseObject databaseObject, Long before) throws EngineException {
    	XMLVector<Long> ordered = orderedStatements.elementAt(0);
    	Long value = new Long(handlePriorities ? databaseObject.priority : databaseObject.newPriority);
    	
    	if (!ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos == 0)
    		return;
    	
    	if (before == null)
    		before = (Long)ordered.elementAt(pos-1);
    	int pos1 = ordered.indexOf(before);
    	
    	ordered.insertElementAt(value, pos1);
    	ordered.remove(pos+1);
    	hasChanged = true;
    }
	
    private void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
    	XMLVector<Long> ordered = orderedStatements.elementAt(0);
    	Long value = new Long(handlePriorities ? databaseObject.priority : databaseObject.newPriority);
    	
    	if (!ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos+1 == ordered.size())
    		return;
    	
    	if (after == null)
    		after = (Long)ordered.elementAt(pos+1);
    	int pos1 = ordered.indexOf(after);
    	
    	ordered.insertElementAt(value, pos1+1);
    	ordered.remove(pos);
    	hasChanged = true;
    }
    
	public void debugStatements() {
		String statements = "";
		if (orderedStatements.size() > 0) {
			XMLVector<Long> ordered = orderedStatements.elementAt(0);
			statements = Arrays.asList(ordered.toArray()).toString();
		}
		Engine.logBeans.trace("["+ name +"] Ordered Statements ["+ statements + "]");
	}
	
	public String toJsString() {
		List<Statement> v = getStatements();
    	String code = "";
    	if (hasStatements()) {
    		for (int i=0; i<v.size(); i++) {
    			Statement statement = (Statement)v.get(i);
    			code += statement.toJsString() + "\n";
    		}
    	}
    	return code;
    }
    
	@Override
	protected void reset() {
		bContinue = true;
	}

	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		currentChildStatement = 0;
		return super.execute(javascriptContext, scope);
	}

	public abstract boolean executeNextStatement(Context javascriptContext, Scriptable scope) throws EngineException;
	
	protected void executeNextStatement(Statement st, Context javascriptContext, Scriptable scope) throws EngineException {
		if (st.isEnable())
			st.execute(javascriptContext, scope);
		currentChildStatement++;
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#configure(org.w3c.dom.Element)
	 */
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
		try {
			String attribute = element.getAttribute("handlePriorities");
			if (attribute.equals("")) throw new Exception("Missing \"handlePriorities\" attribute.");
			handlePriorities = new Boolean(attribute).booleanValue();
			if (!handlePriorities)
				hasChanged = true;
        }
        catch(Exception e) {
        	handlePriorities = false;
        	hasChanged = true;
        }
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#write(java.lang.String)
	 */
	@Override
	public void write(String databaseObjectQName) throws EngineException {
		boolean b = handlePriorities;
		
		if (hasChanged && !isImporting) {
			handlePriorities = true;
			getStatements();
		}
		try {
			super.write(databaseObjectQName);
		}
		catch (EngineException e) {
			handlePriorities = b;
			throw e;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#toXml(org.w3c.dom.Document)
	 */
	@Override
	public Element toXml(Document document) throws EngineException {
		Element element =  super.toXml(document);
		
        // Storing the transaction "handlePriorities" flag
        element.setAttribute("handlePriorities", new Boolean(handlePriorities).toString());
		
		return element;
	}
	
}
