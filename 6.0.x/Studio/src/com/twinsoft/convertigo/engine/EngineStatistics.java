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

package com.twinsoft.convertigo.engine;

import java.util.Formatter;

import com.twinsoft.convertigo.engine.util.*;
 
public class EngineStatistics extends Statistics {
    public static final String GET_CURRENT_SCREEN_CLASS = "GetCurrentScreenClass";
    public static final String GET_JAVELIN_OBJECT = "GetJavelinObject";
    public static final String APPLY_USER_REQUEST = "ApplyUserRequest";
    public static final String HTTP_CONNECT = "HttpConnect";
    public static final String GET_XUL_DOCUMENT = "GetXulDocument";
    public static final String APPLY_BLOCK_FACTORY = "ApplyBlockFactory";
    public static final String APPLY_EXTRACTION_RULES = "ApplyExtractionRules";
    public static final String APPLY_SCREENCLASS_HANDLERS = "ApplyScreenClassHandlers";
    public static final String WORKER_THREAD_START = "WorkerThreadStart";
    public static final String WAIT_HTML_TRIGGER = "WaitHtmlTrigger";
    public static final String EXECUTE_SEQUENCE_STEPS = "ExecuteSequenceSteps";
    public static final String EXECUTE_SEQUENCE_CALLS = "ExecuteSequenceCalls";
    public static final String GENERATE_DOM = "GenerateDOM";
    public static final String OTHERS = "(others)";
    public static final String GET_DOCUMENT = "GetDocument";
    public static final String XSLT = "XSLT";
    public static final String REQUEST = "Request";
    public static final String HOST = "Host";
    public static final String CONVERTIGO = "Convertigo";
    
    public String printStatistics() {
        long dgcsc = getLatestDuration(GET_CURRENT_SCREEN_CLASS);
        long adgcsc = getAverage(GET_CURRENT_SCREEN_CLASS);
        long dgjo = getLatestDuration(GET_JAVELIN_OBJECT);
        long adgjo = getAverage(GET_JAVELIN_OBJECT);
        long daur = getLatestDuration(APPLY_USER_REQUEST);
        long adaur = getAverage(APPLY_USER_REQUEST);
        long dhc = getLatestDuration(HTTP_CONNECT);
        long adhc = getAverage(HTTP_CONNECT);
        long dgxd = getLatestDuration(GET_XUL_DOCUMENT);
        long adgxd = getAverage(GET_XUL_DOCUMENT);
        long dabf = getLatestDuration(APPLY_BLOCK_FACTORY);
        long adabf = getAverage(APPLY_BLOCK_FACTORY);
        long daer = getLatestDuration(APPLY_EXTRACTION_RULES);
        long adaer = getAverage(APPLY_EXTRACTION_RULES);
        long dasch = getLatestDuration(APPLY_SCREENCLASS_HANDLERS);
        long adasch = getAverage(APPLY_SCREENCLASS_HANDLERS);
        long dwht = getLatestDuration(WAIT_HTML_TRIGGER);
        long adwht = getAverage(WAIT_HTML_TRIGGER);
        long dwts = getLatestDuration(WORKER_THREAD_START);
        long adwts = getAverage(WORKER_THREAD_START);
        long dess = getLatestDuration(EXECUTE_SEQUENCE_STEPS);
        long adess = getAverage(EXECUTE_SEQUENCE_STEPS);        
        long desc = getLatestDuration(EXECUTE_SEQUENCE_CALLS);
        long adesc = getAverage(EXECUTE_SEQUENCE_CALLS);        
        long dgdom = getLatestDuration(GENERATE_DOM);
        long adgdom = getAverage(GENERATE_DOM);
        long dgd = getLatestDuration(GET_DOCUMENT);
        long adgd = getAverage(GET_DOCUMENT);

        long dxslt = getLatestDuration(XSLT);
        long adxslt = getAverage(XSLT);

        long dt = getLatestDuration(REQUEST);
        if (dt == -1) {
        	Engine.logEngine.warn("(Statistics) Negative measure detected; recomputing done!");
        	dt = dgd + 27;
        }
        long adt = getAverage(REQUEST);

        // Compute others duration
        long dothers = dgd;
        if (dgcsc != -1) dothers -= dgcsc;
        if (dgjo  != -1) dothers -= dgjo;
        if (daur  != -1) dothers -= daur;
        if (dhc   != -1) dothers -= dhc;
        if (dgxd  != -1) dothers -= dgxd;
        if (dabf  != -1) dothers -= dabf;
        if (daer  != -1) dothers -= daer;
        if (dasch != -1) dothers -= dasch;
        if (dwht  != -1) dothers -= dwht;
        if (dess  != -1) dothers -= dess;
        if (desc  != -1) dothers -= desc;
        if (dgdom != -1) dothers -= dgdom;
        if (dwts  != -1) dothers -= dwts;

        add(OTHERS, dothers);
        long adothers = getAverage(OTHERS);
        
        // Compute host duration
        long dh = 0;
        
        // Legacy connector
        if (dgjo  != -1) dh += dgjo;
        if (daur  != -1) dh += daur;
        if (dasch != -1) dh += dasch;

        // HTML connector
        if (dhc   != -1) dh += dhc;
        if (dwht  != -1) dh += dwht;
        
        // Other
        if (desc  != -1) dh += desc;
        
        add(HOST, dh);
        long adh = getAverage(HOST);
        
        long dc = dt;
        if (dh    > 0) dc -= dh;
        if (dxslt > 0) dc -= dxslt;
        add(CONVERTIGO, dc);
        long adc = getAverage(CONVERTIGO);

        long adt2 = adh + adc + adxslt;
        long ph = Math.round((float) dh * 100 / (float) dt);
        long aph = Math.round((float) adh * 100 / (float) adt2);
        long pc = Math.round((float) dc * 100 / (float) dt);
        long apc = Math.round((float) adc * 100 / (float) adt2);
        long pxslt = Math.round((float) dxslt * 100 / (float) dt);
        long apxslt = Math.round((float) adxslt * 100 / (float) adt2);
        
        StringBuilder stats = new StringBuilder(1024);
    	Formatter formatter = new Formatter(stats);

    	formatter.format(" ====================================================\n");
        formatter.format("|          Task            |   Current  |   Average  |\n");
        formatter.format("|==========================|============|============|\n");
        if (dwts  != -1) formatter.format("| %-24s | %7d ms | %7d ms |\n", WORKER_THREAD_START, dwts, adwts);
        if (dhc   != -1) formatter.format("| %-24s | %7d ms | %7d ms |\n", HTTP_CONNECT, dhc, adhc);
        if (dgxd  != -1) formatter.format("| %-24s | %7d ms | %7d ms |\n", GET_XUL_DOCUMENT, dgxd, adgxd);
        if (dwht  != -1) formatter.format("| %-24s | %7d ms | %7d ms |\n", WAIT_HTML_TRIGGER, dwht, adwht);
        if (dgjo  != -1) formatter.format("| %-24s | %7d ms | %7d ms |\n", GET_JAVELIN_OBJECT, dgjo, adgjo);
        if (dgcsc != -1) formatter.format("| %-24s | %7d ms | %7d ms |\n", GET_CURRENT_SCREEN_CLASS, dgcsc, adgcsc);
        if (daur  != -1) formatter.format("| %-24s | %7d ms | %7d ms |\n", APPLY_USER_REQUEST, daur, adaur);
        if (dabf  != -1) formatter.format("| %-24s | %7d ms | %7d ms |\n", APPLY_BLOCK_FACTORY, dabf, adabf);
        if (daer  != -1) formatter.format("| %-24s | %7d ms | %7d ms |\n", APPLY_EXTRACTION_RULES, daer, adaer);
        if (dasch != -1) formatter.format("| %-24s | %7d ms | %7d ms |\n", APPLY_SCREENCLASS_HANDLERS, dasch, adasch);
        if (dess  != -1) formatter.format("| %-24s | %7d ms | %7d ms |\n", EXECUTE_SEQUENCE_STEPS, dess, adess);
        if (desc  != -1) formatter.format("| %-24s | %7d ms | %7d ms |\n", EXECUTE_SEQUENCE_CALLS, desc, adesc);
        if (dgdom != -1) formatter.format("| %-24s | %7d ms | %7d ms |\n", GENERATE_DOM, dgdom, adgdom);
        formatter.format("| %-24s | %7d ms | %7d ms |\n", OTHERS, dothers, adothers);
        formatter.format("|                          |============|~~~~~~~~~~~~|\n");
        formatter.format("| %-24s | %7d ms | %7d ms |\n", GET_DOCUMENT, dgd, adgd);
        formatter.format(" ==================================================== \n\n");

    	formatter.format(" ======================================================== \n");
    	formatter.format("| Request        |      Current      |      Average      |\n");
        formatter.format("|================|===================|===================|\n");
        formatter.format("| Host           | %7d ms (%3d%%) | %7d ms (%3d%%) |\n", dh, ph, adh, aph);
        formatter.format("| Convertigo     | %7d ms (%3d%%) | %7d ms (%3d%%) |\n", dc, pc, adc, apc);
        if (dxslt != -1) formatter.format("| XSLT           | %7d ms (%3d%%) | %7d ms (%3d%%) |\n", dxslt, pxslt, adxslt, apxslt);
        formatter.format("|                |===================|~~~~~~~~~~~~~~~~~~~|\n");
        formatter.format("| Total          | %7d ms        | %7d ms        |\n", dt, adt);
        formatter.format(" ========================================================");
        
        return stats.toString();
    }
    
    public String addWhiteSpaces(String msg, boolean bRight, int maxLength) {
        int len = msg.length();
        for (int i = len ; i < maxLength ; i++) {
            if (bRight) {
                msg += " ";
            }
            else {
                msg = " " + msg;
            }
        }
        return msg;
    }
}
