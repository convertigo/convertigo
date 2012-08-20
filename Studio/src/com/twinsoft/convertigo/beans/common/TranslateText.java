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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/branches/6.1.3/Studio/src/com/twinsoft/convertigo/beans/common/ReplaceText.java $
 * $Author: fabienb $
 * $Revision: 28379 $
 * $Date: 2011-09-27 11:38:59 +0200 (mar., 27 sept. 2011) $
 */

package com.twinsoft.convertigo.beans.common;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
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
		
		public DictionaryEntry() {
			if (generateOrphans) {
				orphans = Collections.synchronizedMap(new HashMap<String, String>());
			}
		}
	}
	
	private final static Map<String, DictionaryEntry> cachedDictionaries = new HashMap<String, TranslateText.DictionaryEntry>();
    
    private String dictionary = "dictionary.txt";
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
    	
    	if (lang != null && lang.length() > 0) {
    		lang = "." + lang;
    	}
    	
    	// retrieve fullpath of the dictionary    	
    	String filepath = Engine.theApp.filePropertyManager.getFilepathFromProperty(dictionary + lang, getProject().getName());
    	
    	// retrieve the previous loaded dictionary
    	DictionaryEntry dictionaryEntry;
    	synchronized (cachedDictionaries) {
    		dictionaryEntry = cachedDictionaries.get(filepath);
    		if (dictionaryEntry == null) {
    			dictionaryEntry = new DictionaryEntry();
    			cachedDictionaries.put(filepath, dictionaryEntry);
    		}
		}
    	
    	// check dictionary validity
    	Map<String, String> words;
    	synchronized (dictionaryEntry) {
			File file = new File(filepath);
			if (file.lastModified() > dictionaryEntry.lastModified) {
				dictionaryEntry.lastModified = -1;
			}
			
			// read the dictionary
    		if (dictionaryEntry.lastModified == -1) {
    			dictionaryEntry.lastModified = file.lastModified();
				dictionaryEntry.words.clear();
				try {
					FileUtils.loadUTF8Properties(dictionaryEntry.words, file);
				} catch (Exception e) {
					Engine.logBeans.error("(TranslateText) Can't read the propertie file \"" + filepath + "\" : " + e.getMessage());
				}
				if (generateOrphans) {
					String orphanspath = filepath + ".orphans";
					try {
						dictionaryEntry.orphans.clear();
						FileUtils.loadUTF8Properties(dictionaryEntry.orphans, new File(orphanspath));
					} catch (Exception e) {
						Engine.logBeans.debug("(TranslateText) The optional propertie file \"" + orphanspath + "\" cannot be load : " + e.getMessage());
					}
				}
    		}
    		words = dictionaryEntry.words;
		}
    	
    	// Translate the block text
        String txt = block.getText();
        
        if (txt.length() != 0) {
	        String txtTranslated = words.get(txt);
	        
	        if (txtTranslated != null) {
	        	block.setText(txtTranslated);
	        } else if (generateOrphans) {
	        	synchronized (dictionaryEntry.orphans) {
	            	Object ex = dictionaryEntry.orphans.put(txt, txt);
	            	if (ex == null) {
						String orphanspath = filepath + ".orphans";
	            		try {
	            			FileUtils.saveUTF8Properties(dictionaryEntry.orphans, new File(orphanspath));
						} catch (IOException e) {
							Engine.logBeans.warn("(TranslateText) Can't write the orphan propertie file \"" + orphanspath + "\" : " + e.getMessage());
						}
	            	}
				}
	        }
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

	public boolean isGenerateOrphans() {
		return generateOrphans;
	}

	public void setGenerateOrphans(boolean generateOrphans) {
		this.generateOrphans = generateOrphans;
	}
}
