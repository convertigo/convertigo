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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

/**
 * The worker for HTTP request processing. Handles a
 * single request only. Pakcage private for usage within 
 * this package only. Outside of this package only the
 * client of this class (HttpProxy) should be visible.
 * 
 */
class HttpProxyWorker extends Socket implements Runnable {

   //private static final Log logger = LogFactory.getLog(HttpProxyWorker.class);

   /** Flag indicating whether andinterruption of processing has been requested. For future use only. */
   private boolean isInterrupted = false;
   
   /** The byte-array containing the request. */
   private HttpRequest request;

   /** The byte-array containing the response. */
   private HttpResponse response;
   
   /** The socket for the connectio to the proxy client. */
   private Socket proxySocket;
   
   /** The tightly coupled caller client of this worker. */
   private HttpProxy proxy;
  
   /**
    * Package private constructor. Should only be used
    * in conjunction with its specific proxy-server.
    */
   HttpProxyWorker(HttpProxy proxy, Socket sock) {
      this.proxy = proxy;
      this.proxySocket = sock;    
   }
   
   /**
    * @return Returns the request.
    */
   public byte[] getRequest() {
      return request.getRequest();
   }
   
   /**
    * @return Returns the response.
    */
   public byte[] getResponse() {
      return response.getResponse();
   }

   public void run() {
      long duration, starttime = System.currentTimeMillis();
      Socket destinationSocket = null;
      try {
         BufferedOutputStream proxyClientStream = new BufferedOutputStream(proxySocket.getOutputStream());
         
         // read client request
         HttpRequest request = handleRequest(proxySocket);
         this.request = request;
         
         // send request to server
         //logger.debug("connecting to: " + request.getHost() + ":" + request.getPort());
         if (!((System.getProperty("http.proxyHost") == null) || System.getProperty("http.proxyHost").trim().equals(""))) {
             String proxyHost = System.getProperty("http.proxyHost");
             String proxyPortStr = System.getProperty("http.proxyPort");
             if (proxyPortStr == null || proxyPortStr.trim().equals("")) {
                 proxyPortStr = "80";
             }
             int proxyPort = Integer.parseInt(proxyPortStr); 
             //logger.debug("connecting via proxy: " + proxyHost + ":" + proxyPort);
	         destinationSocket = new Socket(proxyHost, proxyPort);
         }
         else {
	         destinationSocket = new Socket(request.getHost(), request.getPort());
         }
         OutputStream destinationOutputStream = destinationSocket.getOutputStream();
         destinationOutputStream.write(request.getRequest(), 0, request.getRequest().length);
         destinationOutputStream.flush();
         //logger.debug("request sent");
         
         // read response from server
         HttpResponse response = handleResponse(destinationSocket, proxyClientStream);
         this.response = response;
         destinationSocket.close();
         destinationSocket = null;

         // send response to client
         proxyClientStream.flush();
         proxySocket.close();
         proxySocket = null;
         duration = System.currentTimeMillis() - starttime;
         //logger.debug("duration: " + duration);
         String path = "http://" + request.getHost() + ((request.getPort()==80)?"":(":"+request.getPort()))  + request.getPath();
         proxy.setWorkerResult(request.getMethod(), String.valueOf(response.getStatusCode()), path, starttime, request.getRequest(), this.response.getResponse(), duration);
      }
      catch (Exception e) {
         //logger.error("Error occurred: " + e.toString());
         duration = System.currentTimeMillis() - starttime;
         if (request != null) {
            String path = "http://" + request.getHost() + ((request.getPort()==80)?"":(":"+request.getPort()))  + request.getPath();            
            if (response != null && response.getResponse() != null) {
               proxy.setWorkerResult(request.getMethod(), String.valueOf(response.getStatusCode()), path, starttime, request.getRequest(), response.getResponse(), duration);
            }
            else {
               proxy.setWorkerResult(request.getMethod(), "XXX", path, starttime, request.getRequest(), ("no response available - " + e.toString()).getBytes(), duration);
            }
         }
         else {
            proxy.setWorkerResult("---", "---", "---", starttime, e.toString().getBytes(), "no response available".getBytes(), duration);
         }
         try {
            if (proxySocket != null) {
               closeSocket(proxySocket);
            }
         }
         finally {
            if (destinationSocket != null) {
               closeSocket(destinationSocket);
            }
         }
      }
   }

   private HttpRequest handleRequest(Socket proxySocket) throws IOException {
      HttpRequest request = new HttpRequest();
      BufferedInputStream proxyInStream = new BufferedInputStream(proxySocket.getInputStream());
      byte b = -1;
      ArrayList<Byte> list = new ArrayList<Byte>(200);
      ArrayList<Byte> listToSend = new ArrayList<Byte>(200);
      int readInt;
      String previousLine = null;
      int lastCRPos = 0;
      int lastSendCRPos = 0;
      boolean hasCompleted = false;
      int lineNo = 0;
      int length = 0;
      while (!isInterrupted && !hasCompleted && (readInt = proxyInStream.read()) != -1) {
         b = (byte)readInt;
         list.add(new Byte(b));
         listToSend.add(new Byte(b));
         if (b == 13) {
            // check for two line breaks without form feed
            if (list.size() > 1) {
               if (list.get(list.size() - 2).equals(new Byte((byte)13))) {
                  hasCompleted = true;
               }
               else {
                  // try to analyze the previous line
                  byte[] bytes = new byte[list.size() - lastCRPos - 1];
                  for (int i = lastCRPos; i < list.size() - 1; i++) {
                     bytes[i - lastCRPos] = ((Byte)list.get(i)).byteValue();
                  }
                  // requests are always in ASCII
                  previousLine = new String(bytes, "ISO-8859-1");
                  //logger.debug("request: " + previousLine);
                  if (lineNo == 0) {
                     // we must have here s.th. like 
                     // GET http://server/xyz.html?param=value HTTP/1.0
                     String[] components = previousLine.split(" ");
                     String method = components[0];                                          
                     String urlStr = components[1];
                     String httpVersion  = components[2];
                     
                     // now parse the URL
                     URL url = new URL(urlStr);
                     String host = url.getHost();
                     String path = url.getPath();
                     String query = url.getQuery();
                     String port = String.valueOf(url.getPort());                        
                     if ("-1".equals(port)) {
                        port = "80";
                     }
                     request.setPort(Integer.parseInt(port));
                     request.setHost(host);
                     request.setPath(path);
                     request.setQuery(query);
                     request.setMethod(method);
                     request.setVersion(httpVersion);
                     
                     // now we can reconstruct this line...
                     if ((System.getProperty("http.proxyHost") == null) || System.getProperty("http.proxyHost").trim().equals("")) {
	                     listToSend = new ArrayList<Byte>();
	                     StringBuffer buff = new StringBuffer(100);
	                     buff.append(method);
	                     buff.append(' ');
	                     buff.append(path);
	                     if (query != null) {
	                        buff.append('?');
	                        buff.append(query);
	                     }
	                     buff.append(' ');
	                     buff.append(components[2].substring(0, components[2].length()));
	                     String newLine = buff.toString();
	                     byte[] newLineBytes = newLine.getBytes("ISO-8859-1");
	                     for (int i = 0; i < newLineBytes.length; i++) {
	                        listToSend.add(new Byte(newLineBytes[i]));
	                     }
	                     listToSend.add(new Byte((byte)13));
                     }
                  }
                  if (previousLine.matches("^[Cc]ontent-[Ll]ength: .+")) {
                     String lengthStr = previousLine.substring(16, previousLine.length());
                     length = Integer.parseInt(lengthStr);
                     //logger.debug("length: " + length + ", " + lengthStr);
                  }
                  if (previousLine.matches("^[Pp]roxy.+" )) {
		              if ((System.getProperty("http.proxyHost") == null) || System.getProperty("http.proxyHost").trim().equals("")) {
		                  //logger.debug("proxy!!! - " + previousLine);
		                  // if not used behind another proxy erase proxy-related headers
		                  for (int i = listToSend.size() - 1; i > lastSendCRPos - 2; i--) {
		                      listToSend.remove(i);
		                  }
		              }
                  }
                  // the CR should be ignored for printing any headerLine
                  lastCRPos = list.size() + 1;
                  lastSendCRPos = listToSend.size() + 1;
                  lineNo++;
               }
            }
         }
         if (b == 10) {
            // check for two line breaks with form feed
            if (list.get(list.size() - 2).equals(new Byte((byte)13)) &&
               list.get(list.size() - 3).equals(new Byte((byte)10)) &&
               list.get(list.size() - 4).equals(new Byte((byte)13))) {
               //logger.debug("length: " + length);
               if (length == 0) {
                  hasCompleted = true;
               }
               else {
                  for (int i = 0; i < length; i++) {
                     readInt = proxyInStream.read();
                     b = (byte)readInt;
                     list.add(new Byte(b));
                     listToSend.add(new Byte(b));
                  }
                  list.add(new Byte((byte)'\n'));
                  listToSend.add(new Byte((byte)'\n'));
                  hasCompleted = true;
               }
            }
         }
      }
      // store original request
      byte[] byteArray = getByteArrayFromList(listToSend);
      request.setRequest(byteArray);    
      //logger.debug("request: \nasText:\n" + new String(byteArray) + "as bytes:\n" + printByteArray(byteArray));
      return request;
   }
   
   private HttpResponse handleResponse(Socket destinationSocket, BufferedOutputStream proxyClientStream) throws IOException {
      HttpResponse response = new HttpResponse();
      BufferedInputStream responseInputStream = new BufferedInputStream(destinationSocket.getInputStream());
      int readInt;
      int length = -1;
      String previousLine;
      byte b = -1;
      boolean hasCompleted = false;
      ArrayList<Byte> list = new ArrayList<Byte>(10000);
      ArrayList<Byte> responseContentList = new ArrayList<Byte>(10000);
      boolean isContent = false;
      boolean hasChunkedEncoding = false;
      int lineNo = 0;
      int lastCRPos = 0;
      while (!isInterrupted && !hasCompleted && (readInt = responseInputStream.read()) != -1) {
         b = (byte)readInt;
         list.add(new Byte(b));
         if (isContent) {
             responseContentList.add(new Byte(b));
         }    
         proxyClientStream.write(readInt);
         if (b == 13) {
            if (list.size() > 1) {
               // try to analyze the previous line
               byte[] bytes = new byte[list.size() - lastCRPos];
               for (int i = lastCRPos; i < list.size() - 1; i++) {
                  bytes[i - lastCRPos] = list.get(i).byteValue();
               }
               // requests are always in ASCII
               previousLine = new String(bytes, "ISO-8859-1");
               if (lineNo == 0) {
                  // we must have here s.th. like 
                  // HTTP/1.0 200 OK
                  String[] components = previousLine.split(" ");
                  if (components.length > 2) {
                     response.setStatusCode(Integer.parseInt(components[1]));
                     response.setHttpVersion(components[0]);
                  }
               }
               if (previousLine.matches("^[Cc]ontent-[Ll]ength: .+")) {
                  String lengthStr = previousLine.substring(16, previousLine.length() - 1);
                  length = Integer.parseInt(lengthStr);
               }
               if (previousLine.matches("^[Tt]ransfer-[Ee]ncoding: [Cc]hunked.+")) {
                  hasChunkedEncoding = true;
               }
               //logger.debug("response: " + previousLine);
               // the CR should be ignored;
               lastCRPos = list.size() + 1;
               lineNo++;
            }
         }
         if (b == 10) {
            // check for two line breaks with form feed
            if (list.get(list.size() - 2).equals(new Byte((byte)13)) &&
               list.get(list.size() - 3).equals(new Byte((byte)10)) &&
               list.get(list.size() - 4).equals(new Byte((byte)13))) {
               // section 4.3 of the http-spec states:
               // All responses to the HEAD request method
               // MUST NOT include a message-body, even though 
               // the presence of entity-header fields might lead
               // one to believe they do. All 1xx (informational),
               // 204 (no content), and 304 (not modified) responses
               // MUST NOT include a message-body. All other 
               // responses do include a message-body, although it 
               // MAY be of zero length.
               // (s. http://www.ietf.org/rfc/rfc2616.txt)
               if ("HEAD".equals(request.getMethod())
                    || response.getStatusCode() == 204
                    || response.getStatusCode() == 304
                    || (response.getStatusCode() > 99 && 
                          response.getStatusCode() < 200)) {
                  // no content allowed:
                  hasCompleted = true;
               }
               else if (length == 0) {
                  hasCompleted = true;
               }
               else if (length == -1) {
                   isContent = true;
                   if (hasChunkedEncoding) {
                       list.addAll(getAllChunks(responseInputStream, proxyClientStream));
                       hasCompleted = true;
                   }
               }
               else {
                  for (int i = 0; i < length; i++) {
                     readInt = responseInputStream.read();
                     b = (byte)readInt;
                     list.add(new Byte(b));
                     proxyClientStream.write(readInt);
                  }
                  hasCompleted = true;
               }
            }
         }
      }
      byte[] byteArray = getByteArrayFromList(list);
      //logger.debug("response: \nasText:\n" + new String(byteArray) + "as bytes:\n" + printByteArray(byteArray));

      response.setResponse(byteArray);
      
      return response;
   }
   
   private byte[] getByteArrayFromList(List<Byte> list) {
      if (list == null) {
         return new byte[0];
      }
      byte[] retBytes = new byte[list.size()];
      for (int i = 0; i < list.size(); i++) {
         retBytes[i] = list.get(i).byteValue();
      }
      return retBytes;
   }
   
   /**
    * Helper method for debugging.
    */
   @SuppressWarnings("unused")
   private String printByteArray(byte[] b) {
      if (b == null) {
         return "";
      }
      StringBuffer buffer = new StringBuffer(10000);
      for (int i = 0; i < b.length; i++) {
         buffer.append(b[i]);
         if (b[i] == (byte)10) {
            buffer.append('\n');
         }
         else {
            buffer.append(' ');
         }
         
      }
      return buffer.toString();
   }
   
   private List<Byte> getAllChunks(BufferedInputStream inStream, BufferedOutputStream proxyClientStream) throws IOException {
	   ArrayList<Byte> retList = new ArrayList<Byte>(10000);
	   List<Byte> chunk;
	   while (!isInterrupted && (chunk = getChunk(inStream, proxyClientStream)).size() != 0) {
	      retList.addAll(chunk);
	   }
	   // dirty hack:
	   byte[] appendBytes = "0\r\n".getBytes("ISO-8859-1");
	   for (int i = 0; i < appendBytes.length; i++) {
		   retList.add(new Byte(appendBytes[i]));
         proxyClientStream.write((int)appendBytes[i]);
	   }
	   return retList;
   }
      
   private List<Byte> getChunk(BufferedInputStream inStream, BufferedOutputStream proxyClientStream) throws IOException {
	   ArrayList<Byte> retList = new ArrayList<Byte>(1024);
	   int readInt;
	   String chunkHeader = null;
	   String lengthStr = "";
	   int length;
       chunkHeader = getNextLine(inStream);
       int posOfSemicolon = chunkHeader.indexOf(';');
       if (posOfSemicolon != -1) {
           lengthStr = chunkHeader.substring(0, posOfSemicolon);
       }
       else {
           lengthStr = chunkHeader.substring(0, chunkHeader.length() - 2);
       }
       int posOfMarks = lengthStr.indexOf('"');
       if (posOfMarks != -1) {
           lengthStr = lengthStr.substring(1, lengthStr.length() -1);
       }
       lengthStr = lengthStr.trim();
       if ("0".equals(lengthStr)) {
           // marks the end; so simply skip to EOL and return empty list
           getNextLine(inStream);
           return retList; 
       }
	   byte[] appendBytes = chunkHeader.getBytes("ISO-8859-1");
	   for (int i = 0; i < appendBytes.length; i++) {
		   retList.add(new Byte(appendBytes[i]));
         proxyClientStream.write((int)appendBytes[i]);
	   }
       length = Integer.parseInt(lengthStr, 16);
       byte b;
       for (int i = 0; i < length; i++) {
           //read bytes
           readInt = inStream.read();
           b = (byte)readInt;
           retList.add(new Byte(b));
           proxyClientStream.write(readInt);
       }
       // skip to EOL
       String chunkEndLine = getNextLine(inStream);
	   appendBytes = chunkEndLine.getBytes("ISO-8859-1");
	   for (int i = 0; i < appendBytes.length; i++) {
		   retList.add(new Byte(appendBytes[i]));
         proxyClientStream.write((int)appendBytes[i]);
	   }
	   return retList;
   }
   
   private String getNextLine(BufferedInputStream inStream) throws IOException {
       StringBuffer buffer = new StringBuffer(20);
       int readInt;
       char c;
	   while (!isInterrupted && (readInt = inStream.read()) != -1) {
	       c = (char)readInt;
	       buffer.append(c);
           if (c == (char)10) {
               // check for end of line
               if (buffer.length() > 1 && buffer.charAt(buffer.length() - 2) == (char)13) {
                   return buffer.toString();
               }
           }
	   }
	   return buffer.toString();       
   }

   private void closeSocket(Socket sock) {
      try {
         sock.close();
      }
      catch (Exception e) {
         // we cannot do anything if this op crashes
      }
   }
}
