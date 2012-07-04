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

package com.twinsoft.convertigo.beans.connectors;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.VersionUtils;

/**
 * The Connector class is the base class for all connectors.
 */
public class ProxyHttpConnector extends HttpConnector {

	private static final long serialVersionUID = 852957358237804395L;

	public ProxyHttpConnector() {
        super();
	}
	
	@Override
	public ProxyHttpConnector clone() throws CloneNotSupportedException {
		ProxyHttpConnector clonedObject = (ProxyHttpConnector) super.clone();

		clonedObject.replacementsForMimeType = new HashMap<String, Replacements>();

		return clonedObject;
	}
	
	private XMLVector<XMLVector<String>> removableHeaders = new XMLVector<XMLVector<String>>();
	
	public void setRemovableHeaders(XMLVector<XMLVector<String>> removableHeaders) {
		this.removableHeaders = removableHeaders;
		removableHeadersSet = null;		
	}

	public XMLVector<XMLVector<String>> getRemovableHeaders() {
		return removableHeaders;
	}
	
	transient private Collection<String> removableHeadersSet = null;
	
	public Collection<String> getRemovableHeadersSet() {
		if (removableHeadersSet == null) {
			removableHeadersSet = new HashSet<String>();
			for (XMLVector<String> header : removableHeaders) {
				removableHeadersSet.add(header.get(0).toLowerCase());
			}
			removableHeadersSet = Collections.unmodifiableCollection(removableHeadersSet);
		}
		return removableHeadersSet;
	}
	
	private XMLVector<XMLVector<String>> replacements = new XMLVector<XMLVector<String>>();

	public XMLVector<XMLVector<String>> getReplacements() {
		return replacements;
	}

	public void setReplacements(XMLVector<XMLVector<String>> replacements) {
		this.replacements = replacements;
		replacementsForMimeType = new HashMap<String, Replacements>();
	}

	transient private Map<String, Replacements> replacementsForMimeType = new HashMap<String, Replacements>();
	
	public Replacements getReplacementsForMimeType(String mimeType) {
		if (mimeType == null) throw new IllegalArgumentException("The parameter 'mimeType' can not be null");
		
		Replacements filteredReplacements = replacementsForMimeType.get(mimeType);
		
		if (filteredReplacements != null) {
			try {
				return (Replacements) filteredReplacements.clone();
			} catch (CloneNotSupportedException e) {
				// Silently ignore: should never occur!
				return null;
			}
		}
		
		List<String> strSearched = new LinkedList<String>();
		List<String> strReplacing = new LinkedList<String>();
		
		for (List<String> replacement : replacements)
			if (mimeType.indexOf(replacement.get(0)) != -1) {
				strSearched.add(replacement.get(1));
				strReplacing.add(replacement.get(2));
			}
		
		filteredReplacements = new Replacements();
		filteredReplacements.strSearched = (String[]) strSearched.toArray(new String[strSearched.size()]);
		filteredReplacements.strReplacing = (String[]) strReplacing.toArray(new String[strReplacing.size()]);
		
		replacementsForMimeType.put(mimeType, filteredReplacements);

		try {
			return (Replacements) filteredReplacements.clone();
		} catch (CloneNotSupportedException e) {
			// Silently ignore: should never occur!
			return null;
		}
	}
	
	public class Replacements implements Cloneable {
		public String[] strSearched;
		public String[] strReplacing;
		
		public boolean isEmpty() {
			return strSearched.length == 0;
		}
		
		@Override
		public Object clone() throws CloneNotSupportedException {
			Replacements replacements = new Replacements();
			replacements.strSearched = (String[]) strSearched.clone();
			replacements.strReplacing = (String[]) strReplacing.clone();
			return replacements;
		}
	}
	
	private String dynamicContentFiles = ".jsp .asp .aspx .php";

	public String getDynamicContentFiles() {
		return dynamicContentFiles;
	}

	public void setDynamicContentFiles(String dynamicContentFiles) {
		this.dynamicContentFiles = dynamicContentFiles;
	}
	
    @Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
		String version = element.getAttribute("version");
		
		 if (version!= null && VersionUtils.compareMigrationVersion(version, ".m004") < 0) {
	        	Engine.logDatabaseObjectManager.info("Migration to m003 for ProxyHttpConnector.replacements");
	        	for (XMLVector<String> replacement : replacements) {
	        		if (replacement.size() < 4) {
	        			replacement.add("comment");
	        		}
	        		hasChanged = true;
	        	}
		 }
    }
}
