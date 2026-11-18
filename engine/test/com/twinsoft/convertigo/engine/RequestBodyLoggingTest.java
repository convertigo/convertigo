/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.util.AbstractList;
import java.util.List;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.transactions.JsonHttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class RequestBodyLoggingTest {
 private Engine previousApp;
 private Logger previousBeans, previousContext, previousEngine, previousSiteClipper;
 private Logger logger;
 private final StringBuilder messages = new StringBuilder();
 private static final String MARKER = "synthetic-a(b$\\value";
 private HttpConnector connector;
 private JsonHttpTransaction transaction;
 private RequestableHttpVariable body;
 private Context context;

 @Before public void setup() throws Exception {
  previousApp=Engine.theApp;
  previousBeans=Engine.logBeans; previousContext=Engine.logContext; previousEngine=Engine.logEngine; previousSiteClipper=Engine.logSiteClipper;
  logger=Logger.getLogger("request-body-logging-test");logger.setLevel(Level.DEBUG);logger.setAdditivity(false);
  logger.addAppender(new AppenderSkeleton() {
   @Override protected void append(LoggingEvent event) {
    messages.append(event.getRenderedMessage()).append('\n');
    if (event.getThrowableStrRep()!=null) for(String line:event.getThrowableStrRep()) messages.append(line).append('\n');
   }
   @Override public void close() { }
   @Override public boolean requiresLayout() { return false; }
  });
  Engine.logBeans=Engine.logContext=Engine.logEngine=Engine.logSiteClipper=logger;
  EnginePropertiesManager.initProperties();Engine.theApp=new Engine();
  Project project=new Project();project.setName("SyntheticProject");
  connector=new HttpConnector();connector.setServer("127.0.0.1");project.add(connector);
  transaction=new JsonHttpTransaction();connector.add(transaction);
  body=new RequestableHttpVariable();body.setName("__body");body.setHttpMethod("POST");
  body.setVisibility(0);transaction.addVariable(body);
  context=new Context("synthetic");context.project=project;context.requestedObject=transaction;
 }

 @After public void restore() {
  logger.removeAllAppenders();Engine.theApp=previousApp;
  Engine.logBeans=previousBeans;Engine.logContext=previousContext;Engine.logEngine=previousEngine;Engine.logSiteClipper=previousSiteClipper;
 }

 private void parseBody() throws Exception {
  context.inputDocument=XMLUtils.createDom();
  var root=context.inputDocument.createElement("input");context.inputDocument.appendChild(root);
  var variables=context.inputDocument.createElement("transaction-variables");root.appendChild(variables);
  var variable=context.inputDocument.createElement("variable");variable.setAttribute("name","__body");
  variable.setAttribute("value",MARKER);variables.appendChild(variable);
  transaction.parseInputDocument(context);
 }

 @Test public void masksTheInputDocumentAndRawPostBodyWithoutChangingExecution() throws Exception {
  parseBody();String original=XMLUtils.prettyPrintDOM(context.inputDocument);
  connector.prepareForTransaction(context);
  assertFalse("A masked body must not appear in INFO or DEBUG logs",messages.toString().contains(MARKER));
  assertTrue(messages.toString().contains(Visibility.STRING_MASK));
  assertEquals(MARKER,connector.postQuery);
  assertEquals(0,body.getVisibility());
  assertEquals(original,XMLUtils.prettyPrintDOM(context.inputDocument));
  assertEquals(MARKER,transaction.getParameterValue("__body"));
 }

 @Test public void masksAConvertedXmlBodyWithoutChangingTheJsonPayload() throws Exception {
  context.inputDocument=null;
  var document=XMLUtils.createDom();var value=document.createElement("value");document.appendChild(value);
  var field=document.createElement("password");field.setTextContent(MARKER);value.appendChild(field);
  context.nativeParameters=java.util.Map.of("__body",value);
  transaction.parseInputDocument(context);
  var contentType=new RequestableHttpVariable();contentType.setName(com.twinsoft.convertigo.engine.enums.Parameter.HttpContentType.getName());contentType.setValueOrNull("application/json");transaction.addVariable(contentType);
  connector.prepareForTransaction(context);
  assertFalse(messages.toString().contains("synthetic-a(b"));
  assertEquals(MARKER,value.getTextContent());
  assertEquals(XMLUtils.XmlToJson(value,true,true,com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot.docChildNodes),connector.postQuery);
 }

 @Test public void masksTheRawBodyEvenWithOnlyStudioVisibility() throws Exception {
  body.setVisibility(Visibility.Studio.getMask());
  parseBody();connector.prepareForTransaction(context);
  assertFalse(messages.toString().contains(MARKER));
  assertEquals(MARKER,connector.postQuery);
 }

 @Test public void appliesDeclaredPasswordVisibilityToInputLogsWithoutChangingItsValue() throws Exception {
  var password=new RequestableHttpVariable();password.setName("password");
  password.setVisibility(Visibility.Logs.getMask());transaction.addVariable(password);
  parseBody();
  var variable=context.inputDocument.createElement("variable");
  variable.setAttribute("name","password");variable.setAttribute("value",MARKER);
  context.inputDocument.getElementsByTagName("transaction-variables").item(0).appendChild(variable);
  messages.setLength(0);
  transaction.parseInputDocument(context);
  assertFalse(messages.toString().contains(MARKER));
  assertEquals(MARKER,transaction.getParameterValue("password"));
  assertEquals(MARKER,variable.getAttribute("value"));
  password.setVisibility(0);
  messages.setLength(0);
  transaction.parseInputDocument(context);
  assertFalse("Password names remain masked without an explicit flag",messages.toString().contains(MARKER));
 }

 @Test public void ordinaryInputsStayVisibleWhileRawBodiesAndHeadersStayMaskedAtEveryLogLevel() throws Exception {
  for (Level level : List.of(Level.INFO, Level.DEBUG, Level.TRACE)) {
   logger.setLevel(level);
   messages.setLength(0);
   parseBody();
   var variable=context.inputDocument.createElement("variable");
   variable.setAttribute("name","search");variable.setAttribute("value","public-search");
   context.inputDocument.getElementsByTagName("transaction-variables").item(0).appendChild(variable);
   var header=context.inputDocument.createElement("variable");header.setAttribute("name","__header_X_Custom");header.setAttribute("value",MARKER);
   context.inputDocument.getElementsByTagName("transaction-variables").item(0).appendChild(header);
   transaction.parseInputDocument(context);connector.prepareForTransaction(context);
   assertFalse(messages.toString().contains(MARKER));
   assertTrue(messages.toString().contains("public-search"));
   assertTrue(messages.toString().contains(MARKER.length()+" characters"));
   assertEquals(MARKER,transaction.getParameterValue("__header_X_Custom"));
  }
 }

 @Test public void callerSequenceAndStepDefaultsStayMaskedWithoutChangingTheirVisibility() throws Exception {
  var sequence=new com.twinsoft.convertigo.beans.sequences.GenericSequence();
  var parameter=new com.twinsoft.convertigo.beans.variables.RequestableVariable();
  parameter.setName("password");parameter.setValueOrNull(MARKER);sequence.addVariable(parameter);
  var step=new com.twinsoft.convertigo.beans.steps.TransactionStep();
  var stepVariable=new com.twinsoft.convertigo.beans.variables.StepVariable();
  stepVariable.setName("userToken");stepVariable.setValueOrNull(MARKER);step.addVariable(stepVariable);
  messages.setLength(0);logger.setLevel(Level.TRACE);
  assertEquals(MARKER,sequence.getVariableValue("password"));
  assertEquals(MARKER,step.getVariableValue("userToken"));
  assertFalse(messages.toString().contains(MARKER));
  assertEquals(0,parameter.getVisibility());assertEquals(0,stepVariable.getVisibility());
 }

 @Test public void sqlWarnUsesLiteralMaskingForBothDeclaredAndDefaultSensitiveValues() throws Exception {
  for (String name : List.of("password", "opaqueValue")) {
   var sql=new com.twinsoft.convertigo.beans.transactions.SqlTransaction();
   var project=new Project();project.setName("SqlLogTest");
   var sqlConnector=new com.twinsoft.convertigo.beans.connectors.SqlConnector() {
    @Override public java.sql.PreparedStatement prepareStatement(String query, com.twinsoft.convertigo.beans.transactions.SqlTransaction.SqlQueryInfos infos) { return null; }
    @Override public void setData(List<List<String>> data, List<String> headers) { }
   };
   project.add(sqlConnector);sqlConnector.add(sql);
   var value=new com.twinsoft.convertigo.beans.variables.RequestableVariable();value.setName(name);value.setValueOrNull(MARKER);
   value.setVisibility(name.equals("password") ? 0 : Visibility.Logs.getMask());sql.addVariable(value);
   var type=com.twinsoft.convertigo.beans.transactions.SqlTransaction.class;
   var connectorField=type.getDeclaredField("connector");connectorField.setAccessible(true);connectorField.set(sql,sqlConnector);
   var infos=sql.new SqlQueryInfos("SELECT '{{"+name+"}}';",sql,true);
   var hidden=new java.util.ArrayList<String>();
   var prepare=type.getDeclaredMethod("prepareQuery",List.class,infos.getClass());prepare.setAccessible(true);
   String query=(String)prepare.invoke(sql,hidden,infos);
   assertTrue(query.contains(MARKER));assertTrue(hidden.contains(MARKER));
   messages.setLength(0);logger.setLevel(Level.WARN);
   var results=type.getDeclaredMethod("getQueryResults",java.sql.ResultSet.class,int.class,org.w3c.dom.Element.class,String.class,List.class,infos.getClass());results.setAccessible(true);
   results.invoke(sql,null,-1,null,query,hidden,infos);
   assertTrue(messages.toString().contains("Could not execute query"));
   assertFalse(messages.toString().contains(MARKER));
  }
 }

 @Test public void hiddenValuesAreReplacedLiterally() {
  for(String value:List.of("a(b","[x","p*ss",".","$\\")) {
   assertEquals("before "+Visibility.STRING_MASK+" after",Visibility.Logs.replaceValues(List.of(value),"before "+value+" after"));
  }
 }

 private static class LoggedConnector extends HttpConnector {
  static String url(Context context, String url) { return logUrl(context, url); }
 }

 @Test public void urlLogsUseDefaultNamesWithoutVariableMetadata() {
  String prefix="https://example.test:8443/path?";
  String query="%70assword="+MARKER+"&api_key="+MARKER+"&password="+MARKER+"&userToken="+MARKER+"&search=a%26b&empty=";
  String logged=LoggedConnector.url(null,prefix+query);
  assertTrue(logged.startsWith(prefix));assertFalse(logged.contains(MARKER));
  assertTrue(logged.endsWith("&search=a%26b&empty="));
  assertTrue(logged.contains("%70assword=\""+Visibility.STRING_MASK+"\""));
 }

 @Test public void urlLogsMaskAnExplicitHttpAliasInTheFirstParameter() throws Exception {
  var variable=new RequestableHttpVariable();variable.setName("opaqueValue");
  variable.setHttpName("q$[x]");variable.setVisibility(Visibility.Logs.getMask());transaction.addVariable(variable);
  String prefix="/path?";
  String query="q%24%5Bx%5D="+MARKER+"&opaqueValue="+MARKER+"&search=a(b$\\value";
  assertEquals(prefix+"q%24%5Bx%5D=\""+Visibility.STRING_MASK+"\"&opaqueValue=\""+Visibility.STRING_MASK+"\"&search=a(b$\\value",
    LoggedConnector.url(context,prefix+query));
 }

 @Test public void urlLogsKeepTheDestinationWhenQueryPreparationFails() {
  String prefix="https://example.test/path?";
  assertEquals(prefix+Visibility.STRING_MASK,LoggedConnector.url(null,prefix+"bad%XX="+MARKER));
  context.requestedObject=new JsonHttpTransaction() {
   @Override public java.util.Vector<com.twinsoft.convertigo.beans.variables.RequestableVariable> getVariables() {
    throw new IllegalStateException("synthetic failure");
   }
  };
  assertEquals(prefix+Visibility.STRING_MASK,LoggedConnector.url(context,prefix+"opaqueValue="+MARKER));
  assertEquals("https://example.test/path",LoggedConnector.url(context,"https://example.test/path"));
  assertNull(LoggedConnector.url(context,null));
 }

 @Test public void siteClipperLogsMaskQueriesWithoutChangingTheLocation() throws Exception {
  var variable=new RequestableHttpVariable();variable.setName("opaqueValue");variable.setVisibility(Visibility.Logs.getMask());transaction.addVariable(variable);
  context.name="synthetic";
  context.httpServletRequest=(jakarta.servlet.http.HttpServletRequest)java.lang.reflect.Proxy.newProxyInstance(
    getClass().getClassLoader(),new Class<?>[]{jakarta.servlet.http.HttpServletRequest.class},(proxy,method,args)->switch(method.getName()) {
     case "getRequestURI" -> "/convertigo/projects/SyntheticProject/request";
     case "getRequestURL" -> new StringBuffer("http://example.test/convertigo/projects/SyntheticProject/request");
     default -> null;
    });
  String query="password="+MARKER+"&opaqueValue="+MARKER+"&search=visible";
  for(Level level:List.of(Level.DEBUG,Level.TRACE)) {
   messages.setLength(0);logger.setLevel(level);
   String location=com.twinsoft.convertigo.beans.connectors.SiteClipperConnector.constructSiteLocation(context,connector,"https://upstream.example/path?"+query);
   assertTrue(location.endsWith("/https/upstream.example/path?"+query));
   String log=messages.toString();
   assertTrue(log.contains("/https/upstream.example/path?"));assertTrue(log.contains("search=visible"));
   assertFalse(log.contains(MARKER));assertTrue(log.contains(Visibility.STRING_MASK));
  }
 }

 @Test public void sqlConnectionLogsMaskQueriesWithoutChangingTheJdbcUrl() throws Exception {
  var variable=new RequestableHttpVariable();variable.setName("opaqueValue");variable.setVisibility(Visibility.Logs.getMask());transaction.addVariable(variable);
  var sqlConnector=new com.twinsoft.convertigo.beans.connectors.SqlConnector();sqlConnector.context=context;
  String url="jdbc:postgresql://example.test/database?password="+MARKER+"&opaqueValue="+MARKER+"&search=visible";
  var field=sqlConnector.getClass().getDeclaredField("realJdbcURL");field.setAccessible(true);field.set(sqlConnector,url);
  for(Level level:List.of(Level.DEBUG,Level.TRACE)) {
   var closed=new java.util.concurrent.atomic.AtomicBoolean();
   sqlConnector.connection=(java.sql.Connection)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{java.sql.Connection.class},
     (proxy,method,args)->switch(method.getName()) {
      case "hashCode" -> 1;
      case "isClosed" -> closed.get();
      case "close" -> { closed.set(true);yield null; }
      default -> throw new UnsupportedOperationException(method.getName());
     });
   messages.setLength(0);logger.setLevel(level);sqlConnector.close();
   assertTrue(closed.get());assertEquals(url,sqlConnector.getRealJdbcURL());
   String log=messages.toString();
   assertTrue(log.contains("jdbc:postgresql://example.test/database?"));assertTrue(log.contains("search=visible"));
   assertFalse(log.contains(MARKER));assertTrue(log.contains(Visibility.STRING_MASK));
  }
 }

 @Test public void invalidSqlUrlLogsDoNotEchoTheQueryThroughTheException() {
  logger.setLevel(Level.TRACE);messages.setLength(0);
  assertThrows(IllegalArgumentException.class,()->com.twinsoft.convertigo.beans.connectors.SqlConnector.getParameterJavaValue(
    java.sql.Types.DATALINK,"https://example.test/path?password="+MARKER));
  String log=messages.toString();
  assertTrue(log.contains("https://example.test/path?"));assertTrue(log.contains("URISyntaxException"));
  assertFalse(log.contains(MARKER));assertTrue(log.contains(Visibility.STRING_MASK));
 }

 @Test public void debugUrlsHideQueryValuesWithoutChangingTheRequest() throws Exception {
  assertQueryLogs(Level.DEBUG);
 }

 @Test public void traceUrlsHideQueryValuesWithoutChangingTheRequest() throws Exception {
  assertQueryLogs(Level.TRACE);
 }

 private void assertQueryLogs(Level level) throws Exception {
  var received = new java.util.concurrent.CopyOnWriteArrayList<String>();
  var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
  server.createContext("/", exchange -> {
   received.add(exchange.getRequestURI().getRawQuery());
   if (exchange.getRequestURI().getPath().equals("/start")) {
    exchange.getResponseHeaders().add("Location", "/done?" + exchange.getRequestURI().getRawQuery());
    exchange.sendResponseHeaders(302, -1);
   } else {
    byte[] response = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(200, response.length);
    exchange.getResponseBody().write(response);
   }
   exchange.close();
  });
  server.start();
  try {
   logger.setLevel(level);
   connector.setPort(server.getAddress().getPort());connector.setBaseDir("/start");
   connector.context=context;context.connector=connector;context.transaction=transaction;transaction.context=context;
   transaction.setHttpPool(com.twinsoft.convertigo.engine.enums.HttpPool.no);
   transaction.createRequestableThread();
   Engine.theApp.pluginsManager=new PluginsManager();
   Engine.theApp.proxyManager=new ProxyManager() {
    @Override public void setProxy(org.apache.commons.httpclient.HostConfiguration host, org.apache.commons.httpclient.HttpState state, java.net.URL url) { }
   };
   var values=new java.util.LinkedHashMap<String,Object>();
   for (String name : List.of("password", "userToken", "api_key", "opaqueValue", "search")) {
    var variable=new RequestableHttpVariable();variable.setName(name);variable.setHttpMethod("GET");
    variable.setVisibility(name.equals("opaqueValue") ? Visibility.Logs.getMask() : 0);
    if (name.equals("opaqueValue")) variable.setHttpName("q$[x]");
    transaction.addVariable(variable);values.put(name,name.equals("search") ? "public-search" : MARKER);
   }
   context.nativeParameters=values;context.inputDocument=null;
   transaction.parseInputDocument(context);connector.prepareForTransaction(context);
   String actualUrl=connector.sUrl;
   messages.setLength(0);
   assertEquals("{}",new String(connector.getData(context),java.nio.charset.StandardCharsets.UTF_8));
   assertEquals(2,received.size());
   for(String query:received) assertEquals(actualUrl.substring(actualUrl.indexOf('?')+1),query);
   assertEquals(actualUrl,connector.sUrl);
   String log=messages.toString();
   assertTrue(log.contains("Connecting to: http://127.0.0.1:"+server.getAddress().getPort()+"/start?"));
   assertTrue(log.contains("Redirecting to : http://127.0.0.1:"+server.getAddress().getPort()+"/done?"));
   assertTrue(log.contains("search=public-search"));
   assertFalse(log.contains(MARKER));
   assertFalse(log.contains(java.net.URLEncoder.encode(MARKER,java.nio.charset.StandardCharsets.UTF_8)));
   assertTrue(log.contains(Visibility.STRING_MASK));
  } finally { server.stop(0); }
 }

 @Test public void repeatedSingleCharacterValuesDoNotExpandTheMask() {
  assertEquals(Visibility.STRING_MASK,Visibility.Logs.replaceValues(List.of("*","*"),"*"));
  assertEquals(Visibility.STRING_MASK,Visibility.Logs.replaceValues(List.of("ab","abc"),"abc"));
 }

 @Test public void hiddenValueFailuresDoNotReturnTheOriginalText() {
  List<String> broken=new AbstractList<>() {
   @Override public int size() { return 1; }
   @Override public String get(int index) { throw new IllegalStateException("synthetic failure"); }
  };
  assertEquals(Visibility.STRING_MASK,Visibility.Logs.replaceValues(broken,MARKER));
 }
}
