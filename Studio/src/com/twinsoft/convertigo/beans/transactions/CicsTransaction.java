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

package com.twinsoft.convertigo.beans.transactions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import com.ibm.ctg.client.ECIRequest;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.connectors.CicsConnector;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.COBOLUtils;
import com.twinsoft.convertigo.engine.util.Copybook;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.util.QuickSort;

public class CicsTransaction extends Transaction {

	private static final long serialVersionUID = -1740845595077433774L;

	/** Holds value of property program. */
    private String program = "";
    
	/** Holds value of property transactionId. */
	private String transactionId = "";
    
	/** Holds value of property userId. */
	private String userId = "";
    
	/** Holds value of property userPassword. */
	private String userPassword = "";
    
	/** Holds value of property inputMap. */
	private XMLVector<XMLVector<Object>> inputMap = new XMLVector<XMLVector<Object>>();
    
	/** Holds value of property outputMap. */
	private XMLVector<XMLVector<Object>> outputMap = new XMLVector<XMLVector<Object>>();
    
	/** Holds value of property mapSize. */
	private String inputMapSize = "";

    transient private Element cicsInput = null;
    
    transient private Element cicsOutput = null;
    
    transient private CicsConnector connector = null;
    
    public CicsTransaction() {
		super();
    }

    @Override
    public CicsTransaction clone() throws CloneNotSupportedException {
        CicsTransaction clonedObject = (CicsTransaction) super.clone();
        return clonedObject;
    }
    
	public void setStatisticsOfRequestFromCache() {
		// Nothing to do
	}
	
	public String getRequestString(Context context) {
		if (context != null) {
			String requestString = context.projectName + " " + context.transactionName;
			Document doc = context.inputDocument;
			if (doc != null) {
				NodeList list = doc.getElementsByTagName("variable");
				Vector<String> vVariables = new Vector<String>(list.getLength());
				for (int i=0; i<list.getLength(); i++) {
					Element elt = (Element)list.item(i);
					String name = elt.getAttribute("name");
					String value = elt.getAttribute("value");
					vVariables.add(name + "=" + value);
				}
				QuickSort quickSort = new QuickSort(vVariables);
				vVariables = GenericUtils.cast(quickSort.perform(true));
		
				requestString += " " + vVariables.toString();
			}
			return requestString;
		}
		return null;
	}
	
	public void parseInputDocument(Context context) {
		super.parseInputDocument(context);
		
		if (cicsInput == null)
			setCicsInputElement();
	}

	public void runCore() throws EngineException {
		ECIRequest eciRequestObject = null;
		String strServer = null;
		String strUserID = null, strUserPassword = null;
		String strProgram = null, strTransactionID = null;
		String errmsg = null;
		byte[] abytCommarea = null;
		int iCommareaSize = 0;
		
		try {
			
			connector = ((CicsConnector) parent);
			
			strServer = connector.getServer();
			strProgram = getProgram();
			strTransactionID = getTransactionId();
			strUserID = getUserId();
			strUserID = (strUserID.equals("") ? connector.getUserId():strUserID);
			strUserPassword = getUserPassword();
			strUserPassword = (strUserPassword.equals("") ? connector.getUserPassword():strUserPassword);
			
			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("(CicsTransaction) \n"+ XMLUtils.prettyPrintDOM(context.inputDocument));
			
			// Initialize CommArea
			abytCommarea = getInputCommarea(cicsInput);
			iCommareaSize = abytCommarea.length;
			dump(abytCommarea,0);
			
			// Use the extended constructor to set the parameters on the ECIRequest object
			eciRequestObject =
			new ECIRequest(ECIRequest.ECI_SYNC,    	//ECI call type
						   strServer,        		//CICS server
						   strUserID,               //CICS userid
						   strUserPassword,         //CICS password
						   strProgram,              //CICS program to be run
						   strTransactionID,       	//CICS transid to be run
						   abytCommarea,           	//Byte array containing the COMMAREA
						   iCommareaSize,			//COMMAREA length
						   ECIRequest.ECI_NO_EXTEND,//ECI extend mode
						   0);                      //ECI LUW token
			
			Engine.logBeans.debug("(CicsTransaction) Request on server: "+ strServer + " - program: " + strProgram + " - transId: "+ strTransactionID);
			Engine.logBeans.debug("(CicsTransaction) Executing '"+ getName() +"' transaction.");
			
			// Call the flowRequest method: if the method returns not null a security error has occurred.
			String t = context.statistics.start(EngineStatistics.APPLY_USER_REQUEST);

	        try {
	        	errmsg = connector.flowRequest(eciRequestObject);
	        }
	        finally {
				context.statistics.stop(t);
	        }
	        
			if (errmsg != null) {
				Engine.logBeans.debug("(CicsTransaction) "+ errmsg);
				throw new EngineException(errmsg);
			}
			
			// Treat Commarea returned data.
			dump(abytCommarea,1);
			if (!connector.existGateway())
				connector.setData(abytCommarea);
			Element cics = parseOutputCommarea(abytCommarea);
			if (cics != null) {
    			Element outputDocumentRootElement = context.outputDocument.getDocumentElement();
				outputDocumentRootElement.appendChild(cics);
				score +=1;
			}
		}
		catch (Exception e) {
			throw new EngineException("Exception in CicsTransaction.runCore()", e);
		}
	}
	
	private void setCicsInputElement() {
		Document doc = null;
		if (cicsInput == null) {
			doc = createDOM("ISO-8859-1");
			doc.appendChild(doc.createElement("document"));
		}
		else {
			doc = cicsInput.getOwnerDocument();
			NodeList list = doc.getDocumentElement().getElementsByTagName("cics_input");
			for (int i=0; i<list.getLength(); i++) {
				doc.getDocumentElement().removeChild(list.item(i));
			}
		}
		cicsInput = getInputExpandedDOM(doc, "cics_input");
		doc.getDocumentElement().appendChild(cicsInput);
	}
	
	private void setCicsOutputElement() {
		Document doc = null;
		if (cicsOutput == null) {
			doc = createDOM("ISO-8859-1");
			doc.appendChild(doc.createElement("document"));
		}
		else {
			doc = cicsOutput.getOwnerDocument();
			NodeList list = doc.getDocumentElement().getElementsByTagName("cics_output");
			for (int i=0; i<list.getLength(); i++) {
				doc.getDocumentElement().removeChild(list.item(i));
			}
		}
		cicsOutput = getOutputExpandedDOM(doc, "cics_output");
		doc.getDocumentElement().appendChild(cicsOutput);
	}
    
    private byte[] getInputCommarea(Element elt) throws IOException {
    	byte[] array = null;
    	int size = 0;
		
    	if (!inputMapSize.equals("")) {
    		try {
				size = Integer.parseInt(inputMapSize,10);
				if ((size > 32000) || (size < 0))
					size = 0;
    		}
    		catch (NumberFormatException e) {;}
    	}
		
    	if (inputMap != null) {
			if (elt != null) {
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				fillCommareaFromXML(elt, stream);
				if (Engine.logBeans.isTraceEnabled())
					Engine.logBeans.debug("(CicsTransaction) \n"+ XMLUtils.prettyPrintDOM(elt));
				
				// padd with spaces if given a fixed size for input commarea
				while (stream.size() < size)
					stream.write((byte)0x40);
				
				array = stream.toByteArray();
			}
    	}
    	return array;	
    }

	private Element parseOutputCommarea(byte[] array) {
		Element elt = null;
		boolean bLoadFromFile = false;
		
		if (bLoadFromFile) {
			String projectsDir = Engine.PROJECTS_PATH;
			String strFilePath = projectsDir + "/" + parent.getParent().getName() + "/" + getName() + "2.xml";
			File f = new File(strFilePath);
			if (f.exists()) {
				array = null;
				Engine.logBeans.debug("(CicsTransaction) Load xml generated from '"+ strFilePath +"' file.");
				Document doc0 = loadXML(f);
				if (doc0 != null) {
					NodeList list = doc0.getElementsByTagName("cics_output");
					Element elt0 = (Element)list.item(0);
					Document doc = context.outputDocument;
					elt = (Element)doc.importNode(elt0,true);
				}
			}
		}
		
		if (array != null) {
			ByteArrayInputStream stream = new ByteArrayInputStream(array);

			String datas = "Commarea returned data (hex)= ";
			for (int i=0;i<array.length;i++)
				datas += Integer.toHexString(array[i]);
			Engine.logBeans.debug("(CicsTransaction) " + datas);
			
			if (cicsOutput == null)
				setCicsOutputElement();
			Document doc = context.outputDocument;
			elt = (Element)doc.importNode(cicsOutput,true);
			if (elt != null)
				fillXMLFromCommarea(elt, stream, null);
		}
		
		return elt;
	}

	private Element getExpandedXMLMap(Document doc, String eltName, XMLVector<XMLVector<Object>> v) {
		return getXMLMap(doc, eltName, v, true);
	}
	
	private Element getReducedXMLMap(Document doc, String eltName, XMLVector<XMLVector<Object>> v) {
		return getXMLMap(doc, eltName, v, false);
	}

	private Element getXMLMap(Document doc, String eltName, XMLVector<XMLVector<Object>> v, boolean bExpand) {
		Element root = null, parent = null, node = null, last = null;
		String xPath = "";
		
		if (doc != null) {
			int curlevel = 0;
			root = doc.createElement(eltName);
			if (v != null) {
				Enumeration<XMLVector<Object>> rows = v.elements();
				while (rows.hasMoreElements()) {
					XMLVector<Object> row = rows.nextElement();
				
					String sLevel = (String)row.elementAt(0);
					String sName = (String)row.elementAt(1);
					String sOccurs = (String)row.elementAt(2);
					String sBytes = ((Integer)row.elementAt(3)).toString();
					String sFormat = (String)row.elementAt(4);
					String sPicture = (String)row.elementAt(5);
					String sDefault = (String)row.elementAt(6);
				
					int level = Integer.parseInt(sLevel,10);
					if (level > 0) {
						node = doc.createElement(sName);
						node.setAttribute("level",sLevel);
						if (!sOccurs.equalsIgnoreCase(""))
							node.setAttribute("occurs",sOccurs);
						if (!sPicture.equalsIgnoreCase(""))
							node.setAttribute("picture",sPicture);
						if (!sBytes.equalsIgnoreCase(""))
							node.setAttribute("bytes",sBytes);
						if (!sFormat.equalsIgnoreCase(""))
							node.setAttribute("format",sFormat);
						if (!sDefault.equalsIgnoreCase(""))
							node.setAttribute("default",sDefault);
					
						if (curlevel == 0)
							parent = root;
						else if (level < curlevel) {
							curlevel = level;
							do {
								parent = (Element)last.getParentNode();
								try {
									level = Integer.parseInt((parent.getAttribute("level")),10);
								}
								catch (NumberFormatException e) {
									level = 0;
								}
								last = parent;
							}
							while (level >= curlevel);
								level = curlevel;
						}
						else if (level == curlevel) {
							parent = (Element)last.getParentNode();
						}
						else  {
							parent = last;
						}
					
						if (parent != null) {
							// set xpath attribute relative to root element in description map
							xPath = parent.getAttribute("relxpath") + "/" + sName;
							if (!sOccurs.equalsIgnoreCase("")) {
								node.setAttribute("occurence","1");
								xPath = xPath + "[1]";
							}
							node.setAttribute("relxpath",xPath);
							
							parent.appendChild(node);
							last = node;
							curlevel = level;
						}
					}
				}
			}
		}
	
		if (root != null) {
			if (bExpand)
				expandXMLMap(root);
		}
		return root;
	}
    
	private void expandXMLMap(Element elt) {
		if (elt != null) {
			NodeList children = elt.getChildNodes();
			Node next = elt.getNextSibling();
			int len = children.getLength();
			if (len > 0) {
				Node child = children.item(0);
				while (child != null) {
					if (child.getNodeType() == Node.ELEMENT_NODE)
						expandXMLMap((Element)child);
					child = child.getNextSibling();
				}
			}
			
			String sOccurs = elt.getAttribute("occurs");
			if ((sOccurs != null) && (!sOccurs.equalsIgnoreCase(""))) {
				String aOccurs[] = sOccurs.split(":");
				int count = 1;
				int j = Integer.parseInt(aOccurs[0],10);
				if (aOccurs.length>1)
					j = Integer.parseInt(aOccurs[1],10);
				
				while (count++ < j) {
					elt.setAttribute("occurs","");
					Element clone = (Element)elt.cloneNode(true);
					clone.setAttribute("occurence",""+count);
					expandXMLElement(elt,clone,count);
					if (next != null)
						elt.getParentNode().insertBefore(clone,next);
					else
						elt.getParentNode().appendChild(clone);
				}
				//elt.setAttribute("occurs",sOccurs);
			}
		}
	}
	
	private void expandXMLElement(Element parent, Element elt, int count) {
		if (parent != null) {
			String toReplace = parent.getTagName()+ "\\[1\\]";
			if (elt != null) {
				NodeList children = elt.getChildNodes();
				int len = children.getLength();
				if (len > 0) {
					for (int i=0; i<len; i++) {
						Node child = children.item(i);
						if (child.getNodeType() == Node.ELEMENT_NODE)
							expandXMLElement(elt,(Element)child, count);
					}
				}
				
				// modify relative xpath attribute
				//String replaceWith = elt.getTagName()+ "["+ count + "]";
				String replaceWith = parent.getTagName()+ "["+ count + "]";
				String xPath = (elt.getAttribute("relxpath")).replaceAll(toReplace,replaceWith);
				elt.setAttribute("relxpath",xPath);
				
				// add item attribute
				String picture = elt.getAttribute("picture");
				String item = elt.getAttribute("item");
				picture = ((picture == null) ? "":picture);
				item = ((item == null) ? "":item);
				if (!picture.equalsIgnoreCase("")) {
					if (item.equalsIgnoreCase(""))
						elt.setAttribute("item",""+count);
				}
			}
		}
	}
	
	private Element fillCommareaFromXML(Element elt, ByteArrayOutputStream stream) {
		if (elt != null) {
			if (elt.getNodeType() == Node.ELEMENT_NODE) {
				fillFromXMLElement(elt, stream);
				
				NodeList children = elt.getChildNodes();
				int len = children.getLength();
				if (len > 0) {
					for (int i=0; i<len; i++) {
						Node child = children.item(i);
						if (child.getNodeType() == Node.ELEMENT_NODE)
							fillCommareaFromXML((Element)child, stream);
					}
				}
			}
		}
		return elt;
	}

	private Element fillXMLFromCommarea(Element elt, ByteArrayInputStream stream, Vector<Integer> v) {
		if (elt != null) {
			if (v == null) {
				v = new Vector<Integer>();
				v.addElement(new Integer(0));
				v.addElement(new Integer(0));
			}
			if (elt.getNodeType() == Node.ELEMENT_NODE) {
				fillXMLElement(elt, stream, v);
				
				NodeList children = elt.getChildNodes();
				int len = children.getLength();
				if (len > 0) {
					for (int i=0; i<len; i++) {
						Node child = children.item(i);
						if (child.getNodeType() == Node.ELEMENT_NODE)
							fillXMLFromCommarea((Element)child, stream, v);
					}
				}
			}
		}
		return elt;
	}
	
	private void fillFromXMLElement(Element elt, ByteArrayOutputStream stream) {
		if (elt != null) {
			if (elt.getNodeType() == Node.ELEMENT_NODE) {
				String sBytes, format, picture, value, defval, item;
				int bytes;
				String name = elt.getTagName();
				picture = ((Element)elt).getAttribute("picture");
				format = ((Element)elt).getAttribute("format");
				format = (((format == null) || (format.equals(""))) ? "":format);
				sBytes = ((Element)elt).getAttribute("bytes");
				sBytes = (((sBytes == null) || (sBytes.equals(""))) ? "0":sBytes);
				bytes  = Integer.parseInt(sBytes,10);
				defval = ((Element)elt).getAttribute("default");
				defval = ((defval == null) ? "":defval);
				item = ((Element)elt).getAttribute("item");
				item = ((item == null) ? "":item);
				
				if (bytes > 0) {
					value = "";
					try {
						Document doc = context.inputDocument;
						NodeList list = doc.getElementsByTagName("input");
						if (list.getLength() > 0) {
							Element input = (Element)list.item(0);
							String path = "transaction-variables/variable[@name='"+ name +"']";
							path = ((item.equalsIgnoreCase("")) ? path:"("+ path +")[position()="+item+"]");
							Element node = (Element)XPathAPI.selectSingleNode(input,path);
							value = node.getAttribute("value");
						}
					}
					catch (Exception e) {
						//System.out.println(e.getMessage());
					}
					
					value = ((value.equals("")) ? defval:value);
					elt.setAttribute("inputValue",value);
					
					byte[] buf = COBOLUtils.encode(value,picture,format);
					try {
						stream.write(buf);
					}
					catch (IOException e) {
						;
					}
				}
			}
		}
	}
	
	private void fillXMLElement(Element elt, ByteArrayInputStream stream, Vector<Integer> v) {
		if (elt != null) {
			if (elt.getNodeType() == Node.ELEMENT_NODE) {
				String sBytes, format, picture, value, defval;
				int bytes, pos, offset;
				byte[] buf = null;
				
				defval = ((Element)elt).getAttribute("default");
				defval = ((defval == null) ? "":defval);
				picture = ((Element)elt).getAttribute("picture");
				format = ((Element)elt).getAttribute("format");
				format = (((format == null) || (format.equals(""))) ? "":format);
				sBytes = ((Element)elt).getAttribute("bytes");
				sBytes = (((sBytes == null) || (sBytes.equals(""))) ? "0":sBytes);
				bytes  = Integer.parseInt(sBytes,10);
				value = "";
				
				pos = v.elementAt(0).intValue();
				offset = v.elementAt(1).intValue();
				
				if (bytes > 0) {
					buf = new byte[bytes];
					if (pos != -1) {
						try {
							while (pos != offset) {
								if (stream.read() != -1)
									pos++;
								else {
									pos = -1;
									break;
								}
							}
							if (pos != -1) {	
								pos += stream.read(buf,0,bytes);
										
								value = COBOLUtils.decode(buf,picture,format);
							}
						}
						catch (Exception e) {
						}
					}
				}
				try {
					elt.removeAttribute("level");
					elt.removeAttribute("picture");
					elt.removeAttribute("format");
					elt.removeAttribute("bytes");
					elt.removeAttribute("defval");
					elt.removeAttribute("occurence");
					elt.removeAttribute("occurs");
					elt.removeAttribute("relxpath");
					elt.removeAttribute("item");
				}
				catch (Exception e) {				
				}
				
				elt.appendChild((elt.getOwnerDocument()).createTextNode(value));
				offset += bytes;
				
				v.setElementAt(new Integer(pos),0);
				v.setElementAt(new Integer(offset),1);
			}
		}
	}
	
	private void dump(byte[] commArea, int i) {
		if (Engine.logBeans.isTraceEnabled()) {
			try {
				FileOutputStream output = null;
				String projectsDir = Engine.PROJECTS_PATH;
				String strFilePath = projectsDir + "/" + parent.getParent().getName() + "/" + getName() + ((i == 0) ? "_input.txt" : "_output.txt");
				try {
					output = new FileOutputStream(strFilePath,true);
				}
				catch (FileNotFoundException e) {;}
			
				if (output == null)
					output = new FileOutputStream(strFilePath);
			
				output.write("\n----BEGIN COMMAREA---\n".getBytes());
				output.write(commArea);
				output.write("\n----END COMMAREA---".getBytes());
			}
			catch (Exception e) {
				Engine.logBeans.error("Unexpected exception", e);
			}
		}
	}
	
	public Element getInputExpandedDOM(Document doc, String eltName) {
		return getExpandedXMLMap(doc, eltName, inputMap);
	}

	public Element getInputReducedDOM(Document doc, String eltName) {
		return getReducedXMLMap(doc, eltName, inputMap);
	}
	
	public Element getOutputExpandedDOM(Document doc, String eltName) {
		if (outputMap.isEmpty())
			return getExpandedXMLMap(doc, eltName, inputMap);
		return getExpandedXMLMap(doc, eltName, outputMap);
	}

	public Element getOutputReducedDOM(Document doc, String eltName) {
		if (outputMap.isEmpty())
			return getReducedXMLMap(doc, eltName, inputMap);
		return getReducedXMLMap(doc, eltName, outputMap);
	}
	
	public void importCopyBook(boolean isInput, Reader reader) throws EngineException {
		try {
			XMLVector<XMLVector<Object>> cblMap = null;

			Copybook cpbk = new Copybook();
			if (cpbk != null)
				cblMap = cpbk.importFromFile2(reader);
			
			if (cblMap != null) {
				this.hasChanged = true;
				if (isInput)
					setInputMap(cblMap);
				else
					setOutputMap(cblMap);
			}
		}
		catch (IOException e) {
			throw new EngineException(e.getMessage());
		}
	}
	
	public void writeToHTMLFile(String strFilePath, String htmlText) {
		try {
			FileOutputStream output = null;
			String projectsDir = Engine.PROJECTS_PATH;
			if (strFilePath == null)
				strFilePath = projectsDir + "/" + parent.getParent().getName() + "/" + getName() + ".htm";
			if (output == null)
				output = new FileOutputStream(strFilePath);
			
			output.write(htmlText.getBytes());
		}
		catch (Exception e) {
			Engine.logBeans.error("Unexpected exception", e);
		}
	}
	
	public String elementToHTML(Element root) {
		String sHtml = "";
		if (root != null) {
			sHtml += "<HTML><BODY>";
			sHtml += "<FORM name='requestForm' method='post' action='request.xml'>";
			sHtml += "<TABLE border='1'>";
			sHtml += elementToHTMLTag(root);
			sHtml += "<TR><TD><input type='hidden' name='"+Parameter.Transaction.getName()+"' value='" + getName() +"'></TD></TR>";
			sHtml += "<TR><TD align='center'><BR/><input type='submit' value='Valider'></TD></TR>";
			sHtml += "</TABLE>";
			sHtml += "</FORM>";
			sHtml += "</BODY></HTML>";
		}
		return sHtml;
	}

	private String elementToHTMLTag(Element elt) {
		String sTags = "";
		if (elt != null) {
			if (elt.hasChildNodes()) {
				NodeList children = elt.getChildNodes();
				int len = children.getLength();
				if (len > 0) {
					for (int i=0; i<len; i++) {
						Node child = children.item(i);
						int type = child.getNodeType();
						switch (type) {
							case Node.ELEMENT_NODE:
								String sOcc = ((Element)child).getAttribute("occurence");
								sOcc = ((sOcc == null) ? "":sOcc);
								boolean bHeader = !sOcc.equalsIgnoreCase("");
								sTags += "<TR>";
								if (child.hasChildNodes()) {
									if (child.getFirstChild().getNodeType() == Node.TEXT_NODE)
										sTags += "<TD>" + addInput(((Element)child).getTagName(),((Element)child).getAttribute("bytes"),child.getFirstChild().getNodeValue()) + "</TD>";
									else {
										sTags += "<TD colspan='2'><TABLE border='1'>";
										sTags += (bHeader ? "<TR><TH colspan='2'>"+ ((Element)child).getTagName() + ": " + sOcc +"</TH></TR>":"");
										sTags += elementToHTMLTag((Element)child);
										sTags += "</TABLE></TD>";
									}
								}
								else {
									sTags += "<TD>" + addInput(((Element)child).getTagName(),((Element)child).getAttribute("bytes"),"") + "</TD>";
								}
								sTags += "</TR>";
								break;
							default:
								break;
						}
					}
				}
			}
		}
		return sTags; 
	}
	
	private String addInput(String name, String sLength, String value) {
		String s = "";
		int len = (((sLength != null) && (!sLength.equalsIgnoreCase(""))) ? Integer.parseInt(sLength,10):0);
		if (len > 0)
			s = name + ":</TD><TD><input type='text' name='"+ name + "' value='"+ value + "' size='"+ len +"' maxlength='"+ len +"' />";
		else
			s = name + ":</TD><TD><input type='text' name='"+ name + "' value='"+ value + "' />";
		return s;
	}
	
	public Document createDOM(String encodingCharSet) {
		Engine.logBeans.debug("(CicsTransaction) XalanServlet: creating DOM");

		Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
        
		Engine.logBeans.debug("(CicsTransaction) XML class: " + document.getClass().getName());

		ProcessingInstruction pi = document.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"" + encodingCharSet + "\"");
		document.appendChild(pi);

		return document;

	}
	
	public Document loadXML(File xmlFile) {
		try {
			Document document = XMLUtils.getDefaultDocumentBuilder().parse(xmlFile);
			return document;
		}
		catch(Exception e) {
			//System.out.println("Unable to load XML file :" + xmlFile.getName());
			return null;
		}
	}

	/** Getter for property program.
	 * @return Value of property program.
	 */
	public String getProgram() {
		return this.program;
	}
    
	/** Setter for property program.
	 * @param program New value of property program.
	 */
	public void setProgram(String program) {
		this.program = program;
	}
    
	/** Getter for property transactionId.
	 * @return Value of property transactionId.
	 */
	public String getTransactionId() {
		return this.transactionId;
	}
    
	/** Setter for property transactionId.
	 * @param transactionId New value of property transactionId.
	 */
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
    
	/** Getter for property userId.
	 * @return Value of property userId.
	 */
	public String getUserId() {
		return this.userId;
	}
    
	/** Setter for property userId.
	 * @param transactionId New value of property userId.
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/** Getter for property userPassword.
	 * @return Value of property userPassword.
	 */
	public String getUserPassword() {
		return this.userPassword;
	}
    
	/** Setter for property userPassword.
	 * @param transactionId New value of property userPassword.
	 */
	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}
    
	/** Getter for property inputMap.
	 * @return Value of property inputMap.
	 */
	public XMLVector<XMLVector<Object>> getInputMap() {
		return this.inputMap;
	}
    
	/** Setter for property inputMap.
	 * @param inputMap New value of property inputMap.
	 */
	public void setInputMap(XMLVector<XMLVector<Object>> inputMap) {
		this.inputMap = inputMap;
		setCicsInputElement();
	}
    
	/** Getter for property outputMap.
	 * @return Value of property outputMap.
	 */
	public XMLVector<XMLVector<Object>> getOutputMap() {
		return this.outputMap;
	}
    
	/** Setter for property outputMap.
	 * @param outputMap New value of property outputMap.
	 */
	public void setOutputMap(XMLVector<XMLVector<Object>> outputMap) {
		this.outputMap = outputMap;
		setCicsOutputElement();
	}
    
	/** Getter for property inputMapSize.
	 * @return Value of property inputMapSize.
	 */
	public String getInputMapSize() {
		return this.inputMapSize;
	}
    
	/** Setter for property inputMapSize.
	 * @param size New value of property inputMapSize.
	 */
	public void setInputMapSize(String size) {
		this.inputMapSize = size;
	}
    
	@Override
	public boolean isMaskedProperty(Visibility target, String propertyName) {
		if ("userPassword".equals(propertyName)) {
			return true;
		}
		return super.isMaskedProperty(target, propertyName);
	}

	@Override
	public boolean isCipheredProperty(String propertyName) {
		if ("userPassword".equals(propertyName)) {
			return true;
		}
		return super.isCipheredProperty(propertyName);
	}
}
