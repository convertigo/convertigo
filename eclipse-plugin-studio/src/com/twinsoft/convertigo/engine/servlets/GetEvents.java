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
