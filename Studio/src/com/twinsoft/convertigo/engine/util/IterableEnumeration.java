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

package com.twinsoft.convertigo.engine.util;

import java.util.*;

// Imported from http://www.javaspecialists.eu/archive/Issue107.html
public class IterableEnumeration<T> implements Iterable<T> {
	private final Enumeration<T> en;

	public IterableEnumeration(Enumeration<T> en) {
		this.en = en;
	}

	// return an adaptor for the Enumeration
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			public boolean hasNext() {
				return en.hasMoreElements();
			}

			public T next() {
				return en.nextElement();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public static <T> Iterable<T> make(Enumeration<T> en) {
		return new IterableEnumeration<T>(en);
	}
}
