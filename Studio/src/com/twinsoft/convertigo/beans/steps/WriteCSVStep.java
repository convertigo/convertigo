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

package com.twinsoft.convertigo.beans.steps;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class WriteCSVStep extends WriteFileStep {

	private static final long serialVersionUID = 484006052411796784L;
	
	protected String separator = ";";
	protected boolean titleLine = false;	

	public WriteCSVStep() {
		super();
	}

	@Override
    public Object clone() throws CloneNotSupportedException {
    	WriteCSVStep clonedObject = (WriteCSVStep) super.clone();
        return clonedObject;
    }
	
	@Override
    public Object copy() throws CloneNotSupportedException {
    	WriteCSVStep copiedObject = (WriteCSVStep) super.copy();
        return copiedObject;
    }
	
	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}
 
    public boolean isTitleLine() {
		return titleLine;
	}

	public void setTitleLine(boolean titleLine) {
		this.titleLine = titleLine;
	}
    
	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += super.getLabel();
		} catch (EngineException e) {}
		return "WriteCSV" + label + (!text.equals("") ? " // "+text:"");
	}

	@Override
	protected void writeFile(String filePath, NodeList nodeList) throws EngineException {
		if (nodeList == null) {
			throw new EngineException("Unable to write to xml file: element is Null");
		}

		writeFileByNat(filePath, nodeList);
		//WriteFileByMartin(element.getOwnerDocument());		
		
	}
	
	protected void writeFileByNat(String filePath, NodeList nodeList) throws EngineException {
		if (separator.equals("")) {
			throw new EngineException("The separator is empty");
		}

		String fullPathName = getAbsoluteFilePath(filePath);
		synchronized (Engine.theApp.filePropertyManager.getMutex(fullPathName)) {
			try {
				boolean skipTitle = isReallyAppend(fullPathName);

				OutputStreamWriter sortie = new OutputStreamWriter(new FileOutputStream(fullPathName, appendResult) , (encoding.length()>0)?encoding:"iso-8859-1");
				
				List<String> line = new ArrayList<String>();
				List<List<String>> lines = new ArrayList<List<String>>();
				
				List<String> titlesY = new ArrayList<String>();
				List<String> titlesX = new ArrayList<String>();
				int cols = 0;

					// Parse element on two levels of children
					for (int i = 0; i<nodeList.getLength(); i++) {
						Node nodey = nodeList.item(i);
						if (nodey.getNodeType() == Node.ELEMENT_NODE) {// first level
							if (titleLine) {
								titlesY.add(transform(((Element)nodey).getTagName()));
							}

							if (nodey.hasChildNodes()) {
								line.clear();
								NodeList listx = nodey.getChildNodes();
								for (int j=0; j<listx.getLength(); j++) {
									Node nodex = listx.item(j);
									if (nodex.getNodeType() == Node.ELEMENT_NODE) {// second level
										if (titleLine && (i==0)) {
											titlesX.add(transform(((Element)nodex).getTagName()));
										}
										line.add(transform(nodex.getTextContent()));
									}
								}
								cols = (line.size()>cols) ? line.size() : cols;
								lines.add(line);
							}
						}
					}

				//System.out.println(Arrays.asList(lines.toArray()));
				//System.out.println(Arrays.asList(titlesY.toArray()));
				//System.out.println(Arrays.asList(titlesX.toArray()));

				// Determines direction
				boolean verticalDirection = false;
				if (titlesY.size() > 0) {
					verticalDirection = !titlesY.get(0).equals(titlesY.get(1));
				}

				// Writes to file
				if (cols > 0) {

					// Fills the array
					int dimX = cols;
					int dimY = lines.size();
					String[][] table = new String[dimY][dimX]; 
					for (int i=0; i<dimY; i++) {
						line = lines.get(i);
						for (int j=0; j<dimX; j++) {
							try {
								table[i][j] = line.get(j);
							}
							catch (ArrayIndexOutOfBoundsException e) {
								table[i][j] = "";
							}
						}
					}

					if(!skipTitle){
						// Writes titles
						List<String> v = (verticalDirection ? titlesY : titlesX);
						if (v.size() > 0) {
							for (int i = 0; i < v.size(); i++) {
								sortie.write(v.get(i));
								if (i + 1 < v.size()) {
									sortie.write(separator);
								}
							}
							sortie.write("\n");
						}
					}

					// Writes datas
					if (verticalDirection) {
						for (int i=0; i<dimX; i++) {
							for (int j=0; j<dimY; j++) {
								sortie.write(table[j][i]);
								if (j+1<dimY) sortie.write(separator);
							}
							sortie.write("\n");
						}
					}
					else {
						for (int i = 0; i < dimY; i++) {
							for (int j = 0; j < dimX; j++) {
								sortie.write(table[i][j]);
								if (j +1 < dimX){
									sortie.write(separator);
								}
							}
							sortie.write("\n");
						}
					}
				}

				sortie.close();				
			}catch (Exception e) {
				throw new EngineException("Unable to write to csv file",e);
			}finally{
				Engine.theApp.filePropertyManager.releaseMutex(fullPathName);
			}
		}
	}
	
	private String transform(String str){
		//return (str.contains(separator)?"\""+str+"\"":str);
		return ((str.contains(separator) && (!str.startsWith("\"")) && (!str.endsWith("\""))) ? "\""+str+"\"" : str);
	}
	
	
	/*
	protected void WriteFileByMartin(Document xmlDoc) throws EngineException {
		if (separator.equals(""))
			throw new EngineException("The separator is empty");

		try {
	    	FileWriter fr=new FileWriter(getAbsoluteFilePath(dataFile));
			BufferedWriter sortie = new BufferedWriter(fr);
			//Engine.logBeans.warn("%%%%%%%%%"+XMLUtils.prettyPrintDOM(doc));
			Node root = xmlDoc.getFirstChild().getFirstChild();
			int prof=codeProfondeur(root);			
			//Engine.logBeans.warn("?????????"+root);
			
			//case where the source is the first level			
			if(prof<2){
				
				if(isTitleLine())
					sortie.write(transform(root.getNodeName())+"\n");
				if(prof==1)
					sortie.write(transform(root.getFirstChild().getNodeValue()));
				
			}	
			else{
				int i=0,j;
				NodeList child= root.getChildNodes();
				NodeList child2=root.getChildNodes();
				//Node bid=root.getFirstChild().getFirstChild().;
				//make the title line
				
				if(isTitleLine()){
					if(prof>3){
						while(child2.item(i).getNodeType()==Node.TEXT_NODE)							
							i++;
						child=child2.item(i).getChildNodes();
						}
					for(i=0;i<child.getLength()-1;i++){
						if(child.item(i).getNodeType()!=Node.TEXT_NODE)
							sortie.write(transform(child.item(i).getNodeName())+separator);
					}
					if(child.item(i).getNodeType()!=Node.TEXT_NODE)
						sortie.write(transform(child.item(i).getNodeName())+"\n");
					else
						sortie.write("\n");
				}
				//make others line if the source is at the second level
				if(prof<4){
					for(i=0;i<child.getLength()-1;i++){
						if(child.item(i).hasChildNodes() && child.item(i).getFirstChild().getNodeValue()!=null)
							if(child.item(i).getFirstChild().getNodeValue().trim().length()>0)
								sortie.write(transform(child.item(i).getFirstChild().getNodeValue())+separator);
						else 							
							sortie.write(separator);
					}
					if(child.item(i).hasChildNodes() && child.item(i).getFirstChild().getNodeValue()!=null && child.item(i).getFirstChild().getNodeValue().trim().length()>0)
						sortie.write(transform(child.item(i).getFirstChild().getNodeValue())+"\n");
					else
						sortie.write("\n");
				}
				else{	//make others line if the source is at the third level
					for(j=0;j<child2.getLength();j++){
						if(child2.item(j).getNodeType()!=Node.TEXT_NODE){
							child=child2.item(j).getChildNodes();
							for(i=0;i<child.getLength()-1;i++){
								if(child.item(i).hasChildNodes() && !child.item(i).getFirstChild().hasChildNodes())
									if(child.item(i).getFirstChild().getNodeValue().trim().length()>0)
										sortie.write(transform(child.item(i).getFirstChild().getNodeValue())+separator);
								else									
										sortie.write(separator);
							}
							//sortie.write(""+child+":"+child.getLength()+"\n");
							if(child.getLength()>1){
							if(child.item(i).hasChildNodes() && !child.item(i).getFirstChild().hasChildNodes())
								if(child.item(i).getFirstChild().getNodeValue().trim().length()>0)
									sortie.write(transform(child.item(i).getFirstChild().getNodeValue())+"\n");							
							else
								sortie.write("\n");
							}
						}	
					}
				}				
				
				
			}
			sortie.close();
		}
		catch (Exception e) {
			throw new EngineException("Unable to write to csv file",e);
		}
	}


	
	private int codeProfondeur(Node root){				
		
		Node fils1;			
		if(root.hasChildNodes()){
			fils1=root.getFirstChild();
			while(fils1.getNodeType()==Node.TEXT_NODE && fils1.getNodeValue().trim().length()==0 && fils1.getNextSibling()!=null)
				fils1=fils1.getNextSibling();
			if(fils1.getNodeType()==Node.TEXT_NODE){
				return 1;
			}					
				return 2+codeProfondeur(fils1);				
			
		}
		else{return 0;}			
		
	}
	*/
}
