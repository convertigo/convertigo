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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

import com.twinsoft.convertigo.engine.util.Crypto2;

public class CertificateManager {
	
	private static final Collection<String> certificateExtension = Arrays.asList(".udv",".store",".cer",".pfx",".p12",".pkcs11"); 
	public static boolean isCertificateExtension(String extensionToTest){
		return certificateExtension.contains(extensionToTest);
	}
	
	public static final String STORES_PROPERTIES_FILE_NAME = "/stores.properties";

	public static boolean checkCertificatePassword(String keyStoreType, String keyStore, String keyStorePassword) {
		try {
			Engine.logCertificateManager.debug("Checking certificate password");
			Engine.logCertificateManager.trace("   keyStoreType: " + keyStoreType);
			Engine.logCertificateManager.trace("   keyStore: " + keyStore);
			Engine.logCertificateManager.trace("   keyStorePassword: " + keyStorePassword);

			KeyStore ks;
			File file = new File(keyStore);
			char[] passPhrase;

			if (keyStoreType.equals("client")) {
				if (keyStore.endsWith(".pkcs11")) {
					Engine.logCertificateManager.debug("Checking pkcs11 certificate");
					String providerName = file.getName();
					providerName = "SunPKCS11-" + providerName.substring(0, providerName.lastIndexOf('.'));
					ks = KeyStore.getInstance("pkcs11", providerName);
					passPhrase = keyStorePassword.toCharArray();
					ks.load((InputStream) null, passPhrase);
				}
				else if (keyStore.endsWith(".udv")) {
					Engine.logCertificateManager.debug("Checking udv certificate");
					return PseudoCertificate.checkCleCrypt(keyStore, keyStorePassword);
				}
				else {
					Engine.logCertificateManager.debug("Checking pkcs12 certificate");
					ks = KeyStore.getInstance("pkcs12");
					passPhrase = keyStorePassword.toCharArray();
					ks.load(new FileInputStream(keyStore), passPhrase);
				}
			}
			else {
				Engine.logCertificateManager.debug("Checking jks keystore");
				ks = KeyStore.getInstance("jks");
				passPhrase = keyStorePassword.toCharArray();
				ks.load(new FileInputStream(keyStore), passPhrase);
			}
			
			return true;
		}
		catch(Exception e) {
			Engine.logCertificateManager.error("Unexpected exception while checking certificate password", e);
			return false;
		}
	}
	
	public boolean hasChanged = true;
	public boolean storeInformationCollected = false;
	
	public String keyStore;
	public String keyStoreName;
	public String keyStoreGroup;
	public String trustStore;
	public String keyStorePassword;
	public String trustStorePassword;
	
	private Properties storesProperties;
	private Context context;
	
	private String previousTasVirtualServerName = null;
	private String previousTasUserGroup = null;
	private String previousTasUserName = null;
	private String previousProjectName = null;

	public synchronized void collectStoreInformation(Context context) throws EngineException {
		try {
			Engine.logCertificateManager.debug("Collecting store information for context " + context.contextID);
			this.context = context;
			
			storeInformationCollected = false;

			if (context.projectName.equals(previousProjectName) &&
					((context.tasVirtualServerName == null && previousTasVirtualServerName == null) ||
					(context.tasVirtualServerName != null && context.tasVirtualServerName.equals(previousTasVirtualServerName))) &&
					((context.tasUserGroup == null && previousTasUserGroup == null) ||
					(context.tasUserGroup != null && context.tasUserGroup.equals(previousTasUserGroup))) &&
					((context.tasUserName  == null && previousTasUserName == null) ||
					(context.tasUserName  != null && context.tasUserName .equals(previousTasUserName)))) {
				hasChanged = false;
				storeInformationCollected = true;
				Engine.logCertificateManager.debug("No changes");
				return;
			}

			Engine.logCertificateManager.debug("Changes detected!");
			Engine.logCertificateManager.debug("context.projectName=" + context.projectName);
			Engine.logCertificateManager.debug("previousProjectName=" + previousProjectName);
			Engine.logCertificateManager.debug("context.tasVirtualServerName=" + context.tasVirtualServerName);
			Engine.logCertificateManager.debug("previousTasVirtualServerName=" + previousTasVirtualServerName);
			Engine.logCertificateManager.debug("context.tasUserGroup=" + context.tasUserGroup);
			Engine.logCertificateManager.debug("previousTasUserGroup=" + previousTasUserGroup);
			Engine.logCertificateManager.debug("context.tasUserName=" + context.tasUserName);
			Engine.logCertificateManager.debug("previousTasUserName=" + previousTasUserName);

			hasChanged = true;

			previousTasVirtualServerName = context.tasVirtualServerName;
			previousTasUserGroup = context.tasUserGroup;
			previousTasUserName = context.tasUserName;
			previousProjectName = context.projectName;

			File file = new File(Engine.CERTIFICATES_PATH + CertificateManager.STORES_PROPERTIES_FILE_NAME);
			storesProperties = new Properties();
			storesProperties.load(new FileInputStream(file));
		
			keyStore = "";
			keyStoreName = "";
			keyStoreGroup = "";
			trustStore = "";
			keyStorePassword = "";
			trustStorePassword = "";

			findClientStore();
			findServerStore();
			
			storeInformationCollected = true;
		}
		catch(IOException e) {
			throw new EngineException("Unable to read the certificate stores properties file!", e);
		}
		finally {
			Engine.logCertificateManager.debug("Client store: " + keyStore);
			Engine.logCertificateManager.debug("Client store password: " + keyStorePassword);
			Engine.logCertificateManager.debug("Client group: " + keyStoreGroup);
			Engine.logCertificateManager.debug("Server store: " + trustStore);
			Engine.logCertificateManager.debug("Server store password: " + trustStorePassword);
		}
	}
	
	private void findClientStore() throws EngineException {
		Engine.logCertificateManager.debug("Finding client store");

		String skey, key;
		
		// TAS driven stores
		if (context.tasUserName != null) {
			skey = "tas." + context.tasVirtualServerName + "." + context.tasUserGroup + "." + context.tasUserName + ".";
			Engine.logCertificateManager.debug("Analyzing root key: " + skey);
			key = getExistingKey(skey + "projects." + context.projectName + "." + "client.store");
			if (key == null) key = getExistingKey(skey + "client.store");
			if (key != null) {
				setClientVariables(key);
				return;
			}

			skey = "tas." + context.tasVirtualServerName + "." + context.tasUserGroup + ".";
			Engine.logCertificateManager.debug("Analyzing root key: " + skey);
			key = getExistingKey(skey + "projects." + context.projectName + "." + "client.store");
			if (key == null) key = getExistingKey(skey + "client.store");
			if (key != null) {
				setClientVariables(key);
				return;
			}

			skey = "tas." + context.tasVirtualServerName + ".";
			Engine.logCertificateManager.debug("Analyzing root key: " + skey);
			key = getExistingKey(skey + "projects." + context.projectName + "." + "client.store");
			if (key == null) key = getExistingKey(skey + "client.store");
			if (key != null) {
				setClientVariables(key);
				return;
			}
		
			skey = "tas.";
			Engine.logCertificateManager.debug("Analyzing root key: " + skey);
			key = getExistingKey(skey + "projects." + context.projectName + "." + "client.store");
			if (key == null) key = getExistingKey(skey + "client.store");
			if (key != null) {
				setClientVariables(key);
				return;
			}
			
			Engine.logCertificateManager.debug("No root key found / TAS driven stores");
		}

		// Projects driven stores
		skey = "projects." + context.projectName + ".";
		Engine.logCertificateManager.debug("Analyzing root key: " + skey);
		key = getExistingKey(skey + "client.store");
		if (key != null) {
			setClientVariables(key);
			return;
		}

		skey = "projects.";
		Engine.logCertificateManager.debug("Analyzing root key: " + skey);
		key = getExistingKey(skey + "client.store");
		if (key != null) {
			setClientVariables(key);
			return;
		}

		Engine.logCertificateManager.debug("No root key found / projects driven stores");

		// No available client store found
		Engine.logCertificateManager.debug("No available client store found!");
	}

	private void setClientVariables(String key) {
		Engine.logCertificateManager.debug("Setting client variables for key " + key);
		
		String clientStore = storesProperties.getProperty(key);
		if (clientStore == null)
			return;
		
		clientStore = setClientUserDefinedVariables(clientStore);
		if (clientStore.equals(""))
			return;
		
		keyStore = Engine.CERTIFICATES_PATH + "/" + clientStore;
		
		String s = storesProperties.getProperty(clientStore);
		int i = s.indexOf('/');
		if (i != -1) s = s.substring(0, i);
		keyStorePassword = Crypto2.decodeFromHexString(s);
		keyStoreGroup = storesProperties.getProperty(clientStore + ".group");
		keyStoreName = clientStore;
		
	}
	
	private String setClientUserDefinedVariables(String clientStore) {
		String cStore = clientStore;
		if (clientStore.endsWith(".udv")) {
			Properties udvProperties;
			String key, value;
			try {
				cStore = "";
				
				// retrieve crypting key
				String s = storesProperties.getProperty(clientStore);
				int i = s.indexOf('/');
				if (i != -1) s = s.substring(0, i);
				String cryptKey = Crypto2.decodeFromHexString(s);
				
				byte[] buf = PseudoCertificate.decrypt(Engine.CERTIFICATES_PATH + "/" + clientStore, cryptKey);
				if (buf != null) {
					// load properties
					udvProperties = new Properties();
					udvProperties.load(new ByteArrayInputStream(buf));
					for (Enumeration<Object> e = udvProperties.keys(); e.hasMoreElements() ;) {
				         key = (String) e.nextElement();
				         value = udvProperties.getProperty(key);
				         if (key.equalsIgnoreCase("certificate")) {
				        	 Engine.logCertificateManager.debug("Overriding client keystore with '"+value+"'");
				        	 cStore = value;
				         }
				         else {
					         context.set(key, value);
					         Engine.logCertificateManager.debug("Adding user defined variable : "+key+"="+value);
				         }
				     }
				}
			} catch (FileNotFoundException e) {
				Engine.logCertificateManager.debug("Could not find file '"+ cStore +"'.");
			} catch (IOException e) {
				Engine.logCertificateManager.debug("Could not load file '"+ cStore +"'.");
			}
		}
		return cStore;
	}
	
	private void findServerStore() throws EngineException {
		Engine.logCertificateManager.debug("Finding server store");

		String skey, key;
		
		// TAS driven stores
		if (context.tasUserName != null) {
			skey = "tas." + context.tasVirtualServerName + "." + context.tasUserGroup + "." + context.tasUserName + ".";
			Engine.logCertificateManager.debug("Analyzing root key: " + skey);
			key = getExistingKey(skey + "projects." + context.projectName + "." + "server.store");
			if (key == null) key = getExistingKey(skey + "server.store");
			if (key != null) {
				setServerVariables(key);
				return;
			}

			skey = "tas." + context.tasVirtualServerName + "." + context.tasUserGroup + ".";
			Engine.logCertificateManager.debug("Analyzing root key: " + skey);
			key = getExistingKey(skey + "projects." + context.projectName + "." + "server.store");
			if (key == null) key = getExistingKey(skey + "server.store");
			if (key != null) {
				setServerVariables(key);
				return;
			}

			skey = "tas." + context.tasVirtualServerName + ".";
			Engine.logCertificateManager.debug("Analyzing root key: " + skey);
			key = getExistingKey(skey + "projects." + context.projectName + "." + "server.store");
			if (key == null) key = getExistingKey(skey + "server.store");
			if (key != null) {
				setServerVariables(key);
				return;
			}
		
			skey = "tas.";
			Engine.logCertificateManager.debug("Analyzing root key: " + skey);
			key = getExistingKey(skey + "projects." + context.projectName + "." + "server.store");
			if (key == null) key = getExistingKey(skey + "server.store");
			if (key != null) {
				setServerVariables(key);
				return;
			}
		}

		// Projects driven stores
		skey = "projects." + context.projectName + ".";
		Engine.logCertificateManager.debug("Analyzing root key: " + skey);
		key = getExistingKey(skey + "server.store");
		if (key != null) {
			setServerVariables(key);
			return;
		}

		skey = "projects.";
		Engine.logCertificateManager.debug("Analyzing root key: " + skey);
		key = getExistingKey(skey + "server.store");
		if (key != null) {
			setServerVariables(key);
			return;
		}

		// No available server store found
		Engine.logCertificateManager.debug("No available server store found!");
	}

	private void setServerVariables(String key) {
		Engine.logCertificateManager.debug("Setting server variables for key " + key);
		
		String serverStore = storesProperties.getProperty(key);
		if ((serverStore == null) || (serverStore.equals("")))
			return;
		trustStore = Engine.CERTIFICATES_PATH + "/" + serverStore;
		String s = storesProperties.getProperty(serverStore);
		int i = s.indexOf('/');
		if (i != -1) s = s.substring(0, i);
		trustStorePassword = Crypto2.decodeFromHexString(s);
	}
	
	private String getExistingKey(String key) {
		Enumeration<Object> keys = storesProperties.keys();
		
		String keyTmp;
		while (keys.hasMoreElements()) {
			keyTmp = (String) keys.nextElement();
			if (keyTmp.equalsIgnoreCase(key)) return keyTmp;
		}
		
		return null;
	}
}
