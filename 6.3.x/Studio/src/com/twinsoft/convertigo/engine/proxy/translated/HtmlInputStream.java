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

package com.twinsoft.convertigo.engine.proxy.translated;

import java.io.*;
import java.net.URL;
import com.twinsoft.convertigo.engine.proxy.util.IntQueue;

public class HtmlInputStream extends InputStream {
	private IntQueue htmlQueue = new IntQueue(1024, 256);
	private IntQueue typeQueue = new IntQueue(64, 8);
	private IntQueue sizeQueue = new IntQueue(64, 8);

	private InputStream in = null;
	private HttpBridge bridge = new HttpBridge();

	public InputStream open(ParameterShuttle infoShuttle, InputStream inStream, URL docURL) {
		//in = inStream;
		in = getConvertedInputStream(inStream);

		htmlQueue.clear();
		typeQueue.clear();
		sizeQueue.clear();

		bridge.init(infoShuttle, docURL, htmlQueue, typeQueue, sizeQueue);

		return this;
	}

	public void close() {
		if (in != null) {
			try {
				in.close();
			}
			catch (IOException e) {
			}
			in = null;

			htmlQueue.clear();
			typeQueue.clear();
			sizeQueue.clear();

			bridge.init(null, null, null, null, null);
		}
	}

	private InputStream getConvertedInputStream(InputStream inStream) {
		InputStream input = null;
		try {
			String encoding = "ISO-8859-1";
			ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
			int b0 = -1, b1 = -1, b2 = -1;
			
			int c = inStream.read();
			b0 = c;
			while (c > -1) {
				baos.write(c);
				c = inStream.read();
				
				if (b1 == -1) {
					b1 = c;
				}
				else if (b2 == -1) {
					b2 = c;
				}
			}
			
			if (b0 == 0xef && b1 == 0xbb && b2 == 0xbf) {
				encoding = "UTF-8";
			}
			else if (b0 == 0xff && b1 == 0xfe || b0 == 0xfe && b1 == 0xff) {
				encoding = "UTF-16";
			}
			
			String sResponse = new String((byte[]) baos.toByteArray(), encoding);
			input = new ByteArrayInputStream(sResponse.getBytes("ISO-8859-1"));
			
			baos.close();
		}
		catch (Exception e) {
		}
		finally {
			if (inStream != null) {
				try {
					inStream.close();
				}
				catch (Exception e) {
				}
			}
			
			if (input == null) {
				input = inStream;
			}
		}
		return input;
	}

	public int read() throws IOException {
		int rt = -2;

		do {
			if (typeQueue.size() == 0) {
				rt = bridge.fillBuffer(in);
				continue;
			}

			if (typeQueue.get() == HttpBridge.OUTPUT) {
				int len = sizeQueue.get();
				if (len <= 0) {
					typeQueue.pop();
					sizeQueue.pop();
					continue;
				}

				rt = htmlQueue.pop();
				sizeQueue.set(len - 1);
			}
			else {
				bridge.convertHtmlTag();
			}
		} while (rt < -1);

		return rt;
	}
}