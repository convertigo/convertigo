/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.servlets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.requesters.Requester;

public class RsaPublicKeyServlet extends GenericServlet {
	private static final long serialVersionUID = -1474797856216501296L;
	
	public RsaPublicKeyServlet() {
	}   	

    public String getName() {
        return "RsaPublicKeyServlet";
    }

    @Override
    public String getServletInfo() {
        return "Twinsoft Convertigo RsaPublicKeyServlet";
    }
    
    public String getDocumentExtension() {
        return "*";
    }

	public Requester getRequester() {
		return null;
	}
	
	@Override
	protected void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
		response.getWriter().print(Engine.theApp.rsaManager.getPublicKey(request.getSession()));
	}
}
