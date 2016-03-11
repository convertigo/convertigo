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

package com.twinsoft.convertigo.engine.admin.services.trace;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.scheduler.AbstractBase;
import com.twinsoft.convertigo.beans.scheduler.SchedulerXML;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.TracePlayerManager;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

@ServiceDefinition(
		name = "Create",
		roles = { Role.WEB_ADMIN, Role.TRACE_CONFIG },
		parameters = {},
		returnValue = ""
	)
public class Create extends XmlService {
	
	final String parametersPrefix = "parameters_";
	final String parametersSendPrefix = "parametersSend_";
	SchedulerXML schedulerXML = null;
	AbstractBase currentBase = null;
	AbstractBase currentEdited = null;
	
	Document document;
	Element rootElement=null;
	
	private boolean modify;

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		
		this.document = document;
		rootElement = document.getDocumentElement();
		    
        createElements(request);
	}
	
	
	long id_cpt = 0;
	TracePlayerManager tpm = Engine.theApp.tracePlayerManager;
	String message = "None operation selected";

	public String genId(){
		return "genid_"+id_cpt++;
	}

	private boolean validateTraceParameter(HttpServletRequest request) {
		if (request.getParameter("port") != null && request.getParameter("file") != null) {
			try {
				int port = Integer.parseInt(request.getParameter("port"));
				if (1024 < port && port < 65000) {
					if (!Engine.isCloudMode() || (15000 <= port && port < 16000)) {
						//String oldport = request.getParameter("modifytrace");
						if (!tpm.exists(port) || (modify)) { 
							if(modify) {
								message = "trace modify on port ";
							} else {		
								message = "trace added on port ";
							}
							message +=port;
							return true;
						} else {
							message = "This number of port already exists";
						}
					}else {
						message = "Choose a port number between 15000 and 15999";
					}
				} else {
					message = "Choose a port number between 1025 and 64999";
				}
			} catch(Exception e) {
				message = "The port must be a number";
			}
		} else {
			message = "You have to add a port number and a pathfile";
		}
		return false;
	}
	
	
	private void createElements(HttpServletRequest request){
		//TracePlayerManager.TraceConfig currentTrace = null;		
	
		if(request.getParameter("enableTrace") != null ||request.getParameter("disableTrace") != null ||request.getParameter("del") != null){
		
			String[] listSelected = request.getParameterValues("port");
			ArrayList<String> list=new ArrayList<String>();
			boolean first=true;
			
			if(listSelected != null){
				message="port(s) ";
				for(int i=0;i<listSelected.length;i++){
					if(tpm.exists(Integer.parseInt(listSelected[i]))){
						if(!first)
							message+=" ,";
						message+=listSelected[i];
						list.add(listSelected[i]);
						first=false;
					}
				}	
			}
			if(list.size()>0){
				if(request.getParameter("del") != null){ 
					tpm.delTraces(listSelected);
					tpm.disableTraces(listSelected);
					message+=" deleted";
				}				
				else if(request.getParameter("disableTrace") != null){ 
					tpm.disableTraces(listSelected);
					message+=" disabled";
				}	
				else if(request.getParameter("enableTrace") != null){	
					tpm.enableTraces(listSelected);	
					message+=" enabled";
				}	
			}
			else message="You have to select one/many valid trace(s)"; /** TODO: TRANSLATE */
		}
		else{
			modify=false;
			//if(request.getParameter("add") != null){
			modify=tpm.exists(Integer.parseInt(request.getParameter("port")));					
			
			if(validateTraceParameter(request)) 
				if(modify)
					tpm.modifyTrace(request.getParameter("port"), request.getParameter("port"), request.getParameter("file"), request.getParameter("enable"));
				else
					tpm.addTrace(request.getParameter("port"), request.getParameter("file"), request.getParameter("enable"));
			
		}
		ServiceUtils.addMessage(document,rootElement,message,"message");
	}
}
