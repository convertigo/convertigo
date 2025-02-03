/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.twinsoft.convertigo.engine.events.BaseEvent;
import com.twinsoft.convertigo.engine.events.BaseEventListener;


public class EventHelper<T extends BaseEventListener<E>, E extends BaseEvent> {    
    private final Map<T, Boolean> listeners = new ConcurrentHashMap<>();

    public void addListener(T listener) {
        listeners.put(listener, true);
    }

    public void removeListener(T listener) {
        listeners.remove(listener);
    }

    public void fireEvent(E event) {
        for (T listener : listeners.keySet()) {
            listener.onEvent(event);
        }
    }
}
