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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.apache.xalan.xsltc.dom.SimpleResultTreeImpl;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.util.StringEx;

public class PobiXslUtils {
	
	public static final int cstNothing = 0;
	public static final int cstSiren = 1;
	public static final int cstBDFKey = 2;
	
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
	
	public static String escapeString(Object ob) {
		try {
			if (ob instanceof NodeList) {
				NodeList nodeList = (NodeList)ob;
			
				if (nodeList.getLength() == 0) return "";
				
				String s = getNodeValue(nodeList.item(0));
				if (s != null) {
					s = urlEncode(s);
					s = s.replace('+', ' ');
					s = s.replaceAll("%27", "\"");
					return s;
				}
			}
			else if (ob instanceof SimpleResultTreeImpl) {
				return escapeString(((SimpleResultTreeImpl)ob).getStringValue());
			}
			else if (ob instanceof String) {
				return escapeString((String)ob);
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
			return URLEncoder.encode(string, "ISO-8859-1").replace('+', ' ');
		} catch (UnsupportedEncodingException e) {
			// The system should always have the platform default
			return null;
		}
	}
	
	public static String getFibenBackUrl(String contextId, String transactionId) {
		return XslUtils.getContextVariableValue(contextId,"backurl");
	}

	public static String getResponseExpiryDate(String contextId, String module, String id) {
		String mod = module.startsWith("m") ? module.toUpperCase().substring(1):module.toUpperCase();
		String modules = "|"+XslUtils.getContextValue(contextId, "modules")+"|";
		String caches = "|"+XslUtils.getContextValue(contextId, "caches")+"|";
		try {
			if (module.equals("m27") || module.equals("m37")) {
				try {
					int iCheckIdent = iCheckIdentifiant(id);
					if (iCheckIdent == cstSiren) {
						if (module.equals("m27"))
							mod = "2S";
						if (module.equals("m37"))
							mod = "3S";
					}
					if (iCheckIdent == cstBDFKey) {
						if (module.equals("m27"))
							mod = "2D";
						if (module.equals("m37"))
							mod = "3D";
					}
				}
				catch (Exception e) {
				}
			}
			if (modules.indexOf("|"+mod+"|") != -1) {
				ArrayList<String> ar1 = new ArrayList<String>(Arrays.asList(modules.split("\\|",-1)));
				ArrayList<String> ar2 = new ArrayList<String>(Arrays.asList(caches.split("\\|",-1)));
				if (ar1.size() == ar2.size()) {
					int index = ar1.indexOf(mod);
					return ar2.get(index);
				}
			}
		} catch (Exception e) {}
		return "";
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
			boolean isNumberedModule = false;
			String nextModule = escapeString(nextModules.substring(0,2));
			try {
				Integer.parseInt(nextModule, 10);
				isNumberedModule = true;
			} catch (Exception e) {}
			String nextTransaction = (isNumberedModule ? "m":"") + nextModule;
			nextUrl += ".cxml?"+Parameter.Transaction.getName()+"=" + nextTransaction;
			String red = getResponseExpiryDate(contextId, nextTransaction, id);
			if (!red.equals(""))
				nextUrl += "&__responseExpiryDate=" + escapeString("absolute,"+red);
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
				String red = getResponseExpiryDate(contextId, transactionId, nextId);
				if (!red.equals(""))
					nextUrl += "&__responseExpiryDate=" + escapeString("absolute,"+red);				
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
	
	// fonction qui teste l'identifiant
	public static int iCheckIdentifiant(String sIdent) {
		int iCheckIdent = cstSiren; // on suppose que c'est un n°siren
		char sLetter1;
		if (!bCheckSirenNumber(sIdent)) { // ce n'est pas un n°siren
			if (!bCheckBirthDay(sIdent.substring(0,6))) { // erreur au niveau de la date de naissance
				iCheckIdent = cstNothing;
			}
			else {
				
				try {
					sLetter1 = sIdent.charAt(6);
					String nom = sIdent.substring(6);
					if ((isNum("" + sLetter1)) || (sLetter1 == ' ')) { // erreur au niveau de la premiere lettre
						iCheckIdent = cstNothing;
					}
					else {
						if (!bCheckBdfKeySuffix(nom)) { // erreur au niveau des chiffres
							iCheckIdent = cstNothing;
						}
						else {
							if (!bCheckBdfKeyBlank(nom)) { // erreur au niveau des blancs
								iCheckIdent = cstNothing;
							}
							else {
								iCheckIdent = cstBDFKey;
							}
						}
					}
				} catch (StringIndexOutOfBoundsException e) {
					iCheckIdent = cstNothing;
				}
			}
		}

		return iCheckIdent;
	}

	// fonction qui valide un code guichet
	public static boolean bCheckCdGuichet(String sCdGuichet) {
		try {		
			if (sCdGuichet.length() == 5) {
				Integer.parseInt(sCdGuichet);
				return true;
			}
		}
		catch(NumberFormatException e) {}
		
		return false;
	}
	
	private static boolean isNaN(String s) {
		try {		
			Integer.parseInt(s);
		}
		catch(NumberFormatException e) {
			return true;
		}
		return false;
	}

	private static boolean isNum(String s) {
		try {		
			Integer.parseInt(s);
		}
		catch(NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	// validation cleBDF pour FICP
	public static boolean validClePP(String sKey) {
		String regExp = "^\\d{6}[A-Z]+$";
		String key = sKey.trim().toUpperCase();
		
		// Test du format 6 numeriques suivis d'au moins 1 caract. MAJ
		if (!Pattern.matches(regExp, key))
			return false;
		
		int day = Integer.parseInt(key.substring(0, 2), 10);
		int month = Integer.parseInt(key.substring(2, 4), 10);
		int year = Integer.parseInt(key.substring(4, 6), 10) + 1900;
		
		//Test des cas personnes nees a l'etranger 0000AA ou 00MMAA
		if ( !(day == 0 && month == 0 ) 
			&& ( (month < 1 || month > 12) || (day < 0 || day > 31)
				|| (year < 1900 || year > 2050)
				|| ((month == 4 || month == 6 || month == 9 || month == 11) && day == 31 )))
		{
			return false;
		}
		else if (month == 2 && ((day > 29) || ((day == 29 &&
					(year%4 != 0 || (year%4 == 0 &&
					(year%100 == 0 && year%400 != 0 )))))))
		{
			return false;
		}

		return true;
	}
	
	// public static qui verifie qu'on a bien un numéro SIREN
	private static boolean bCheckSirenNumber(String sString) {
		int iLengthIdent = sString.length(); // longueur de l'identifiant
		boolean bIsSiren = false;
		
		if ((iLengthIdent == 9) && (sString.indexOf(" ") == -1) && (isNum(sString))) {
			if (sString.substring(0, 2).equals("20")) {
				bIsSiren = true;
			}
			else {
				if (nGetAlgoSiren(sString) % 10 == 0) { // multiple de 10 ?
					bIsSiren = true ;
				}
			}
		}
		return bIsSiren;
	}

	// function qui retourne le resultat de l'algorithme siren qui doit être
	// multiple de 10
	private static int nGetAlgoSiren(String sString) {
		int iLengthString = sString.length();
		int nAlgoSiren = 0;
		
		if (iLengthString == 9) {
			nAlgoSiren = 0;
		}

		if (sString.length() != 0) {
			int nTemp;
			if (iLengthString % 2 == 0) { // rang pair
				nTemp = 2 * Integer.parseInt(sString.substring(0, 1));
				try {
					String sTemp = Integer.toString(nTemp);
					if (sTemp.length() == 2) {	
						nTemp = Integer.parseInt(sTemp.substring(0, 1)) + Integer.parseInt(sTemp.substring(1, 2));
					}
				}
				catch(NumberFormatException e) {}
				nAlgoSiren += nTemp + nGetAlgoSiren(sString.substring(1));
			}
			else { // rang impair
				try {
					nAlgoSiren += Integer.parseInt(sString.substring(0, 1)) + nGetAlgoSiren(sString.substring(1));
				}
				catch(NumberFormatException e) {}
			}
		}
		return nAlgoSiren;
	}

	private static boolean bCheckDate(int nDay, int nMonth, int nYear) {
		Date ladate = new Date(nYear, nMonth-1, nDay);
		return ((nYear == (ladate.getYear())) && (nMonth == (ladate.getMonth() + 1)) && (nDay == ladate.getDate()));
	}
	
	// public static qui vérifie la validité de la date
	private static boolean bCheckBirthDay(String sDate) {
		boolean bIsDate = false;

		// exception
		if (sDate.equals("000000")) {
			bIsDate = true;
		}
		else {
			try {
				// exception encore
				if ((sDate.substring(0, 2).equals("00"))
						&& (Integer.parseInt(sDate.substring(2, 4)) >= 0)
						&& (Integer.parseInt(sDate.substring(2, 4)) < 13)
						&& (Integer.parseInt(sDate.substring(4, 6)) >= 0)
						&& (Integer.parseInt(sDate.substring(4, 6)) < 100)) {
					bIsDate = true;
				}
				else {
					bIsDate = bCheckDate(
							Integer.parseInt(sDate.substring(0, 2)),
							Integer.parseInt(sDate.substring(2, 4)),
							Integer.parseInt("20" + sDate.substring(4, 6)));
				}
			}
			catch (NumberFormatException e) {
				return false;
			}
		}
		return bIsDate;
	}
			
	// fonction récursive qui verifie que si la chaine comporte un chiffre,
	// il s'agit du premier du suffixe
	private static boolean bCheckBdfKeySuffix(String sString) {
		boolean bIsCheck = true;
		if (sString.length() > 0) {
			char sLetter = sString.charAt(0);
			if (isNum("" + sLetter)) { // la lettre est un chiffre
				// on verifie qu'il s'agit des 2 derniers caracteres
				if ((sString.length() != 2) || isNaN(sString)) {
					bIsCheck = false;
				}
			}
			else {
				bIsCheck = bCheckBdfKeySuffix(sString.substring(1));
			}
		}

		return bIsCheck;
	}

	// fonction qui verifie :
	// si la chaine comporte des blancs, ils sont contigus et en fin de zone
	// la chaine hors suffixe fait au maximum 5 caracteres
	private static boolean bCheckBdfKeyBlank(String sString) {
		int iBlank, iLetter;
		String sLetter, sBlank;

		// on retire le suffixe s'il est présent
		if (sString.length() > 3) {
			if (isNum(sString.substring(sString.length() - 2))) {
				sString = sString.substring(0, sString.length() - 2);
			}
		}

		if (sString.length() > 5) {
			return false;
		}
		else {
			sLetter = sString.substring(0, 1);
			if (!sLetter.equals("")) {
				iBlank = sString.indexOf(" ");
				if (iBlank != -1) {
					// les blancs doivent être contigus et en fin de zone
					sBlank = sString.substring(iBlank);
					sBlank = sBlank.trim();
					if (sBlank.length() != 0) {
						return false;
					}
				}
				sString = sString.toUpperCase();
				while (sString.length() > 0) {
					iLetter = sString.charAt(0);
					if ((iLetter < 65 || iLetter > 90 ) && iLetter != 32) {
						return false;
					}
					sString = sString.substring(1);
				}
			}
		}
		return true;
	}
}
