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

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import com.twinsoft.convertigo.engine.events.BaseEvent;
import com.twinsoft.convertigo.engine.events.BaseEventListener;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class EventManager implements AbstractManager{
	private Map<Class<? extends BaseEvent>, Collection<WeakReference<? extends BaseEventListener>>> listeners;
	
	synchronized public void addListener(BaseEventListener listener, Class<? extends BaseEventListener> cl) {
		try {
			Class<? extends BaseEvent> key = GenericUtils.cast(cl.getField("c").get(null));
			Collection<WeakReference<? extends BaseEventListener>> lst = listeners.get(key);
			if (lst == null) {
				listeners.put(key, lst = new HashSet<WeakReference<? extends BaseEventListener>>());
			}
			lst.add(new WeakReference<BaseEventListener>(listener));
		} catch (Exception e) {
			Engine.logEngine.error("Unexpected exception", e);
		}
	}

	synchronized public void dispatchEvent(BaseEvent event) {
		for (Entry<Class<? extends BaseEvent>, Collection<WeakReference<? extends BaseEventListener>>> entry : listeners.entrySet()) {
			if (entry.getKey().isAssignableFrom(event.getClass())) {
				GenericUtils.removeWeak(entry.getValue());
				for (WeakReference<? extends BaseEventListener> wlistener : new ArrayList<WeakReference<? extends BaseEventListener>>(entry.getValue())) {
					try {
						BaseEventListener listener = wlistener.get();
						Method m = listener.getClass().getMethod("onEvent", entry.getKey());
						m.invoke(listener, event);
					} catch (Exception e) {
						Engine.logEngine.error("Unexpected exception", e);
					}
				}
			}
		}
	}

	synchronized public void removeListener(BaseEventListener listener, Class<? extends BaseEventListener> cl) {
		try {
			Class<? extends BaseEvent> key = GenericUtils.cast(cl.getField("c").get(null));
			Collection<WeakReference<? extends BaseEventListener>> lst = listeners.get(key);
			if (lst != null) {
				WeakReference<? extends BaseEventListener> wo = null;
				for (WeakReference<? extends BaseEventListener> iwo : lst) {
					if(listener.equals(iwo.get())) {
						wo = iwo;
						break;
					}
				}
				if(wo != null) {
					lst.remove(wo);
				}
			}
		} catch (Exception e) {
			Engine.logEngine.error("Unexpected exception", e);
		}
	}

	public void destroy() throws EngineException {
		listeners = null;
	}

	public void init() throws EngineException {
		listeners = new HashMap<Class<? extends BaseEvent>, Collection<WeakReference<? extends BaseEventListener>>>();
	}
}
