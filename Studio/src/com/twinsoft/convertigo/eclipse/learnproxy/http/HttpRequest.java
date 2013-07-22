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

package com.twinsoft.convertigo.eclipse.learnproxy.http;

/**
 * Encapsulates HTTP request data.
 * 
 */
public class HttpRequest {

   private byte[] request;
   private String method;
   private String path;
   private String host;
   private int port = 80;
   private String query;
   private String version;
   

   public String getHost() {
      return host;
   }
   
   public void setHost(String host) {
      this.host = host;
   }

   public String getMethod() {
      return method;
   }

   public void setMethod(String method) {
      this.method = method;
   }

   public String getQuery() {
      return query;
   }
   
   public void setQuery(String query) {
      this.query = query;
   }

   public String getPath() {
      return path;
   }
   
   public void setPath(String path) {
      this.path = path;
   }
   public int getPort() {
      return port;
   }

   public void setPort(int port) {
      this.port = port;
   }

   public byte[] getRequest() {
      return (byte[])request.clone();
   }

   public void setRequest(byte[] request) {
      this.request = (byte[])request.clone();
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }
}
