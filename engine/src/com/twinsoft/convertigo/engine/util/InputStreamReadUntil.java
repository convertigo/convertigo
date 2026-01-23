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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InputStreamReadUntil extends InputStream {
	
	private InputStream is;
	private byte[] until;
	private int state = 0;
	private int markedState = 0;
	
	public InputStreamReadUntil(InputStream is, byte[] until) {
		this.is = is;
		this.until = until;
	}

	@Override
	public int read() throws IOException {
		int r;
		if (state == until.length) {
			state = 0;
			r = -1;
		} else {
			r = is.read();
			if (r != -1) {
				if (r == until[state]) {
					state++;
				} else if (state != 0) {
					state = 0;
				}
			}
		}
		return r;
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		int r = super.read(b);
		if (state == until.length) {
			state = 0;
		}
		return r;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int r = super.read(b, off, len);
		if (state == until.length) {
			state = 0;
		}
		return r;
	}

	@Override
	public byte[] readAllBytes() throws IOException {
		byte[] r = super.readAllBytes();
		if (state == until.length) {
			state = 0;
		}
		return r;
	}

	@Override
	public byte[] readNBytes(int len) throws IOException {
		byte[] r = super.readNBytes(len);
		if (state == until.length) {
			state = 0;
		}
		return r;
	}

	@Override
	public int readNBytes(byte[] b, int off, int len) throws IOException {
		int r = super.readNBytes(b, off, len);
		if (state == until.length) {
			state = 0;
		}
		return r;
	}

	@Override
	public long transferTo(OutputStream out) throws IOException {
		long r = super.transferTo(out);
		if (state == until.length) {
			state = 0;
		}
		return r; 
	}

	@Override
	public long skip(long n) throws IOException {
		state = 0;
		return is.skip(n);
	}

	@Override
	public void close() throws IOException {
		state = 0;
		is.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		markedState = state;
		is.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		state = markedState;
		is.reset();
	}

	@Override
	public boolean markSupported() {
		return is.markSupported();
	}
}
