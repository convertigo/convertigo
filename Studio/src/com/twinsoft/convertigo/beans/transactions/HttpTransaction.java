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

package com.twinsoft.convertigo.beans.transactions;

import java.io.UnsupportedEncodingException;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.util.Base64;

public class HttpTransaction extends AbstractHttpTransaction {
    
	private static final long serialVersionUID = -3501999011297702027L;

	private static int HTTP_DATA_ENCODING_STRING = 0;
    private static int HTTP_DATA_ENCODING_BASE64 = 1;
    
    private int dataEncoding = HTTP_DATA_ENCODING_STRING;
    
    public int getDataEncoding() {
		return dataEncoding;
	}

	public void setDataEncoding(int dataEncoding) {
		this.dataEncoding = dataEncoding;
	}

    public HttpTransaction() {
		super();
    }

	@Override
	public void makeDocument(byte[] httpData) throws Exception {
        String t = context.statistics.start(EngineStatistics.GENERATE_DOM);

        try {
        	String stringData = "";
        	
        	if (httpData == null || httpData.length == 0) {
        		// nothing to do
        	}
        	else if (dataEncoding == HTTP_DATA_ENCODING_STRING) {
            	String charset  = ((HttpConnector) parent).getCharset();

            	if (charset == null) {
            		charset = "ascii";
            	}
            	try {
            		stringData = new String(httpData, charset);
            	} catch (UnsupportedEncodingException e) {
            		Engine.logBeans.warn("(HttpTransaction) Unsupported Encoding to decode the response, use ascii instead",  e);
            		stringData = new String(httpData, "ascii");
            	}
        	}
        	else if (dataEncoding == HTTP_DATA_ENCODING_BASE64) {
        		stringData = Base64.encodeBytes(httpData);
        	}
        	else {
        		throw new IllegalArgumentException("Unknown data encoding: " + dataEncoding);
        	}
    		
        	CDATASection cdata = context.outputDocument.createCDATASection(stringData); // remove TextCodec.UTF8Encode for #453
        	Element outputDocumentRootElement = context.outputDocument.getDocumentElement();
        	outputDocumentRootElement.appendChild(cdata);
        }
        finally {
    		context.statistics.stop(t);
        }
    }
}