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

package com.twinsoft.convertigo.beans.connectors;

import java.io.IOException;

import com.ibm.ctg.client.ECIRequest;
import com.ibm.ctg.client.JavaGateway;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.ConnectorEvent;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.CicsTransaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Visibility;

/**
 * This is the CICS connector.
 */
public class CicsConnector extends Connector {

	private static final long serialVersionUID = -4124670352505790070L;


	public CicsConnector() {
		super();
	}
	
	/**
	 * Initialize the JavaGateway object to flow data to the Gateway
	 * @throws IOException
	 */
	private void initializeGateway() throws IOException {
		if (javaGatewayObject == null) {
			javaGatewayObject = new JavaGateway(mainframeName, port);
			Engine.logBeans.debug("(CicsConnector) The address of the Cics Transaction Gateway has been set to "+ mainframeName + ", port:" + port);
		}
		else {
			if (!javaGatewayObject.isOpen())
				javaGatewayObject.open();
		}
	}
	
	/**
	 * Close the JavaGateway object
	 * @throws IOException
	 */
	private void closeGateway() throws IOException {
		if (javaGatewayObject != null) {
			if (javaGatewayObject.isOpen())
				javaGatewayObject.close();
		}
	}
	
	public boolean existGateway() {
		return (javaGatewayObject != null) ? true:false;
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.Connector#addTransaction(com.twinsoft.convertigo.beans.core.Transaction)
	 */
	@Override
	protected void addTransaction(Transaction transaction) throws EngineException {
		if (!(transaction instanceof CicsTransaction))
			throw new EngineException("You cannot add to a CICS connector a database object of type " + transaction.getClass().getName());
		super.addTransaction(transaction);
	}

	/**
	 * 
	 */
	@Override
	protected void finalize() throws Throwable {
		closeGateway();
		javaGatewayObject = null;
		super.finalize();
	}

	/**
	 * Flows data contained in the ECIRequest object to the Gateway and
	 * determines whether it has been successful by checking the return code.
	 * @param requestObject the ECIRequest object.
	 * @return Error message if one, null otherwise.
	 * @throws IOException
	 */
	public String flowRequest(ECIRequest requestObject) throws IOException {
		String errmsg = "";

		// Initialize the JavaGateway if needed
		initializeGateway();
		
		// Retrieve return code
		int iRc = javaGatewayObject.flow(requestObject);

		setData(requestObject.Commarea);
	
		// Checks for gateway errors and returns false if there are no errors
		switch (requestObject.getCicsRc())
		{
			case ECIRequest.ECI_NO_ERROR:
				if (iRc == 0)
					return null;
				else
					errmsg += " Error from Gateway (" + requestObject.getRcString() + "). ";
				break;
	
			// Checks for security errors and returns true if validation has failed
			case ECIRequest.ECI_ERR_SECURITY_ERROR:
				errmsg += " A security error occured while running the transaction. ";
				return errmsg;
	
			// Checks for transaction abend errors where the user is authorised
			// to access the server but not run the program.
			case ECIRequest.ECI_ERR_TRANSACTION_ABEND:
				errmsg += " An abend occured while running the transaction. ";
				break;
			
			default:
				break;
		}
	  
		errmsg += " ECI returned: " + requestObject.getCicsRcString();
		errmsg += "; Abend code was: " + requestObject.Abend_Code;
		
		return errmsg;
	}
	
	public byte[] getData() throws Exception {
		throw new IllegalArgumentException("The getData() method is not allowed within the Cics connector!");
	}
	
	public void setData(byte[] data) {
		fireDataChanged(new ConnectorEvent(this, data));
	}
	
	@Override
	public void prepareForTransaction(Context context) throws EngineException {
		Engine.logBeans.debug("(CicsConnector) Preparing for transaction");
		Engine.logBeans.debug("(CicsConnector) Request on CICS Gateway: "+ mainframeName);
	}

	/** The JavaGateway object to flow data to the Gateway. */
	transient private JavaGateway javaGatewayObject;
	    
	/** Holds value of property mainframeName. */
	private String mainframeName = "localhost";

	/** Holds value of property server. */
	private String server = "";

	/** Holds value of property port. */
	private int port = 2006;
    
	/** Holds value of property userId. */
	private String userId = "";
    
	/** Holds value of property userPassword. */
	private String userPassword = "";
    
    
	/** Getter for property server.
	 * @return Value of property server.
	 */
	public String getMainframeName() {
		return this.mainframeName;
	}
    
	/** Setter for property server.
	 * @param server New value of property server.
	 */
	public void setMainframeName(String mainframeName) {
		mainframeName = ((!mainframeName.equals("")) ? mainframeName:"localhost");
		this.mainframeName = mainframeName;
	}
    
	/** Getter for property server.
	 * @return Value of property server.
	 */
	public String getServer() {
		return this.server;
	}
    
	/** Setter for property server.
	 * @param server New value of property server.
	 */
	public void setServer(String server) {
		this.server = server;
	}
    
	/** Getter for property port.
	 * @return Value of property port.
	 */
	public int getPort() {
		return this.port;
	}
    
	/** Setter for property port.
	 * @param port New value of property port.
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	/** Getter for property userId.
	 * @return Value of property userId.
	 */
	public String getUserId() {
		return this.userId;
	}
    
	/** Setter for property userId.
	 * @param transactionId New value of property userId.
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/** Getter for property userPassword.
	 * @return Value of property userPassword.
	 */
	public String getUserPassword() {
		return this.userPassword;
	}
    
	/** Setter for property userPassword.
	 * @param transactionId New value of property userPassword.
	 */
	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}
	
	@Override
	public CicsTransaction newTransaction() {
		return new CicsTransaction();
	}
	
	@Override
	public boolean isMaskedProperty(Visibility target, String propertyName) {
		if ("userPassword".equals(propertyName)) {
			return true;
		}
		return super.isMaskedProperty(target, propertyName);
	}

	@Override
	public boolean isCipheredProperty(String propertyName) {
		if ("userPassword".equals(propertyName)) {
			return true;
		}
		return super.isCipheredProperty(propertyName);
	}
}
