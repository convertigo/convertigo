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
 private Logger previousBeans, previousContext, previousEngine;
 private Logger logger;
 private final StringBuilder messages = new StringBuilder();
 private static final String MARKER = "synthetic-a(b$\\value";
 private HttpConnector connector;
 private JsonHttpTransaction transaction;
 private RequestableHttpVariable body;
 private Context context;

 @Before public void setup() throws Exception {
  previousApp=Engine.theApp;
  previousBeans=Engine.logBeans; previousContext=Engine.logContext; previousEngine=Engine.logEngine;
  logger=Logger.getLogger("request-body-logging-test");logger.setLevel(Level.DEBUG);logger.setAdditivity(false);
  logger.addAppender(new AppenderSkeleton() {
   @Override protected void append(LoggingEvent event) { messages.append(event.getRenderedMessage()).append('\n'); }
   @Override public void close() { }
   @Override public boolean requiresLayout() { return false; }
  });
  Engine.logBeans=Engine.logContext=Engine.logEngine=logger;
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
  Engine.logBeans=previousBeans;Engine.logContext=previousContext;Engine.logEngine=previousEngine;
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
