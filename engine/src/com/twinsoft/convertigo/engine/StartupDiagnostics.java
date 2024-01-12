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

package com.twinsoft.convertigo.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;

import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;

class StartupDiagnostics {

	private static final String TEST_SUCCESS = "OK\n";
	private static final String TEST_WARN = "WARN\n";
	private static final String TEST_FAILED = "FAILED\n";

	private static enum Architecture {
		x32bits, x64bits, unknown
	}

	private static Architecture getArchitecture() {
		String osArchitecture = System.getProperty("os.arch");
		if ("i386".equals(osArchitecture) || "x86".equals(osArchitecture)) {
			return Architecture.x32bits;
		}
		else if ("ia64".equals(osArchitecture) || "amd64".equals(osArchitecture)) {
			return Architecture.x64bits;
		}
		else {
			return Architecture.unknown;
		}
	}

	protected static void run() {
		String testsSummary = "";
		Level currentLevel = Engine.logEngine.getEffectiveLevel();

		// To avoid debug traces due to the ZipUtils helper routines,
		// set the engine logger level to INFO.
		Engine.logEngine.setLevel(Level.INFO);

		try {
			Engine.logEngine.info("*** STARTUP DIAGNOSTICS ***");

			String os = System.getProperty("os.name");

			boolean isLinux = os.startsWith("Linux");
			// boolean isWindows = os.startsWith("Windows");
			boolean isMacOS = os.startsWith("Mac OS X");
			
			Architecture architecture = getArchitecture();
			Engine.logEngine.info("Detected OS: " + os + " "
					+ (architecture == Architecture.x32bits ? "(32 bits)" :
						architecture == Architecture.x64bits ? "(64 bits)" : "(unknown architecture)"));

			testsSummary += " - WAR architecture ........................... ";
			File buildInfoFile = new File(Engine.WEBAPP_PATH + "/WEB-INF/build.txt");
			try {
				Properties buildProperties = PropertiesUtils.load(buildInfoFile);

				String buildFileName = buildProperties.getProperty("build.filename");
				if (buildFileName == null) {
					Engine.logEngine.warn("The build info file (" + buildInfoFile.getPath() + ") does not contain build file name info!");
					testsSummary += TEST_WARN;
				}
				else {
					Engine.logEngine.info("WAR file name: " + buildFileName);
					testsSummary += TEST_SUCCESS;
				}
			} catch (FileNotFoundException e) {
				Engine.logEngine.warn("The build info file (" + buildInfoFile.getPath() + ") does not exist!");
				testsSummary += TEST_WARN;
			} catch (Exception e) {
				Engine.logEngine.warn("Unable to read the build info file (" + buildInfoFile.getPath() + ")!", e);
				testsSummary += TEST_WARN;
			}

			if (isLinux) {
				String sysLdLibraryPath = System.getenv("LD_LIBRARY_PATH");
				Engine.logEngine.info("System LD_LIBRARY_PATH: " + sysLdLibraryPath);
			}			

			String javaHome = System.getProperty("java.home");
			Engine.logEngine.info("Java home: " + javaHome);

			String javaLibraryPath = System.getProperty("java.library.path");
			Engine.logEngine.info("Java library path: " + javaLibraryPath);

			String workingDir = System.getProperty("user.dir");
			Engine.logEngine.info("Java working dir: " + workingDir);

			testsSummary += " - Test Java working directory write access ... ";
			try {
				StartupDiagnostics.testWriteAccess(new File(workingDir), true);
				testsSummary += TEST_SUCCESS;
			} catch (IOException e) {
				Engine.logEngine.error("The Java working directory is not writeable!");
				testsSummary += TEST_FAILED;
			}

			// Checking running user
			testsSummary += " - Running user ............................... ";

			String userName = System.getProperty("user.name");
			Engine.logEngine.info("Java running user: " + userName);

			if (isLinux || isMacOS) {
				try {
					// Checking user returned by the command 'whoami'
					Process process = Runtime.getRuntime().exec("whoami");
					InputStreamReader isrStdout = new InputStreamReader(process.getInputStream());
					BufferedReader stdout = new BufferedReader(isrStdout);
					String whoami = stdout.readLine();
					Engine.logEngine.info("Whoami running user: " + whoami);

					if (!userName.equals(whoami)) {
						Engine.logEngine.warn("Java user name is different than 'whoami' user name!");
						testsSummary += TEST_WARN;
					} else {
						testsSummary += TEST_SUCCESS;
					}
				} catch (IOException e) {
					Engine.logEngine.error("Unable to retrieve the running user with 'whoami': "
							+ e.getMessage());
					testsSummary += TEST_WARN;
				}
			} else {
				testsSummary += TEST_SUCCESS;
			}

			// Checking user home
			testsSummary += " - User home directory ........................ ";
			String userHome = System.getProperty("user.home");
			Engine.logEngine.info("Java user home: " + userHome);

			boolean isUserHomeWritable;
			try {
				StartupDiagnostics.testWriteAccess(new File(userHome), true);
				isUserHomeWritable = true;
			} catch (IOException e) {
				Engine.logEngine.error("The Java user home directory is not writeable!");
				isUserHomeWritable = false;
			}

			if (isLinux || isMacOS) {
				String sysEnvHome = System.getenv("HOME");
				Engine.logEngine.info("System env HOME: " + sysEnvHome);

				if (!userHome.equals(sysEnvHome)) {
					Engine.logEngine.warn("Java user home is different than the system environment HOME!");
					testsSummary += TEST_WARN;

					try {
						StartupDiagnostics.testWriteAccess(new File(userHome), true);
						isUserHomeWritable &= true;
					} catch (IOException e) {
						Engine.logEngine.error("The system env HOME directory is not writeable!");
						isUserHomeWritable = false;
					}
				} else {
					if (!isMacOS) {
						// Checking /etc/passwd file under linux
						try {
							File etcPasswdFile = new File("/etc/passwd");
							List<String> lines = FileUtils.readLines(etcPasswdFile, "UTF-8");

							String etcPasswdUserHome = null;
							for (String line : lines) {
								if (line.startsWith(userName)) {
									String[] parts = line.split(":");
									if (parts.length > 5) {
										etcPasswdUserHome = parts[5];
										break;
									}
								}
							}

							if (etcPasswdUserHome == null) {
								Engine.logEngine.warn("Unable to find the user home in /etc/passwd");
								testsSummary += TEST_WARN;
							} else {
								Engine.logEngine.info("User home as defined in /etc/passwd: " + etcPasswdUserHome);
								if (!etcPasswdUserHome.equals(userHome)) {
									Engine.logEngine
									.error("The user home defined in /etc/passwd differs from the user home!");
									testsSummary += TEST_FAILED;
								} else {
									testsSummary += TEST_SUCCESS;
								}
							}
						} catch (IOException e) {
							Engine.logEngine.error("Unable to read the /etc/passwd file: " + e.getMessage());
							testsSummary += TEST_WARN;
						}
					} else {
						testsSummary += TEST_SUCCESS;
					}
				}
			} else {
				testsSummary += TEST_SUCCESS;
			}

			// Test write access in user home dir (Java & system env)
			// The user dir must be writeable in order eclipse to write its
			// working files such as .eclipse...
			testsSummary += " - Test user home directory write access ...... ";
			testsSummary += (isUserHomeWritable ? TEST_SUCCESS : TEST_FAILED);

			// Check JBoss tmp dir
			String jbossTmpDir = System.getProperty("jboss.server.temp.dir");
			if (jbossTmpDir != null) {
				testsSummary += " - JBoss tmp directory write access ........... ";

				Engine.logEngine.info("JBoss detected");
				Engine.logEngine.info("JBoss tmp dir: " + jbossTmpDir);

				// Test write access in JBoss tmp dir
				try {
					StartupDiagnostics.testWriteAccess(new File(jbossTmpDir), true);
					testsSummary += TEST_SUCCESS;
				} catch (IOException e) {
					Engine.logEngine.error("The JBoss tmp directory is not writeable!");
					testsSummary += TEST_FAILED;
				}
			}

			// Check Tomcat tmp dir
			String tomcatTmpDir = System.getenv("CATALINA_TMPDIR");
			if (tomcatTmpDir != null) {
				testsSummary += " - Tomcat tmp directory write access .......... ";

				Engine.logEngine.info("Tomcat detected");
				Engine.logEngine.info("Tomcat tmp dir: " + tomcatTmpDir);

				// Test write access in Tomcat tmp dir
				try {
					StartupDiagnostics.testWriteAccess(new File(tomcatTmpDir), true);
					testsSummary += TEST_SUCCESS;
				} catch (IOException e) {
					Engine.logEngine.error("The Tomcat tmp directory is not writeable!");
					testsSummary += TEST_FAILED;
				}
			}

			// TODO: test websphere tmp dir

			// System tmp dir
			testsSummary += " - System tmp directory write access .......... ";

			String sysTempDir = System.getProperty("java.io.tmpdir");
			Engine.logEngine.info("System tmp dir: " + sysTempDir);

			// Test write access in system tmp dir
			File testTmpDir = null;
			try {
				testTmpDir = StartupDiagnostics.testWriteAccess(new File(sysTempDir), true);
				testsSummary += TEST_SUCCESS;
			} catch (IOException e) {
				Engine.logEngine.error("The system tmp directory is not writeable!");
				testsSummary += TEST_FAILED;
				return;
			} finally {
				if (!FileUtils.deleteQuietly(testTmpDir)) {
					Engine.logEngine.warn("Unable to delete tmp test dir: " + testTmpDir.getPath());
				}
			}

			if (testsSummary.indexOf("FAILED") == -1 && testsSummary.indexOf("WARN") == -1) {
				testsSummary += " :::  Bravo! All environment tests succeeded :o)  :::";
			}
		} catch (Throwable e) {
			Engine.logEngine.error("Error while checking environment", e);
		} finally {
			Engine.logEngine.info("*** ENVIRONMENT DIAGNOSTICS SUMMARY ***\n" + testsSummary);
			// Restore engine logger level
			Engine.logEngine.setLevel(currentLevel);
		}
	}

	private static File testWriteAccess(File dir, boolean deleteTmpDirOnExit) throws IOException {
		String c8oTmpDirName = "convertigo-" + (int) (Math.random() * 1000000000) + ".tmp";
		File tmpDirFile = new File(dir, c8oTmpDirName);

		try {
			if (tmpDirFile.exists()) {
				FileUtils.deleteQuietly(tmpDirFile);
			}

			tmpDirFile.mkdir();

			if (!tmpDirFile.exists()) {
				throw new IOException("The directory '" + dir.getPath() + "' is not writeable!");
			}

			return tmpDirFile;
		} finally {
			if (deleteTmpDirOnExit) {
				tmpDirFile.delete();
			}
		}
	}
}
