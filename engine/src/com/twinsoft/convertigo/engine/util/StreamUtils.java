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

package com.twinsoft.convertigo.engine.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.twinsoft.convertigo.engine.Engine;

public class StreamUtils {
	
	static public long copyAutoFlush(final InputStream is, final OutputStream os) throws IOException {
		final boolean[] running = {true};
		final long[] nbBytes = {0};
		final Throwable[] throwable = {null};
		
		Engine.execute(new Runnable() {

			@Override
			public void run() {
				try {
					byte[] buf = new byte[1024 * 5];
					int read = is.read(buf);
					while (read >= 0 && running[0]) {
						nbBytes[0] += read;
						synchronized (os) {
							os.write(buf, 0, read);
						}
						read = is.read(buf);
					}
				} catch (Throwable t) {
					if (throwable[0] == null) {
						throwable[0] = t;
					}
				}
				synchronized (running) {
					running[0] = false;
					running.notify();
				}
			}
			
		});
		
		synchronized (running) {
			while (running[0]) {
				try {
					running.wait(1000);
				} catch (InterruptedException e) { }
				
				if (running[0]) {
					try {
						synchronized (os) {
							os.flush();
						}
					} catch (IOException ioex) {
						if (throwable[0] == null) {
							throwable[0] = ioex;
						}
						running[0] = false;
					}
				}
			}
		}
		
		if (throwable[0] != null) {
			if (throwable[0] instanceof IOException) {
				throw (IOException) throwable[0];
			} else {
				throw new IOException(throwable[0]);
			}
		}
		
		return nbBytes[0];
	}
}
