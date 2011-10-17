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
 * Encapsulates HTTP response data.
 * 
 */
public class HttpResponse {
   
   private byte[] response;
   private int statusCode;
   private String httpVersion;

   /**
    * @return Returns the response.
    */
   public byte[] getResponse() {
      return (byte[])response.clone();
   }
   /**
    * @param response The response to set.
    */
   public void setResponse(byte[] response) {
      this.response = (byte[])response.clone();
   }
   /**
    * @return Returns the statusCode.
    */
   public int getStatusCode() {
      return statusCode;
   }
   /**
    * @param statusCode The statusCode to set.
    */
   public void setStatusCode(int statusCode) {
      this.statusCode = statusCode;
   }
   /**
    * @return Returns the httpVersion.
    */
   public String getHttpVersion() {
      return httpVersion;
   }
   /**
    * @param httpVersion The httpVersion to set.
    */
   public void setHttpVersion(String httpVersion) {
      this.httpVersion = httpVersion;
   }
}
