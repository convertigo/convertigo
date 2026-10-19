/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.requesters;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.EngineException;

/**
 * An internal requester answering the JSON a .json request answers (JsonServletRequester): the Studio runs the JSON
 * native sequences with it, so that their native outputs are shown as the clients receive them, without XML.
 */
public class InternalJsonRequester extends InternalRequester {

	public InternalJsonRequester(Map<String, Object> request, HttpServletRequest httpServletRequest) throws EngineException {
		super(request, httpServletRequest);
	}

	@Override
	public String getName() {
		return "InternalJsonRequester";
	}

	@Override
	public Object postGetDocument(Document document) throws Exception {
		return JsonServletRequester.toJson(context, document);
	}

	@Override
	protected Object addStatisticsAsData(Object result) {
		return result;
	}

	@Override
	protected Object addStatisticsAsText(String stats, Object result) {
		return result;
	}
}
