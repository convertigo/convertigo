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

package com.twinsoft.convertigo.engine.process;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.Locale;

import org.apache.log4j.Logger;

/**
 * On Windows, a job object with JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE holds the managed processes: the
 * system kills them, with the processes they start, when the JVM exits, even killed by the task manager.
 * Only the managed processes are assigned to it, never the JVM itself (a browser or an explorer it opens
 * would be killed with it) nor the process reaper (it would die with the job).
 * <p>
 * It uses the Foreign Function API; any failure disables it, the other mechanisms remain (Node.js preload,
 * process reaper, registry).
 */
final class WindowsJob {
	static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");

	private static final int JOB_OBJECT_EXTENDED_LIMIT_INFORMATION = 9;
	private static final int JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE = 0x2000;
	private static final int PROCESS_TERMINATE = 0x0001;
	private static final int PROCESS_SET_QUOTA = 0x0100;
	/** sizeof(JOBOBJECT_EXTENDED_LIMIT_INFORMATION) on 64 bits */
	private static final long INFORMATION_SIZE = 144;
	/** offsetof(JOBOBJECT_EXTENDED_LIMIT_INFORMATION, BasicLimitInformation.LimitFlags) */
	private static final long LIMIT_FLAGS_OFFSET = 16;

	private static boolean failed = !WINDOWS;
	private static MemorySegment job = null;
	private static MethodHandle openProcess;
	private static MethodHandle assignProcess;
	private static MethodHandle closeHandle;

	private WindowsJob() {
	}

	/**
	 * Assigns the process pid, just started by this JVM, to the job.
	 *
	 * @return whether it is in the job
	 */
	static synchronized boolean assign(long pid, Logger log) {
		if (failed) {
			return false;
		}
		try {
			if (job == null) {
				create();
			}
			MemorySegment process = (MemorySegment) openProcess.invokeExact(PROCESS_SET_QUOTA | PROCESS_TERMINATE, 0, (int) pid);
			if (process.address() == 0) {
				return false;
			}
			try {
				return (int) assignProcess.invokeExact(job, process) != 0;
			} finally {
				int closed = (int) closeHandle.invokeExact(process);
			}
		} catch (Throwable e) {
			failed = true;
			log.debug("No job object for the managed processes: " + e);
			return false;
		}
	}

	private static void create() throws Throwable {
		Linker linker = Linker.nativeLinker();
		SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", Arena.global());
		MethodHandle createJob = linker.downcallHandle(kernel32.find("CreateJobObjectW").orElseThrow(),
				FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
		MethodHandle setInformation = linker.downcallHandle(kernel32.find("SetInformationJobObject").orElseThrow(),
				FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
		openProcess = linker.downcallHandle(kernel32.find("OpenProcess").orElseThrow(),
				FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT));
		assignProcess = linker.downcallHandle(kernel32.find("AssignProcessToJobObject").orElseThrow(),
				FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		closeHandle = linker.downcallHandle(kernel32.find("CloseHandle").orElseThrow(),
				FunctionDescriptor.of(JAVA_INT, ADDRESS));
		MemorySegment handle = (MemorySegment) createJob.invokeExact(MemorySegment.NULL, MemorySegment.NULL);
		if (handle.address() == 0) {
			throw new IllegalStateException("CreateJobObjectW failed");
		}
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment information = arena.allocate(INFORMATION_SIZE, 8);
			information.set(JAVA_INT, LIMIT_FLAGS_OFFSET, JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE);
			int set = (int) setInformation.invokeExact(handle, JOB_OBJECT_EXTENDED_LIMIT_INFORMATION, information, (int) INFORMATION_SIZE);
			if (set == 0) {
				int closed = (int) closeHandle.invokeExact(handle);
				throw new IllegalStateException("SetInformationJobObject failed");
			}
		}
		// never closed: the system closes it when the JVM exits, and kills the processes of the job
		job = handle;
	}
}
