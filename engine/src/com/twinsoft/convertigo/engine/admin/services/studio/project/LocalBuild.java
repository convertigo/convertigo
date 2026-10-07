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

package com.twinsoft.convertigo.engine.admin.services.studio.project;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.localbuild.BuildLocally;

/**
 * The Cordova builds of the mobile platforms on the machine of the engine, as the local build actions of
 * the Eclipse Studio run them: Cordova, the JDK, the Android SDK and Gradle are installed when needed, the
 * build runs in the background and tells its end in the logs.
 */
public final class LocalBuild {
	/** the build of each platform, by its qname */
	private static final Map<String, Build> BUILDS = new ConcurrentHashMap<>();

	private static class Build {
		private BuildLocally local;
		private boolean run;
		private volatile String state = "running";
		private volatile String message = "";
	}

	private LocalBuild() {
	}

	private static BuildLocally local(MobilePlatform platform, Build build) {
		return new BuildLocally(platform) {

			@Override
			protected String getLocalBuildAdditionalPath() {
				return "";
			}

			@Override
			protected void logException(Throwable e, String message) {
				Engine.logEngine.error(message, e);
				if (build != null) {
					build.message = message;
				}
			}

			@Override
			protected void showLocationInstallFile(MobilePlatform mobilePlatform, int exitValue, String errorLines,
					String buildOption) {
				if (build == null) {
					return;
				}
				var file = getAbsolutePathOfBuiltFile(mobilePlatform, buildOption);
				build.state = exitValue == 0 ? "done" : "failed";
				build.message = exitValue == 0
						? "The application is built" + (file == null ? "." : " in " + file.getAbsolutePath() + ".")
						: "The build ended with the code " + exitValue + (errorLines == null ? "." : ":\n" + errorLines);
				Engine.logEngine.info("(LocalBuild) " + mobilePlatform.getQName() + ": " + build.message);
			}
		};
	}

	/**
	 * Starts the build of a platform in the background.
	 *
	 * @param option debug or release
	 * @param run true to run the application after its build
	 * @param target device or emulator, for a run
	 * @return what the Studio tells
	 */
	public static String start(MobilePlatform platform, String option, boolean run, String target) {
		if (!Engine.isServerBuildAllowed()) {
			return "This server does not allow builds (allow_server_build).";
		}
		var qname = platform.getQName();
		var current = BUILDS.get(qname);
		if (current != null && "running".equals(current.state)) {
			return "A local build of " + platform.getName() + " is already running.";
		}
		var build = new Build();
		build.run = run;
		build.local = local(platform, build);
		BUILDS.put(qname, build);
		var thread = new Thread(() -> {
			try {
				var status = build.local.installCordova();
				if (status == BuildLocally.Status.OK) {
					status = build.local.runBuild(option, run, target);
				}
				if (status == BuildLocally.Status.CANCEL) {
					build.state = build.local.isProcessCanceled() ? "canceled" : "failed";
					if (build.message.isEmpty()) {
						build.message = "The local build is " + build.state + ".";
					}
				} else if ("running".equals(build.state)) {
					build.state = build.local.isProcessCanceled() ? "canceled" : "done";
				}
			} catch (Throwable e) {
				build.state = "failed";
				build.message = String.valueOf(e.getMessage());
				Engine.logEngine.error("(LocalBuild) " + qname + " failed", e);
			}
			Engine.logEngine.info("(LocalBuild) " + qname + " " + build.state);
		}, "LocalBuild " + qname);
		thread.setDaemon(true);
		thread.start();
		return "The local " + option + " build of " + platform.getName() + " started, the logs follow it.";
	}

	/**
	 * @return the state of the last build of the platform
	 */
	public static String status(MobilePlatform platform) {
		var build = BUILDS.get(platform.getQName());
		if (build == null) {
			return "No local build of " + platform.getName() + " ran since the engine started.";
		}
		return "The local build of " + platform.getName() + " is " + build.state
				+ (build.message.isEmpty() ? "." : ": " + build.message);
	}

	/**
	 * @return what the Studio tells, the running build of the platform being stopped
	 */
	public static String cancel(MobilePlatform platform) {
		var build = BUILDS.get(platform.getQName());
		if (build == null || !"running".equals(build.state)) {
			return "No local build of " + platform.getName() + " is running.";
		}
		try {
			build.local.cancelBuild(build.run);
		} catch (NullPointerException e) {
			// no command runs yet: the build stops at its next step
		}
		build.state = "canceled";
		return "The local build of " + platform.getName() + " is canceled.";
	}

	/**
	 * Removes the Cordova environment of the platform, as the Clear Cordova action of the Eclipse Studio.
	 */
	public static String clear(MobilePlatform platform) {
		var build = BUILDS.get(platform.getQName());
		if (build != null && "running".equals(build.state)) {
			return "A local build of " + platform.getName() + " is running: cancel it first.";
		}
		local(platform, null).removeCordovaDirectory();
		return "The Cordova environment of " + platform.getName() + " is removed.";
	}
}
