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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

/**
 * A tree object listener that only reacts to property changes: the added and removed tree objects are not
 * dispatched to it. A pasted page fires one added event per new object, sent to every listener of the loaded
 * projects: NGX components are most of them and have nothing to do with these events.
 */
public interface TreeObjectPropertyListener extends TreeObjectListener {

}
