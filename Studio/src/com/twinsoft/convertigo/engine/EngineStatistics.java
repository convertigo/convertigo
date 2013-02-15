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

package com.twinsoft.convertigo.engine;

import java.util.Formatter;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.util.*;
 
public class EngineStatistics extends Statistics {
    public static final String GET_CURRENT_SCREEN_CLASS = "GetCurrentScreenClass";
    public static final String GET_JAVELIN_OBJECT = "GetJavelinObject";
    public static final String APPLY_USER_REQUEST = "ApplyUserRequest";
    public static final String HTTP_CONNECT = "HttpConnect";
    public static final String GET_XUL_DOCUMENT = "GetXulDocument";
    public static final String APPLY_BLOCK_FACTORY = "ApplyBlockFactory";
    public static final String APPLY_EXTRACTION_RULES = "ApplyExtractionRules";
    public static final String APPLY_SCREENCLASS_HANDLERS = "ApplyScreenClassHandlers";
    public static final String WORKER_THREAD_START = "WorkerThreadStart";
    public static final String WAIT_HTML_TRIGGER = "WaitHtmlTrigger";
    public static final String EXECUTE_SEQUENCE_STEPS = "ExecuteSequenceSteps";
    public static final String EXECUTE_SEQUENCE_CALLS = "ExecuteSequenceCalls";
    public static final String GENERATE_DOM = "GenerateDOM";
    public static final String OTHERS = "(others)";
    public static final String GET_DOCUMENT = "GetDocument";
    public static final String XSLT = "XSLT";
    public static final String REQUEST = "Request";
    public static final String HOST = "Host";
    public static final String CONVERTIGO = "Convertigo";
    
	//Statistics variable
	private long dgcsc,adgcsc,dgjo,adgjo,daur,adaur,dhc,adhc,dgxd,adgxd,dabf,adabf,daer,adaer,dasch,adasch,dwht,adwht,dwts,adwts,dess,adess,desc,adesc,dgdom,
		adgdom,dgd,adgd,dxslt,adxslt,dt,adt,dothers,adothers,dh,adh,dc,adc;
	
	//Percentage variable
	private long adt2,ph,aph,pc,apc,pxslt,apxslt;
	
	public String printStatistics() {
		getStatsData();
		getPercentageData();

		StringBuilder stats = new StringBuilder(1024);
		Formatter formatter = new Formatter(stats);

		formatter
				.format(" ====================================================\n");
		formatter
				.format("|          Task            |   Current  |   Average  |\n");
		formatter
				.format("|==========================|============|============|\n");
		if (dwts != -1)
			formatter.format("| %-24s | %7d ms | %7d ms |\n",
					WORKER_THREAD_START, dwts, adwts);
		if (dhc != -1)
			formatter.format("| %-24s | %7d ms | %7d ms |\n", HTTP_CONNECT,
					dhc, adhc);
		if (dgxd != -1)
			formatter.format("| %-24s | %7d ms | %7d ms |\n", GET_XUL_DOCUMENT,
					dgxd, adgxd);
		if (dwht != -1)
			formatter.format("| %-24s | %7d ms | %7d ms |\n",
					WAIT_HTML_TRIGGER, dwht, adwht);
		if (dgjo != -1)
			formatter.format("| %-24s | %7d ms | %7d ms |\n",
					GET_JAVELIN_OBJECT, dgjo, adgjo);
		if (dgcsc != -1)
			formatter.format("| %-24s | %7d ms | %7d ms |\n",
					GET_CURRENT_SCREEN_CLASS, dgcsc, adgcsc);
		if (daur != -1)
			formatter.format("| %-24s | %7d ms | %7d ms |\n",
					APPLY_USER_REQUEST, daur, adaur);
		if (dabf != -1)
			formatter.format("| %-24s | %7d ms | %7d ms |\n",
					APPLY_BLOCK_FACTORY, dabf, adabf);
		if (daer != -1)
			formatter.format("| %-24s | %7d ms | %7d ms |\n",
					APPLY_EXTRACTION_RULES, daer, adaer);
		if (dasch != -1)
			formatter.format("| %-24s | %7d ms | %7d ms |\n",
					APPLY_SCREENCLASS_HANDLERS, dasch, adasch);
		if (dess != -1)
			formatter.format("| %-24s | %7d ms | %7d ms |\n",
					EXECUTE_SEQUENCE_STEPS, dess, adess);
		if (desc != -1)
			formatter.format("| %-24s | %7d ms | %7d ms |\n",
					EXECUTE_SEQUENCE_CALLS, desc, adesc);
		if (dgdom != -1)
			formatter.format("| %-24s | %7d ms | %7d ms |\n", GENERATE_DOM,
					dgdom, adgdom);
		formatter.format("| %-24s | %7d ms | %7d ms |\n", OTHERS, dothers,
				adothers);
		formatter
				.format("|                          |============|~~~~~~~~~~~~|\n");
		formatter.format("| %-24s | %7d ms | %7d ms |\n", GET_DOCUMENT, dgd,
				adgd);
		formatter
				.format(" ==================================================== \n\n");

		formatter
				.format(" ======================================================== \n");
		formatter
				.format("| Request        |      Current      |      Average      |\n");
		formatter
				.format("|================|===================|===================|\n");
		formatter.format(
				"| Host           | %7d ms (%3d%%) | %7d ms (%3d%%) |\n", dh,
				ph, adh, aph);
		formatter.format(
				"| Convertigo     | %7d ms (%3d%%) | %7d ms (%3d%%) |\n", dc,
				pc, adc, apc);
		if (dxslt != -1)
			formatter.format(
					"| XSLT           | %7d ms (%3d%%) | %7d ms (%3d%%) |\n",
					dxslt, pxslt, adxslt, apxslt);
		formatter
				.format("|                |===================|~~~~~~~~~~~~~~~~~~~|\n");
		formatter
				.format("| Total          | %7d ms        | %7d ms        |\n",
						dt, adt);
		formatter
				.format(" ========================================================");

		formatter.close();
		
		return stats.toString();
	}

	public String addWhiteSpaces(String msg, boolean bRight, int maxLength) {
		int len = msg.length();
		for (int i = len; i < maxLength; i++) {
			if (bRight) {
				msg += " ";
			} else {
				msg = " " + msg;
			}
		}
		return msg;
	}

	public void printStatisticsXML(Document document) {
		getStatsData();
		
		try {

			Element statistics = document.createElement("statistics");
				Element task = document.createElement("task");
					Element wts = document.createElement(WORKER_THREAD_START.toLowerCase());
					if (dwts != -1) {
						wts.setAttribute("average", adwts + "");
						wts.setAttribute("current", dwts + "");
						task.appendChild(wts);
					}
					Element httpc = document.createElement(HTTP_CONNECT.toLowerCase());
					if (dhc != -1) {
						httpc.setAttribute("average", adhc + "");
						httpc.setAttribute("current", dhc + "");
						task.appendChild(httpc);
					}
					Element gxd = document
							.createElement(GET_XUL_DOCUMENT.toLowerCase());
					if (dgxd != -1) {
						gxd.setAttribute("average", adgxd + "");
						gxd.setAttribute("current", dgxd + "");
						task.appendChild(gxd);
					}
					Element wht = document.createElement(WAIT_HTML_TRIGGER.toLowerCase());
					if (dwht != -1) {
						wht.setAttribute("average", adwht + "");
						wht.setAttribute("current", dwht + "");
						task.appendChild(wht);
					}
					Element gjo = document.createElement(GET_JAVELIN_OBJECT.toLowerCase());
					if (dgjo != -1) {
						gjo.setAttribute("average", adgjo + "");
						gjo.setAttribute("current", dgjo + "");
						task.appendChild(gjo);
					}
					Element gcsc = document.createElement(GET_CURRENT_SCREEN_CLASS.toLowerCase());
					if (dgcsc != -1) {
						gcsc.setAttribute("average", adgcsc + "");
						gcsc.setAttribute("current", dgcsc + "");
						task.appendChild(gcsc);
					}
					Element aur = document.createElement(APPLY_USER_REQUEST.toLowerCase());
					if (daur != -1) {
						aur.setAttribute("average", adaur + "");
						aur.setAttribute("current", daur + "");
						task.appendChild(aur);
					}
					Element abf = document.createElement(APPLY_BLOCK_FACTORY.toLowerCase());
					if (dabf != -1) {
						abf.setAttribute("average", adabf + "");
						abf.setAttribute("current", dabf + "");
						task.appendChild(abf);
					}
					Element aer = document.createElement(APPLY_EXTRACTION_RULES.toLowerCase());
					if (daer != -1) {
						aer.setAttribute("average", adaer + "");
						aer.setAttribute("current", daer + "");
						task.appendChild(aer);
					}
					Element ash = document.createElement(APPLY_SCREENCLASS_HANDLERS.toLowerCase());
					if (dasch != -1) {
						ash.setAttribute("average", adasch + "");
						ash.setAttribute("current", dasch + "");
						task.appendChild(ash);
					}
					Element ess = document.createElement(EXECUTE_SEQUENCE_STEPS.toLowerCase());
					if (dess != -1) {
						ess.setAttribute("average", adess + "");
						ess.setAttribute("current", dess + "");
						task.appendChild(ess);
					}
					Element esc = document.createElement(EXECUTE_SEQUENCE_CALLS.toLowerCase());
					if (desc != -1) {
						esc.setAttribute("average", adesc + "");
						esc.setAttribute("current", desc + "");
						task.appendChild(esc);
					}
					Element gdom = document.createElement(GENERATE_DOM.toLowerCase());
					if (dgdom != -1) {
						gdom.setAttribute("average", adgdom + "");
						gdom.setAttribute("current", dgdom + "");
						task.appendChild(gdom);
					}
					Element oth = document.createElement("others");
					oth.setAttribute("average", adothers + "");
					oth.setAttribute("current", dothers + "");
					task.appendChild(oth);
		
					Element gdoc = document.createElement(GET_DOCUMENT.toLowerCase());
					gdoc.setAttribute("average", adgd + "");
					gdoc.setAttribute("current", dgd + "");
					task.appendChild(gdoc);
					statistics.appendChild(task);
				//End of task
				
				//Beginning of request
				Element request = document.createElement("request");
					Element host = document.createElement("host");
					host.setAttribute("average", adh + "");
					host.setAttribute("current", dh + "");
					request.appendChild(host);
		
					Element econvertigo = document.createElement("convertigo");
					econvertigo.setAttribute("average", adc + "");
					econvertigo.setAttribute("current", dc + "");
					request.appendChild(econvertigo);
		
					Element exslt = document.createElement("xsltstats");
					if (dxslt != -1) {
						exslt.setAttribute("average", adxslt + "");
						exslt.setAttribute("current", dxslt + "");
						request.appendChild(exslt);
					}
					Element etotal = document.createElement("totalstat");
					etotal.setAttribute("average", adt + "");
					etotal.setAttribute("current", dt + "");
				request.appendChild(etotal);
				
			statistics.appendChild(request);
			//End of request
			
			//Add Element statistics into Document XML
			document.getDocumentElement().appendChild(statistics);

		} catch (Exception e) {
			Engine.logEngine.error("Unable to append XML statistics into the generated document!", e);
		}
	}
	
	private void getStatsData(){
		dgcsc = getLatestDuration(GET_CURRENT_SCREEN_CLASS);
		adgcsc = getAverage(GET_CURRENT_SCREEN_CLASS);
		dgjo = getLatestDuration(GET_JAVELIN_OBJECT);
		adgjo = getAverage(GET_JAVELIN_OBJECT);
		daur = getLatestDuration(APPLY_USER_REQUEST);
		adaur = getAverage(APPLY_USER_REQUEST);
		dhc = getLatestDuration(HTTP_CONNECT);
		adhc = getAverage(HTTP_CONNECT);
		dgxd = getLatestDuration(GET_XUL_DOCUMENT);
		adgxd = getAverage(GET_XUL_DOCUMENT);
		dabf = getLatestDuration(APPLY_BLOCK_FACTORY);
		adabf = getAverage(APPLY_BLOCK_FACTORY);
		daer = getLatestDuration(APPLY_EXTRACTION_RULES);
		adaer = getAverage(APPLY_EXTRACTION_RULES);
		dasch = getLatestDuration(APPLY_SCREENCLASS_HANDLERS);
		adasch = getAverage(APPLY_SCREENCLASS_HANDLERS);
		dwht = getLatestDuration(WAIT_HTML_TRIGGER);
		adwht = getAverage(WAIT_HTML_TRIGGER);
		dwts = getLatestDuration(WORKER_THREAD_START);
		adwts = getAverage(WORKER_THREAD_START);
		dess = getLatestDuration(EXECUTE_SEQUENCE_STEPS);
		adess = getAverage(EXECUTE_SEQUENCE_STEPS);
		desc = getLatestDuration(EXECUTE_SEQUENCE_CALLS);
		adesc = getAverage(EXECUTE_SEQUENCE_CALLS);
		dgdom = getLatestDuration(GENERATE_DOM);
		adgdom = getAverage(GENERATE_DOM);
		dgd = getLatestDuration(GET_DOCUMENT);
		adgd = getAverage(GET_DOCUMENT);

		dxslt = getLatestDuration(XSLT);
		adxslt = getAverage(XSLT);

		dt = getLatestDuration(REQUEST);

		if (dt == -1) {
			Engine.logEngine
					.warn("(Statistics) Negative measure detected; recomputing done!");
			dt = dgd + 27;
		}
		adt = getAverage(REQUEST);

		// Compute others duration
		dothers = dgd;
		if (dgcsc != -1)
			dothers -= dgcsc;
		if (dgjo != -1)
			dothers -= dgjo;
		if (daur != -1)
			dothers -= daur;
		if (dhc != -1)
			dothers -= dhc;
		if (dgxd != -1)
			dothers -= dgxd;
		if (dabf != -1)
			dothers -= dabf;
		if (daer != -1)
			dothers -= daer;
		if (dasch != -1)
			dothers -= dasch;
		if (dwht != -1)
			dothers -= dwht;
		if (dess != -1)
			dothers -= dess;
		if (desc != -1)
			dothers -= desc;
		if (dgdom != -1)
			dothers -= dgdom;
		if (dwts != -1)
			dothers -= dwts;

		add(OTHERS, dothers);
		adothers = getAverage(OTHERS);

		// Compute host duration
		dh = 0;

		// Legacy connector
		if (dgjo != -1)
			dh += dgjo;
		if (daur != -1)
			dh += daur;
		if (dasch != -1)
			dh += dasch;

		// HTML connector
		if (dhc != -1)
			dh += dhc;
		if (dwht != -1)
			dh += dwht;

		// Other
		if (desc != -1)
			dh += desc;

		add(HOST, dh);
		adh = getAverage(HOST);

		dc = dt;
		if (dh > 0)
			dc -= dh;
		if (dxslt > 0)
			dc -= dxslt;
		add(CONVERTIGO, dc);
		adc = getAverage(CONVERTIGO);
	}
	
	private void getPercentageData(){
		adt2 = adh + adc + adxslt;
		ph = Math.round((float) dh * 100 / (float) dt);
		aph = Math.round((float) adh * 100 / (float) adt2);
		pc = Math.round((float) dc * 100 / (float) dt);
		apc = Math.round((float) adc * 100 / (float) adt2);
		pxslt = Math.round((float) dxslt * 100 / (float) dt);
		apxslt = Math.round((float) adxslt * 100 / (float) adt2);
	}

	public static Object addStatisticsAsXML(Context context, Object result) {
		if (result != null && context.requestedObject != null && context.requestedObject.getAddStatistics()) {
			if (result instanceof Document) {
				Document document = (Document) result;
				context.statistics.printStatisticsXML(document);
			}
		}
		return result;
	}
	
	public void printStatisticsSOAP(SOAPMessage soapmessage){
		getStatsData();
		
		SOAPMessage requestMessage = soapmessage;
		try{
			SOAPPart sp = requestMessage.getSOAPPart();
			SOAPEnvelope se = sp.getEnvelope();
			SOAPBody sb = se.getBody();
			SOAPElement response = (SOAPElement) sb.getFirstChild();
			
			SOAPElement statistics = response.addChildElement("statistics");	
				SOAPElement task = statistics.addChildElement("task");
					if (dwts != -1) {
						SOAPElement wts = task.addChildElement(WORKER_THREAD_START.toLowerCase());
						wts.setAttribute("average", adwts + "");
						wts.setAttribute("current", dwts + "");
					}
					if (dhc != -1) {
						SOAPElement httpc = task.addChildElement(HTTP_CONNECT.toLowerCase());
						httpc.setAttribute("average", adhc + "");
						httpc.setAttribute("current", dhc + "");
					}
					if (dgxd != -1) {
						SOAPElement gxd = task.addChildElement(GET_XUL_DOCUMENT.toLowerCase());
						gxd.setAttribute("average", adgxd + "");
						gxd.setAttribute("current", dgxd + "");
					}
					if (dwht != -1) {
						SOAPElement wht = task.addChildElement(WAIT_HTML_TRIGGER.toLowerCase());
						wht.setAttribute("average", adwht + "");
						wht.setAttribute("current", dwht + "");
					}
					if (dgjo != -1) {
						SOAPElement gjo = task.addChildElement(GET_JAVELIN_OBJECT.toLowerCase());
						gjo.setAttribute("average", adgjo + "");
						gjo.setAttribute("current", dgjo + "");
					}
					if (dgcsc != -1) {
						SOAPElement gcsc = task.addChildElement(GET_CURRENT_SCREEN_CLASS.toLowerCase());
						gcsc.setAttribute("average", adgcsc + "");
						gcsc.setAttribute("current", dgcsc + "");
					}
					if (daur != -1) {
						SOAPElement aur = task.addChildElement(APPLY_USER_REQUEST.toLowerCase());
						aur.setAttribute("average", adaur + "");
						aur.setAttribute("current", daur + "");
					}
					if (dabf != -1) {
						SOAPElement abf = task.addChildElement(APPLY_BLOCK_FACTORY.toLowerCase());
						abf.setAttribute("average", adabf + "");
						abf.setAttribute("current", dabf + "");
					}
					if (daer != -1) {
						SOAPElement aer = task.addChildElement(APPLY_EXTRACTION_RULES.toLowerCase());
						aer.setAttribute("average", adaer + "");
						aer.setAttribute("current", daer + "");
					}
					if (dasch != -1) {
						SOAPElement ash = task.addChildElement(APPLY_SCREENCLASS_HANDLERS.toLowerCase());
						ash.setAttribute("average", adasch + "");
						ash.setAttribute("current", dasch + "");
					}
					if (dess != -1) {
						SOAPElement ess = task.addChildElement(EXECUTE_SEQUENCE_STEPS.toLowerCase());
						ess.setAttribute("average", adess + "");
						ess.setAttribute("current", dess + "");
					}
					if (desc != -1) {
						SOAPElement esc = task.addChildElement(EXECUTE_SEQUENCE_CALLS.toLowerCase());
						esc.setAttribute("average", adesc + "");
						esc.setAttribute("current", desc + "");
					}
					if (dgdom != -1) {
						SOAPElement gdom = task.addChildElement(GENERATE_DOM.toLowerCase());
						gdom.setAttribute("average", adgdom + "");
						gdom.setAttribute("current", dgdom + "");
					}
					SOAPElement oth = task.addChildElement("others");
					oth.setAttribute("average", adothers + "");
					oth.setAttribute("current", dothers + "");
		
					SOAPElement gdoc = task.addChildElement(GET_DOCUMENT.toLowerCase());
					gdoc.setAttribute("average", adgd + "");
					gdoc.setAttribute("current", dgd + "");
					
				//Beginning of request
				SOAPElement request = statistics.addChildElement("request");
					Element host = request.addChildElement("host");
					host.setAttribute("average", adh + "");
					host.setAttribute("current", dh + "");
		
					SOAPElement econvertigo = request.addChildElement("convertigo");
					econvertigo.setAttribute("average", adc + "");
					econvertigo.setAttribute("current", dc + "");
		
					if (dxslt != -1) {
						SOAPElement exslt = request.addChildElement("xsltstats");
						exslt.setAttribute("average", adxslt + "");
						exslt.setAttribute("current", dxslt + "");
					}
					SOAPElement etotal = request.addChildElement("totalstat");
					etotal.setAttribute("average", adt + "");
					etotal.setAttribute("current", dt + "");

			
		}catch(Exception e){
			Engine.logEngine.error("Unable to append SOAP statistics into the generated message!", e);
		}
	}
}
