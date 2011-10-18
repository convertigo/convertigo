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

package com.twinsoft.convertigo.engine.plugins;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.AccessController;

import javax.xml.transform.TransformerException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import sun.security.action.GetPropertyAction;

import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.util.StringEx;

public class PobiXslUtils {
	
	public static String getNodeValue(Node node) {
		String value = null;
		short type = node.getNodeType();
		switch (type) {
			case Node.DOCUMENT_NODE:
			case Node.ELEMENT_NODE:
				value = "";
				if (node.hasChildNodes()) {
					NodeList list = node.getChildNodes();
					for (int i=0; i<list.getLength(); i++)
						value += getNodeValue(list.item(i));
				}
				break;
			case Node.ATTRIBUTE_NODE:
			case Node.TEXT_NODE:
				value = node.getNodeValue();
				break;
		}
		return value;
	}
	
	public static String displayDate(String s) {
		if (s == null) return "";
		
		String sDate = s;
		switch (s.length()) {
			case 2:
				sDate = s + " mois";
				break;
			case 4:
				break;
			case 5:
				if (s.substring(0, 1).equals("1")) {
					sDate = s.substring(0, 1) + " er trimestre " + s.substring(1);
				}
				else {
					sDate = s.substring(0, 1) + " eme trimestre " + s.substring(1);
				}
				break;
			case 6:
				sDate = s.substring(0, 2) + "/" + s.substring(2);
				break;
			case 8:
				sDate = s.substring(0, 2) + "/" + s.substring(2, 4) + "/" + s.substring(4);
				break;
			case 16:
				sDate = s.substring(0, 2) + "/" + s.substring(3, 5) + "/" + s.substring(6, 10);
				break;
			default:
				break;
		}
		return sDate;
	}
	
	public static String displayDate(NodeList nodeList) {
		try {
			if (nodeList.getLength() == 0) return "";
			
			String s = "";
			s = getNodeValue(nodeList.item(0));
			return displayDate(s);
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}

	public static String displayNumber(NodeList nodeList) {
		return displayNumber(nodeList, 0);
	}

	public static String displayNumber(NodeList nodeList, int iDec) {
		try {
			if (nodeList.getLength() == 0) return "";
			
			String sDec, sInt;
			String sNumber = getNodeValue(nodeList.item(0));

			if (sNumber.equals(""))
				return "";
			
			int iPointPos = sNumber.indexOf(',');
			if (iPointPos == -1) {
				if (iDec == 0) {
					return RFormatNb(sNumber.substring(0, sNumber.length()));
				}
				else {
					sNumber += ",";
					iPointPos = sNumber.indexOf(',');
				}
			}
			
			for (int iFormatNb = 0 ; iFormatNb < iDec ; iFormatNb++) {
				sNumber += '0';
			}
			
			sInt = sNumber.substring(0, iPointPos);
			sDec = sNumber.substring(iPointPos + 1, iPointPos + 1 + iDec);

			if ((iPointPos != -1) && (iDec == 0)) {
				sDec = sNumber.substring(iPointPos + 1);
				sDec = "," + sDec;
			}

			if (iDec > 0) {
				sDec = "," + sDec;
			}
			
			return RFormatNb(sInt) + sDec;
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}

	public static String displayNumberValue(String sValue,int iDec) {
		try {
			String sDec, sInt;
			String sNumber = new String(sValue);
			int iPointPos = sNumber.indexOf('.');
			if (iPointPos == -1)
			{
				if (iDec == 0)
				{
					return(RFormatNb(sNumber.substring(0,sNumber.length())));
				}
				else
				{
					sNumber += '.';
					iPointPos = sNumber.indexOf('.');
				}
			}
			
			for (int iFormatNb=0; iFormatNb<iDec; iFormatNb++)
			{
				sNumber += '0';		
			}
			sInt = sNumber.substring(0, iPointPos);
			sDec = sNumber.substring(iPointPos+1, iDec);
			if ((iPointPos!=-1) && (iDec == 0))
			{
				sDec = sNumber.substring(iPointPos+1, sNumber.length());
				sDec = "." + sDec;
			}
			if (iDec > 0)
			{
				sDec = "." + sDec;
			}
			return(RFormatNb(sInt) + sDec);
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}
	
	public static String RFormatNb(String sNumber) {
		int len = sNumber.length();
		if (len <= 3) {
			return sNumber;
		}
		else {
			String s1 = sNumber.substring(0, len - 3);
			String s2 = sNumber.substring(len - 3, len);
			return RFormatNb(s1) + " " + s2;
		}
	}
	
	public static String displayInLowerCase(NodeList nodeList) {
		try {
			if (nodeList.getLength() == 0) return "";
			
			String s = getNodeValue(nodeList.item(0));
			String r = s.charAt(0) + s.substring(1).toLowerCase();
			String tmp = r;
			
			for(int i = 0 ; i < s.length() ; i++) {
				if (s.substring(i, i+1).equals(" ") || s.substring(i, i+1).equals("-")) {
					tmp = tmp.substring(0, i + 1) + tmp.substring(i + 1, i + 2).toUpperCase() + tmp.substring(i + 2, tmp.length());
				}
			}
			return tmp;
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}
	
	public static String displaySiret(NodeList nodeList) {
		try {
			if (nodeList.getLength() == 0) return "";
			String s = getNodeValue(nodeList.item(0));
			return s.substring(0, 3) + ' ' + s.substring(3, 6) + ' ' +  s.substring(6, 9) + ' ' +  s.substring(9, 14); 
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}
	
	public static String displaySiren(NodeList nodeList) {
		try {
			if (nodeList.getLength() == 0) return "";
			
			String s = getNodeValue(nodeList.item(0));
			//if ((s == null) || (s.length() == 0)) return "";
			if (s.length() < 9) return s;
			return s.substring(0, 3) + " " + s.substring(3, 6) + " " +  s.substring(6, 9); 
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}
	
	
	public static String displayIdent(String ident) {
		try {
			if (ident == null) return "";
			if (ident.length() < 9) return "";
			
			Integer.parseInt(ident);
			return ident.substring(0, 3) + " " + ident.substring(3, 6) + " " + ident.substring(6, 9);
		}
		catch(NumberFormatException e) {
			return ident;
		}
	}
	
	public static String displayIdent2(String ident) {
		try {
			if (ident == null) return "";
			if (ident.length() < 9) return "";
			
			Integer.parseInt(ident);
			return ident.substring(0, 3) + " " + ident.substring(3, 6) + " " + ident.substring(6, 9);
		}
		catch(NumberFormatException e) {
			return ident;
		}
	}

   // ci-dessous function displayIdent2(identifiant)  - version Dotnet	
//	   if ( (identifiant == 'Recherche sur') || (identifiant == 'Search on') )  return identifiant;
//	   else
//	   {
//	     if (isNaN(identifiant))
//	     {
//	
//	        var s = displayIdent(identifiant);
//	        if (s == "") {
//	          return identifiant;
//	        }
//	        else
//	        {
//	          return s;      
//	        }
//	     }
//	     else
//	     {
//	        return identifiant.substring(0, 3) + ' ' + identifiant.substring(3, 6) + ' ' +  identifiant.substring(6, 9);
//	     }
//	   }
//	}
	
	public static String displayTeleph(NodeList nodeList){
		try {
			if (nodeList.getLength() == 0) return "";
			
			String s = getNodeValue(nodeList.item(0));
			return s.substring(0,2) + ' ' + s.substring(2,4) + ' ' +s.substring(4,6) + ' ' +s.substring(6,8) + ' ' + s.substring(8,10);
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}
	
	public static String displayUnit(NodeList nodeList) {
		return displayUnit(nodeList, false);
	}
	
	public static String displayUnit(NodeList nodeList, int indic) {
		return displayUnit(nodeList, (indic == 0 ? true : false));
	}
	
	public static String displayUnit(NodeList nodeList, boolean indic) {
		try {
			if (nodeList.getLength() == 0) return "";
			
			String s = getNodeValue(nodeList.item(0));
			if (indic) {
				if ("U".equals(s)) return " ";
				if ("k".equals(s)) return " k";
				if ("K".equals(s)) return " k";
				if ("M".equals(s)) return " M";
				if ("G".equals(s)) return " G";				
			}
			else {
				if ("U".equals(s)) return "";
				if ("k".equals(s)) return "milliers d'";
				if ("K".equals(s)) return "milliers d'";
				if ("M".equals(s)) return "millions d'";
				if ("G".equals(s)) return "milliards d'";
			}
			return "";
			
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}
	
	public static String replaceSimpleQuote(NodeList nodeList) {
		try {
			if (nodeList.getLength() == 0) return "";

			String s = getNodeValue(nodeList.item(0));
			s = s.replace('\'', '_');
			s = s.replace(' ', '_');
			return s;
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}
	
	public static String escapeString(NodeList nodeList) {
		try {
			if (nodeList.getLength() == 0) return "";
			
			String s = getNodeValue(nodeList.item(0));
			if (s != null) {
				s = urlEncode(s);
				s = s.replace('+', ' ');
				s = s.replaceAll("%27", "\"");
				return s;
			}
			return "";
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}
	
	public static String escapeString(String string) {
		return urlEncode(string).replace('+', ' ');
	}
	
	public static String urlEncode(String string) {
		try {
			return URLEncoder.encode(string, "UTF-8").replace('+', ' ');
		} catch (UnsupportedEncodingException e) {
			// The system should always have the platform default
			return null;
		}
	}
	
	public static String getFibenBackUrl(String contextId, String transactionId) {
		return XslUtils.getContextVariableValue(contextId,"backurl");
	}

	public static String getFibenNextUrl(String contextId, String transactionId) {
		String nextUrl = "";
		
		String backUrl = getFibenBackUrl(contextId, transactionId);
		String id = XslUtils.getContextVariableValue(contextId, "id");
		String cdguichet = XslUtils.getContextVariableValue(contextId, "cdguichet");
		String cdbanque = XslUtils.getContextVariableValue(contextId, "cdbanque");
		String RefClient = XslUtils.getContextVariableValue(contextId, "RefClient");
		String denom = XslUtils.getContextVariableValue(contextId, "denom");
		
		String Code = XslUtils.getContextVariableValue(contextId, "Code");
		String Type = XslUtils.getContextVariableValue(contextId, "Type");
		String RechEt = XslUtils.getContextVariableValue(contextId, "RechEt");
		String SousSect = XslUtils.getContextVariableValue(contextId, "SousSect");
		String Classe = XslUtils.getContextVariableValue(contextId, "Classe");
		
		String nextModules = XslUtils.getContextVariableValue(contextId, "nextModules");
		String nextIds = XslUtils.getContextVariableValue(contextId, "nextIds");
		
		if ((nextModules != null) && (!nextModules.equals(""))) {
			String nextModule = escapeString(nextModules.substring(0,2));
			nextUrl += ".cxml?"+Parameter.Transaction.getName()+"=" + ((!nextModule.equals("vb") && !nextModule.equals("sc") && !nextModule.equals("de") && !nextModule.equals("an")) ? "m":"") + nextModule;
			nextUrl += "&id=" + escapeString(id);
			nextUrl += "&cdguichet=" + escapeString(cdguichet);
			nextUrl += "&cdbanque=" + escapeString(cdbanque);
			nextUrl += "&RefClient=" + escapeString(RefClient);
			nextUrl += "&backurl=" + escapeString(backUrl);
			if (transactionId.equals("m07")) {
				nextUrl += "&denom=" + escapeString(denom);
				nextUrl += "&Code=" + escapeString(Code);
				nextUrl += "&Type=" + escapeString(Type);
				nextUrl += "&RechEt=" + escapeString(RechEt);
				nextUrl += "&SousSect=" + escapeString(SousSect);
				nextUrl += "&Classe=" + escapeString(Classe);
			}
			int size = nextModules.length();
			if (size > 2)
				nextUrl = nextUrl + "&nextModules=" + escapeString(nextModules.substring(3, size));
		}
		else {
			if ((nextIds != null) && (!nextIds.equals(""))) {
				int index = nextIds.indexOf(' ');
				String nextId = escapeString(index == -1 ? nextIds : nextIds.substring(0, index));
				nextUrl += ".cxml?"+Parameter.Transaction.getName()+"=" + transactionId;
				nextUrl += "&id=" + escapeString(nextId);
				nextUrl += "&cdguichet=" + escapeString(cdguichet);
				nextUrl += "&cdbanque=" + escapeString(cdbanque);
				nextUrl += "&RefClient=" + escapeString(RefClient);
				nextUrl += "&backurl=" + escapeString(backUrl);
				if (transactionId.equals("m07")) {
					nextUrl += "&denom=" + escapeString(denom);
					nextUrl += "&Code=" + escapeString(Code);
					nextUrl += "&Type=" + escapeString(Type);
					nextUrl += "&RechEt=" + escapeString(RechEt);
					nextUrl += "&SousSect=" + escapeString(SousSect);
					nextUrl += "&Classe=" + escapeString(Classe);
				}
				if (index != -1)
					nextUrl = nextUrl + "&nextIds=" + escapeString(nextIds.substring(index + 1));
			}
		}
		return nextUrl;
	}
	
	public static String makeUrlSuivi(String backurl) {
		if (backurl == null) return "";
		
		StringEx sx = new StringEx(backurl);
		sx.replaceAll("UserFiben", "FIBENGESTSUIVI");
		sx.replaceAll("FIBENMULTMOD", "FIBENGESTSUIVI");
		sx.replaceAll("FIBENMULTIDENT", "FIBENGESTSUIVI");
		return sx.toString();
	}

	
	public static String displayDateOrg(NodeList nodeList) {
		try {
			if (nodeList.getLength() == 0) return "";
			
			String s = getNodeValue(nodeList.item(0));
			switch (s.length()) {
				case 8 :
						return s.substring(0, 2) + "/" + s.substring(2, 4) + "/" + s.substring(6);
				default :
						return s.substring(0);
			}
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}
	
	public static String displayDateForGraph(NodeList nodeList) {
		try {
			if (nodeList.getLength() == 0) return "";
			
			String s = getNodeValue(nodeList.item(0));
			return s.substring(0, 2) + s.substring(3, 5) + s.substring(6, 10);
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}
	
	public static String isFrenchLocali(NodeList nodeList) {
		try {
			if (nodeList.getLength() == 0) return "";
			
			String locali = getNodeValue(nodeList.item(0));
			if (locali == null) locali = ((Text) nodeList.item(0).getFirstChild()).getNodeValue();
			String cp = locali.substring(0, 2);
			if ((cp.compareTo("" + 96) < -1) || (cp.equals("MC")) || (cp.equals("2A")) || (cp.equals("2B"))) {
				return "true";
			}
			else {
				return "false";
			}
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}

	public static String genDatesList(NodeList donmod) {
		try {
			String val, temp;
			int tot = donmod.getLength();
			if (tot == 0) return "";
			
			String[] list = new String[tot];
			int i, j;
			Node node;
			NamedNodeMap attributes;
			boolean isDO = false;
			
			for (i = 0 ; i < tot ; i++) {
				node = donmod.item(i);
				isDO = node.getNodeName().equalsIgnoreCase("DONMODDO");
				attributes = node.getAttributes();
				temp = attributes.getNamedItem(isDO ? "DATCLOTV":"DATCLOT").getNodeValue();
				list[i] = temp.substring(2, 6) + temp.substring(0, 2);
				temp = attributes.getNamedItem(isDO ? "DUREEEXV":"DUREEEX").getNodeValue();
				list[i] = list[i] + temp;
			}
			
			for (i = 0 ; i < tot ; i++) {
				for (j = 0 ; j < tot - 1 ; j++) {
					if (list[j].compareTo(list[j + 1]) > 0) {
						val = list[j];
						list[j] = list[j + 1];
						list[j + 1] = val;
					}
				}
			}
			
			val = "";
			for (i = 0 ; i < tot - 1 ; i++) {
				if (list[i].compareTo(list[i + 1]) != 0) {
					val += list[i].substring(4, 6) + list[i].substring(0, 4) + list[i].substring(6, 8);
				}
			}
			val += list[i].substring(4, 6) + list[i].substring(0,4) + list[i].substring(6, 8); 

			return val;
		}
		catch(Exception e) {
			return "erreur:" + e.getMessage();
		}
	}
	
	public static String genDates(String DatesList1) {
		if (DatesList1 == null) return "";

		String html = "";
		for (int i = 0 ; i < DatesList1.length() ; i = i + 8) {
			html += "<td class=\"clGrasFond\" align=\"center\">" + DatesList1.substring(i, i + 2) + "/" + DatesList1.substring(i + 2, i + 6) + "</td>";
		}
		return html;
	}
	
	public static String genDurees(String DatesList1) {
		String html = "";
		for (int i = 0 ; i < DatesList1.length() ; i = i + 8) {
			html += "<td class=\"clTexte\" align=\"center\">" + DatesList1.substring(i + 6, i + 8) + " mois</td>"; 
		}
		return html;
	}

	public static String genModule(String DatesList1, NodeList module, String classe) throws TransformerException {
		Node oElem;
		oElem = module.item(0);
		if (oElem == null) {
			oElem = module.item(0);
		}

		String html = "";
		boolean bResult;
		
		for (int i = 0 ; i < DatesList1.length() ; i = i + 8) {
			try {
				if (oElem == null) {
					html += "<td class=\"" + classe + "\" align=\"center\">?</td>";
				}
				else {
					bResult = nodeCanBeSelected(oElem, DatesList1.substring(i, i + 6), DatesList1.substring(i + 6, i + 8));
					html += "<td class=\"" + classe + "\" align=\"center\">" + (bResult ? "X" : "&#160;") + "</td>";
				}
			}
			catch(Exception e) {
				html += "<td class=\"" + classe + "\" align=\"center\">?</td>";
			}
		}
		return html;
	}

	public static boolean nodeCanBeSelected(Node node, String datclot, String dureeex) {
		NodeList childNodes = node.getChildNodes();
		int len = childNodes.getLength();
		Node tnode;
		NamedNodeMap attributes;
		
		String tnodeName;
		String attrDATCLOT, attrDUREEEX;
		
		for (int i = 0 ; i < len ; i++) {
			tnode = childNodes.item(i);
			tnodeName = tnode.getNodeName();
			if ("DONMOD".equalsIgnoreCase(tnodeName)) {
				attributes = tnode.getAttributes();
				attrDATCLOT = attributes.getNamedItem("DATCLOT").getNodeValue();
				attrDUREEEX = attributes.getNamedItem("DUREEEX").getNodeValue();
				if (datclot.equals(attrDATCLOT) && dureeex.equals(attrDUREEEX)) {
					return true;
				}
			}
			else if ("DONMODDO".equalsIgnoreCase(tnodeName)) {
				attributes = tnode.getAttributes();
				attrDATCLOT = attributes.getNamedItem("DATCLOTV").getNodeValue();
				attrDUREEEX = attributes.getNamedItem("DUREEEXV").getNodeValue();
				if (datclot.equals(attrDATCLOT) && dureeex.equals(attrDUREEEX)) {
					return true;
				}
			}
		}
		return false;
	}
}
