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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */
package com.twinsoft.convertigo.beans.transactions.couchdb;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Document;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchClient;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class GetViewTransaction extends AbstractDocumentTransaction implements ITagsProperty {

	private static final long serialVersionUID = -3684374492418313635L;
	
	private String viewname = "";

	public GetViewTransaction() {
		super();
	}

	@Override
	public GetViewTransaction clone() throws CloneNotSupportedException {
		GetViewTransaction clonedObject = (GetViewTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	public List<CouchDbParameter> getDeclaredParameters() {
		return getDeclaredParameters(var_database, var_docid, var_viewname, 
				var_view_limit, var_view_skip, var_view_key, var_view_startkey, var_view_endkey, var_view_reduce);
	}

	@Override
	protected Object invoke() throws Exception {
		String docId, viewname;
		
		Pattern splitViewname = Pattern.compile("(.+?)/(.+)");
		Matcher mSplitViewname = splitViewname.matcher(this.viewname);
		
		if (mSplitViewname.matches()) {
			docId = mSplitViewname.group(1);
			viewname = mSplitViewname.group(2);
		} else {
			docId = getParameterStringValue(var_docid);
			viewname = getParameterStringValue(var_viewname);
		}
		
		List<NameValuePair> options = new LinkedList<NameValuePair>();
		
		for (RequestableVariable variable : getVariablesList()) {
			String variableName = variable.getName();

			if (!variableName.equals(var_database.variableName()) &&
					!variableName.equals(var_docid.variableName()) &&
					!variableName.equals(var_viewname.variableName())) {
				String value = getParameterStringValue(variableName);
				CouchClient.addParameter(options, variableName, value);
			}
		}
		
		return getCouchClient().getView(getTargetDatabase(), docId, viewname, options);
	}

	public String getViewname() {
		return viewname;
	}

	public void setViewname(String viewname) {
		this.viewname = viewname;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if ("viewname".equals(propertyName)) {
			List<String> values = new LinkedList<String>();
			values.add("");
			
			for (Document document : getConnector().getDocumentsList()) {
				if (document instanceof DesignDocument) {
					JSONObject views = CouchKey.views.JSONObject(((DesignDocument) document).getJSONObject());
					for (Iterator<String> i = GenericUtils.cast(views.keys()); i.hasNext(); ) {
						values.add(document.getName() + "/" + i.next());
					}
				}
			}
			return values.toArray(new String[values.size()]);
		}
		return null;
	}
}
