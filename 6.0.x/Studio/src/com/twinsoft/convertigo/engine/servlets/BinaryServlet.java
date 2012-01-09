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

package com.twinsoft.convertigo.engine.servlets;

import com.twinsoft.convertigo.engine.requesters.BinaryServletRequester;
import com.twinsoft.convertigo.engine.requesters.Requester;

public class BinaryServlet extends GenericServlet {
	private static final long serialVersionUID = 8273215871882400280L;

	public BinaryServlet() {
    }

    public String getName() {
        return "BinaryServlet";
    }

    @Override
    public String getDefaultContentType() {
    	return "application/bin";
    }

    @Override
    public String getServletInfo() {
        return "Twinsoft Convertigo BinaryServlet";
    }
    
    @Override
    public String getDocumentExtension() {
        return ".bin";
    }

    @Override
	public Requester getRequester() {
		return new BinaryServletRequester();
	}
}