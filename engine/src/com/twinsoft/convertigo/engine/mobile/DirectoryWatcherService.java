package com.twinsoft.convertigo.engine.mobile;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.mobile.ComponentRefManager.Mode;
import com.twinsoft.convertigo.engine.util.FileUtils;

public class DirectoryWatcherService implements Runnable {
    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    /*
     * Wait this long after an event before processing the files.
     */
    private final int DELAY = 10; // ms

    /*
     * Use a SET to prevent duplicates from being added when multiple events on the 
     * same file arrive in quick succession.
     */
    private HashSet<String> filesToProcess = new HashSet<String>();

    /*
     * Keep a map that will be used to resolve WatchKeys to the parent directory
     * so that we can resolve the full path to an event file. 
     */
    private final Map<WatchKey,Path> keys;

    Timer processDelayTimer = null;

    private volatile Thread server;

    private boolean trace = false;

    private WatchService watcher = null;

    private Project project = null;
    
    protected DirectoryWatcherService(Project project, boolean recursive) throws IOException {
    	this.project = project;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey,Path>();

        Path dir = Paths.get(Engine.projectDir(this.project.getName()),"_private/ionic/src");
        if (recursive) {
            registerAll(dir);
        } else {
            register(dir);
        }
    }

    private String getCompQName(String compName) {
    	MobileApplication mobileApplication = project.getMobileApplication();
    	ApplicationComponent app = (ApplicationComponent)mobileApplication.getApplicationComponent();
    	for (UISharedComponent uisc: app.getSharedComponentList()) {
    		if (compName.equals(uisc.getName().toLowerCase())) {
    			return uisc.getQName();
    		}
    	}
    	return null;
    }
    
    private boolean hasComp(String compName) {
    	MobileApplication mobileApplication = project.getMobileApplication();
    	ApplicationComponent app = (ApplicationComponent)mobileApplication.getApplicationComponent();
    	for (UISharedComponent uisc: app.getSharedComponentList()) {
    		if (compName.equals(uisc.getName().toLowerCase())) {
    			return true;
    		}
    	}
    	return false;
    }
    
    private boolean isDirectory(String filename) {
        File f = new File(filename);
        if (f.exists()) {
            return f.isDirectory();
        } else {
             return f.getName().lastIndexOf('.') == -1;
        }
    }
    
    private boolean isFile(String filename) {
        File f = new File(filename);
        if (f.exists()) {
            return f.isFile();
        } else {
             return f.getName().lastIndexOf('.') != -1;
        }
    }
    
    private boolean ignore(String filename) {
        Path p = Paths.get(filename);
        
        // ignore non components directory or files
        if (isDirectory(filename) && !p.getParent().endsWith("components")) {
        	return true;
        }
        if (isFile(filename) && !p.getParent().getParent().endsWith("components")) {
        	return true;
        }

        // ignore temporary files
    	if (filename.endsWith(".temp.ts")) {
    		return true;
    	}
    	
        // accept deleted component directory - ignore deleted component files
    	File f = new File(filename);
    	if (!f.exists()) {
    		return isFile(filename);
    	}
    	
        return !hasComp(isDirectory(filename) ? p.getFileName().toString() : p.getParent().getFileName().toString());
    }
    
    private synchronized void addFileToProcess(String filename) {
    	if (ignore(filename)) {
    		return;
    	}
    	
        filesToProcess.add(filename);
        
        if (processDelayTimer != null) {
            processDelayTimer.cancel();
        }
        processDelayTimer = new Timer();
        processDelayTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                processFiles();
            }
        }, DELAY);
    }

    private static String pname(String qname) {
    	if (qname != null && !qname.isBlank()) {
    		try {
    			return qname.substring(0, qname.indexOf('.'));
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    	return "none";
    }
    
    private synchronized void processFiles() {
        /*
         * Iterate over the set of file to be processed
         */
        for (Iterator<String> it = filesToProcess.iterator(); it.hasNext();) {
            String filename = it.next();
            Path p = Paths.get(filename);
            File src = new File(filename);
            
            // copy
            if (src.exists()) {
            	if (src.isDirectory()) {
            		try {
						registerAll(p);
					} catch (IOException e) {
						e.printStackTrace();
					}
            	}
            	
	            String compName = src.isDirectory() ? p.getFileName().toString() : p.getParent().getFileName().toString();
	            if (compName != null) {
	            	for (String useQName: ComponentRefManager.get(Mode.use).getAllConsumers(getCompQName(compName))) {
		    			if (pname(useQName).equals(project.getName()))
		    				continue;
		        		try {
		                    File dest = new File(filename.replace(project.getName(), pname(useQName)));
		    				Engine.logEngine.debug("(DirectoryWatcherService) Copying " + src + " to " + dest);
		        			if (src.isDirectory()) {
		        				FileUtils.copyDirectory(src, dest, true);
		        			} else {
		        				FileUtils.copyFile(src, dest, true);
		        			}
		        		} catch (Exception e) {
		        			Engine.logEngine.warn(e.getMessage());
		        		}
		    		}
	            }
            }
            // delete
            else {
   				Engine.logEngine.debug("(DirectoryWatcherService) Deleted " + filename);
            }
            
            /*
             * Remove this file from the set.
             */
            it.remove();
        }
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                System.out.format("register: %s\n", dir);
            } else {
                if (!dir.equals(prev)) {
                    System.out.format("update: %s -> %s\n", prev, dir);
                }
            }
        }
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    protected void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException
            {
                if (dir.getFileName().toString().startsWith(".")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        Thread thisThread = Thread.currentThread();

        while (server == thisThread) {
            try {
                // wait for key to be signaled
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (Exception x) {
                    return;
                }

                Path dir = keys.get(key);
                if (dir == null) {
                    continue;
                }

                for (WatchEvent<?> event: key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == OVERFLOW) {
                        continue;
                    }
                    if (kind == ENTRY_MODIFY || kind == ENTRY_CREATE || kind == ENTRY_DELETE) {
                        WatchEvent<Path> ev = (WatchEvent<Path>)event;
                        Path name = ev.context();
                        Path child = dir.resolve(name);

                        String filename = child.toAbsolutePath().toString();
                        if (trace) {
	                        if (kind == ENTRY_CREATE) {
	                            System.out.println("created: " +filename);
	                        } else if (kind == ENTRY_MODIFY) {
	                        	System.out.println("modified: " +filename);
	                        } else if (kind == ENTRY_DELETE) {
	                        	System.out.println("deleted: " +filename);
	                        }
                        }
                        
                        addFileToProcess(filename);
                    }
                }

                key.reset();
            } catch (Exception e) {
            	Engine.logEngine.error(e.getMessage());
            }
        }
    }

    public void start() {
        server = new Thread(this);
        server.setName(project.getName() + "Directory Watcher Service");
        server.start();
    }


    public void stop() {
    	try {
	        Thread moribund = server;
	        server = null;
	        if (moribund != null) {
	            moribund.interrupt();
	        }
    	} catch (Exception e) {
    		Engine.logEngine.error(e.getMessage());
    	} finally {
    		this.watcher = null;
    		this.project = null;
    		if (filesToProcess != null) {
    			filesToProcess.clear();
    		}
    		if (keys != null) {
    			keys.clear();
    		}
    	}
    }
}