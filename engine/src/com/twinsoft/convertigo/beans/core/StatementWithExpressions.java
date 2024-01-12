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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public abstract class StatementWithExpressions extends Statement implements IContainerOrdered {
	private static final long serialVersionUID = -1354987626710764475L;

	/**
     * The vector of ordered statement objects which can be applied on the StatementWithExpressions.
     */
    transient private XMLVector<XMLVector<Long>> orderedStatements = null;
	
    transient private List<Statement> vStatements = new ArrayList<Statement>();
    
    transient private List<Statement> vAllStatements = null;
    
	transient protected int currentChildStatement;

    transient public boolean bContinue = true;
    
	public StatementWithExpressions() {
		super();
		
		orderedStatements = new XMLVector<XMLVector<Long>>();
		orderedStatements.add(new XMLVector<Long>());
	}

	@Override
	public void add(DatabaseObject databaseObject, Long after) throws EngineException {
        if (databaseObject instanceof Statement) {
        	addStatement((Statement) databaseObject);
        }
        else {
            throw new EngineException("You cannot add to a statement a database object of type " + databaseObject.getClass().getName());
        }
	}
	
	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
		add(databaseObject, null);
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

	public void addStatementAfter(Statement statement, Statement afterStatement) throws EngineException {
		checkSubLoaded();
		
		String newDatabaseObjectName = getChildBeanName(vStatements, statement.getName(), statement.bNew);
		statement.setName(newDatabaseObjectName);
        
        vStatements.add(statement);
        
        super.add(statement);
        
        insertOrderedStatement(statement, afterStatement != null ? afterStatement.priority : null);
    }
	
	public void addStatement(Statement statement) throws EngineException {
		this.addStatementAfter(statement, null);
    }

    public void insertOrderedStatement(Statement statement, Long after) {
    	XMLVector<Long> ordered = orderedStatements.get(0);
    	int size = ordered.size();
    	
    	Long value = statement.priority;
    	
    	if (ordered.contains(value))
    		return;
    	
    	if (after == null) {
    		after = 0L;
    		if (size > 0)
    			after = (Long)ordered.lastElement();
    	}
    	
   		int order = ordered.indexOf(after);
    	ordered.add(order+1, value);
    	hasChanged = !isImporting;
    }
	
    public void removeStatement(Statement statement) {
    	checkSubLoaded();
    	
    	vStatements.remove(statement);
    	
    	Long value = statement.priority;
        removeOrderedStatement(value);
    }
    
    public void removeOrderedStatement(Long value) {
    	XMLVector<Long> ordered = orderedStatements.get(0);
        ordered.remove(value);
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
    
    /**
     * Get representation of order for quick sort of a given database object.
     */
    @Override
    public Object getOrder(Object object) throws EngineException	{
        if (object instanceof Statement) {
        	List<Long> ordered = orderedStatements.get(0);
        	long time = ((Statement)object).priority;
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
    public StatementWithExpressions clone() throws CloneNotSupportedException {
    	StatementWithExpressions clonedObject = (StatementWithExpressions) super.clone();
        clonedObject.vStatements = new ArrayList<Statement>();
        clonedObject.vAllStatements = null;
        clonedObject.bContinue = true;
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
    	XMLVector<Long> ordered = orderedStatements.get(0);
    	Long value = databaseObject.priority;
    	
    	if (!ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos == 0)
    		return;
    	
    	if (before == null)
    		before = (Long)ordered.get(pos-1);
    	int pos1 = ordered.indexOf(before);
    	
    	ordered.add(pos1, value);
    	ordered.remove(pos+1);
    	hasChanged = true;
    }
	
    private void decreaseOrder(DatabaseObject databaseObject, Long after) throws EngineException {
    	XMLVector<Long> ordered = orderedStatements.get(0);
    	Long value = databaseObject.priority;
    	
    	if (!ordered.contains(value))
    		return;
    	int pos = ordered.indexOf(value);
    	if (pos+1 == ordered.size())
    		return;
    	
    	if (after == null)
    		after = (Long)ordered.get(pos+1);
    	int pos1 = ordered.indexOf(after);
    	
    	ordered.add(pos1+1, value);
    	ordered.remove(pos);
    	hasChanged = true;
    }
    
	public void debugStatements() {
		String statements = "";
		if (orderedStatements.size() > 0) {
			XMLVector<Long> ordered = orderedStatements.get(0);
			statements = Arrays.asList(ordered.toArray()).toString();
		}
		Engine.logBeans.trace("["+ getName() +"] Ordered Statements ["+ statements + "]");
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
		if (st.isEnabled()) {
			st.checkSymbols();
			st.execute(javascriptContext, scope);
		}
		currentChildStatement++;
	}

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#write(java.lang.String)
	 */
	@Override
	public void write(String databaseObjectQName) throws EngineException {
		if (hasChanged && !isImporting) {
			getStatements();
		}
		super.write(databaseObjectQName);
	}	
}
