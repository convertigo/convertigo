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

package com.twinsoft.convertigo.engine.admin.logmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.FileReloadInputStream;

class UnifiedInputStream extends InputStream {
	private record FileEntry(File file, long start, long length) {
		long end() {
			return start + length;
		}
	}

	private File basefile;
	private List<FileEntry> files;
	private InputStream current_file;
	private long pre_size = 0;
	private long current_position;
	private int current_index = -1;

	UnifiedInputStream(List<File> files, File basefile) throws IOException {
		this(files, null, basefile);
	}

	UnifiedInputStream(List<File> files, List<Long> fileLengths, File basefile) throws IOException {
		this.basefile = basefile;
		this.files = new ArrayList<FileEntry>(files.size());
		long start = 0;
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			long length = fileLengths != null && i < fileLengths.size() ? fileLengths.get(i) : file.length();
			this.files.add(new FileEntry(file, start, length));
			start += length;
		}
		if (this.files.size() == 0) {
			throw new FileNotFoundException("File needed");
		}
		for (int i = 0; i < this.files.size() - 1; i++) {
			pre_size += this.files.get(i).length();
		}
		reset();
	}

	public long getPointer() {
		return current_position;
	}

	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		int n = read(b, 0, 1);
		return n == -1 ? -1 : b[0] & 0xff;
	}

	@Override
	public int available() throws IOException {
		long size = pre_size + getLastEntryLength() - current_position;
		if (size > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		} if (size < 0) {
			return current_file == null ? 0 : current_file.available();
		} else {
			return (int) size;
		}
	}

	@Override
	public void close() throws IOException {
		if (current_file != null) {
			current_file.close();
		}
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return read(b, off, len, -1);
	}

	int read(byte[] b, int off, int len, long limit) throws IOException {
		if (len > b.length - off) {
			len = b.length - off;
		}
		if (len < 0) {
			len = 0;
		}
		if (len == 0) {
			return 0;
		}

		int total = 0;
		while (len > 0 && (limit < 0 || current_position < limit)) {
			if (current_index < 0 || current_index >= files.size()) {
				break;
			}
			if (current_file == null) {
				skipMissingCurrentFile();
				if (limit >= 0 && current_position >= limit) {
					break;
				}
				if (!nextFile()) {
					break;
				}
				continue;
			}

			int readLength = len;
			if (limit >= 0) {
				readLength = (int) Math.min(readLength, limit - current_position);
			}

			int n;
			try {
				n = current_file.read(b, off, readLength);
			} catch (Exception e1) {
				try {
					Engine.logEngine.info("Failed to read log: b.length=" + b.length + " off=" + off + " len=" + readLength, e1);
					n = current_file.read(b, off, readLength);
				} catch (Exception e2) {
					Engine.logEngine.info("Failed the retry to read log: b.length=" + b.length + " off=" + off + " len=" + readLength, e2);
					n = -1;
				}
			}

			if (n > 0) {
				current_position += n;
				total += n;
				off += n;
				len -= n;
			} else if (n == -1) {
				completeCurrentFile();
				if (!nextFile()) {
					break;
				}
			} else {
				break;
			}
		}
		return total > 0 ? total : -1;
	}

	@Override
	public long skip(long n) throws IOException {
		if (n <= 0) {
			return 0;
		}

		long skipped = 0;
		while (n > 0 && current_index >= 0 && current_index < files.size()) {
			if (current_file == null) {
				long remaining = files.get(current_index).end() - current_position;
				if (remaining > n) {
					current_position += n;
					skipped += n;
					break;
				}
				current_position += Math.max(0, remaining);
				skipped += Math.max(0, remaining);
				n -= Math.max(0, remaining);
				if (!nextFile()) {
					break;
				}
				continue;
			}

			long currentSkipped = current_file.skip(n);
			if (currentSkipped > 0) {
				current_position += currentSkipped;
				skipped += currentSkipped;
				n -= currentSkipped;
			} else {
				int i = current_file.read();
				if (i == -1) {
					completeCurrentFile();
					if (!nextFile()) {
						break;
					}
				} else {
					current_position++;
					skipped++;
					n--;
				}
			}
		}
		return skipped;
	}

	@Override
	public synchronized void reset() throws IOException {
		current_position = 0;
		if (current_file != null) {
			current_file.close();
		}
		current_index = 0;
		current_file = open(files.get(current_index));
	}

	private boolean nextFile() throws IOException {
		if (current_file != null) {
			current_file.close();
			current_file = null;
		}
		current_index++;
		if (current_index >= files.size()) {
			return false;
		}
		current_position = Math.max(current_position, files.get(current_index).start());
		current_file = open(files.get(current_index));
		return true;
	}

	private InputStream open(FileEntry entry) throws IOException {
		try {
			File file = entry.file();
			return file.equals(basefile) ? new FileReloadInputStream(file) : new FileInputStream(file);
		} catch (FileNotFoundException e) {
			Engine.logAdmin.debug("Skip missing log file: " + entry.file().getName());
			return null;
		}
	}

	private void skipMissingCurrentFile() {
		current_position = Math.max(current_position, files.get(current_index).end());
	}

	private void completeCurrentFile() {
		FileEntry entry = files.get(current_index);
		if (!entry.file().equals(basefile)) {
			current_position = Math.max(current_position, entry.end());
		}
	}

	private long getLastEntryLength() {
		FileEntry entry = files.get(files.size() - 1);
		return entry.file().equals(basefile) ? entry.file().length() : entry.length();
	}
}
