/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.engine.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Queue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.studio.events.AbstractEvent;


public class GetEvents extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Queue<AbstractEvent> events = new LinkedList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        
        try (PrintWriter writer = resp.getWriter()) {
        	synchronized (events) {
        		while (!writer.checkError()) {
        			if (events.isEmpty()) {
        				writer.append("\n");
        				writer.flush();
        				events.wait(1000);
        			} else {
        				AbstractEvent event = events.poll();
        				if (event != null) {
        					writer.append(createEventMessage(event));
        					writer.flush();
        				}
        			}
        		}
        	}
        } catch (InterruptedException e) {
			throw new IOException(e);
		}
    }

    private String createEventMessage(AbstractEvent event) {
        String eventStr = "event: " + event.getName() + "\n";
        String data = "data: " + event.getData().replaceAll("\r\n", "\\\\r\\\\n");
        return eventStr + data + "\n\n";
    }

    public static void addEvent(AbstractEvent event) {
    	synchronized (events) {
            events.add(event);
			events.notify();
		}
    }
}
