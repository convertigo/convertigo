<%--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements. See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership. The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License. You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied. See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@ page import="org.apache.axis2.Constants" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.apache.axis2.context.ServiceContext" %>
<%@ page import="org.apache.axis2.context.ServiceGroupContext" %>
<%@ page import="org.apache.axis2.deployment.util.PhasesInfo" %>
<%@ page import="org.apache.axis2.description.AxisModule" %>
<%@ page import="org.apache.axis2.description.AxisOperation" %>
<%@ page import="org.apache.axis2.description.AxisService" %>
<%@ page import="org.apache.axis2.description.AxisServiceGroup" %>
<%@ page import="org.apache.axis2.description.Parameter" %>
<%@ page import="org.apache.axis2.engine.AxisConfiguration" %>
<%@ page import="org.apache.axis2.engine.Handler" %>
<%@ page import="org.apache.axis2.engine.Phase" %>
<%@ page import="org.apache.axis2.util.JavaUtils" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Hashtable" %>
<%@ page import="java.util.Iterator" %>
<%--
~ Licensed to the Apache Software Foundation (ASF) under one
~ or more contributor license agreements. See the NOTICE file
~ distributed with this work for additional information
~ regarding copyright ownership. The ASF licenses this file
~ to you under the Apache License, Version 2.0 (the
~ "License"); you may not use this file except in compliance
~ with the License. You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
--%>
<%@ page contentType="text/html;charset=iso-8859-1" language="java" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <jsp:include page="../include/httpbase.jsp"/>
    <title>Axis2 :: Administration Page</title>
    <link href="axis2-web/css/axis-style.css" rel="stylesheet" type="text/css"/>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
  </head>

  <body>
    <jsp:include page="header.inc"/>
    <table class="FULL_BLANK">
      <tr>
        <td valign="top" width="20%">
          <jsp:include page="../LeftFrame.jsp"/>
        </td>
        <td valign="top" align="left" width="80%">
        <table width="100%">
          <tr>
            <td align="right" colspan="2"><a href="#" onclick="javascript:history.back();">Back</a>&nbsp;&nbsp;|&nbsp;&nbsp;<a
              href="axis2-admin/logout">Log out</a></td>
          </tr>
        </table>
