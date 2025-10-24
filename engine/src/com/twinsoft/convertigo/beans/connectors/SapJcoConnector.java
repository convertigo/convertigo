/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.beans.connectors;

import java.io.File;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.SapJcoTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.providers.sapjco.SapJCoProvider;


public class SapJcoConnector extends Connector {

	private static final long serialVersionUID = -9176104169720510775L;
	
	private String asHost = "";
	private String systemNumber = "00";
	private String client = "000";
	private String user = "SAP*";
	private String password = "";
	private String language = "en";
	
	private String systemId = "";
	private String msService = "";
	private String msHost = "";
	private String group = "";
			
	private transient SapJcoProviderImpl provider = null;
	
    public class SapJcoProviderImpl extends SapJCoProvider {
    	
		@Override
		protected String getDestinationName() {
			return getQName();
		}
    	
		@Override
		protected String getDestinationHost() {
			return getAsHost();
		}

		@Override
		protected String getDestinationSystemNumber() {
			return getSystemNumber();
		}

		@Override
		protected String getDestinationClient() {
			return getClient();
		}

		@Override
		protected String getDestinationUser() {
			return getUser();
		}

		@Override
		protected String getDestinationPassword() {
			return getPassword();
		}

		@Override
		protected String getDestinationLanguage() {
			return getLanguage();
		}

		@Override
		protected String getJcoFunctionFilePath(String functionName) {
			return getJcoFunctionDirPath() +"/"+functionName+".ser";
		}

		@Override
		protected Context getContext() {
			return context;
		}

		@Override
		protected String getDestinationSystemId() {
			return getSystemId();
		}

		@Override
		protected String getDestinationMsHost() {
			return getMsHost();
		}

		@Override
		protected String getDestinationMsService() {
			return getMsService();
		}

		@Override
		protected String getDestinationGroup() {
			return getGroup();
		}

    }	
	
	@Override
	public void release() {
		super.release();

		try {
			if (provider != null) {
				provider.release();
				Engine.logBeans.debug("(SapConnector) Provider released");
			}
        } catch (Exception ee) {
			Engine.logBeans.error("(SapConnector) An error occured while releasing provider", ee);
		}
		finally {
			provider = null;
		}
		
	}

	@Override
	protected void addTransaction(Transaction transaction) throws EngineException {
		if (!(transaction instanceof SapJcoTransaction))
			throw new EngineException("You cannot add to a SAP connector a database object of type "
					+ transaction.getClass().getName());
		super.addTransaction(transaction);
	}


	@Override
	public boolean isMaskedProperty(Visibility target, String propertyName) {
		if ("password".equals(propertyName)) {
			return true;
		}
		return super.isMaskedProperty(target, propertyName);
	}

	@Override
	public boolean isCipheredProperty(String propertyName) {
		if ("password".equals(propertyName)) {
			return true;
		}
		return super.isCipheredProperty(propertyName);
	}

	@Override
	public Transaction newTransaction() {
		return new SapJcoTransaction();
	}
	
	@Override
	public void prepareForTransaction(Context context) throws EngineException {
		getSapJCoProvider().prepareCustomDestination((SapJcoTransaction) context.requestedObject);
	}

	public static Document executeJCoSearch(SapJcoConnector connector, String pattern) throws EngineException {
		SapJcoTransaction transaction = (SapJcoTransaction)connector.newTransaction();
		transaction.setBapiName("RFC_FUNCTION_SEARCH");
		transaction.context = connector.context;
		transaction.setParent(connector);
		RequestableVariable variable = new RequestableVariable();
		variable.setName("FUNCNAME");
		variable.setValueOrNull(pattern);
		transaction.addVariable(variable);
		return connector.getSapJCoProvider().executeJCoFunction(transaction, true);
	}
	
	public static SapJcoTransaction createSapJcoTransaction(SapJcoConnector sapConnector, String bapiName) throws EngineException {
		return sapConnector.createTransaction(bapiName);
	}
	
	public Document executeTransaction(SapJcoTransaction transaction) throws EngineException {
		return getSapJCoProvider().executeJCoFunction(transaction);
	}
	
	protected SapJcoTransaction createTransaction(String bapiName) throws EngineException {
		return getSapJCoProvider().createSapJcoTransaction(bapiName);
	}
	
	public void removeSerializedData(String bapiName) {
		try {
			String filePath = getSapJCoProvider().getJcoFunctionFilePath(bapiName);
			File file = new File(filePath);
			if (file.exists()) {
				if (!file.delete()) {
					throw new EngineException("Unable to delete file \""+filePath+"\"");
				}
			}
		} catch (Exception e) {
			Engine.logEngine.warn("Unable to remove serialized data for bapi named \""+bapiName+"\"", e);
		}
		
	}
	
	protected String getJcoFunctionDirPath() {
		String dirPath = Engine.projectDir(getProject().getName()) + "/_private/bapis/" + getName();
		File dir = new File(dirPath);
		if (!dir.exists())
			dir.mkdirs();
		return dirPath;
	}
	
	public String getAsHost() {
		return asHost;
	}

	public void setAsHost(String asHost) {
		this.asHost = asHost;
	}

	public String getSystemNumber() {
		return systemNumber;
	}

	public void setSystemNumber(String systemNumber) {
		this.systemNumber = systemNumber;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
	
	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	public String getMsService() {
		return msService;
	}

	public void setMsService(String msService) {
		this.msService = msService;
	}

	public String getMsHost() {
		return msHost;
	}

	public void setMsHost(String msHost) {
		this.msHost = msHost;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}
	
	public SapJcoProviderImpl getSapJCoProvider() throws EngineException {
		if (provider == null) {
	        try {
	    		provider = new SapJcoProviderImpl();
	    		provider.init();
	    		Engine.logBeans.debug("(SapConnector) Provider created");
	        } catch (Exception e) {
	        	provider = null;
				Engine.logBeans.error("(SapConnector) An error occured while creating provider", e);
				throw new EngineException("(SapConnector) An error occured while creating provider", e);
	        }
		}
		return provider;
	}
	
	public void setData(Object data) {
		fireDataChanged(new ConnectorEvent(this, data));
	}

}
