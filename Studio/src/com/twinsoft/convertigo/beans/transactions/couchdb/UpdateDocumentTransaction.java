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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;

import org.codehaus.jettison.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.cdbproxy.CouchClient;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class UpdateDocumentTransaction extends AbstractDocumentTransaction {

	private static final long serialVersionUID = -7606732916561433014L;
	
	public UpdateDocumentTransaction() {
		super();
	}

	@Override
	public UpdateDocumentTransaction clone() throws CloneNotSupportedException {
		UpdateDocumentTransaction clonedObject =  (UpdateDocumentTransaction) super.clone();
		return clonedObject;
	}
	
	@Override
	public List<CouchDbParameter> getDeclaredParameters() {
		return Arrays.asList(new CouchDbParameter[] {var_database, var_id, var_data});
	}
		
	@Override
	protected Object invoke() throws Exception {
		if (getCouchClient() != null) {
			CouchClient client = getCouchClient();
			JSONObject jsonDocument = new JSONObject();
			
			// add document members from variables
			for (RequestableVariable variable : getVariablesList()) {
				String variableName = variable.getName();

				if (!variableName.equals(var_database.variableName())) {
					Object jsonElement = toJson(getParameterValue(variableName));
					addJson(jsonDocument, variableName, jsonElement);
				}
			}
			
			JSONObject response = client.updateDocument(getTargetDatabase(), jsonDocument);
			return response;
		}
		
		// build input document
		JsonObject jsonDocument = new JsonObject();
		
		// add document members from variables
		for (RequestableVariable variable : getVariablesList()) {
			String variableName = variable.getName();
			if (variableName.equals(var_database.variableName())) continue;
			JsonElement jsonElement = toJson(getGson(), new JsonParser(), getParameterValue(variableName));
			addJson(jsonDocument, variableName, jsonElement);
		}
		
		// retrieve document from database
		JsonObject jsonDatabaseDoc = getCouchDBDocument().get(getIdFromDoc(jsonDocument), null).getAsJsonObject();
		
		// merge documents
		merge(jsonDatabaseDoc,jsonDocument);
		
		String jsonString = jsonDatabaseDoc.toString();
		return getCouchDBDocument().update(encode(jsonString));
	}
	
	private static void merge(JsonObject jsonTarget, JsonObject jsonSource) {
		Set<Entry<String, JsonElement>> set = jsonSource.entrySet();
		for (Iterator<Entry<String, JsonElement>> it = GenericUtils.cast(set.iterator()); it.hasNext();) {
			Entry<String, JsonElement> entry = it.next();
			String key = entry.getKey();
			if (jsonTarget.has(key)) {
				mergeEntries(getEntry(jsonTarget, key), entry);
			}
			else {
				jsonTarget.add(key, entry.getValue());
			}
		}
	}
	
	private static void merge(JsonArray targetArray, JsonArray sourceArray) {
		int targetSize = targetArray.size();
		int sourceSize = sourceArray.size();
		
		/*while (targetSize > sourceSize) {
			targetArray.remove(targetSize-1);
			targetSize--;
		}*/
		
		for (int i=0; i<sourceSize; i++) {
			JsonElement targetValue = targetSize > i ? targetArray.get(i):null;
			JsonElement sourceValue = sourceArray.get(i);
			if (sourceValue!=null && targetValue!=null) {
				if (targetValue.isJsonObject() && sourceValue.isJsonObject()) {
					merge(targetValue.getAsJsonObject(), sourceValue.getAsJsonObject());
				}
				else if (targetValue.isJsonArray() && sourceValue.isJsonArray()) {
					merge(targetValue.getAsJsonArray(), sourceValue.getAsJsonArray());
				}
				else {
					targetArray.set(i, sourceValue);
				}
			}
			else if (sourceValue!=null && targetValue==null) {
				targetArray.add(sourceValue);
			}
		}
	}
	
	private static void mergeEntries(Entry<String, JsonElement> targetEntry, Entry<String, JsonElement> sourceEntry) {
		JsonElement targetValue = targetEntry.getValue();
		JsonElement sourceValue = sourceEntry.getValue();
		if (targetValue.isJsonObject() && sourceValue.isJsonObject()) {
			merge(targetValue.getAsJsonObject(), sourceValue.getAsJsonObject());
		}
		else if (targetValue.isJsonArray() && sourceValue.isJsonArray()) {
			merge(targetValue.getAsJsonArray(), sourceValue.getAsJsonArray());
		}
		else {
			targetEntry.setValue(sourceValue);
		}
	}

	private static Entry<String, JsonElement> getEntry(JsonObject jso, String key) {
		Set<Entry<String, JsonElement>> set = jso.entrySet();
		for (Iterator<Entry<String, JsonElement>> it = GenericUtils.cast(set.iterator()); it.hasNext();) {
			Entry<String, JsonElement> entry = it.next();
			if (entry.getKey().equals(key)) {
				return entry;
			}
		}
		return null;
	}
	
	@Override
	public QName getComplexTypeAffectation() {
		return new QName(COUCHDB_XSD_NAMESPACE, "docUpdateType");
	}
}
