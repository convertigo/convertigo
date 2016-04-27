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

package com.twinsoft.convertigo.eclipse.editors.jscript;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.wst.jsdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.util.QuickSort;
import com.twinsoft.util.QuickSortItem;
import com.twinsoft.util.StringEx;




public class MyJScriptCompletionProcessor implements IJavaCompletionProposalComputer
{
	protected 	char 	completionProposalAutoActivationCharacters[] = {'.'};
    private 	String 	objectNamePrefix = "";
    private 	Class<?> 	clazz;

    private String formatType(String type) {
        int i = type.lastIndexOf('.');
        if (i != -1) return type.substring(i + 1);
        else return type;
    }
	
    private Vector<String> introspect() {
        try {
            if (clazz == null) {
                Vector<String> vPublicObjects = new Vector<String>(3);
                vPublicObjects.add("[entry] \"redetect\"");
                vPublicObjects.add("[entry] \"skip\"");
                vPublicObjects.add("[exit] \"accumulate\"");
                return vPublicObjects;
            }

            QuickSort quickSort;
            Field[] fields = clazz.getFields();
            Method[] methods = clazz.getMethods();
            Vector<String> vPublicObjects = new Vector<String>(fields.length + methods.length);
            Vector<Object> vTmp;

            // Find public fields
            vTmp = new Vector<Object>(fields.length);
            for (int i = 0 ; i < fields.length ; i++) {
                vTmp.add(fields[i].getName());
            }
            quickSort = new QuickSort(vTmp);
            vTmp = GenericUtils.cast(quickSort.perform(true));

            Field field;
            String fieldName, fieldSignature, fieldDescription;
            for (int i = 0 ; i < fields.length ; i++) {
                field = clazz.getField((String) vTmp.elementAt(i));
                fieldName = field.getName();
                fieldDescription = field.toString();

                if (fieldName.startsWith(objectNamePrefix)) {
                    fieldSignature = formatType(field.getType().getName()) + " " + fieldName;
                    vPublicObjects.add(fieldSignature + "("+fieldDescription+")");
                }
            }

            if (clazz == HandlerObjects.class) return vPublicObjects;
            
            // Find public methods
            vTmp = new Vector<Object>(methods.length);
            Method method;
            for (int i = 0 ; i < methods.length ; i++) {
                method = methods[i];
                vTmp.add(new QuickSortItem(method) {
                    public Object getQuickSortValue() {
                        return ((Method) object).getName();
                    }
                });
            }
            quickSort = new QuickSort(vTmp);
            vTmp = GenericUtils.cast(quickSort.perform(true));

            String helpFilePath = Engine.WEBAPP_PATH + "/javadoc/com/twinsoft/twinj/iJavelin.html";
            File helpFile = new File(helpFilePath);

            String helpFileContent = null;
            try {
            	BufferedReader br = new BufferedReader(new FileReader(helpFile));
            	String line;
            	StringBuffer sb = new StringBuffer((int) helpFile.length());
            	while ((line = br.readLine()) != null) {
            		sb.append(line);
            	}
        		helpFileContent = sb.toString();
        		br.close();
        		
        		/*String[] defs = helpFileContent.split("<HR>");
        		for (int i=0; i<defs.length; i++)
        			System.out.println(defs[i]);*/
            }
            catch(Exception e) {
            	ConvertigoPlugin.logWarning("Help file not found");
            }
            
            String methodName, methodSignature, paramName, methodJavadoc, paramSignature;
            for (int i = 0 ; i < methods.length ; i++) {
                method = (Method) ((QuickSortItem) vTmp.elementAt(i)).getObject();
                methodName = method.getName();
            	
                if (methodName.startsWith(objectNamePrefix)) {
                	paramSignature = "";
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    for (int j = 0 ; j < parameterTypes.length ; j++) {
                        paramSignature += parameterTypes[j].getName();
                        if (j < parameterTypes.length - 1) paramSignature += ", ";
                    }

                    // Trying to extract the best method signature from the javelin javadoc
                    if ((clazz == com.twinsoft.twinj.iJavelin.class) && (helpFileContent != null)) {
                    	String searchedText = "com/twinsoft/twinj/iJavelin.html#" + methodName + "(" + paramSignature + ")";
                    	int j = helpFileContent.indexOf(searchedText);
                    	if (j != -1) {
                    		j += searchedText.length() + 2;
                    		int k = helpFileContent.indexOf(")", j);
                    		
                    		if (k != -1) {
	                    		methodJavadoc = helpFileContent.substring(j, k + 1);
	                    		/*int z = helpFileContent.indexOf("<HR>",k);
	                    		if (z != -1) {
	                    			methodDescription = helpFileContent.substring(j, z);
	                    			int y = methodDescription.indexOf("<B>See Also:</B>");
	                    			if (y != -1)
	                    				methodDescription = methodDescription.substring(0, y);
	                    		}
	                    		else
	                    			methodDescription = helpFileContent.substring(j);
	                    		*/
	                    		
	                    		StringEx sx = new StringEx(methodJavadoc);
	                    		sx.replaceAll("  ", "");
	                    		sx.replaceAll("</A>", "");
	                    		sx.replaceAll("</B>", "");
	                    		sx.replaceAll("&nbsp;", " ");
	                    		sx.replaceAll("java.lang.String", "String");
	                    		sx.replaceAll("\n", " ");
	
	                            methodSignature = formatType(method.getReturnType().getName()) + " " + sx.toString();
	                            vPublicObjects.add(methodSignature);
	
	                            continue;
                    		}
                    	}
                    }

                    // Standard method signature
                    methodSignature = formatType(method.getReturnType().getName()) + " " + methodName + "(";

                    parameterTypes = method.getParameterTypes();
                    for (int j = 0 ; j < parameterTypes.length ; j++) {
                        methodSignature += formatType(parameterTypes[j].getName());

                    	paramName = "param" + (j+1);
                    	methodSignature += " " + paramName;

                        if (j < parameterTypes.length - 1) methodSignature += ", ";
                    }

                    methodSignature += ")";

                    vPublicObjects.add(methodSignature);
                }
            }
            return vPublicObjects;
        }
        catch(NoSuchFieldException e) {
            return null;
        }
    }


	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#computeContextInformation(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
    @SuppressWarnings("rawtypes")
	@Override
	public List computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		return null;
	}
    
    public static Class<?> findObjectClass(String sLine) {
        if (sLine.length() == 0) return HandlerObjects.class;
        
        int indexSeparator = sLine.lastIndexOf('.');
        if (indexSeparator == -1) {
            char lastChar = sLine.charAt(sLine.length() - 1);

            if (!Character.isJavaIdentifierPart(lastChar)) return HandlerObjects.class;
            else return null;
        }
        // Convertigo objects method/field call case
        else {
            String sObject = sLine.substring(0, indexSeparator);

            if (sObject.endsWith("javelin")) return com.twinsoft.twinj.iJavelin.class;
            else if (sObject.endsWith("log")) return com.twinsoft.util.Log.class;
            else if (sObject.endsWith("steps")) return java.util.Vector.class;
            else if (sObject.endsWith("dom")) return org.w3c.dom.Document.class;
            else if (sObject.endsWith("context")) return com.twinsoft.convertigo.engine.AbstractContext.class;
            else return null;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) 
	{
		IDocument				document	= context.getDocument();
		int						offset		= context.getInvocationOffset();

		try {
			int 	lineNo 		= document.getLineOfOffset(offset);
			int 	lineOffset 	= document.getLineInformation(lineNo).getOffset();
			int 	lineLen    	= document.getLineInformation(lineNo).getLength();
			String	sLine 		= document.get(lineOffset, lineLen);

	        clazz = findObjectClass(sLine);
	        if (clazz == null) {
	        	ICompletionProposal[] result = new ICompletionProposal[0];
	            return Arrays.asList(result);
	        }
			
	        int indexSeparator = sLine.lastIndexOf('.');
	        objectNamePrefix = sLine.substring(indexSeparator + 1);
	        
	        // Retrieve description of properties/methods
			Object[] entries = introspect().toArray();

			// Computes proposals
			ICompletionProposal[] result = new ICompletionProposal[entries.length];
			for (int i = 0; i < entries.length; i++) {
				String entry = (String) entries[i];
				
				//Set additional information
				String descr = "/** TODO : retrieve description*/ " + entry;
				
				//Set the string to display
				int index = entry.indexOf(" ");
				String showing = entry.substring(index+1);
				
				//Set the replacement string
				String value = showing.substring(objectNamePrefix.length());
				
				IContextInformation info = new ContextInformation(showing, descr);
				result[i] = new CompletionProposal(value, offset, 0, value.length(), null, showing, info, descr);
			}
	        
			return Arrays.asList(result);
			
		} catch (BadLocationException e) {
			return null;
		}
	}
	
	public void sessionEnded() {
		return;
	}

	/*
	 * @see org.eclipse.wst.jsdt.ui.text.java.IJavaCompletionProposalComputer#sessionStarted()
	 */
	public void sessionStarted() {
		return;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#getErrorMessage()
	 */
	public String getErrorMessage() {
		return "";
	}
	
}
