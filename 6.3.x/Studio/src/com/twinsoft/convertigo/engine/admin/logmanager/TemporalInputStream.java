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
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.Engine;

import edu.emory.mathcs.backport.java.util.Arrays;

public class TemporalInputStream extends InputStream {
	private final static Pattern split_dot = Pattern.compile("\\.");
	private ParsePosition date_position = new ParsePosition(0);
	private long position_start = -1;
	private long position_end = 0;
	private UnifiedInputStream is;
	private DateFormat date_format;
	private int date_offset;
	private final Matcher is_ts;
	private final Matcher is_ori;
	private final List<File> files;
	
	
	public TemporalInputStream(File directory, String base_file, DateFormat date_format, int date_offset, Date date_start, Date date_end) throws IOException {
		if (date_start.compareTo(date_end) >= 0) {
			throw new IllegalArgumentException("date_start must be less than date_end");
		}
		
		this.date_format = date_format;
		if (date_format.isLenient()) {  
			this.date_format.setLenient(false); // see #1567, date_format need to be strict
		}
		
		this.date_offset = date_offset;
		files = new LinkedList<File>();
		boolean end_finded = false;
		
		{
			String[] split = split_dot.split(base_file);
			this.is_ori = Pattern.compile("(" + split[0] + ")(\\." + split[1] + ")\\..*").matcher("");
			 this.is_ts = Pattern.compile(split[0] + "\\.([\\w]*)\\.([\\w]*)\\." + split[1]).matcher("");
		}
		
		renameFiles(directory);
		
		String [] filenames = directory.list();
		Arrays.sort(filenames);
		
		for (String filename : filenames) {
			is_ts.reset(filename);
			if (is_ts.matches() && is_ts.groupCount() == 2) {
				Date file_date_end = new Date(Long.parseLong(is_ts.group(2), Character.MAX_RADIX));
				File file = new File(directory, filename);
				if (position_start == -1) {
					if (date_start.compareTo(file_date_end) <= 0) {
						position_start = findPosition(file, date_start);
					}
				}
				if (position_start != -1) {
					Date file_date_start = new Date(Long.parseLong(is_ts.group(1), Character.MAX_RADIX));
					if (date_end.compareTo(file_date_start) >= 0) {
						files.add(file);
						if (date_end.compareTo(file_date_end) <= 0) {
							position_end += findPosition(file, date_end);
							end_finded = true;
							break;
						} else {
							position_end += file.length();
						}
					}
				}
			}
		}
		
		File basefile = new File(directory, base_file);
		if (position_start == -1) {
			try {
				position_start = findPosition(basefile, date_start);
			} catch (Exception e) {
				position_start = basefile.length();
			}
		}
		if (!end_finded) {
			files.add(basefile);
			try {
				position_end += findPosition(basefile, date_end);
			} catch(Exception e) {
				position_end = -1;
			}
		}
		
		is = new UnifiedInputStream(files);
		is.skip(position_start);
	}
	
	@Override
	public int read() throws IOException {
		int n = is.read();
		if (position_end != -1 && is.getPointer() > position_end) {
			n = -1;
		}
		return n;
	}
	
	@Override
	public int available() throws IOException {
		if (position_end != -1) {
			return (int) (position_end - is.getPointer());	
		} else {
			return is.available();
		}
	}

	@Override
	public void close() throws IOException {
		is.close();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int available = available();
		if (len > available) {
			len = available;
		}
		if (len == 0) {
			return -1;
		} else {
			return is.read(b, off, len);
		}
	}

	@Override
	public long skip(long n) throws IOException {
		return is.skip(n);
	}
	
	@Override
	public synchronized void reset() throws IOException {
		is.reset();
		is.skip(position_start);
	}
	
	public SortedMap<Date, File> getTimedFiles() {
		SortedMap<Date, File> map = new TreeMap<Date, File>();
		for (File file : files) {
			is_ts.reset(file.getName());
			if (is_ts.matches() && is_ts.groupCount() == 2) {
				Date file_date_end = new Date(Long.parseLong(is_ts.group(2), Character.MAX_RADIX));
				map.put(file_date_end, file);
			}
		}
		return map;
	}
	
	private Date extractDate(RandomAccessFile raf, boolean seek) throws IOException {
		return extractDate(raf, seek, Long.MAX_VALUE);
	}
	
	private Date extractDate(RandomAccessFile raf, boolean seek, long max_pos) throws IOException {
		Date date = null;
		long pos = raf.getFilePointer();
		String line = raf.readLine();
		while (line != null && pos < max_pos) {
			date = parseDate(line);
			if (date == null) {
				pos = raf.getFilePointer();
				line = raf.readLine();
			} else {
				line = null;
			}
		}
		if (seek) {
			raf.seek(pos);
		}
		return date;
	}
	
	private 	long findPosition(File file, Date date) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		
		try {
			Date cur_date = extractDate(raf, true);
			if (cur_date != null) {
				int compare = date.compareTo(cur_date);
				if (compare > 0) {
					date.setTime(date.getTime() - 1);
					long min = raf.getFilePointer();
					long max = raf.length();
					Date last_date = null;
					long last_pos = 0;
					while (cur_date != null && !cur_date.equals(last_date) && !cur_date.equals(date)) {
						last_date = cur_date;
						last_pos = raf.getFilePointer();
						long cur = min + ((max - min) / 2);
						raf.seek(cur);
						cur_date = extractDate(raf, true);
						if (cur_date == null || date.compareTo(cur_date) < 0) {
							max = cur;
						} else {
							min = cur;
						}
					}
					date.setTime(date.getTime() + 1);
					
					if (cur_date != null && date.compareTo(cur_date) < 0) {
						raf.seek(min);
						cur_date = extractDate(raf, true);
					}
					
					// Fix #1788 : 'Out of range Date' exception when start date is 2011-01-25 12:32 for log file of #1787
					// can find next cur_date but last_date is a valid candidate for the end date
					if (cur_date == null && last_date != null) {
						cur_date = last_date;
						raf.seek(last_pos);
					}
					while (cur_date != null && date.compareTo(cur_date) > 0) {
						raf.skipBytes(1);
						cur_date = extractDate(raf, true);
					}
				}
				if (cur_date != null) {
					return raf.getFilePointer();
				}
			}
			throw new IOException("Out of range Date");
		} finally {
			raf.close();
		}
	}
	
	private Date parseDate(String line) throws IllegalArgumentException {
		date_position.setIndex(date_offset);
		return date_format.parse(line, date_position);
	}
	
	private void renameFile(File file, String basename, String extension) throws IOException {
		Date first_date = null;
		Date last_date = null;
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		try {
			first_date = extractDate(raf, false);
			if (first_date != null) {
				long max_pos = raf.length();
				while (last_date == null && max_pos > 0) {
					long cur_pos = max_pos - 1024;
					if (cur_pos < 0) {
						cur_pos = 0;
					}
					raf.seek(cur_pos);
					
					// Fix #1787 : Logviewer doesn't respond : wait for rename an engine.log file
					// doesn't recurse on already read bytes
					Date ex_date = extractDate(raf, false, max_pos);
					while (ex_date != null) {
						last_date = ex_date;
						ex_date = extractDate(raf, false, max_pos);
					}
					max_pos = cur_pos;
				}
			}
		} finally {
			raf.close();	
		}
		if (first_date != null && last_date != null) {
			file.renameTo(new File(file.getParentFile(), basename + '.' + Long.toString(first_date.getTime(), Character.MAX_RADIX) + '.' + Long.toString(last_date.getTime(), Character.MAX_RADIX) + extension));
		}
	}
	
	private void renameFiles(File dir) {
		synchronized (TemporalInputStream.class) {
			for (File file : dir.listFiles()) {
				is_ori.reset(file.getName());
				if (is_ori.matches() && is_ori.groupCount() == 2) {
					try {
						renameFile(file, is_ori.group(1), is_ori.group(2));
					} catch (IOException e) {
						Engine.logAdmin.error("Rename " + file.getAbsolutePath() + " rename failed", e);
					}
				}
			}
		}
	}
}