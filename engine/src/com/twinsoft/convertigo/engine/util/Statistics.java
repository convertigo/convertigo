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

import java.util.HashMap;
import java.util.Map;

import com.twinsoft.convertigo.engine.Engine;

/**
 * This class is useful for calculating statistics on tasks duration.
 */
public class Statistics {
    private static int BUFFER_SIZE = 1024;
    
    private Map<String, Long> latestDuration = new HashMap<String, Long>(32);
    
    // Shared by all the instances: only used under the lock of the class.
    private static Map<String, String> tasks = new HashMap<String, String>(BUFFER_SIZE);
    
    private static Map<String, Long> sums = new HashMap<String, Long>(BUFFER_SIZE);
    
    private static Map<String, Long> dividers = new HashMap<String, Long>(BUFFER_SIZE);
    
    private static long id = 0;
    
    public synchronized void clearLatestDurations() {
    	latestDuration.clear();
    }
    
    /**
     * Retrieves the latest duration for a given task.
     *
     * @param taskID the ID for the task.
     *
     * @return the latest duration of the specified task
     * or <code>-1</code> if any error occurs.
     */
    public synchronized long getLatestDuration(String taskID) {
        try {
            return latestDuration.getOrDefault(taskID, -1L);
        }
        catch(Exception e) {
            return -1L;
        }
    }
    
    /**
     * Retrieves the average duration for a given task.
     *
     * @param taskID the ID for the task.
     *
     * @return the average duration of the specified task
     * or <code>-1</code> if any error occurs.
     */
    public synchronized static long getAverage(String taskID) {
        try {
        	if (sums.containsKey(taskID) && dividers.containsKey(taskID)) {
        	long taskDurations = sums.get(taskID).longValue();
        	long taskNum = dividers.get(taskID).longValue();
            return taskDurations / taskNum;
        }
            return -1L;
        }
        catch(Exception e) {
            return -1L;
        }
    }
    
    /**
     * Starts a statistic calculation.
     *
     * @param taskID the ID for the task to observed (it can be any ID,
     * and it is used to compute averages).
     *
     * @return the statistic ID used for stopping the statistic.
     */
    public synchronized String start(String taskID) {
        long t0 = System.currentTimeMillis();
        String statID = getNextId() + "/" + t0;
        String currentTaskID;
        synchronized (Statistics.class) {
            currentTaskID = tasks.put(statID, taskID);
        }
        
        if (currentTaskID != null) {
        	Engine.logEngine.warn("(Statistics) Duplicate statistics ID: " + statID);
        }
        
        return statID;
    }
    
    /**
     * Stops a statistic calculation and update average duration for
     * the task associated with this statistic calculation.
     *
     * @param the statistic ID returned by the <code>start()</code> method.
     *
     * @return the duration of the task or <code>-1</code> if any error occurs.
     */
    public synchronized long stop(String statID) {
        return stop(statID, false);
    }
    
    /**
     * Stops a statistic calculation and update average duration for
     * the task associated with this statistic calculation.
     *
     * @param statID the statistic ID returned by the <code>start()</code> method.
     * @param bAdd means if the statistic should be added to the last stored.
     *
     * @return the duration of the task or <code>-1</code> if any error occurs.
     */
    public synchronized long stop(String statID, boolean bAdd) {
    	String taskID = null;
    	long l = 0;;
        try {
            long t1 = System.currentTimeMillis();
            long t0 = Long.parseLong(statID.substring(statID.indexOf("/") + 1));
            synchronized (Statistics.class) {
                taskID = tasks.remove(statID);
            }
            
            long thisDuration = Math.max(t1 - t0, 0);
            
            if (bAdd) {
                long ld = getLatestDuration(taskID);
                if (ld > 0) {
                    thisDuration += ld;
                }
            }

            l = accumulate(taskID, thisDuration);
                
			latestDuration.put(taskID, thisDuration);
            
            return thisDuration;
        }
        catch(Exception e) {
        	Engine.logEngine.warn("Unexpected error in EngineStatistics", e);
        	Engine.logEngine.warn("   statID=" + statID);
        	Engine.logEngine.warn("   taskID=" + taskID);
        	Engine.logEngine.warn("   l=" + l);
        	String dividersText;
        	synchronized (Statistics.class) {
        		dividersText = String.valueOf(dividers);
        	}
        	Engine.logEngine.warn("   dividers=" + dividersText);
            return -1;
        }
    }
    
    public synchronized void add(String taskID, long duration) {
        try {
            accumulate(taskID, duration);

            latestDuration.put(taskID, Long.valueOf(duration));
        }
        catch(Exception e) {
        	Engine.logEngine.warn("Unexpected error in EngineStatistics", e);
            // Silently ignore
        }
    }
    
    /**
     * Resets the statistics object.
     */
    public static synchronized void reset() {
        tasks = new HashMap<String, String>(BUFFER_SIZE);
        sums = new HashMap<String, Long>(BUFFER_SIZE);
        dividers = new HashMap<String, Long>(BUFFER_SIZE);
        id = 0;
        System.gc();
    }

    /**
     * Resets aggregated statistics for a specific task.
     *
     * @param taskID the task identifier to reset
     */
    public static synchronized void reset(String taskID) {
        sums.remove(taskID);
        dividers.remove(taskID);
    }
    
    /**
     * Adds a duration to the totals of a task, used to compute its average.
     *
     * @return the number of durations of the task.
     */
    private static synchronized long accumulate(String taskID, long duration) {
        long l = dividers.getOrDefault(taskID, 0L) + 1;
        dividers.put(taskID, l);
        sums.put(taskID, sums.getOrDefault(taskID, 0L) + duration);
        return l;
    }
    
    /**
     * Resets the statistics object.
     */
    private static synchronized long getNextId() {
    	return id++;
    }
}
