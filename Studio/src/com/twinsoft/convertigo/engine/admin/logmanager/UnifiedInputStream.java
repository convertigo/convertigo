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

package com.twinsoft.convertigo.engine.admin.logmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.twinsoft.convertigo.engine.Engine;

public class UnifiedInputStream extends InputStream {
	private List<File> files;
	private FileInputStream current_file;
	private long pre_size = 0;
	private long current_position;
	private Iterator<File> chain;
	
	public UnifiedInputStream(List<File> files) throws IOException {
		this.files = new ArrayList<File>(files);
		if (this.files.size() == 0) {
			throw new FileNotFoundException("File needed");
		}
		for (int i = 0;i < this.files.size()-1;i++) {
			pre_size += this.files.get(i).length();
		}
		reset();		
	}

	public long getPointer() {
		return current_position;
	}
	
	@Override
	public int read() throws IOException {
		int i = current_file.read();
		if (i == -1) {
			if (nextFile()) { 
				i = read();
			}
		} else {
			current_position++;
		}
		return i;
	}

	@Override
	public int available() throws IOException {
		long size = pre_size + getLastFile().length() - current_position;
		if (size > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		} else {
			return (int) size;
		}
	}

	@Override
	public void close() throws IOException {
		current_file.close();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (len > b.length) {
			len = b.length;
		}
		int n;
		try {
			n = current_file.read(b, off, len);
		} catch (Exception e1) {
			try {
				Engine.logEngine.info("Failed to read log: b.length=" + b.length + " off=" + off + " len=" + len, e1);
				n = current_file.read(b, off, len);
			} catch (Exception e2) {
				Engine.logEngine.info("Failed the retry to read log: b.length=" + b.length + " off=" + off + " len=" + len, e2);
				n = -1;
			}
		}
		if (n != -1) {
			current_position += n;
			off += n;
			len -= n;
			if (len > 0 && nextFile()) {
				return n + read(b, off, len); 
			}
		}
		return n;
	}

	@Override
	public long skip(long n) throws IOException {
		n = current_file.skip(n);
		current_position += n;
		
		int available = current_file.available();
		if (available < 0 && nextFile()) {
			current_position += available;
			skip(-1 * available);
		}
		return n;
	}
	
	@Override
	public synchronized void reset() throws IOException {
		current_position = 0;
		if (current_file != null) current_file.close();
		chain = this.files.iterator();
		current_file = new FileInputStream(chain.next());
	}

	private boolean nextFile() throws IOException {
		if (chain.hasNext() && current_file != null) {
			current_file.close();
			current_file = new FileInputStream(chain.next());
			return true;
		} else {
			return false;
		}
	}
	
	private File getLastFile() {
		return files.get(files.size() - 1);
	}
}