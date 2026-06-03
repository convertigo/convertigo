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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

public class FileReloadInputStream extends InputStream {
	private record FileIdentity(Object fileKey, long creationTime) {
	}

	private final Object lock = new Object();
	private final Path path;
	private InputStream is;
	private FileIdentity fileIdentity;
	private boolean close = false;

	public FileReloadInputStream(File file) throws IOException {
		path = file.toPath();
		openCurrentFile();
	}

	@Override
	public int read() throws IOException {
		synchronized (lock) {
			int n = is.read();
			if (n == -1 && reloadIfCurrentFileChanged()) {
				n = is.read();
			}
			return n;
		}
	}

	@Override
	public int read(byte[] b) throws IOException {
		synchronized (lock) {
			int n = is.read(b);
			if (n == -1 && reloadIfCurrentFileChanged()) {
				n = is.read(b);
			}
			return n;
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		synchronized (lock) {
			int n = is.read(b, off, len);
			if (n == -1 && reloadIfCurrentFileChanged()) {
				n = is.read(b, off, len);
			}
			return n;
		}
	}

	@Override
	public long skip(long n) throws IOException {
		synchronized (lock) {
			return is.skip(n);
		}
	}

	@Override
	public int available() throws IOException {
		synchronized (lock) {
			int available = is.available();
			if (available == 0 && reloadIfCurrentFileChanged()) {
				available = is.available();
			}
			return available;
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (lock) {
			close = true;
			is.close();
		}
	}

	@Override
	public synchronized void mark(int readlimit) {
		synchronized (lock) {
			is.mark(readlimit);
		}
	}

	@Override
	public synchronized void reset() throws IOException {
		synchronized (lock) {
			is.reset();
		}
	}

	@Override
	public boolean markSupported() {
		synchronized (lock) {
			return is.markSupported();
		}
	}

	private void openCurrentFile() throws IOException {
		fileIdentity = getFileIdentity();
		is = Files.newInputStream(path, StandardOpenOption.READ);
	}

	private boolean reloadIfCurrentFileChanged() throws IOException {
		if (close) {
			return false;
		}

		FileIdentity currentFileIdentity;
		try {
			currentFileIdentity = getFileIdentity();
		} catch (NoSuchFileException e) {
			return false;
		}

		if (fileIdentity.equals(currentFileIdentity)) {
			return false;
		}

		is.close();
		openCurrentFile();
		return true;
	}

	private FileIdentity getFileIdentity() throws IOException {
		BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
		return new FileIdentity(attributes.fileKey(), attributes.creationTime().toMillis());
	}
}
