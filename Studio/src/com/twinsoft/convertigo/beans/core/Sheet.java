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

package com.twinsoft.convertigo.beans.core;

import com.twinsoft.convertigo.beans.common.XMLVector;

/**
 * This class defines a XSL sheet.
 */
public class Sheet extends DatabaseObject implements ITagsProperty{

	private static final long serialVersionUID = 2474285022734204501L;

    public static final String BROWSER_ALL = "*";
    
    /** Holds value of property browserSignature. */
    private String browser = Sheet.BROWSER_ALL;
    
    /** Holds value of property url. */
    private String url = "";
    
	/**
	 * Constructs a new Sheet object.
	 */
	public Sheet() {
        super();
		databaseType = "Sheet";
	}
    
    /** Getter for property browser.
     * @return Value of property browser.
     */
    public String getBrowser() {
        return browser;
    }
    
    /** Setter for property browser.
     * @param browser New value of property browser.
     */
    public void setBrowser(String browser) {
        this.browser = browser;
    }
    
    /** Getter for property url.
     * @return Value of property url.
     */
    public String getUrl() {
        return url;
    }
    
    /** Setter for property url.
     * @param url New value of property url.
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
	public String[] getTagsForProperty(String propertyName) {
		if(propertyName.equals("browser")){
	    	Project project = getProject();
			XMLVector<XMLVector<String>> vBrowsers = ((project == null) ? new XMLVector<XMLVector<String>>()
					: project.getBrowserDefinitions());

	        String[] browsers = new String[vBrowsers.size() + 1];
	        browsers[0] = Sheet.BROWSER_ALL;
	        int i = 1;
	        for(XMLVector<String> browserDef : vBrowsers) {
	        	browsers[i] = browserDef.elementAt(0);
	        	i++;
	        }
	        return browsers;
		}
		return new String[0];
	}
}