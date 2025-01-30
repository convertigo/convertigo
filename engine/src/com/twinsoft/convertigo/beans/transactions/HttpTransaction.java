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

package com.twinsoft.convertigo.beans.transactions;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Header;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.engine.AttachmentManager.Policy;
import com.twinsoft.convertigo.engine.AttachmentManager.Status;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.util.VersionUtils;

public class HttpTransaction extends AbstractHttpTransaction {

	private static final long serialVersionUID = -3501999011297702027L;

	private static int HTTP_DATA_ENCODING_STRING = 0;
	private static int HTTP_DATA_ENCODING_BASE64 = 1;

	private int dataEncoding = HTTP_DATA_ENCODING_STRING;
	private String dataStringCharset = "UTF-8";
	private boolean responseInCDATA = false;

	public int getDataEncoding() {
		return dataEncoding;
	}

	public void setDataEncoding(int dataEncoding) {
		this.dataEncoding = dataEncoding;
	}

	public String getDataStringCharset() {
		return dataStringCharset;
	}

	public void setDataStringCharset(String dataStringCharset) {
		this.dataStringCharset = dataStringCharset;
	}

	public boolean isResponseInCDATA() {
		return responseInCDATA;
	}

	public void setResponseInCDATA(boolean responseInCDATA) {
		this.responseInCDATA = responseInCDATA;
	}

	public HttpTransaction() {
		super();
	}

	@Override
	public void makeDocument(byte[] httpData) throws Exception {
		String t = context.statistics.start(EngineStatistics.GENERATE_DOM);

		try {
			/***********************************************************************
			 * For projects with no DownloadHttpTransaction - CEMS version < 8.0.0
			 * [SP47] Download file and add attachment
			 ***********************************************************************/
			if (getAllowDownloadAttachment() && Boolean.TRUE.equals(context.get("doAddAttachment"))) {
				try {
					String contentDisposition = null;
					Header[] responseHeaders = context.getResponseHeaders();
					for (int i = 0; i < responseHeaders.length; i++) {
						Header head = responseHeaders[i];
						if (HeaderName.ContentDisposition.is(head)) {
							contentDisposition = head.getValue();
							break;
						}
					}
					if (contentDisposition != null && context.contentType != null) {
						int i = contentDisposition.indexOf("filename=");
						if (i != -1) {
							contentDisposition += ";";
							int j = contentDisposition.indexOf(';', i + "filename=".length());
							if (j != -1) {
								String referer = ((HttpConnector) parent).getReferer();
								String downloadDir = getParameterStringValue("downloadDir");
								String downloadPolicy = getParameterStringValue("downloadPolicy");
								String downloadStatus = getParameterStringValue("downloadStatus");
								Policy policy = downloadPolicy != null ? Policy.valueOf(downloadPolicy) : null;
								Status status = downloadStatus != null ? Status.valueOf(downloadStatus) : Status.direct;
								String filename = contentDisposition.substring(i + "filename=".length(), j);
								String filepath = downloadDir != null ? downloadDir + (downloadDir.endsWith("/") ? "":"/") + filename : filename;
								getAttachmentManager().addAttachment(httpData, filename, context.contentType, referer, null, policy, filepath, status);
								httpData = new byte[] {};
							}
						}
					}
				} catch (Exception e) {
					Engine.logBeans.error("(HttpTransaction) Unable to handle attachment", e);
				}
			}
			/***********************************************************************/

			String stringData = "";

			if (httpData == null || httpData.length == 0) {
				// nothing to do
			}
			else if (dataEncoding == HTTP_DATA_ENCODING_STRING) {
				String charset = dataStringCharset;

				if (StringUtils.isBlank(charset)) {
					charset  = ((HttpConnector) parent).getCharset();
					Engine.logBeans.debug("(HttpTransaction) 'HTTP string charset' is blank, use detected charset: " + charset);
				}

				if (charset == null) {
					charset = "ascii";
					Engine.logBeans.info("(HttpTransaction) No valid charset defined, use basic charset: " + charset);
				}
				try {
					stringData = new String(httpData, charset);
				} catch (UnsupportedEncodingException e) {
					Engine.logBeans.warn("(HttpTransaction) Unsupported Encoding to decode the response, use ascii instead",  e);
					stringData = new String(httpData, "ascii");
				}
			}
			else if (dataEncoding == HTTP_DATA_ENCODING_BASE64) {
				stringData = Base64.encodeBase64String(httpData);
			}
			else {
				throw new IllegalArgumentException("Unknown data encoding: " + dataEncoding);
			}

			Node child = responseInCDATA ?
				context.outputDocument.createCDATASection(stringData) // remove TextCodec.UTF8Encode for #453
				: context.outputDocument.createTextNode(stringData);

			Element outputDocumentRootElement = context.outputDocument.getDocumentElement();
			outputDocumentRootElement.appendChild(child);
		}
		finally {
			context.statistics.stop(t);
		}
	}

	@Override
	public void configure(Element element) throws Exception {
		super.configure(element);

		String version = element.getAttribute("version");

		if (version!= null) {			
			if (VersionUtils.compare(version, "7.4.3") < 0) {
				responseInCDATA = true;
				dataStringCharset = "";
			}
		}
	}
	
	
}