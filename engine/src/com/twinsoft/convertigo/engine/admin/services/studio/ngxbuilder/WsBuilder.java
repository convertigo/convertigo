package com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder;

import java.io.IOException;

import javax.websocket.Session;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.WebSocketService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;


@ServiceDefinition(name = "WsBuilder", roles = { Role.WEB_ADMIN}, parameters = {}, returnValue = "")
public class WsBuilder extends WebSocketService {

	@Override
	public void onOpen(Session session) {
		Engine.logAdmin.warn("onOpen");
		new Thread(() -> {
			while (session.isOpen()) {
				try {
					Thread.sleep(2000);
					send("pong");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public void onMessage(String message, Session session) {
		Engine.logAdmin.warn("onMessage: " + message);
		try {
			send(message);
		} catch (IOException e) {
			Engine.logAdmin.error("failed to send back message", e);
		}
	}

	@Override
	public void onClose(Session session) {
		Engine.logAdmin.warn("onClose");
	}

	@Override
	public void onError(Throwable throwable, Session session) {
		Engine.logAdmin.warn("onError", throwable);
	}

}
