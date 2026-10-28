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

package com.twinsoft.convertigo.engine.sessions;

import com.twinsoft.convertigo.engine.Context;

public interface ContextStore {
    Context read(String contextId);

    /**
     * The last access time of the stored state of a context, or null if the context is not stored: a request compares it
     * with the context it holds to know whether another request changed the context since.
     */
    default Long readLastAccessTime(String contextId) {
        var stored = read(contextId);
        return stored == null ? null : stored.lastAccessTime;
    }

    void save(Context context, int ttlSeconds);
    void delete(String contextId);
    void deleteBySessionPrefix(String sessionIdPrefix);
    void shutdown();
}
