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

import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.net.ssl.X509KeyManager;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class MyX509KeyManager implements X509KeyManager {

	private X509KeyManager impl;
	private String alias;
	private KeyStore keyStore;
	private KeyStore trustStore;

	public MyX509KeyManager(X509KeyManager impl, KeyStore keyStore, KeyStore trustStore, String alias) {
		this.impl = impl;
		this.keyStore = keyStore;
		this.trustStore = trustStore;
		this.alias = alias;
	}
	
	public String[] getClientAliases(String keyType, Principal[] issuers) {
		return impl.getClientAliases(keyType, issuers);
	}

	public String[] getServerAliases(String keyType, Principal[] issuers) {
		return impl.getServerAliases(keyType, issuers);
	}

	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
		Engine.logCertificateManager.trace("MyX509KeyManager.chooseClientAlias(): alias=" + alias);
		if (alias == null) {
			String[] issuersToAdd = EnginePropertiesManager.getPropertyAsStringArray(PropertyName.SSL_ISSUERS);
			Engine.logCertificateManager.debug("MyX509KeyManager.chooseClientAlias(): issuersToAdd=" + issuersToAdd.toString());

			String alias;
			int nbIssuersToAdd = issuersToAdd.length;
			
			if (nbIssuersToAdd == 0) {
				alias = impl.chooseClientAlias(keyType, issuers, socket);
			}
			else {
				int len = issuers.length;
				Principal[] issuers2 = new Principal[len + nbIssuersToAdd];
				for (int i = 0 ; i < len ; i++) {
					issuers2[i] = issuers[i];
				}
				
				Engine.logCertificateManager.debug("MyX509KeyManager.chooseClientAlias(): Analizing issuers...");
				for (int i = 0 ; i < nbIssuersToAdd ; i++) {
					String issuer = issuersToAdd[i];
					Engine.logCertificateManager.debug("MyX509KeyManager.chooseClientAlias(): issuer added: " + issuer);
					issuers2[len + i] = new javax.security.auth.x500.X500Principal(issuer);
				}

				alias = impl.chooseClientAlias(keyType, issuers2, socket);
			}
			
			Engine.logCertificateManager.info("Chosen client alias: " + alias);
			return alias;
		}
		return alias;
	}

	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
		return impl.chooseServerAlias(keyType, issuers, socket);
	}

	public X509Certificate[] getCertificateChain(String alias) {
		try {
			Engine.logCertificateManager.trace("MyX509KeyManager.getCertificateChain(): alias=" + alias);
			
			X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
			Engine.logCertificateManager.trace("MyX509KeyManager.getCertificateChain(): first certificate: " + certificate.toString());

			Principal issuer = null, previousIssuer;
			ArrayList<X509Certificate> chainList = new ArrayList<X509Certificate>();
			Enumeration<String> enumAliases;

			Engine.logCertificateManager.trace("MyX509KeyManager.getCertificateChain(): searching for certificate chain...");			
			do {
				chainList.add(certificate);
				Engine.logCertificateManager.trace("MyX509KeyManager.getCertificateChain(): added certificate: " + certificate.toString());
				
				previousIssuer = issuer;
				issuer = (Principal) certificate.getIssuerDN();
				Engine.logCertificateManager.trace("MyX509KeyManager.getCertificateChain(): issuer=" + issuer.toString());				
				
				if (issuer.equals(previousIssuer)) break;

				enumAliases = trustStore.aliases();
				certificate = null;
				while (enumAliases.hasMoreElements()) {
					alias = (String) enumAliases.nextElement();
					certificate = (X509Certificate) trustStore.getCertificate(alias);
					Engine.logCertificateManager.trace("MyX509KeyManager.getCertificateChain(): certificate=" + certificate.toString());
					if (certificate.getSubjectDN().equals(issuer)) {
						Engine.logCertificateManager.trace("MyX509KeyManager.getCertificateChain(): issuer found; break");
						break;
					}
				}
			} while (certificate != null);
			
			X509Certificate[] chain = (X509Certificate[]) chainList.toArray(new X509Certificate[] {});
			
			Engine.logCertificateManager.info("Certificate chain: " + chain);
			return chain;
		}
		catch (Exception e) {
			Engine.logCertificateManager.error("MyX509KeyManager.getCertificateChain(): Error while updating certificate chain; returning implementor certificate chain", e);
			return impl.getCertificateChain(alias);
		}
	}

	public java.security.PrivateKey getPrivateKey(String alias) {
		return impl.getPrivateKey(alias);
	}
}    
