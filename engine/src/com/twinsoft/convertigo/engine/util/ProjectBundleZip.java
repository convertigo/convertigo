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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.CRC32;
import java.util.zip.ZipFile;

/** Bounded ZIP reader for bundle staging. No entry is written before directory validation. */
public final class ProjectBundleZip {
	private static final Charset LEGACY_CHARSET = Charset.forName("CP437");

	public record Limits(int projects, long entries, long totalBytes, long entryBytes, int ratio,
			long uploadBytes, int uploads) {
		public Limits {
			if (projects < 1 || projects > 1_000 || entries < 1 || entries > 1_000_000
					|| totalBytes < 1 || entryBytes < 1 || ratio < 1 || uploadBytes < 1 || uploads < 1) {
				throw new IllegalArgumentException("Invalid project bundle limits");
			}
		}

		public static Limits defaults() {
			return new Limits(200, 200_000, 4L << 30, 1L << 30, 200, 1L << 30, 64);
		}
	}

	/** One instance must cover the outer ZIP and every project archive in a request. */
	public static final class Budget {
		private final Limits limits;
		private long entries;
		private long bytes;

		public Budget(Limits limits) {
			this.limits = limits;
		}

		void entry() throws IOException {
			if (++entries > limits.entries()) throw new IOException("The bundle contains too many entries");
		}

		void bytes(long count, long entryBytes, long compressedBytes) throws IOException {
			if (count > limits.totalBytes() - bytes || entryBytes > limits.entryBytes()) {
				throw new IOException("The bundle exceeds its expanded size limit");
			}
			bytes += count;
			if (entryBytes > (1L << 20) && (compressedBytes == 0
					|| (entryBytes - 1) / limits.ratio() >= compressedBytes)) {
				throw new IOException("A bundle entry exceeds its expansion ratio limit");
			}
		}

		public long expandedBytes() { return bytes; }
		public long entries() { return entries; }
		public Limits limits() { return limits; }
	}

	public record Entry(String name, boolean directory, long size, long compressedSize, long crc,
			int method, int flags, long localOffset, byte[] rawName) {
	}

	private ProjectBundleZip() {
	}

	public static List<Entry> inspect(Path archive, Budget budget) throws IOException {
		try (var file = new RandomAccessFile(archive.toFile(), "r")) {
			long length = file.length();
			if (length < 22 || length > budget.limits.uploadBytes()) throw new IOException("Invalid archive size");
			var tail = new byte[(int) Math.min(length, 65_557)];
			file.seek(length - tail.length);
			file.readFully(tail);
			int end = -1;
			for (int i = tail.length - 22; i >= 0; i--) {
				if (u32(tail, i) == 0x06054b50L && i + 22 + u16(tail, i + 20) == tail.length) {
					end = i;
					break;
				}
			}
			if (end < 0 || u16(tail, end + 4) != 0 || u16(tail, end + 6) != 0) {
				throw new IOException("Invalid archive directory");
			}
			long directoryEnd = length - tail.length + end;
			long count = u16(tail, end + 10);
			long size = u32(tail, end + 12);
			long offset = u32(tail, end + 16);
			if (count == 0xffff || size == 0xffffffffL || offset == 0xffffffffL) {
				var locator = read(file, directoryEnd - 20, 20);
				if (u32(locator, 0) != 0x07064b50L || u32(locator, 4) != 0 || u32(locator, 16) != 1) {
					throw new IOException("Invalid ZIP64 directory");
				}
				long zip64Offset = u64(locator, 8);
				var zip64 = read(file, zip64Offset, 56);
				long recordSize = u64(zip64, 4);
				if (u32(zip64, 0) != 0x06064b50L || recordSize < 44 || recordSize > directoryEnd - 20 - zip64Offset - 12
						|| u32(zip64, 16) != 0 || u32(zip64, 20) != 0 || u64(zip64, 24) != u64(zip64, 32)) {
					throw new IOException("Invalid ZIP64 directory");
				}
				count = u64(zip64, 32);
				size = u64(zip64, 40);
				offset = u64(zip64, 48);
				directoryEnd = zip64Offset;
			} else if (u16(tail, end + 8) != count) {
				throw new IOException("Multi-part archives are not supported");
			}
			if (offset > directoryEnd || size > directoryEnd - offset || size > (64L << 20) || count > budget.limits.entries()) {
				throw new IOException("Invalid archive directory size");
			}
			long directoryLimit = offset + size;
			var result = new ArrayList<Entry>();
			var ranges = new java.util.TreeMap<Long, Long>();
			var names = new HashSet<String>();
			var paths = new HashMap<String, String>();
			var types = new HashMap<String, Boolean>();
			long position = offset;
			while (position < directoryLimit) {
				budget.entry();
				if (directoryLimit - position < 46) throw new IOException("Incomplete archive directory");
				var header = read(file, position, 46);
				if (u32(header, 0) != 0x02014b50L) throw new IOException("Invalid archive directory entry");
				int nameLength = u16(header, 28), extraLength = u16(header, 30), commentLength = u16(header, 32);
				long next = position + 46L + nameLength + extraLength + commentLength;
				if (next > directoryLimit || nameLength > 1024 || nameLength == 0) throw new IOException("Invalid entry name size");
				var rawName = read(file, position + 46, nameLength);
				int flags = u16(header, 8), method = u16(header, 10);
				if ((flags & (1 | 64 | 8192)) != 0 || (method != 0 && method != 8)) throw new IOException("Unsupported archive entry format");
				String name = decode(rawName, (flags & 2048) != 0 ? StandardCharsets.UTF_8 : LEGACY_CHARSET);
				boolean directory = name.endsWith("/");
				validateName(name);
				if (!names.add(name)) throw new IOException("Duplicate archive entry");
				registerPath(name, directory, paths, types);
				int platform = Byte.toUnsignedInt(header[5]);
				int mode = platform == 3 ? (int) (u32(header, 38) >>> 16) : 0;
				int kind = mode & 0170000;
				if (kind != 0 && kind != (directory ? 0040000 : 0100000)) throw new IOException("Unsupported archive entry attributes");
				long expanded = u32(header, 24), compressed = u32(header, 20), local = u32(header, 42);
				int disk = u16(header, 34);
				if (expanded == 0xffffffffL || compressed == 0xffffffffL || local == 0xffffffffL || disk == 0xffff) {
					var extra = read(file, position + 46 + nameLength, extraLength);
					boolean found = false;
					for (int i = 0; i + 4 <= extra.length;) {
						int id = u16(extra, i), bytes = u16(extra, i + 2);
						int limit = i + 4 + bytes;
						if (limit > extra.length) throw new IOException("Invalid entry metadata");
						if (id == 1) {
							int cursor = i + 4;
							if (expanded == 0xffffffffL) { expanded = extra64(extra, cursor, limit); cursor += 8; }
							if (compressed == 0xffffffffL) { compressed = extra64(extra, cursor, limit); cursor += 8; }
							if (local == 0xffffffffL) { local = extra64(extra, cursor, limit); cursor += 8; }
							if (disk == 0xffff) {
								if (cursor + 4 > limit || u32(extra, cursor) != 0) throw new IOException("Invalid entry disk");
								disk = 0;
							}
							found = true;
							break;
						}
						i = limit;
					}
					if (!found) throw new IOException("Missing ZIP64 entry metadata");
				}
				if ((directory && (expanded != 0 || u32(header, 16) != 0)) || disk != 0 || expanded > budget.limits.entryBytes() || local > offset - 30) throw new IOException("Invalid archive entry size");
				var entry = new Entry(name, directory, expanded, compressed, u32(header, 16), method, flags, local, rawName);
				long entryEnd = validateLocal(file, entry, offset);
				var previous = ranges.floorEntry(local);
				var following = ranges.ceilingEntry(local);
				if ((previous != null && previous.getValue() > local) || (following != null && entryEnd > following.getKey())) {
					throw new IOException("Overlapping archive entries");
				}
				ranges.put(local, entryEnd);
				result.add(entry);
				position = next;
			}
			if (result.size() != count || position != directoryLimit) throw new IOException("Inconsistent archive entry count");
			return List.copyOf(result);
		}
	}

	public static void extract(Path archive, Path target, List<Entry> entries, Budget budget,
			Predicate<String> excluded) throws IOException {
		extractEntries(archive, target, entries, budget, excluded, "");
	}

	public static void extractProject(Path archive, Path target, List<Entry> entries, Budget budget, String name) throws IOException {
		String prefix = name + "/";
		for (var entry : entries) if (!entry.name().startsWith(prefix)) throw new IOException("The prepared archive contains another project");
		extractEntries(archive, target, entries, budget, ProjectBundleImport::localProjectStatePath, prefix);
	}

	private static void extractEntries(Path archive, Path target, List<Entry> entries, Budget budget,
			Predicate<String> excluded, String prefix) throws IOException {
		if (!Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) throw new IOException("An extraction directory is required");
		var root = target.toRealPath();
		long required = 0;
		for (var entry : entries) {
			if (!excluded.test(entry.name().substring(prefix.length()))) {
				if (entry.size() > budget.limits.totalBytes() - required) throw new IOException("The bundle exceeds its expanded size limit");
				required += entry.size();
			}
		}
		if (Files.getFileStore(root).getUsableSpace() < required + (64L << 20)) throw new IOException("Insufficient space to prepare the bundle");
		try (var zip = new ZipFile(archive.toFile(), LEGACY_CHARSET)) {
			if (zip.size() != entries.size()) throw new IOException("Inconsistent archive directory");
			byte[] buffer = new byte[64 * 1024];
			for (var entry : entries) {
				String relative = entry.name().substring(prefix.length());
				if (relative.isEmpty() || excluded.test(relative)) continue;
				var metadata = zip.getEntry(entry.name());
				if (metadata == null || metadata.getSize() != entry.size() || metadata.getCompressedSize() != entry.compressedSize()
						|| metadata.getMethod() != entry.method() || metadata.getCrc() != entry.crc()) throw new IOException("Inconsistent archive entry");
				var output = root.resolve(relative).normalize();
				if (!output.startsWith(root)) throw new IOException("Invalid entry destination");
				Files.createDirectories(entry.directory() ? output : output.getParent());
				if (!output.getParent().toRealPath().startsWith(root)) throw new IOException("Invalid entry parent");
				if (entry.directory()) continue;
				var crc = new CRC32();
				long copied = 0;
				// Installation replaces files like a single CAR; staging still requires new files.
				var create = prefix.isEmpty() ? StandardOpenOption.CREATE_NEW : StandardOpenOption.CREATE;
				try (var input = zip.getInputStream(metadata);
						var stream = Files.newOutputStream(output, create, StandardOpenOption.TRUNCATE_EXISTING,
								StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
					int read;
					while ((read = input.read(buffer)) != -1) {
						copied += read;
						budget.bytes(read, copied, entry.compressedSize());
						stream.write(buffer, 0, read);
						crc.update(buffer, 0, read);
					}
				}
				if (copied != entry.size() || crc.getValue() != entry.crc()) throw new IOException("Inconsistent expanded entry");
			}
		}
	}

	public static void validateName(String name) throws IOException {
		if (name.isEmpty() || name.getBytes(StandardCharsets.UTF_8).length > 1024 || name.startsWith("/")
				|| name.indexOf('\\') >= 0 || name.indexOf(':') >= 0) throw new IOException("Invalid archive entry name");
		String path = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
		for (var segment : path.split("/", -1)) {
			if (segment.isEmpty() || segment.equals(".") || segment.equals("..") || segment.endsWith(".") || segment.endsWith(" ")
					|| segment.getBytes(StandardCharsets.UTF_8).length > 255
					|| segment.codePoints().anyMatch(c -> Character.isISOControl(c) || "<>\"|?*".indexOf(c) >= 0)
					|| segment.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?")) {
				throw new IOException("Invalid archive entry segment");
			}
		}
	}

	private static void registerPath(String name, boolean directory, Map<String, String> paths,
			Map<String, Boolean> types) throws IOException {
		String path = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
		int end = 0;
		do {
			end = path.indexOf('/', end + 1);
			String part = end < 0 ? path : path.substring(0, end);
			String normalized = Normalizer.normalize(part, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
			String old = paths.putIfAbsent(normalized, part);
			boolean isDirectory = end >= 0 || directory;
			Boolean oldType = types.putIfAbsent(normalized, isDirectory);
			if ((old != null && !old.equals(part)) || (oldType != null && oldType != isDirectory)) {
				throw new IOException("Conflicting archive entry names");
			}
		} while (end >= 0);
	}

	private static long validateLocal(RandomAccessFile file, Entry entry, long directoryOffset) throws IOException {
		var local = read(file, entry.localOffset(), 30);
		int length = u16(local, 26);
		long data = entry.localOffset() + 30 + length + u16(local, 28);
		if (u32(local, 0) != 0x04034b50L || u16(local, 6) != entry.flags() || u16(local, 8) != entry.method()
				|| length != entry.rawName().length || data > directoryOffset || entry.compressedSize() > directoryOffset - data
				|| !java.util.Arrays.equals(entry.rawName(), read(file, entry.localOffset() + 30, length))) {
			throw new IOException("Inconsistent local archive entry");
		}
		long localSize = u32(local, 22), localCompressed = u32(local, 18);
		boolean zip64 = localSize == 0xffffffffL || localCompressed == 0xffffffffL;
		if (zip64) {
			var extra = read(file, entry.localOffset() + 30 + length, u16(local, 28));
			boolean found = false;
			for (int i = 0; i + 4 <= extra.length;) {
				int limit = i + 4 + u16(extra, i + 2);
				if (limit > extra.length) throw new IOException("Invalid local entry metadata");
				if (u16(extra, i) == 1) {
					int cursor = i + 4;
					if (localSize == 0xffffffffL) { localSize = extra64(extra, cursor, limit); cursor += 8; }
					if (localCompressed == 0xffffffffL) localCompressed = extra64(extra, cursor, limit);
					found = true;
					break;
				}
				i = limit;
			}
			if (!found) throw new IOException("Missing local ZIP64 metadata");
		}
		long end = data + entry.compressedSize();
		if ((entry.flags() & 8) == 0) {
			if (localSize != entry.size() || localCompressed != entry.compressedSize() || u32(local, 14) != entry.crc()) {
				throw new IOException("Inconsistent local entry sizes");
			}
		} else {
			var signature = read(file, end, 4);
			boolean signed = u32(signature, 0) == 0x08074b50L;
			int bytes = zip64 ? 20 : 12;
			var descriptor = read(file, end + (signed ? 4 : 0), bytes);
			long compressed = zip64 ? u64(descriptor, 4) : u32(descriptor, 4);
			long expanded = zip64 ? u64(descriptor, 12) : u32(descriptor, 8);
			end += bytes + (signed ? 4 : 0);
			if (end > directoryOffset || u32(descriptor, 0) != entry.crc() || compressed != entry.compressedSize() || expanded != entry.size()) {
				throw new IOException("Inconsistent archive data descriptor");
			}
		}
		return end;
	}

	private static String decode(byte[] name, Charset charset) throws IOException {
		try { return charset.newDecoder().decode(ByteBuffer.wrap(name)).toString(); }
		catch (CharacterCodingException e) { throw new IOException("Invalid entry encoding", e); }
	}

	private static byte[] read(RandomAccessFile file, long offset, int length) throws IOException {
		if (offset < 0 || offset > file.length() - length) throw new IOException("Incomplete archive metadata");
		byte[] bytes = new byte[length];
		file.seek(offset);
		file.readFully(bytes);
		return bytes;
	}

	private static int u16(byte[] bytes, int offset) {
		return Short.toUnsignedInt(ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
	}

	private static long u32(byte[] bytes, int offset) {
		return Integer.toUnsignedLong(ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
	}

	private static long u64(byte[] bytes, int offset) throws IOException {
		long value = ByteBuffer.wrap(bytes, offset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
		if (value < 0) throw new IOException("Unsupported archive size");
		return value;
	}

	private static long extra64(byte[] bytes, int offset, int limit) throws IOException {
		if (offset + 8 > limit) throw new IOException("Incomplete ZIP64 entry metadata");
		return u64(bytes, offset);
	}
}
