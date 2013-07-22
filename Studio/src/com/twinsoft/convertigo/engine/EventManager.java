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

import java.lang.reflect.Method;

import com.twinsoft.convertigo.engine.events.BaseEvent;
import com.twinsoft.convertigo.engine.events.BaseEventListener;
import com.twinsoft.convertigo.engine.util.EventHelper;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class EventManager implements AbstractManager {
	private EventHelper eventHelper;
	
	synchronized public void addListener(BaseEventListener listener, Class<? extends BaseEventListener> cl) {
		eventHelper.addListener(GenericUtils.<Class<BaseEventListener>>cast(cl), listener);
	}

	synchronized public void dispatchEvent(BaseEvent event, Class<? extends BaseEventListener> cl) {
		for (BaseEventListener listener : eventHelper.getListeners(cl)) {
			try {
				Method m = listener.getClass().getMethod("onEvent", event.getClass());
				m.invoke(listener, event);
			} catch (Exception e) {
				Engine.logEngine.error("(EventManager) Unexpected exception", e);
			}
		}
	}

	synchronized public void removeListener(BaseEventListener listener, Class<? extends BaseEventListener> cl) {
		eventHelper.removeListener(GenericUtils.<Class<BaseEventListener>>cast(cl), listener);
	}

	synchronized public void destroy() throws EngineException {
		eventHelper = null;
	}

	synchronized public void init() throws EngineException {
		eventHelper = new EventHelper();
	}
}
