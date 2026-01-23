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

package com.twinsoft.convertigo.engine.servlets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.fop.apps.FopFactory;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.requesters.PdfServletRequester;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.util.FopUtils;

public class PdfServlet extends GenericServlet {

	private static final long serialVersionUID = -851070384164515957L;
	
	private FopFactory fopFactory; // The reusable fop factory instance

	public PdfServlet() {
    }

	@Override
	public void init() throws ServletException {
		super.init();
		fopFactory = FopUtils.newFopFactory(null, Engine.TEMPLATES_PATH);
	}

	@Override
	public String getCacheControl(HttpServletRequest request) {
		return "true";    	
	}

	public String getName() {
        return "PdfServlet";
    }

    public String getDefaultContentType() {
    	return MimeType.Pdf.value();
    }

    public String getServletInfo() {
        return "Twinsoft Convertigo PdfServlet";
    }
    
    public String getDocumentExtension() {
        return ".cpdf";
    }
    
    public Requester getRequester() {
    	return new PdfServletRequester(fopFactory);
    }
}
