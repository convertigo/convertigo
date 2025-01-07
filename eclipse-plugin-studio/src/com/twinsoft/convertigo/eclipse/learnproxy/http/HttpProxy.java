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

package com.twinsoft.convertigo.eclipse.learnproxy.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.learnproxy.http.gui.HttpProxyEvent;
import com.twinsoft.convertigo.eclipse.learnproxy.http.gui.HttpProxyEventListener;

/**
 * The actual proxy server listening for incoming
 * requests. Any request will be passed to tightly 
 * coupled HttpProxyWorker objects.
 * 
 * <br><br><i>HTTPS-tunneling is not yet supported. For a 
 * specification of this see: 
 * http://www.ietf.org/rfc/rfc2817.txt</i>
 * 
 */
public class HttpProxy implements Runnable {

   //private static final Log logger = LogFactory.getLog(HttpProxy.class);

   private int proxyPort = 8800;
   private int timeout = 0;
   private boolean isStopped = false;
   private List<HttpProxyEventListener> listeners = Collections.synchronizedList(new ArrayList<HttpProxyEventListener>(2));
   
   public HttpProxy() {
   }
   
   @Override
   public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(proxyPort);
			serverSocket.setSoTimeout(timeout);
			// logger.info("Proxy started at " + proxyPort);
			while (!isStopped) {
				try {
					Socket sock = serverSocket.accept();
					HttpProxyWorker worker = new HttpProxyWorker(this, sock);
					Thread t = new Thread(worker);
					t.start();
				} catch (IOException e) {
					ConvertigoPlugin.logException(e, "Unexpected exception");
				}
			}
		} catch (IOException e) {
			ConvertigoPlugin.logException(e, "Unexpected exception");
		}
	}
   
   /**
    * Sets an individual request/response pair created by 
    * its worker. This method is purposefully package private 
    * since it should be used only in conjunction with its worker.
    */
   synchronized void setWorkerResult(String method, String responseCode, String path, long starttime, byte[] request, byte[] response, long duration) {      
      // now notifiy gui...
      if (listeners != null && listeners.size() > 0) {
         // generate event...
         HttpProxyEvent event = new HttpProxyEvent(
               new String(request), 
               new String(response), 
               path,
               responseCode, duration, starttime, method, response.length);
         for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).modelChanged(event);
         }
      }
   }
   
   /**
    * Adds ans HttpProxyEventListener to this HttpProxy.
    */
   public void addProxyEventListener(HttpProxyEventListener listener) {
      if (listener != null && listeners != null) {
         this.listeners.add(listener);
      }
   }
  
   /**
    * Removes ans HttpProxyEventListener to this HttpProxy.
    */
   public void removeProxyEventListener(HttpProxyEventListener listener) {
      if (listener != null && listeners != null) {
         this.listeners.remove(listener);
      }
   }
}
