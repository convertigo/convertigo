/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class FileReloadInputStream extends InputStream {

	Path path;
	InputStream is;
	WatchService ws;
	boolean close = false;

	public FileReloadInputStream(File file) throws IOException {
		path = file.toPath();
		is = Files.newInputStream(path, StandardOpenOption.READ);

		Thread th = new Thread(() -> {
			try {
				Path ppath = path.getParent();
				ws = ppath.getFileSystem().newWatchService();
				WatchKey wk = ppath.register(ws, StandardWatchEventKinds.ENTRY_CREATE);
				while (!close) {
					wk = ws.take();
					if (close) {
						break;
					}
					for (final WatchEvent<?> event: wk.pollEvents()) {
						Path ctx = (Path) event.context();
						if (!close && path.endsWith(ctx)) {
							synchronized (path) {
								is.close();
								is = Files.newInputStream(path, StandardOpenOption.READ);
							}
						}
					}
					if (!wk.reset() || close) {
						wk.cancel();
						ws.close();
						break;
					}
				}
			} catch (Exception e) {
			} finally {
				if (ws != null) {
					try {
						ws.close();
					} catch (IOException e) {
					}
				}
			}
		});
		th.setName("FileReloadInputStream:" + file.getName());
		th.setDaemon(true);
		th.start();
	}

	@Override
	public int read() throws IOException {
		synchronized (path) {
			return is.read();
		}
	}

	@Override
	public int read(byte[] b) throws IOException {
		synchronized (path) {
			return is.read(b);
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		synchronized (path) {
			return is.read(b, off, len);
		}
	}

	@Override
	public long skip(long n) throws IOException {
		synchronized (path) {
			return is.skip(n);
		}
	}

	@Override
	public int available() throws IOException {
		synchronized (path) {
			return is.available();
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (path) {
			close = true;
			try {
				ws.close();
			} catch (Exception e) {
			}
			is.close();
		}
	}

	@Override
	public synchronized void mark(int readlimit) {
		synchronized (path) {
			is.mark(readlimit);
		}
	}

	@Override
	public synchronized void reset() throws IOException {
		synchronized (path) {
			is.reset();
		}
	}

	@Override
	public boolean markSupported() {
		synchronized (path) {
			return is.markSupported();
		}
	}
}
