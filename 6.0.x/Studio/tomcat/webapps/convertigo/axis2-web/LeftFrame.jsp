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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<table width="100%" style="border-right:1px solid #CCCCCC;">
    <tr>
     <td colspan="2" >
       <b>Tools </b>
     </td>
    </tr>
    <tr>
       <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
       </td>
       <td>
         <a href="axis2-admin/upload">Upload Service</a>
       </td>
    </tr>
<tr>
     <td colspan="2" >
      <b><nobr>System Components&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</nobr></b>
     </td>
  </tr>
  <tr>
    <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
    </td>
    <td >
      <a href="axis2-admin/listService">Available Services</a>
    </td>
 </tr>
 <tr>
    <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
    </td>
    <td >
      <a href="axis2-admin/listServiceGroups">Available Service Groups</a>
    </td>
 </tr>
 <tr>
    <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
    </td>
    <td >
      <a href="axis2-admin/listModules">Available Modules</a>
    </td>
 </tr>
 <tr>
    <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
    </td>
    <td>
      <a href="axis2-admin/globalModules">Globally Engaged Modules</a>
    </td>
 </tr>
 <tr>
    <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
    </td>
    <td >
      <a href="axis2-admin/listPhases">Available Phases</a>
    </td>
 </tr>
  <tr>
     <td colspan="2" >
       <b>Execution Chains</b>
     </td>
  </tr>
   <tr>
       <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
       </td>
       <td>
         <a href="axis2-admin/viewGlobalHandlers">Global Chains</a>
       </td>
    </tr>
    <tr>
       <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
       </td>
       <td>
         <a href="axis2-admin/selectService">Operation Specific Chains</a>
       </td>
    </tr>
    <tr>
     <td colspan="2" >
       <b>Engage Module</b>
     </td>
  </tr>
   <tr>
       <td>
        &nbsp;&nbsp;&nbsp;&nbsp;
       </td>
       <td>
         <a href="axis2-admin/engagingglobally">For all Services</a>
       </td>
    </tr>
     <tr>
        <td>
         &nbsp;&nbsp;&nbsp;&nbsp;
        </td>
        <td>
          <a href="axis2-admin/engageToServiceGroup">For a Service Group</a>
        </td>
     </tr>


    <tr>
       <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
       </td>
       <td>
         <a href="axis2-admin/engageToService">For a Service</a>
       </td>
    </tr>

     <tr>
       <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
       </td>
       <td>
         <a href="axis2-admin/listOperation">For an Operation</a>
       </td>
    </tr>

    <tr>
     <td colspan="2" >
       <b>Services</b>
     </td>
  </tr>
    <tr>
       <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
       </td>
       <td>
         <a href="axis2-admin/deactivateService">Deactivate Service</a>
       </td>
    </tr>
    <tr>
       <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
       </td>
       <td>
         <a href="axis2-admin/activateService">Activate Service</a>
       </td>
    </tr>
    <tr>
       <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
       </td>
       <td>
         <a href="axis2-admin/selectServiceParaEdit">Edit Parameters</a>
       </td>
    </tr>
    <tr>
     <td colspan="2" >
       <b>Contexts</b>
     </td>
    </tr>
    <tr>
       <td>
       &nbsp;&nbsp;&nbsp;&nbsp;
       </td>
       <td>
         <a href="axis2-admin/listContexts">View Hierarchy</a>
       </td>
    </tr>
</table>
