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

package com.twinsoft.convertigo.eclipse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.Status;

import com.twinsoft.convertigo.engine.Engine;

public class ConsolePipes {

	public static final int MAX_CONSOLE_START_SIZE = 16000;
	public static final int REFRESH_DELAY = 250;

	private Thread writerThreadEngine;

	public ByteArrayOutputStream outputStreamConnector;

	public ByteArrayOutputStream outputStreamTrace;

	private boolean bContinue = true;

	private ConvertigoPlugin convertigoPlugin;

	public ConsolePipes() {
		convertigoPlugin = ConvertigoPlugin.getDefault();
	}

	public void stopConsoleThreads() {
		bContinue = false;
		int nbRetry = 10;
		while ((nbRetry > 0) && writerThreadEngine.isAlive()) {
			try {
				Thread.sleep(100);
				nbRetry--;
			} catch (InterruptedException e) {
				return;
			}
		}
		if (writerThreadEngine.isAlive()) {
			convertigoPlugin.getLog().log(
					new Status(Status.ERROR, ConvertigoPlugin.PLUGIN_UNIQUE_ID, "Unable to close consoles"));
		}
	}

	public void initConsoleStreams() {
		try {
			outputStreamConnector = new ByteArrayOutputStream(4096);
		} catch (Exception e) {
			String message = java.text.MessageFormat.format(
					java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/studio/res/ConsolePanel")
							.getString("unable_to_initiate_console"), new Object[] { "Connector" });
			convertigoPlugin.getLog().log(
					new Status(Status.ERROR, ConvertigoPlugin.PLUGIN_UNIQUE_ID, Status.OK, message, e));
			return;
		}

		try {
			outputStreamTrace = new ByteArrayOutputStream(1024);
		} catch (Exception e) {
			String message = java.text.MessageFormat.format(
					java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/studio/res/ConsolePanel")
							.getString("unable_to_initiate_console"), new Object[] { "Trace" });
			convertigoPlugin.getLog().log(
					new Status(Status.ERROR, ConvertigoPlugin.PLUGIN_UNIQUE_ID, Status.OK, message, e));
			return;
		}
	}

	public void startConsoleThreads() {
		startEngineConsoleThread();
		startStudioConsoleThread();
		startTraceConsoleThread();
	}

	public void startEngineConsoleThread() {
		writerThreadEngine = new Thread() {
			@Override
			public void run() {
				try {
					int nbAvailableBytes;
					String sBuffer;
					char[] buffer = new char[1024];
					long logFileSize = 0;
					long seek = -1;
					boolean alertOnSettings = false;
					
					while (bContinue) {
						if (Engine.logEngine != null) {
							if (ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_SHOW_ENGINE_INTO_CONSOLE).equalsIgnoreCase("true")) {
								alertOnSettings = false;
								String logFileName = Engine.LOG_PATH + "/" + Engine.LOG_ENGINE_NAME;
								File logFile = new File(logFileName);
								long logFileSizeCurrent = logFile.length();
								if (logFileSize > logFileSizeCurrent) {
									seek = -1;
								}
	
								
								if (logFileSize != logFileSizeCurrent) {
									InputStreamReader fr = null;
									try {
										long fileLength = logFile.length();
										fr = new InputStreamReader(new FileInputStream(logFile), "UTF-8");
										
										if (seek == -1) {
											seek = 0;
											if (fileLength > MAX_CONSOLE_START_SIZE) {
												seek = fileLength - MAX_CONSOLE_START_SIZE;
											}
										}
										fr.skip(seek);
	
										boolean loop = false;
										do {
											nbAvailableBytes = fr.read(buffer, 0, buffer.length);
											if (nbAvailableBytes == -1) {
												loop = false;
											} else {
												if (loop)
													Thread.sleep(25); // prevent Eclipse from freezing with big logs
												else
													loop = true;
												sBuffer = new String(buffer, 0, nbAvailableBytes);
												seek += sBuffer.length();
												convertigoPlugin.engineConsoleStream.print(sBuffer);
											}
										} while (loop && bContinue);
									} catch (FileNotFoundException e) {
										// Ignore: the file has yet been created
										seek = -1;
									} catch (IOException e) {
										seek = -1;
									} finally {
										if (fr != null)
											fr.close();
									}
									logFileSize = logFileSizeCurrent;
								}
							} else {
								if (!alertOnSettings) {
									convertigoPlugin.engineConsoleStream.print("*****  ENGINE LOGS NOT WRITTEN INTO THE CONSOLE, IF YOU WANT TO SET THEM  *****");
									convertigoPlugin.engineConsoleStream.println();
									convertigoPlugin.engineConsoleStream.print("***** PLEASE ENABLE \"Show Engine logs...\" SETTING IN STUDIO PREFERENCES *****");
									convertigoPlugin.engineConsoleStream.println();
									convertigoPlugin.engineConsoleStream.print("*****                    THEN RESTART THE STUDIO                          *****");
									convertigoPlugin.engineConsoleStream.println();
									alertOnSettings = true;
									bContinue = false;
								}
							}
						} 
						
						if (bContinue) Thread.sleep(REFRESH_DELAY);
					}
					convertigoPlugin.getLog().log(
							new Status(Status.INFO, ConvertigoPlugin.PLUGIN_UNIQUE_ID, Status.OK,
									"Stop thread for engine console", null));
				} catch (Exception e) {
					String message = "Unable to write to engine console";
					convertigoPlugin.getLog()
							.log(new Status(Status.ERROR, ConvertigoPlugin.PLUGIN_UNIQUE_ID, Status.OK,
									message, e));
				}
				
			}
		};
		writerThreadEngine.start();
	}

	public void startStudioConsoleThread() {
		// writerThreadStudio = new Thread() {
		// public void run() {
		// try {
		// int nbAvailableBytes;
		// String sBuffer, text;
		// int len;
		// char[] buffer = new char[1024];
		// long modTime = 0;
		// long seek = -1;
		//
		// while (bContinue) {
		// if (Studio.log != null) {
		// synchronized(Studio.log) {
		// String logFileName = Studio.log.getLogFileName();
		// File logFile = new File(logFileName);
		// long modTimeTmp = logFile.lastModified();
		//
		// if (modTime != modTimeTmp) {
		// FileReader fr = null;
		// try {
		// long fileLength = logFile.length();
		// fr = new FileReader(logFile);
		//
		// if (seek == -1) {
		// seek = 0;
		// if (fileLength > MAX_CONSOLE_SIZE) {
		// seek = fileLength - MAX_CONSOLE_SIZE;
		// }
		// }
		// fr.skip(seek);
		//
		// while (seek < fileLength) {
		// nbAvailableBytes = fr.read(buffer, 0, buffer.length);
		// seek += nbAvailableBytes;
		// sBuffer = new String(buffer, 0, nbAvailableBytes);
		// text = jTextAreaStudio.getText();
		// len = text.length();
		// if (len > MAX_CONSOLE_SIZE) {
		// jTextAreaStudio.replaceRange("[...]", 0, text.indexOf("\n", len -
		// MAX_CONSOLE_SIZE + 10));
		// }
		// jTextAreaStudio.append(sBuffer);
		// }
		// }
		// catch(FileNotFoundException e) {
		// // Ignore: the file has yet been created
		// seek = -1;
		// }
		// catch(IOException e) {
		// jTextAreaStudio.append("------------------------------------------------------------------------\n");
		// jTextAreaStudio.append(java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/studio/res/ConsolePanel").getString("unable_to_load_studio_log"));
		// jTextAreaStudio.append(Log.getStackTrace(e));
		// jTextAreaStudio.append("------------------------------------------------------------------------\n");
		// seek = -1;
		// }
		// finally {
		// if (fr != null) fr.close();
		// jTextAreaStudio.setCaretPosition(jTextAreaStudio.getText().length());
		// }
		// modTime = modTimeTmp;
		// }
		// }
		// }
		//
		// Thread.sleep(REFRESH_DELAY);
		// }
		// convertigoPlugin.getLog().log(new Status(Status.INFO,
		// ConvertigoPlugin.PLUGIN_UNIQUE_ID, Status.OK,
		// "Stop thread for studio console", null));
		// }
		// catch(Exception e) {
		// String message = java.text.MessageFormat.format(
		// java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/studio/res/ConsolePanel").getString("unable_to_write_console"),
		// new Object[] { "Engine" }
		// );
		// convertigoPlugin.getLog().log(new Status(Status.ERROR,
		// ConvertigoPlugin.PLUGIN_UNIQUE_ID, Status.OK, message, e));
		// }
		// }
		// };
		// writerThreadStudio.start();
	}

	public void startTraceConsoleThread() {
		// writerThreadTrace = new Thread() {
		// public void run() {
		// try {
		// String sBuffer;
		//
		// while (bContinue) {
		// sBuffer = outputStreamTrace.toString();
		// outputStreamTrace.reset();
		// jTextAreaTrace.append(sBuffer);
		// jTextAreaTrace.setCaretPosition(jTextAreaTrace.getText().length());
		//
		// Thread.sleep(REFRESH_DELAY);
		// }
		// convertigoPlugin.getLog().log(new Status(Status.INFO,
		// ConvertigoPlugin.PLUGIN_UNIQUE_ID, Status.OK,
		// "Stop thread for trace console", null));
		// }
		// catch(Exception e) {
		// String message = java.text.MessageFormat.format(
		// java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/studio/res/ConsolePanel").getString("unable_to_write_console"),
		// new Object[] { "Trace" }
		// );
		// convertigoPlugin.getLog().log(new Status(Status.ERROR,
		// ConvertigoPlugin.PLUGIN_UNIQUE_ID, Status.OK, message, e));
		// }
		// }
		// };
		// writerThreadTrace.start();
	}
}
