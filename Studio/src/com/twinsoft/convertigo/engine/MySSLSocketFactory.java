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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import com.twinsoft.convertigo.engine.util.Crypto2;

public class MySSLSocketFactory implements SecureProtocolSocketFactory {
	static Map<String, MySSLSocketFactory> cache = new HashMap<String, MySSLSocketFactory>();
	static long checkExpires = System.currentTimeMillis() + 300000;

	protected String keyStore;
	protected String trustStore;
	protected String keyStorePassword;
	protected String trustStorePassword;
	private boolean trustAllServerCertificates;
	long expire;
	
	private SSLContext sslcontext = null;
	
	static public ProtocolSocketFactory getSSLSocketFactory(String keyStore, String keyStorePassword, String trustStore, String trustStorePassword, boolean trustAllServerCertificates) {
		String key = "" + keyStore + "|" + keyStorePassword + "|" + trustStore + "|" + trustStorePassword + "|" + trustAllServerCertificates;
		
		synchronized (cache) {
			MySSLSocketFactory mySSLSocketFactory = cache.get(key);
			if (mySSLSocketFactory == null) {
				Engine.logCertificateManager.debug("(MySSLSocketFactory) Create new SSLSocketFactory (" + key + ")");
				mySSLSocketFactory = new MySSLSocketFactory(keyStore, keyStorePassword, trustStore, trustStorePassword, trustAllServerCertificates);
				cache.put(key, mySSLSocketFactory);
			} else {
				Engine.logCertificateManager.debug("(MySSLSocketFactory) Retrieve SSLSocketFactory from cache (" + key + ")");
			}
			
			long now = System.currentTimeMillis();
			mySSLSocketFactory.expire = now + 3600000;
			
			if (now >= checkExpires) {
				int removed = 0;
				
				for (Iterator<MySSLSocketFactory> i = cache.values().iterator(); i.hasNext();) {
					MySSLSocketFactory cachedSSLSocketFactory = i.next();
					if (now >= cachedSSLSocketFactory.expire) {
						removed++;
						i.remove();
					}
				}
				Engine.logCertificateManager.info("(MySSLSocketFactory) Clear " + removed + " cache entries, remains " + cache.size() + " entries");
				checkExpires += 300000;
			}
			
			return mySSLSocketFactory;
		}
	}
	
	private MySSLSocketFactory(String keyStore, String keyStorePassword, String trustStore, String trustStorePassword, boolean trustAllServerCertificates) {
		this.keyStore = keyStore == null ? "" : keyStore;
		this.trustStore = trustStore == null ? "" : trustStore;
		this.keyStorePassword = keyStorePassword == null ? "" : keyStorePassword;
		this.trustStorePassword = trustStorePassword == null ? "" : trustStorePassword;
		this.trustAllServerCertificates = trustAllServerCertificates;
	}
	
	private SSLContext createEasySSLContext() throws NoSuchProviderException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException, CertificateException, IOException {
		Engine.logCertificateManager.debug("(MySSLSocketFactory) Creating SSL context");
		
		String algorithm = KeyManagerFactory.getDefaultAlgorithm();
		Engine.logCertificateManager.debug("(MySSLSocketFactory) Using KeyManager algorithm " + algorithm);
		
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
		
		String keyStoreType = keyStore.endsWith(".pkcs11") ? "pkcs11" : "pkcs12";
		Engine.logCertificateManager.debug("(MySSLSocketFactory) Key store type: " + keyStoreType);
		
		String alias = null;
		KeyStore ks, ts;
		char[] passPhrase;
		
		if (keyStore.equals("") || (keyStore.endsWith(".udv"))) {
			ks = KeyStore.getInstance(keyStoreType);
			ks.load(null, keyStorePassword.toCharArray());
			kmf.init(ks, null);
		}
		else {
			File file = new File(keyStore);

	
			Properties properties = new Properties();
			properties.load(new FileInputStream(Engine.CERTIFICATES_PATH + CertificateManager.STORES_PROPERTIES_FILE_NAME));
			String p = properties.getProperty(file.getName(), "");
			int i = p.indexOf('/');
			if (i != -1) {
				alias = p.substring(i + 1);
			}
			
			if (keyStoreType.equals("pkcs11")) {
				String providerName = file.getName();
				providerName = "SunPKCS11-" + providerName.substring(0, providerName.lastIndexOf('.'));
				Engine.logCertificateManager.debug("(MySSLSocketFactory) Provider name: '" + providerName + "'");
				
				String pinCode;
				if (i == -1) {
					pinCode = Crypto2.decodeFromHexString(p);
				}
				else {
					pinCode = Crypto2.decodeFromHexString(p.substring(0, i));
				}
	
				Engine.logCertificateManager.debug("(MySSLSocketFactory) PIN code: " + pinCode);
	
				ks = KeyStore.getInstance("pkcs11", providerName);
				ks.load((InputStream) null, pinCode.toCharArray());
				kmf.init(ks, null);
			}
			else {
				ks = KeyStore.getInstance(keyStoreType);
				passPhrase = keyStorePassword.toCharArray();
				ks.load(new FileInputStream(keyStore), passPhrase);
				kmf.init(ks, passPhrase);
			}
		}
		Engine.logCertificateManager.debug("(MySSLSocketFactory) Client alias: " + (alias == null ? "<to be chosen by the security implementor>" : alias));

		ts = KeyStore.getInstance("jks");
		passPhrase = trustStorePassword.toCharArray();
		if (trustStore.equals(""))
			ts.load(null, passPhrase);
		else
			ts.load(new FileInputStream(trustStore), passPhrase);
		
		algorithm = TrustManagerFactory.getDefaultAlgorithm();
		Engine.logCertificateManager.debug("(MySSLSocketFactory) Using TrustManager algorithm " + algorithm);
		
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
		tmf.init(ts);
	
		TrustManager[] tm = { TRUST_MANAGER };
		
		MyX509KeyManager xkm = new MyX509KeyManager((X509KeyManager) kmf.getKeyManagers()[0], ks, ts, alias);
		
		Engine.logCertificateManager.debug("(MySSLSocketFactory) trusting all certificates : " + trustAllServerCertificates);
		
		//SSLContext context = SSLContext.getInstance("SSLv3");
		SSLContext context = SSLContext.getInstance("TLS");
		if (trustAllServerCertificates)
			context.init(new KeyManager[] { xkm }, tm, null);
		else
			context.init(new KeyManager[] { xkm }, tmf.getTrustManagers(), null);

		Engine.logCertificateManager.debug("(MySSLSocketFactory) SSL context created: " + context.getProtocol());
		return context;
    }

    private SSLContext getSSLContext() throws NoSuchProviderException, NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException, CertificateException, IOException {
        if (sslcontext == null) {
            sslcontext = createEasySSLContext();
        }
        return sslcontext;
    }

	public synchronized Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
		try {
			return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
		}
		catch(Exception e) {
			Engine.logCertificateManager.error("Error while trying to create the SSL socket", e);
			throw new IOException("Unable to create the SSL socket: [" + e.getClass().getName() + "] " + e.getMessage());
		}
	}
	
	public synchronized Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		try {
			return getSSLContext().getSocketFactory().createSocket(host, port);
		}
		catch(Exception e) {
			Engine.logCertificateManager.error("Error while trying to create the SSL socket", e);
			throw new IOException("Unable to create the SSL socket: [" + e.getClass().getName() + "] " + e.getMessage());
		}
	}

	public synchronized Socket createSocket(String host, int port, InetAddress inetAddress, int clientPort) throws IOException, UnknownHostException {
		try {
			return getSSLContext().getSocketFactory().createSocket(host, port, inetAddress, clientPort);
		}
		catch(Exception e) {
			Engine.logCertificateManager.error("Error while trying to create the SSL socket", e);
			throw new IOException("Unable to create the SSL socket: [" + e.getClass().getName() + "] " + e.getMessage());
		}
	}

	
	public synchronized Socket createSocket(String host, int port, InetAddress inetAddress,	int clientPort, HttpConnectionParams httpParams) throws IOException, UnknownHostException, ConnectTimeoutException {
		try {
			return getSSLContext().getSocketFactory().createSocket(host, port, inetAddress, clientPort);
		}
		catch(Exception e) {
			Engine.logCertificateManager.error("Error while trying to create the SSL socket", e);
			throw new IOException("Unable to create the SSL socket: [" + e.getClass().getName() + "] " + e.getMessage());
		}
	}
	
	
	/** This leads the application to trust ALL certificates */
	private static final TrustManager TRUST_MANAGER = new X509TrustManager() {
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[] {};
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			// Nothing to do
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			// Nothing to do
		}
	};
}    
