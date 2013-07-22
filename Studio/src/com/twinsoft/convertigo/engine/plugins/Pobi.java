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

import java.io.FileInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * @author nathalieh
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Pobi {
	
	private static String FCC_TYPREL_RGPP 	= "RGPP";
	private static String FCC_TYPREL_RGPM 	= "RGPM";
	private static String FCC_TYPREL_RDPP 	= "RDPP";
	private static String FCC_TYPREL_RDPM 	= "RDPM";
	
	private static String FCC_ELT_ROOT 		= "FCC";
	private static String FCC_ELT_DEMANDE 	= "DEMANDE";
	private static String FCC_ELT_REPONSE 	= "REPONSE";
	private static String FCC_ELT_PERSONNE 	= "PERSONNE";
	private static String FCC_ELT_PP 		= "PP";
	private static String FCC_ELT_PM 		= "PM";
	private static String FCC_ELT_GLOBAL 	= "GLOBAL";
	private static String FCC_ELT_DETAIL_VDX= "DETAILVDX";
	private static String FCC_ELT_DETAIL	= "DETAIL";
	private static String FCC_ELT_DETAILINCIDENT = "DETAILINCIDENT";
	
	private static String FCC_ELT_CLEBDF 	= "CLEBDF";
	private static String FCC_ELT_NATIMMAT 	= "NATIMMAT";
	private static String FCC_ELT_NUMIMMAT 	= "NUMIMMAT";
	private static String FCC_ELT_NOM	 	= "NOM";
	private static String FCC_ELT_DENOM	 	= "DENOM";
	private static String FCC_ELT_NATJUR 	= "NATJUR";
	private static String FCC_ELT_PRENOMS 	= "PRENOMS";
	private static String FCC_ELT_SEXE	 	= "SEXE";
	private static String FCC_ELT_NOMMARI 	= "NOMMARI";
	private static String FCC_ELT_PNOMMARI 	= "PNOMMARI";
	private static String FCC_ELT_NAISSANCE	= "NAISSANCE";
	private static String FCC_ELT_CODPCS	= "CODPCS";
	private static String FCC_ELT_CODNAF	= "CODNAF";
	private static String FCC_ELT_ADRESSE	= "ADRESSE";
	private static String FCC_ELT_FRUP		= "FRUP";
	private static String FCC_ELT_DATNAIS	= "DATNAIS";
	private static String FCC_ELT_COMM		= "COMM";
	private static String FCC_ELT_DEPT		= "DEPT";
	private static String FCC_ELT_LIG1		= "LIG1";
	private static String FCC_ELT_LIG2		= "LIG2";
	private static String FCC_ELT_CP		= "CP";
	
	private static String FCC_ELT_COORDBANC	= "COORDBANC";
	private static String FCC_ELT_CODETC	= "CODETC";
	private static String FCC_ELT_CODGUICH	= "CODGUICH";
	private static String FCC_ELT_NUMCOMPTE	= "NUMCOMPTE";
	private static String FCC_ELT_NUMVERSION= "NUMVERSION";

	private static String FCC_ELT_RETCARTE	= "RETCARTE";
	private static String FCC_ELT_DATDEC	= "DATDEC";
	private static String FCC_ELT_INCCHEQUE	= "INCCHEQUE";
	private static String FCC_ELT_NUMENREG	= "NUMENREG";
	private static String FCC_ELT_MTINSUF	= "MTINSUF";
	private static String FCC_ELT_MTNOMIN	= "MTNOMIN";
	
	private static String FCC_ELT_DETAILIJ	= "DETAILIJ";
	private static String FCC_ELT_NATJURIDICTION = "NATJURIDICTION";
	private static String FCC_ELT_SIEGEJURIDIC = "SIEGEJURIDIC";
	private static String FCC_ELT_REFPARQ	= "REFPARQ";
	private static String FCC_ELT_DATDECIJ	= "DATDECIJ";
	private static String FCC_ELT_DATEFFIJ	= "DATEFFIJ";
	
	private static String FCC_ELT_DATEXPIB	= "DATEXPIB";
	private static String FCC_ELT_DATEXPIJ	= "DATEXPIJ";
	private static String FCC_ELT_DATDERRCB	= "DATDERRCB";
	private static String FCC_ELT_COMPTE	= "COMPTE";
	private static String FCC_ELT_NBCPTE	= "NBCPTE";
	private static String FCC_ELT_NBCHQ		= "NBCHQ";
	private static String FCC_ELT_MTINSUFCUM= "MTINSUFCUM";
	private static String FCC_ELT_NBRCB		= "NBRCB";
	
	private static String FCC_ELT_CPTE		= "CPTE";
	private static String FCC_ELT_RETRAIT	= "RETRAIT";
	private static String FCC_ELT_DATRET	= "DATRET";
	private static String FCC_ELT_CHEQUE	= "CHEQUE";
	private static String FCC_ELT_INCIDENT	= "INCIDENT";
	private static String FCC_ELT_DATREFUS	= "DATREFUS";
	private static String FCC_ELT_MTINSUFF	= "MTINSUFF";
	private static String FCC_ELT_MTNOMINAL	= "MTNOMINAL";
	
	private static String FCC_ATTR_FCC_TYPREL		= "TYPREL";
	private static String FCC_ATTR_REPONSE_TYPREL	= "TYPREL";
	private static String FCC_ATTR_REPONSE_DATHEU	= "DATHEU";
	private static String FCC_ATTR_CLEBDF_SUFFIX	= "SUFFIX";
	private static String FCC_ATTR_REPONSE_NBP		= "NBP";
	private static String FCC_ATTR_DEPT_CODE		= "CODE";
	private static String FCC_ATTR_FRUP_CODE		= "CODE";
	private static String FCC_ATTR_COMPTE_TYPE		= "TYPE";
	private static String FCC_ATTR_CPTE_TYPE		= "TYPE";
	private static String FCC_ATTR_CPTE_NUMERO		= "NUMERO";
	private static String FCC_ATTR_CPTE_VERSION		= "VERSION";
	
	public static boolean includeVariableIntoRequestString(String projectName, String variableName) {
		if (projectName.startsWith("Pobi")) {
			if (variableName.equals("cdbanque")) return false;
			if (variableName.equals("cdguichet")) return false;
			
			// POBI Fiben
			if (projectName.equals("PobiFiben")) {
				if (variableName.equals("backurl")) return false;
				if (variableName.equals("bal")) return false;
				if (variableName.equals("nextModules")) return false;
				if (variableName.equals("nextIds")) return false;
				if (variableName.equals("RefClient")) return false;
			}
		}
		return true;
	}
	
	public static void globalePersonnePhysique(Document dom) {
		Element doc = null, fcc = null, dem = null, rep = null, pers = null;
		int i;
		
		//System.out.println("- Pobi Xml -");
		
		doc = dom.getDocumentElement();
		if (doc != null) {
			if (doc.getElementsByTagName("clebdf").getLength() != 0) {
				fcc = createFCCElement(dom,null,FCC_TYPREL_RGPP,FCC_ELT_ROOT);
				dem = createFCCElement(dom,fcc,FCC_TYPREL_RGPP,FCC_ELT_DEMANDE);
				fcc.appendChild(dem);
				
				if (doc.getElementsByTagName("prenoms").getLength() != 0) {
					fcc.appendChild(rep = createFCCElement(dom,fcc,FCC_TYPREL_RGPP,FCC_ELT_REPONSE));
					
					NodeList blocks = doc.getElementsByTagName("blocks");
					for (i=0;i<blocks.getLength();i=i+2) {
						rep.appendChild(pers = createFCCElement(dom,rep,FCC_TYPREL_RGPP,FCC_ELT_PERSONNE));
						parseBlock((Element)blocks.item(i),(Element)pers.getElementsByTagName(FCC_ELT_PP).item(0));
						parseBlock((Element)blocks.item(i+1),(Element)pers.getElementsByTagName(FCC_ELT_GLOBAL).item(0));
					}
					
					finalizeFCC(dom,(Element)pers.getElementsByTagName(FCC_ELT_PP).item(0));					
				}
				else {
					//fcc.appendChild(err = createFCCElement(dom,fcc,FCC_TYPREL_RGPP,FCC_ELT_ERREUR));
				}
				
				doc.appendChild(fcc);
			}
			//System.out.println(XMLUtils.prettyPrintDOM(dom));
		}		
	}


	public static void globalePersonneMorale(Document dom) {
		Element doc = null, fcc = null, dem = null, rep = null, pers = null;
		int i;
		
		//System.out.println("- Pobi Xml -");
		
		doc = dom.getDocumentElement();
		if (doc != null) {
			if (doc.getElementsByTagName("numimmat").getLength() != 0) {
				fcc = createFCCElement(dom,null,FCC_TYPREL_RGPM,FCC_ELT_ROOT);
				dem = createFCCElement(dom,fcc,FCC_TYPREL_RGPM,FCC_ELT_DEMANDE);
				fcc.appendChild(dem);
				
				if (doc.getElementsByTagName("denom").getLength() != 0) {
					fcc.appendChild(rep = createFCCElement(dom,fcc,FCC_TYPREL_RGPM,FCC_ELT_REPONSE));
					
					NodeList blocks = doc.getElementsByTagName("blocks");
					for (i=0;i<blocks.getLength();i=i+2) {
						rep.appendChild(pers = createFCCElement(dom,rep,FCC_TYPREL_RGPM,FCC_ELT_PERSONNE));
						parseBlock((Element)blocks.item(i),(Element)pers.getElementsByTagName(FCC_ELT_PM).item(0));
						parseBlock((Element)blocks.item(i+1),(Element)pers.getElementsByTagName(FCC_ELT_GLOBAL).item(0));
					}
					
					finalizeFCC(dom,(Element)pers.getElementsByTagName(FCC_ELT_PM).item(0));					
				}
				else {
					//fcc.appendChild(err = createFCCElement(dom,fcc,FCC_TYPREL_RGPM,FCC_ELT_ERREUR));
				}
				
				doc.appendChild(fcc);
			}
			//System.out.println(XMLUtils.prettyPrintDOM(dom));
		}		
	}

	public static void detaillePersonnePhysique(Document dom) {
		Element doc = null, fcc = null, dem = null, rep = null, pers = null;
		int i;
		
		//System.out.println("- Pobi Xml -");
		
		doc = dom.getDocumentElement();
		if (doc != null) {
			if (doc.getElementsByTagName("clebdf").getLength() != 0) {
				fcc = createFCCElement(dom,null,FCC_TYPREL_RDPP,FCC_ELT_ROOT);
				dem = createFCCElement(dom,fcc,FCC_TYPREL_RDPP,FCC_ELT_DEMANDE);
				fcc.appendChild(dem);
				
				if (doc.getElementsByTagName("prenoms").getLength() != 0) {
					fcc.appendChild(rep = createFCCElement(dom,fcc,FCC_TYPREL_RDPP,FCC_ELT_REPONSE));
					
					NodeList blocks = doc.getElementsByTagName("blocks");
					rep.appendChild(pers = createFCCElement(dom,rep,FCC_TYPREL_RDPP,FCC_ELT_PERSONNE));
					for (i=0;i<blocks.getLength();i++) {
						parseBlock((Element)blocks.item(i),(Element)pers.getElementsByTagName(FCC_ELT_PP).item(0));
						parseBlock((Element)blocks.item(i),(Element)pers.getElementsByTagName(FCC_ELT_DETAIL_VDX).item(0));
					}
					
					finalizeFCC(dom,(Element)pers.getElementsByTagName(FCC_ELT_PP).item(0));
					translateToPobiXml(dom,pers);
				}
				else {
					//fcc.appendChild(err = createFCCElement(dom,fcc,FCC_TYPREL_RDPP,FCC_ELT_ERREUR));
				}
				
				doc.appendChild(fcc);
			}
			//System.out.println(XMLUtils.prettyPrintDOM(dom));
		}		
	}

	public static void detaillePersonneMorale(Document dom) {
		Element doc = null, fcc = null, dem = null, rep = null, pers = null;
		int i;
		
		//System.out.println("- Pobi Xml -");
		
		doc = dom.getDocumentElement();
		if (doc != null) {
			if (doc.getElementsByTagName("numimmat").getLength() != 0) {
				fcc = createFCCElement(dom,null,FCC_TYPREL_RDPM,FCC_ELT_ROOT);
				dem = createFCCElement(dom,fcc,FCC_TYPREL_RDPM,FCC_ELT_DEMANDE);
				fcc.appendChild(dem);
				
				if (doc.getElementsByTagName("denom").getLength() != 0) {
					fcc.appendChild(rep = createFCCElement(dom,fcc,FCC_TYPREL_RDPM,FCC_ELT_REPONSE));
					
					NodeList blocks = doc.getElementsByTagName("blocks");
					rep.appendChild(pers = createFCCElement(dom,rep,FCC_TYPREL_RDPM,FCC_ELT_PERSONNE));
					for (i=0;i<blocks.getLength();i++) {
						parseBlock((Element)blocks.item(i),(Element)pers.getElementsByTagName(FCC_ELT_PM).item(0));
						parseBlock((Element)blocks.item(i),(Element)pers.getElementsByTagName(FCC_ELT_DETAIL_VDX).item(0));
					}
					
					finalizeFCC(dom,(Element)pers.getElementsByTagName(FCC_ELT_PM).item(0));
					translateToPobiXml(dom,pers);
				}
				else {
					//fcc.appendChild(err = createFCCElement(dom,fcc,FCC_TYPREL_RDPM,FCC_ELT_ERREUR));
				}
				
				doc.appendChild(fcc);
			}
			//System.out.println(XMLUtils.prettyPrintDOM(dom));
		}		
	}

//	private static void print(Node node, int indent) {
//		String s = "";
//		for (int j=0;j<=indent;j++)
//			s += "  ";
//		System.out.println(s+"type:"+node.getNodeType()+", Name:"+node.getNodeName()+", Value:"+node.getNodeValue());
//		NodeList children = node.getChildNodes();
//		for (int i=0;i<children.getLength();i++) {
//			print(children.item(i), indent+1);
//		}		
//	}
	
	private static void parseBlock(Element block, Element elt) {
		if (block != null) {
			Document dom = block.getOwnerDocument();
			if (elt != null) {
				if (block.hasChildNodes()) {
					NodeList children = block.getChildNodes();
					for (int i=0;i<children.getLength();i++) {
						Node child = children.item(i);
					
						if (child.getNodeType() == Node.ELEMENT_NODE) {
							String childTag = child.getNodeName();
							String textVal = null;
							if (child.hasChildNodes()) {
								NodeList list = child.getChildNodes();
								Node text = list.item(0);
								int len = list.getLength();
								if ((len == 1) && (text.getNodeType() == Node.TEXT_NODE))
									textVal = text.getNodeValue();
								else
									parseBlock((Element)child,elt);
							}
							if (textVal != null)
								setFCCNodeText(dom,elt,childTag.toUpperCase(),textVal);
						}
					}
				}
			}
		}
	}
	
	private static void finalizeFCC(Document dom, Element parent) {
		if (parent != null) {
			if (parent.hasChildNodes()) {
				String lig1 = null, lig2 = "", natimmat = null, numimmat = null;
				NodeList list = null, children = null;
				Element elt = null;
				Node node = null;
				int len, i, j;
				
				// Parse LIG2
				list = parent.getElementsByTagName(FCC_ELT_LIG2);
				len = list.getLength();
				if (len == 1) {
					elt = (Element)list.item(0);
					children = elt.getChildNodes();
					for (i=0;i<children.getLength();i++) {
						node = children.item(i);
						if (node.getNodeType() == Node.TEXT_NODE)
							lig2 += node.getNodeValue();
					}
					
					if ((j = lig2.indexOf("*")) > 0) {
						lig1 = lig2.substring(0,j);
						lig2 = lig2.substring(j+1);
					}
					
					// set LIG2 text node value
					while (elt.hasChildNodes()) {
						node = elt.getFirstChild();
						elt.removeChild(node);
					}
					elt.appendChild(dom.createTextNode(lig2));
				}
				
				//set LIG1 text node value
				if (lig1 != null) {
					list = parent.getElementsByTagName(FCC_ELT_LIG1);
					len = list.getLength();
					if (len == 1) {
						elt = (Element)list.item(0);
						elt.appendChild(dom.createTextNode(lig1));
					}
				}
				

				// Parse NUMIMMAT
				list = parent.getElementsByTagName(FCC_ELT_NUMIMMAT);
				len = list.getLength();
				if (len == 1) {
					elt = (Element)list.item(0);
					if (elt.hasChildNodes()) {
						node = elt.getFirstChild();
						if (node.getNodeType() == Node.TEXT_NODE) {
							numimmat = node.getNodeValue();
							natimmat = numimmat.substring(0,2);
							numimmat = numimmat.substring(2);
							
							node.setNodeValue(numimmat); 
						}
					}
				}
				
				//set NATIMMAT text node value
				if (natimmat != null) {
					list = parent.getElementsByTagName(FCC_ELT_NATIMMAT);
					len = list.getLength();
					if (len == 1) {
						elt = (Element)list.item(0);
						elt.appendChild(dom.createTextNode(natimmat));
					}
				}

				// clean empty nodes
			}
		}
	}

	private static void translateToPobiXml(Document dom, Element parent) {
		if (parent != null) {
			Element pers = parent;
			String typerel = ((Element)pers.getParentNode()).getAttribute(FCC_ATTR_REPONSE_TYPREL);
			
			if (parent.hasChildNodes()) {
				Element vdx = (Element)pers.getElementsByTagName(FCC_ELT_DETAIL_VDX).item(0);
				if (vdx != null) {
					Element global, detail, cptind, cptcol, cpt, detij, retcb, incident, coord, chq, incchq, inc;
					String datexpib, daterrcb, datexpij, nbcpte, nbchq, nbrcb, mtinsufcum;
					String numero, type, version, codetc, codguich, numcpt;
					String numenreg, datrefus, mtinsuf, mtnomin;
					NodeList cptList, list, chqList;
					
					// GLOBAL
					datexpib = getElementTextNodeValue(vdx,FCC_ELT_DATEXPIB);
					daterrcb = getElementTextNodeValue(vdx,FCC_ELT_DATDERRCB);
					datexpij = getElementTextNodeValue(vdx,FCC_ELT_DATEXPIJ);
					nbcpte = getElementTextNodeValue(vdx,FCC_ELT_NBCPTE);
					nbchq = getElementTextNodeValue(vdx,FCC_ELT_NBCHQ);
					nbrcb = getElementTextNodeValue(vdx,FCC_ELT_NBRCB);
					mtinsufcum = getElementTextNodeValue(vdx,FCC_ELT_MTINSUFCUM);
					
					global = createFCCElement(dom,pers,typerel,FCC_ELT_GLOBAL);
					((Element)global.getElementsByTagName(FCC_ELT_DATEXPIB).item(0)).appendChild(dom.createTextNode(datexpib));
					((Element)global.getElementsByTagName(FCC_ELT_DATDERRCB).item(0)).appendChild(dom.createTextNode(daterrcb));
					((Element)global.getElementsByTagName(FCC_ELT_DATEXPIJ).item(0)).appendChild(dom.createTextNode(datexpij));
					
					cptind = (Element)global.getElementsByTagName(FCC_ELT_COMPTE).item(0);
					((Element)cptind.getElementsByTagName(FCC_ELT_NBCPTE).item(0)).appendChild(dom.createTextNode(nbcpte));
					((Element)cptind.getElementsByTagName(FCC_ELT_NBCHQ).item(0)).appendChild(dom.createTextNode(nbchq));
					((Element)cptind.getElementsByTagName(FCC_ELT_MTINSUFCUM).item(0)).appendChild(dom.createTextNode(mtinsufcum));
					((Element)cptind.getElementsByTagName(FCC_ELT_NBRCB).item(0)).appendChild(dom.createTextNode(nbrcb));
					
					cptcol = (Element)global.getElementsByTagName(FCC_ELT_COMPTE).item(1);
					((Element)cptcol.getElementsByTagName(FCC_ELT_NBCPTE).item(0)).appendChild(dom.createTextNode("0"));
					((Element)cptcol.getElementsByTagName(FCC_ELT_NBCHQ).item(0)).appendChild(dom.createTextNode("0"));
					((Element)cptcol.getElementsByTagName(FCC_ELT_MTINSUFCUM).item(0)).appendChild(dom.createTextNode("0"));
					((Element)cptcol.getElementsByTagName(FCC_ELT_NBRCB).item(0)).appendChild(dom.createTextNode("0"));
					
					// DETAIL
					detail = createFCCElement(dom,pers,typerel,FCC_ELT_DETAIL);
					
					if (!datexpij.equals("")) {
						detij = createFCCElement(dom,detail,typerel,FCC_ELT_DETAILIJ);
						((Element)detij.getElementsByTagName(FCC_ELT_DATEXPIJ).item(0)).appendChild(dom.createTextNode(datexpij));
						detail.appendChild(detij);
					}
					
					cptList = vdx.getElementsByTagName(FCC_ELT_CPTE);
					for (int i=0; i<cptList.getLength(); i++) {
						incident = createFCCElement(dom,detail,typerel,FCC_ELT_DETAILINCIDENT);
						
						cpt = (Element)cptList.item(i);
						numero = cpt.getAttribute(FCC_ATTR_CPTE_NUMERO);
						type = cpt.getAttribute(FCC_ATTR_CPTE_TYPE);
						version = cpt.getAttribute(FCC_ATTR_CPTE_VERSION);
						
						codetc = "";
						codguich = "";
						numcpt = "";
						String[] cptdata = numero.split(" ");
						if (cptdata.length == 3) {
							codetc = cptdata[0];
							codguich = cptdata[1];
							numcpt = cptdata[2];
						}
						
						coord = (Element)incident.getElementsByTagName(FCC_ELT_COORDBANC).item(0);
						coord.setAttribute(FCC_ATTR_CPTE_TYPE,type);
						((Element)coord.getElementsByTagName(FCC_ELT_CODETC).item(0)).appendChild(dom.createTextNode(codetc));
						((Element)coord.getElementsByTagName(FCC_ELT_CODGUICH).item(0)).appendChild(dom.createTextNode(codguich));
						((Element)coord.getElementsByTagName(FCC_ELT_NUMCOMPTE).item(0)).appendChild(dom.createTextNode(numcpt));
						((Element)coord.getElementsByTagName(FCC_ELT_NUMVERSION).item(0)).appendChild(dom.createTextNode(version));
						
						if (cpt.hasChildNodes()) {
							list = cpt.getElementsByTagName(FCC_ELT_RETRAIT);
							if (list.getLength() == 1) {
								daterrcb = getElementTextNodeValue((Element)list.item(0),FCC_ELT_DATRET);
								if (!daterrcb.equals("")) {
									retcb = createFCCElement(dom,incident,typerel,FCC_ELT_RETCARTE);
									((Element)retcb.getElementsByTagName(FCC_ELT_DATDEC).item(0)).appendChild(dom.createTextNode(daterrcb));
									incident.appendChild(retcb);
								}
							}
							
							list = cpt.getElementsByTagName(FCC_ELT_CHEQUE);
							if (list.getLength() == 1) {
								chq = (Element)list.item(0);
								chqList = chq.getElementsByTagName(FCC_ELT_INCIDENT);
								for (int j=0; j<chqList.getLength(); j++) {
									inc = (Element)chqList.item(j);
									numenreg = getElementTextNodeValue(inc,FCC_ELT_NUMENREG);
									datrefus = getElementTextNodeValue(inc,FCC_ELT_DATREFUS);
									mtinsuf = getElementTextNodeValue(inc,FCC_ELT_MTINSUFF);
									mtnomin = getElementTextNodeValue(inc,FCC_ELT_MTNOMINAL);
									
									incchq = createFCCElement(dom,incident,typerel,FCC_ELT_INCCHEQUE);
									((Element)incchq.getElementsByTagName(FCC_ELT_NUMENREG).item(0)).appendChild(dom.createTextNode(numenreg));
									((Element)incchq.getElementsByTagName(FCC_ELT_DATREFUS).item(0)).appendChild(dom.createTextNode(datrefus));
									((Element)incchq.getElementsByTagName(FCC_ELT_MTINSUF).item(0)).appendChild(dom.createTextNode(mtinsuf));
									((Element)incchq.getElementsByTagName(FCC_ELT_MTNOMIN).item(0)).appendChild(dom.createTextNode(mtnomin));
									incident.appendChild(incchq);
								}
							}
						}
						
						detail.appendChild(incident);
					}
				
					// add new nodes
					pers.appendChild(global);
					pers.appendChild(detail);
				}
			}
		}
	}
		
	private static void setFCCNodeText(Document dom, Element parent, String elttag, String elttext) {
		if (parent != null) {
			if (parent.hasChildNodes()) {
				NodeList list = null;
				String eltattr = null, type = "";
				boolean bAttr = false;
				int len;
				
				if (elttag.equals(FCC_ATTR_CLEBDF_SUFFIX)) {
					eltattr = elttag;
					elttag = FCC_ELT_CLEBDF;
					bAttr = true;
				}
				if (elttag.equals(FCC_ATTR_DEPT_CODE)) {
					eltattr = elttag;
					elttag = FCC_ELT_DEPT;
					bAttr = true;
				}
				if (elttag.equals(FCC_ATTR_FRUP_CODE)) {
					eltattr = elttag;
					elttag = FCC_ELT_FRUP;
					bAttr = true;
				}
				if (elttag.startsWith("CI-")) {
					elttag = elttag.substring(3);
					type = "IND";
				}
				if (elttag.startsWith("CC-")) {
					elttag = elttag.substring(3);
					type = "COL";
				}
				if (elttag.equals("CB-NUMERO")) {
					if (parent.getTagName().equals(FCC_ELT_DETAIL_VDX)) {
						parent.appendChild(createFCCElement(dom,parent,"",FCC_ELT_CPTE));
						eltattr = elttag.substring(3);
						elttag = FCC_ELT_CPTE;
						bAttr = true;
					}
				}
				if (elttag.equals("CB-VERSION")) {
					eltattr = elttag.substring(3);
					elttag = FCC_ELT_CPTE;
					bAttr = true;
				}
				if (elttag.equals("CB-TYPE")) {
					eltattr = elttag.substring(3);
					elttag = FCC_ELT_CPTE;
					bAttr = true;
				}
				if (elttag.equals("RETRAIT-CARTE")) {
					list = parent.getElementsByTagName(FCC_ELT_CPTE);
					len = list.getLength();
					if (len>0) {
						Element elt = (Element)list.item(len-1);
						list = elt.getElementsByTagName(FCC_ELT_RETRAIT);
						len = list.getLength();
						if (len>0) {
							elt = (Element)list.item(len-1);
							elt.appendChild(createFCCElement(dom,elt,"",FCC_ELT_DATRET));
							elttag = FCC_ELT_DATRET;
						}
					}
				}
				if (elttag.equals("DATREFUS")) {
					list = parent.getElementsByTagName(FCC_ELT_CPTE);
					len = list.getLength();
					if (len>0) {
						Element elt = (Element)list.item(len-1);
						list = elt.getElementsByTagName(FCC_ELT_CHEQUE);
						len = list.getLength();
						if (len>0) {
							elt = (Element)list.item(len-1);
							elt.appendChild(createFCCElement(dom,elt,"",FCC_ELT_INCIDENT));
							elttag = FCC_ELT_DATREFUS;
						}
					}
				}
				
				
				list = parent.getElementsByTagName(elttag);
				len = list.getLength();
				
				if (len == 0)
					return;
				
				if (elttext != null) {
					if (len == 1) {
						Element elt = (Element)list.item(0);
						if (!bAttr)
							elt.appendChild(dom.createTextNode(elttext));
						else
							elt.setAttribute(eltattr,elttext);
					}
					else {
						if ((elttag.equals(FCC_ELT_CPTE)) 		||
							(elttag.equals(FCC_ELT_DATRET)) 	||
							(elttag.equals(FCC_ELT_DATREFUS)) 	||
							(elttag.equals(FCC_ELT_MTINSUFF))	||
							(elttag.equals(FCC_ELT_MTNOMINAL)) 	) {
							Element elt = (Element)list.item(len-1);
							if (!bAttr)
								elt.appendChild(dom.createTextNode(elttext));
							else
								elt.setAttribute(eltattr,elttext);
						}
						else {
							for (int i=0;i<len;i++) {
								Element elt = (Element)list.item(i);
								Element par = (Element)elt.getParentNode();
								if (par.getTagName().equals(FCC_ELT_COMPTE)) {
									if (par.getAttribute("TYPE").equals(type)) {
										elt.appendChild(dom.createTextNode(elttext));
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param dom
	 * @param parent
	 * @param typerel
	 * @param elttag
	 * @return
	 */
	private static Element createFCCElement(Document dom, Element parent, String typerel, String elttag) {
		Element elt = null;
		
		if (dom != null) {
			
			elt = dom.createElement(elttag);
			/*val = getTextNodeValue(dom,elttag);
			if (val != null)
				elt.appendChild(dom.createTextNode(val));*/
			
			if (elt != null) {
				// FCC_ELT_ROOT
				if (elttag.equalsIgnoreCase(FCC_ELT_ROOT)) {
					elt.setAttribute(FCC_ATTR_FCC_TYPREL,typerel);
					//elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DEMANDE));
					//elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_REPONSE));
				}
			
				// FCC_ELT_DEMANDE
				if (elttag.equalsIgnoreCase(FCC_ELT_DEMANDE)) {
					if (typerel.equalsIgnoreCase(FCC_TYPREL_RGPP) || typerel.equalsIgnoreCase(FCC_TYPREL_RDPP)) {
						elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_CLEBDF));
					}
					if (typerel.equalsIgnoreCase(FCC_TYPREL_RGPM) || typerel.equalsIgnoreCase(FCC_TYPREL_RDPM)) {
						elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NATIMMAT));
						elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NUMIMMAT));
					}
				}
			
				// FCC_ELT_REPONSE
				if (elttag.equalsIgnoreCase(FCC_ELT_REPONSE)) {
					elt.setAttribute(FCC_ATTR_REPONSE_TYPREL,typerel);
					elt.setAttribute(FCC_ATTR_REPONSE_DATHEU,"");
					elt.setAttribute(FCC_ATTR_REPONSE_NBP,"1");
					//elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_PERSONNE));
				}
			
				// FCC_ELT_PERSONNE
				if (elttag.equalsIgnoreCase(FCC_ELT_PERSONNE)) {
					if (typerel.equalsIgnoreCase(FCC_TYPREL_RGPP) || typerel.equalsIgnoreCase(FCC_TYPREL_RDPP))
						elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_PP));
					if (typerel.equalsIgnoreCase(FCC_TYPREL_RGPM) || typerel.equalsIgnoreCase(FCC_TYPREL_RDPM))
						elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_PM));
					if (typerel.equalsIgnoreCase(FCC_TYPREL_RGPP) || typerel.equalsIgnoreCase(FCC_TYPREL_RGPM))
						elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_GLOBAL));
					if (typerel.equalsIgnoreCase(FCC_TYPREL_RDPP) || typerel.equalsIgnoreCase(FCC_TYPREL_RDPM))
						elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DETAIL_VDX));
				}
			
				// FCC_ELT_PP
				if (elttag.equalsIgnoreCase(FCC_ELT_PP)) {
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_CLEBDF));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NOM));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_PRENOMS));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_SEXE));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NOMMARI));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_PNOMMARI));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NAISSANCE));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NATIMMAT));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NUMIMMAT));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_CODPCS));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_ADRESSE));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_FRUP));
				}
						
				// FCC_ELT_PM
				if (elttag.equalsIgnoreCase(FCC_ELT_PM)) {
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DENOM));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NATJUR));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NATIMMAT));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NUMIMMAT));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_CODNAF));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_ADRESSE));
				}
			
				// FCC_ELT_GLOBAL
				if (elttag.equalsIgnoreCase(FCC_ELT_GLOBAL)) {
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATEXPIB));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATDERRCB));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATEXPIJ));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_COMPTE));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_COMPTE));
				}
			

				// FCC_ELT_DETAIL_VDX
				if (elttag.equalsIgnoreCase(FCC_ELT_DETAIL_VDX)) {
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATEXPIB));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATDERRCB));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATEXPIJ));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NBCPTE));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NBCHQ));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NBRCB));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_MTINSUFCUM));
					//elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_CPTE));
				}

				// FCC_ELT_DETAILIJ
				if (elttag.equalsIgnoreCase(FCC_ELT_DETAILIJ)) {
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NATJURIDICTION));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_SIEGEJURIDIC));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_REFPARQ));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATDECIJ));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATEFFIJ));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATEXPIJ));
				}

				// FCC_ELT_DETAILINCIDENT
				if (elttag.equalsIgnoreCase(FCC_ELT_DETAILINCIDENT)) {
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_COORDBANC));
				}
				
				// FCC_ELT_COORDBANC
				if (elttag.equalsIgnoreCase(FCC_ELT_COORDBANC)) {
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_CODETC));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_CODGUICH));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NUMCOMPTE));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NUMVERSION));
				}
				
				// FCC_ELT_RETCARTE
				if (elttag.equalsIgnoreCase(FCC_ELT_RETCARTE)) {
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATDEC));
				}
				
				// FCC_ELT_INCCHEQUE
				if (elttag.equalsIgnoreCase(FCC_ELT_INCCHEQUE)) {
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NUMENREG));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATREFUS));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_MTINSUF));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_MTNOMIN));
				}

				// FCC_ELT_CLEBDF
				if (elttag.equalsIgnoreCase(FCC_ELT_CLEBDF)) {
					if (parent.getTagName().equalsIgnoreCase(FCC_ELT_PP)) {
						//val = getAttrNodeValue(dom,FCC_ATTR_CLEBDF_SUFFIX);
						elt.setAttribute(FCC_ATTR_CLEBDF_SUFFIX,"");
					}
				}			

				// FCC_ELT_NAISSANCE
				if (elttag.equalsIgnoreCase(FCC_ELT_NAISSANCE)) {
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATNAIS));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_COMM));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DEPT));
				}
			
				// FCC_ELT_DEPT
				if (elttag.equalsIgnoreCase(FCC_ELT_DEPT)) {
					//val = getAttrNodeValue(dom,FCC_ATTR_DEPT_CODE);
					elt.setAttribute(FCC_ATTR_DEPT_CODE,"");
				}
			
				// FCC_ELT_ADRESSE
				if (elttag.equalsIgnoreCase(FCC_ELT_ADRESSE)) {
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_LIG1));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_LIG2));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_CP));
				}

				// FCC_ELT_FRUP
				if (elttag.equalsIgnoreCase(FCC_ELT_FRUP)) {
					elt.setAttribute(FCC_ATTR_FRUP_CODE,"");
				}

				// FCC_ELT_COMPTE
				if (elttag.equalsIgnoreCase(FCC_ELT_COMPTE)) {
					if (parent.getLastChild().getNodeName().equalsIgnoreCase(FCC_ELT_DATEXPIJ))
						elt.setAttribute(FCC_ATTR_COMPTE_TYPE,"IND");
					else
						elt.setAttribute(FCC_ATTR_COMPTE_TYPE,"COL");
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NBCPTE));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NBCHQ));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_MTINSUFCUM));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_NBRCB));
				}

				// FCC_ELT_CPTE
				if (elttag.equalsIgnoreCase(FCC_ELT_CPTE)) {
					elt.setAttribute(FCC_ATTR_CPTE_NUMERO,"");
					elt.setAttribute(FCC_ATTR_CPTE_TYPE,"");
					elt.setAttribute(FCC_ATTR_CPTE_VERSION,"");
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_RETRAIT));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_CHEQUE));
				}
				
				// FCC_ELT_RETRAIT
				if (elttag.equalsIgnoreCase(FCC_ELT_RETRAIT)) {
					//elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATRET));
				}

				// FCC_ELT_CHEQUE
				if (elttag.equalsIgnoreCase(FCC_ELT_CHEQUE)) {
					//elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_INCIDENT));
				}
				
				// FCC_ELT_INCIDENT
				if (elttag.equalsIgnoreCase(FCC_ELT_INCIDENT)) {
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_DATREFUS));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_MTINSUFF));
					elt.appendChild(createFCCElement(dom,elt,typerel,FCC_ELT_MTNOMINAL));
				}

				return elt;
			}
		}
		return null;
	}
	
//	private static String getTextNodeValue(Document dom, String elttag) {
//		Element doc = null, elt = null;
//		NodeList list = null, children = null;
//		Node child = null;
//		String value = "";
//		int len, i, j;
//		
//		doc = dom.getDocumentElement();
//		list = doc.getElementsByTagName(elttag.toLowerCase());
//		len = list.getLength();
//		
//		// Element does not exist
//		if (len == 0)
//			return null;
//		
//		for (i=0; i<len; i++) {
//			elt = (Element)list.item(i);
//			children = elt.getChildNodes();
//			for (j=0; j<children.getLength(); j++) {
//				child = children.item(j);
//				if (child.getNodeType() == Node.TEXT_NODE)
//					value += child.getNodeValue();
//			}
//		}
//		
//		return value;
//	}

//	private static String getAttrNodeValue(Document dom, String elttag) {
//		return getTextNodeValue(dom,elttag);
//	}

	private static String getElementTextNodeValue(Element parent, String elttag) {
		Element elt = null;
		NodeList list = null, children = null;
		Node child = null;
		String value = "";
		int len, i, j;
		
		list = parent.getElementsByTagName(elttag);
		len = list.getLength();
		
		// Element does not exist
		if (len == 0)
			return "";
		
		for (i=0; i<len; i++) {
			elt = (Element)list.item(i);
			children = elt.getChildNodes();
			for (j=0; j<children.getLength(); j++) {
				child = children.item(j);
				if (child.getNodeType() == Node.TEXT_NODE)
					value += child.getNodeValue();
			}
		}
		
		return value;
	}
	
	/**
	* @param args the command line arguments
	*/
	public static void main(String args[]) {
		try {
			if (args.length != 0) {
				String sDocument = args[0];
				Document document = XMLUtils.getDefaultDocumentBuilder().parse(new FileInputStream(sDocument));
				
				//Pobi.globalePersonnePhysique(document);
				Pobi.detaillePersonnePhysique(document);
			}
		}
		catch(Exception e) {
			Engine.logEngine.error("Unexpected exception", e);
		}
		finally {
			System.gc();
			System.exit(-1);
		}
	}

}
