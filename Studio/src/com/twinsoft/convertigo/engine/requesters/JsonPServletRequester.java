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

package com.twinsoft.convertigo.engine.requesters;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.ExpiredSecurityTokenException;
import com.twinsoft.convertigo.engine.NoSuchSecurityTokenException;
import com.twinsoft.convertigo.engine.enums.Parameter;

public class JsonPServletRequester extends JsonServletRequester {

    public JsonPServletRequester() {
    }

    @Override
    public String getName() {
        return "JsonPServletRequester";
    }

    @Override
	protected void handleParameter(Context context, String parameterName, String parameterValue) throws NoSuchSecurityTokenException, ExpiredSecurityTokenException {
		super.handleParameter(context, parameterName, parameterValue);
		
		// This gives the required context name
		if (parameterName.equals(Parameter.JsonCallback.getName())) {
			context.set("jsonpCallback", parameterValue);
		}
	}
    
	@Override
	public String postGetDocument(Document document) throws Exception {
		String callback = (String) context.get("jsonpCallback");
		String json = super.postGetDocument(document);
		return (callback!=null)?
			(callback+"("+json+");"):
			json;
	}
}