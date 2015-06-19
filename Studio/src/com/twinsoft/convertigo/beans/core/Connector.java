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

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.event.EventListenerList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLRectangle;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

/**
 * The Connector class is the base class for all connectors.
 */
public abstract class Connector extends DatabaseObject implements ITagsProperty{
	private static final long serialVersionUID = 5573688971345318823L;
    
    /**
     * The context associated to the XML producer. The XML producer is
     * responsible for updating relevantly this context.
     */
    transient public Context context = null;

    public Connector() {
        super();
		databaseType = "Connector";
	}
	
    private transient EventListenerList connectorListeners = new EventListenerList();
    
    public void addConnectorListener(ConnectorListener connectorListener) {
    	connectorListeners.add(ConnectorListener.class, connectorListener);
    }
    
    public void removeConnectorListener(ConnectorListener connectorListener) {
    	connectorListeners.remove(ConnectorListener.class, connectorListener);
    }
    
    public void fireDataChanged(ConnectorEvent connectorEvent) {
        // Guaranteed to return a non-null array
        Object[] listeners = connectorListeners.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2 ; i >= 0 ; i-=2)
        	if (listeners[i] == ConnectorListener.class)
                ((ConnectorListener) listeners[i+1]).dataChanged(connectorEvent);
    }
    
	/**
	 * Prepares the connector to execute a transaction.
	 */
	public abstract void prepareForTransaction(Context context) throws EngineException;

	/**
	 * The vector of available transactions for this project.
	 */
	transient private List<Transaction> vTransactions = new LinkedList<Transaction>();
	
	/** Holds value of property billingClassName. */
	private String billingClassName = "";
    
	public String getBillingClassName() {
		return billingClassName;
	}
    
	public void setBillingClassName(String billingClassName) {
		this.billingClassName = billingClassName;
	}
    
	/** Holds value of property endTransactionName. */
	private String endTransactionName = "";
    
	public String getEndTransactionName() {
		return endTransactionName;
	}
    
	public void setEndTransactionName(String endTransactionName) {
		this.endTransactionName = endTransactionName;
	}
    
	/**
	 * Defines if we must be authenticated by the TAS before trying to
	 * connect the connector.
	 */
	private boolean isTasAuthenticationRequired = false;

	public boolean isTasAuthenticationRequired() {
		return isTasAuthenticationRequired;
	}

	public void setTasAuthenticationRequired(boolean isTasAuthenticationRequired) {
		this.isTasAuthenticationRequired = isTasAuthenticationRequired;
	}

	/**
	 * The boolean which specify if Transaction is the default one
	 * for the project to which it belongs.
	 */
	transient public boolean isDefault = false;
    
	/**
	 * Sets the transaction to be the default one.
	 */
	public final void setByDefault() throws EngineException {
		try {
			((Project) parent).setDefaultConnector(this);
		} catch(NullPointerException e) {
			throw new EngineException("You should first add this connector to a project in order to be able to set it by default.");
		}
	}

	/**
	 * The default transaction for this project.
	 */
	transient private Transaction defaultTransaction = null;
    
	/**
	 * Retrieves the default transaction.
	 */
	public Transaction getDefaultTransaction() throws EngineException {
		if (defaultTransaction == null) {
			checkSubLoaded();
			for (Transaction transaction : vTransactions)
				if (transaction.isDefault) {
					defaultTransaction = transaction;
					break;
				}
		}
        
		// Report from 4.5: fix #401
		if (defaultTransaction == null) {
			// Fire exception in Engine mode only!
			if (Engine.isEngineMode())
				throw new EngineException("There is no default transaction defined for connector \"" + getName() + "\" in project \""+ getProject().getName()+"\".");
			else {
				//In Studio mode we must be able to set a default transaction!
			}
		}
		
		return defaultTransaction;
	}

	/**
	 * Sets the default transaction.
	 */
	public synchronized void setDefaultTransaction(Transaction transaction) throws EngineException {
		if (transaction == null)
			throw new IllegalArgumentException("The value of argument 'transaction' is null");
		checkSubLoaded();
		if (vTransactions.contains(transaction)) {
			if (defaultTransaction == null) getDefaultTransaction();
			if (defaultTransaction != null) defaultTransaction.isDefault = false;
			transaction.isDefault = true;
			defaultTransaction = transaction;
		} else throw new IllegalArgumentException("The value of argument 'transaction' is invalid: the transaction does not belong to the project");
	}
	
	/**
	 * The current learning transaction if connector is learning.
	 */
	transient private Transaction learningTransaction = null;
	
	public Transaction getLearningTransaction() {
		return learningTransaction;
	}

	public synchronized void setLearningTransaction(Transaction transaction) {
		if (transaction == null) {
			if (getLearningTransaction() != null) learningTransaction.isLearning = false;
			learningTransaction = null;
		} else {
			checkSubLoaded();
			if (vTransactions.contains(transaction)) {
				if (getLearningTransaction() != null) learningTransaction.isLearning = false;
				transaction.isLearning = true;
				learningTransaction = transaction;
			} else throw new IllegalArgumentException("The value of argument 'transaction' is invalid: the transaction does not belong to the project");
		}
	}

	/**
	 * Holds the learning state of connector.
	 */
	transient private boolean learning = false;
	
	public boolean isLearning() {
		return learning;
	}
	
	public void markAsLearning(boolean learning) {
		this.learning = learning;
	}	
	
	/**
	 * Holds the learning mode of connector.
	 */
	transient private boolean accumulate = false;
	
	public boolean isAccumulating() {
		return accumulate;
	}
	
	public void setAccumulate(boolean accumulate) {
		this.accumulate = accumulate;
	}
	
	/**
	 * Holds the debugging state of connector.
	 */
	transient private boolean debugging = false;
	
	public boolean isDebugging() {
		return debugging;
	}
	
	public void markAsDebugging(boolean debugging) {
		this.debugging = debugging;
	}	
	
	/**
	 * Adds an object to the connector.
	 */
	@Override
	public void add(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Transaction)
			addTransaction((Transaction) databaseObject);
		else if (databaseObject instanceof Pool)
			addPool((Pool) databaseObject);
		else if (databaseObject instanceof com.twinsoft.convertigo.beans.core.Document)
			addDocument((com.twinsoft.convertigo.beans.core.Document) databaseObject);
		else if (databaseObject instanceof com.twinsoft.convertigo.beans.core.Listener)
			addListener((com.twinsoft.convertigo.beans.core.Listener) databaseObject);
		else throw new EngineException("You cannot add to a connector a database object of type " + databaseObject.getClass().getName());
	}

	/**
	 * Removes an object from the connector.
	 */
	@Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
		if (databaseObject instanceof Pool)
			removePool((Pool) databaseObject);
		else if (databaseObject instanceof Transaction)
			removeTransaction((Transaction) databaseObject);
		else if (databaseObject instanceof com.twinsoft.convertigo.beans.core.Document)
			removeDocument((com.twinsoft.convertigo.beans.core.Document)databaseObject);
		else if (databaseObject instanceof com.twinsoft.convertigo.beans.core.Listener)
			removeListener((com.twinsoft.convertigo.beans.core.Listener)databaseObject);
		else throw new EngineException("You cannot remove from a connector a database object of type " + databaseObject.getClass().getName());
		super.remove(databaseObject);
	}

	/**
	 * Adds a transaction.
	 */
	protected void addTransaction(Transaction transaction) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vTransactions, transaction.getName(), transaction.bNew);
		transaction.setName(newDatabaseObjectName);
		vTransactions.add(transaction);
		if (transaction.isDefault)
			setDefaultTransaction(transaction);
		super.add(transaction);
	}

	public void removeTransaction(Transaction transaction) throws EngineException {
		checkSubLoaded();
		vTransactions.remove(transaction);
	}

	public Transaction getTransactionByName(String transactionName) {
		checkSubLoaded();
		for (Transaction transaction : vTransactions) {
			if (transaction.getName().equalsIgnoreCase(transactionName)) return transaction;
		}
		return null;
	}
    
	@Deprecated
	public Vector<Transaction> getTransactions() {
		return new Vector<Transaction>(getTransactionsList());
	}
	
	public List<Transaction> getTransactionsList() {
		checkSubLoaded();
		return sort(vTransactions);
	}

	/**
	 * The vector of pools for this project.
	 */
	transient private List<Pool> vPools = new Vector<Pool>();
	
	public Pool getPoolByName(String poolName) {
		checkSubLoaded();
		for (Pool pool : vPools)
			if (pool.getName().equalsIgnoreCase(poolName)) return pool;
		return null;
	}

	@Deprecated
	public Vector<Pool> getPools() {
		return new Vector<Pool>(getPoolsList());
	}
	
	public List<Pool> getPoolsList() {
		checkSubLoaded();
		return sort(vPools);
	}
	
	public void removePool(Pool pool) throws EngineException {
		checkSubLoaded();
		vPools.remove(pool);
	}

	/**
	 * Adds a pool.
	 */
	protected void addPool(Pool pool) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vPools, pool.getName(), pool.bNew);
		pool.setName(newDatabaseObjectName);
		vPools.add(pool);
		super.add(pool);
	}
    
	/**
	 * The vector of documents for this connector.
	 */
	transient private List<com.twinsoft.convertigo.beans.core.Document> vDocuments = new Vector<com.twinsoft.convertigo.beans.core.Document>();
	
	public com.twinsoft.convertigo.beans.core.Document getDocumentByName(String documentName) {
		checkSubLoaded();
		
		for (com.twinsoft.convertigo.beans.core.Document document : vDocuments) {
			if (document.getName().equalsIgnoreCase(documentName)) {
				return document;
			}
		}
		
		return null;
	}

	public List<com.twinsoft.convertigo.beans.core.Document> getDocumentsList() {
		checkSubLoaded();
		return sort(vDocuments);
	}
	
	public void removeDocument(com.twinsoft.convertigo.beans.core.Document document) throws EngineException {
		checkSubLoaded();
		vDocuments.remove(document);
	}
	
	/**
	 * Adds a document.
	 */
	protected void addDocument(com.twinsoft.convertigo.beans.core.Document document) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vDocuments, document.getName(), document.bNew);
		document.setName(newDatabaseObjectName);
		vDocuments.add(document);
		super.add(document);
	}
	
	/**
	 * The list of listeners for this connector.
	 */
	transient private List<com.twinsoft.convertigo.beans.core.Listener> vListeners = new Vector<com.twinsoft.convertigo.beans.core.Listener>();
	
	public com.twinsoft.convertigo.beans.core.Listener getListenerByName(String listenerName) {
		checkSubLoaded();
		
		for (com.twinsoft.convertigo.beans.core.Listener listener : vListeners) {
			if (listener.getName().equalsIgnoreCase(listenerName)) {
				return listener;
			}
		}
		
		return null;
	}

	public List<com.twinsoft.convertigo.beans.core.Listener> getListenersList() {
		checkSubLoaded();
		return sort(vListeners);
	}
	
	public void removeListener(com.twinsoft.convertigo.beans.core.Listener listener) throws EngineException {
		checkSubLoaded();
		vListeners.remove(listener);
	}
	
	/**
	 * Adds a Listener.
	 */
	protected void addListener(com.twinsoft.convertigo.beans.core.Listener listener) throws EngineException {
		checkSubLoaded();
		String newDatabaseObjectName = getChildBeanName(vListeners, listener.getName(), listener.bNew);
		listener.setName(newDatabaseObjectName);
		vListeners.add(listener);
		super.add(listener);
	}
	
	@Override
	public Connector clone() throws CloneNotSupportedException {
		Connector clonedObject = (Connector) super.clone();
		clonedObject.defaultTransaction = null;
		clonedObject.connectorListeners = new EventListenerList();
		clonedObject.vTransactions = new Vector<Transaction>();
		clonedObject.vPools = new Vector<Pool>();
		clonedObject.vDocuments = new Vector<com.twinsoft.convertigo.beans.core.Document>();
		clonedObject.vListeners = new Vector<com.twinsoft.convertigo.beans.core.Listener>();
		clonedObject.debugging = false;
		return clonedObject;
	}

	@Override
	public Element toXml(Document document) throws EngineException {
		Element element = super.toXml(document);
        
		// Storing the transaction "default" flag
		element.setAttribute("default", new Boolean(isDefault).toString());
        
		return element;
	}
	
	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		try {
			isDefault = new Boolean(element.getAttribute("default")).booleanValue();
		}catch(Exception e) {
			throw new EngineException("Unable to configure the property 'By default' of the transaction \"" + getName() + "\".", e);
		}
	}
    
	/**
	 * Returns the selected zone from the connector
	 * @return XMLRectangle
	 */
	public XMLRectangle getSelectionZone() {
		return null;
	}
	
	public void release(){
	}
	
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("endTransactionName")) {
			return getNamesWithFirstEmptyItem(sort(vTransactions));
		}
		return new String[0];
	}
	
	@Override
	public List<DatabaseObject> getAllChildren() {	
		List<DatabaseObject> rep = super.getAllChildren();
		
		for (Transaction tran : getTransactionsList()){
			rep.add(tran);
		}
		
		for (Pool pool : getPoolsList()) {
			rep.add(pool);
		}
		
		for (com.twinsoft.convertigo.beans.core.Document document : getDocumentsList()) {
			rep.add(document);
		}
		
		for (Listener listener : getListenersList()) {
			rep.add(listener);
		}
		
		return rep;
	}
	
	public abstract Transaction newTransaction();
}
