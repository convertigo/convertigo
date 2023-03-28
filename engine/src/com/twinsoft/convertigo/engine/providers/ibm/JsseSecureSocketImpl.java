/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine.providers.ibm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Arrays;
import java.util.Hashtable;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;

import com.eicon.iConnect.acl.ACL;
import com.eicon.iConnect.acl.net.SecureSocket;
import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.MySSLSocketFactory;

public class JsseSecureSocketImpl extends SecureSocket implements HandshakeCompletedListener, ACL {

	public JsseSecureSocketImpl() {
		
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void finalize() throws Throwable {
		if (secureSocket != null) {
			((SSLSocket)secureSocket).removeHandshakeCompletedListener(this);
		}
		super.finalize();
	}


	@SuppressWarnings("rawtypes")
	@Override
	public void connect(InetAddress adr, int port, short[] suites,
			byte[][] certs, Hashtable clientCertProperties) throws IOException {
		Engine.logEmulators.debug("[JsseSecureSocketImpl] Connecting...");
    	Engine.logEmulators.trace("jdk.certpath.disabledAlgorithms="+java.security.Security.getProperty( "jdk.certpath.disabledAlgorithms" ));
    	Engine.logEmulators.trace("jdk.tls.disabledAlgorithms="+java.security.Security.getProperty( "jdk.tls.disabledAlgorithms" ));

    	Boolean bTrustAllServerCertificates = Boolean.TRUE;
    	String contextID = null;
    	if( clientCertProperties != null ) {
    		if( clientCertProperties.get( ACL.CERT_PROPERTY_NAME_FORMAT ).equals( "c8o" ) ) {
    			Hashtable customProps = (Hashtable) clientCertProperties.get( ACL.CERT_PROPERTY_NAME_CONTENT );
    			contextID = (String) customProps.get("ContextID");
    			bTrustAllServerCertificates = (Boolean) customProps.get("TrustAllServerCertificates");
    		}
    	}
    	if (contextID == null) {
    		throw new IOException("Invalid c8o ContextID");
    	}
    	
    	try {
        	CertificateManager cm = new CertificateManager();
			cm.collectStoreInformation(Engine.theApp.contextManager.get(contextID));
			
			SSLSocket socket = (SSLSocket) MySSLSocketFactory.getSSLSocketFactory(cm.keyStore, cm.keyStorePassword, 
    			cm.trustStore, cm.trustStorePassword, bTrustAllServerCertificates).
    				createSocket(adr.getHostAddress(), Integer.valueOf(port));
    	
			if (socket != null) {
		        String[] supportedProtocols = socket.getSupportedProtocols();
		        Engine.logEmulators.trace("[JsseSesureSocketImpl] Supported protocols:"+Arrays.asList(supportedProtocols));
		        String[] enabledProtocols = socket.getEnabledProtocols();
		        Engine.logEmulators.trace("[JsseSesureSocketImpl] Enabled protocols:"+Arrays.asList(enabledProtocols));
		        //Engine.logEmulators.trace("[JsseSesureSocketImpl] Enabling all supported protocols...");
		        //socket.setEnabledProtocols(supportedProtocols);
		        
		        String[] supportedSuites = socket.getSupportedCipherSuites();
		        Engine.logEmulators.trace("[JsseSesureSocketImpl] Supported cipher suites:"+Arrays.asList(supportedSuites));
		        String[] enabledSuites = socket.getEnabledCipherSuites();
		        Engine.logEmulators.trace("[JsseSesureSocketImpl] Enabled cipher suites:"+Arrays.asList(enabledSuites));
		        //Engine.logEmulators.trace("[JsseSesureSocketImpl] Enabling all supported cipher suites...");
		        //socket.setEnabledCipherSuites(supportedSuites);
		        
		        socket.addHandshakeCompletedListener(this);
		        
		        Engine.logEmulators.trace("[JsseSesureSocketImpl] Start handshake...");
		        socket.startHandshake();
			    
		    	isTrusted = true;
		    	isExpired = false;
			    secureSocket = socket;
			    Engine.logEmulators.debug("[JsseSesureSocketImpl] Socket retrieved !");
			}
			else
				throw new IOException("Invalid socket (null)");
			
		}
    	catch (Exception e) {
    		Engine.logEmulators.error(e);
			if (e instanceof IOException) {
				if (e instanceof SSLPeerUnverifiedException ) {
		    		isTrusted = false;
		    		throw new IOException("Remote server is not verified", e);
				}
				if (e instanceof CertificateExpiredException ) {
		    		isExpired = true;
		    		throw new IOException("Certificate has expired", e);
				}
				if (e instanceof CertificateNotYetValidException ) {
		    		isExpired = true;
		    		throw new IOException("Certificate has expired", e);
				}
				else {
					throw ((IOException)e);
				}
			}
			else {
				throw new IOException("Unable to connect through JSSE", e);
			}
		}
	}

	@SuppressWarnings("unused")
	private void sendPobiRequest(Socket socket) throws IOException {
		Engine.logEmulators.trace("[JsseSesureSocketImpl] Retrieving Pobi Fiben Home page...");
	    PrintWriter out = new PrintWriter(
				  new BufferedWriter(
				  new OutputStreamWriter(
     				  socket.getOutputStream())));
	    out.println("GET " + "/fiben/home.aspx" + " HTTP/1.0");
	    out.println();
	    out.flush();

	    if (out.checkError())
	    	Engine.logEmulators.trace("[JsseSesureSocketImpl] java.io.PrintWriter error");

	    BufferedReader in = new BufferedReader(
				    new InputStreamReader(
				    socket.getInputStream()));

	    String inputLine;
	    while ((inputLine = in.readLine()) != null)
	    	Engine.logEmulators.trace("[JsseSesureSocketImpl] "+inputLine);

	    in.close();
	    out.close();
	    socket.close();
	    
	    throw new IOException("Socket has been closed");
	}
	
	@Override
	public Socket getSecureSocket() {
		return secureSocket;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Hashtable getCertProps() {
		// TODO
		return null;
	}

	@Override
	public boolean isTrusted() {
		return isTrusted;
	}

	@Override
	public boolean isExpired() {
		return isExpired;
	}

	@Override
	public void handshakeCompleted(HandshakeCompletedEvent event) {
		Engine.logEmulators.trace("[JsseSesureSocketImpl] Handshake completed !");
		
		javax.net.ssl.SSLSocket socket = event.getSocket();
		Engine.logEmulators.trace("[JsseSesureSocketImpl] Connected to " + socket.getRemoteSocketAddress());
		
        javax.net.ssl.SSLSession session = socket.getSession();
        Engine.logEmulators.trace("[JsseSesureSocketImpl] Protocol is " + session.getProtocol() 
	    										+ ", Cipher is " + session.getCipherSuite());
	    // Server certificates
        try {
			checkServerCertificates(socket);
		} catch (Exception e) {
			Engine.logEmulators.error("[JsseSesureSocketImpl] Invalid server certificate", e);
		}
        
	    // Client certificates
        try {
			checkClientCertificates(socket);
		} catch (Exception e) {
			Engine.logEmulators.error("[JsseSesureSocketImpl] Invalid client certificate" + e);
		}
	}
	
	private void checkServerCertificates(javax.net.ssl.SSLSocket socket) throws SSLPeerUnverifiedException, 
				CertificateExpiredException, CertificateNotYetValidException {
		
		// Retrieve socket's session
        javax.net.ssl.SSLSession session = socket.getSession();
        
	    // Server certificates
	    java.security.cert.Certificate[] cpeers = session.getPeerCertificates();
	    if (cpeers != null) {
	    	Engine.logEmulators.trace("[JsseSesureSocketImpl] Server certificates :");
		    for (int i = 0; i < cpeers.length; i++) {
		    	java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) cpeers[i];
		    	javax.security.auth.x500.X500Principal subject = cert.getSubjectX500Principal();
		    	Engine.logEmulators.trace(" - "+subject);
		    	
		    	cert.checkValidity();
		    }
	    }
	    else {
	    	Engine.logEmulators.trace("[JsseSesureSocketImpl] Server does not use any certificate.");
	    }
	}
	
	private void checkClientCertificates(javax.net.ssl.SSLSocket socket) throws CertificateExpiredException, 
				CertificateNotYetValidException {
		// Retrieve socket's session
		javax.net.ssl.SSLSession session = socket.getSession();
		
		// Client certificates
	    java.security.cert.Certificate[] clocal = session.getLocalCertificates();
	    if (clocal != null) {
	    	Engine.logEmulators.trace("[JsseSesureSocketImpl] Client certificates :");
		    for (int i = 0; i < clocal.length; i++) {
		    	java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) clocal[i];
		    	javax.security.auth.x500.X500Principal subject = cert.getSubjectX500Principal();
		    	Engine.logEmulators.trace(" - "+subject);
		    	
				cert.checkValidity();
		    }
	    }
	    else {
	    	Engine.logEmulators.trace("[JsseSesureSocketImpl] Client does not use any certificate.");
	    }
	}
}
