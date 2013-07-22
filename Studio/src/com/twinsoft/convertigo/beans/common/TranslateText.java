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

package com.twinsoft.convertigo.beans.common;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.twinj.iJavelin;

public class TranslateText extends JavelinExtractionRule {

	public static final long serialVersionUID = -6735666205939933877L;
	
	private class DictionaryEntry {
		Map<String, String> words = Collections.synchronizedMap(new HashMap<String, String>());
		Map<String, String> orphans;
		long lastModified = -1;
	}
	
	private final static Map<String, DictionaryEntry> cachedDictionaries = new HashMap<String, TranslateText.DictionaryEntry>();
    
    private String dictionary = "";
    private String encoding = "UTF-8";
    private boolean generateOrphans = false;
    
    /** Creates new ReplaceText */
    public TranslateText() {
        super();
    }
    
    /**
     * Applies the extraction rule to the current iJavelin object.
     *
     * @param javelin the Javelin object.
     * @param block the current block to analyze.
     * @param blockFactory the block context of the current block.
     * @param dom the XML DOM.
     *
     * @return an ExtractionRuleResult object containing the result of
     * the query.
     */
    public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
    	JavelinExtractionRuleResult xrs  =  new JavelinExtractionRuleResult();
    	
    	String lang = dom.getDocumentElement().getAttribute("lang");
    	
    	if (lang == null || lang.length() > 0) {
	    	// retrieve fullpath of the dictionary
	    	String filepath = Engine.theApp.filePropertyManager.getFilepathFromProperty(dictionary + "_" + lang + ".txt", getProject().getName());
	    	String orphanspath = null;
	    	if (generateOrphans) {
	    		orphanspath = Engine.theApp.filePropertyManager.getFilepathFromProperty(dictionary + "_" + lang + "_orphans.txt", getProject().getName());
	    	}
	    	
	    	Engine.logBeans.trace("(TranslateText) Waiting for chachedDictionaries for " + filepath);
	    	
	    	// retrieve the previous loaded dictionary
	    	DictionaryEntry dictionaryEntry;
	    	synchronized (cachedDictionaries) {
	    		dictionaryEntry = cachedDictionaries.get(filepath);
	    		if (dictionaryEntry == null) {
	    			Engine.logBeans.debug("(TranslateText) Create a new dictionaryEntry for " + filepath);
	    			dictionaryEntry = new DictionaryEntry();
	    			cachedDictionaries.put(filepath, dictionaryEntry);
	    		}
			}
	    	
	    	// check dictionary validity
	    	Map<String, String> words;
	    	synchronized (dictionaryEntry) {
				File file = new File(filepath);
				
				if (!file.exists()) {
					file.getParentFile().mkdirs();
					Engine.logBeans.error("(TranslateText) The dictionary file \"" + filepath + "\" does not exist");
				} else {
					if (file.lastModified() > dictionaryEntry.lastModified) {
						dictionaryEntry.lastModified = -1;
					}
					
					// read the dictionary
		    		if (dictionaryEntry.lastModified == -1) {
		    			Engine.logBeans.trace("(TranslateText) Dictionary change for " + filepath);
		    			dictionaryEntry.lastModified = file.lastModified();
						dictionaryEntry.words.clear();
						try {
							FileUtils.loadProperties(dictionaryEntry.words, file, encoding);
							Engine.logBeans.debug("(TranslateText) Dictionary had load " + dictionaryEntry.words.size() + " rules");
						} catch (Exception e) {
							dictionaryEntry.words.clear();
							Engine.logBeans.error("(TranslateText) Error while opening dictionary.\nCan't read the dictionary file \"" + filepath + "\" :\n" + e.getMessage() + "\nPlease refer to the rule documentation.");
						}
		    		}
				}
				if (generateOrphans) {
					if (dictionaryEntry.orphans == null) {
						dictionaryEntry.orphans = Collections.synchronizedMap(new LinkedHashMap<String, String>());
						Engine.logBeans.trace("(TranslateText) Prepare to generate orphans words");
						
						try {
							FileUtils.loadProperties(dictionaryEntry.orphans, new File(orphanspath), encoding);
						} catch (Exception e) {
							Engine.logBeans.debug("(TranslateText) The optional dictionary file \"" + orphanspath + "\" cannot be load :\n" + e.getMessage());
						}
					}
				} else {
					dictionaryEntry.orphans = null;
				}
		    	words = dictionaryEntry.words;
			}
	    	
	    	// Translate the block text
	        String txt = block.getText();
	        Engine.logBeans.debug("(TranslateText) Search translation for the string \"" + txt + "\"");
	        
	        if (txt.length() != 0) {
		        String txtTranslated = words.get(txt);
		        
		        if (txtTranslated != null) {
		        	Engine.logBeans.debug("(TranslateText) Translated to \"" + txtTranslated + "\"");
		        	block.setText(txtTranslated);
		        } else if (generateOrphans) {
		        	synchronized (dictionaryEntry.orphans) {
		            	Object ex = dictionaryEntry.orphans.put(txt, txt);
		            	File file = new File(orphanspath);
		            	if (ex == null || !file.exists()) {
		            		try {
		            			Engine.logBeans.debug("(TranslateText) New orphan word \"" + txt + "\", write to " + orphanspath);
		            			FileUtils.saveProperties(dictionaryEntry.orphans, file, encoding);
							} catch (IOException e) {
								Engine.logBeans.warn("(TranslateText) Can't write the orphan dictionary file \"" + orphanspath + "\" :\n" + e.getMessage());
							}
		            	}
					}
		        }
	        }
    	} else {
    		Engine.logBeans.debug("(TranslateText) No lang, no translation.");
    	}
    	
        xrs.hasMatched = true;
        xrs.newCurrentBlock = block;
        return xrs;
    }
    
    /** Getter for property dictionary.
     * @return Value of property dictionary.
     */
    public String getDictionary() {
        return dictionary;
    }
    
    /** Setter for property dictionary.
     * @param dictionary New value of property dictionary.
     */
    public void setDictionary(String dictionary) {
        this.dictionary = dictionary;
    }

	public boolean getGenerateOrphans() {
		return generateOrphans;
	}

	public void setGenerateOrphans(boolean generateOrphans) {
		this.generateOrphans = generateOrphans;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
}
