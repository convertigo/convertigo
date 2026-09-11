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

package com.twinsoft.convertigo.engine.enums;

/**
 * Controls how the request-supplied {@code __uri} parameter may change the
 * target of an HTTP transaction.
 *
 * <ul>
 * <li>{@link #deny}: the {@code __uri} parameter is ignored.</li>
 * <li>{@link #relative}: the value is only used as a sub path, appended to the
 * connector origin; a value that looks like a full URL never replaces the
 * connector scheme, host and port.</li>
 * <li>{@link #absolute}: the value may replace the whole destination URL
 * (legacy behavior).</li>
 * </ul>
 */
public enum HttpUriOverridePolicy {
	deny,
	relative,
	absolute;
}
