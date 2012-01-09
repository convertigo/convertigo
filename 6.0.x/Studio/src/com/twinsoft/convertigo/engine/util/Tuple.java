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

import java.util.ArrayList;
import java.util.List;


public class Tuple {
	static public class T2<t1, t2> {
		List<Object> objects = new ArrayList<Object>();
		
		public T2(t1 v1, t2 v2){
			objects.add(v1);
			objects.add(v2);
		}
		
		@SuppressWarnings("unchecked")
		public t1 v1(){
			return (t1) objects.get(0);
		}
		
		@SuppressWarnings("unchecked")
		public t2 v2(){
			return (t2) objects.get(1);
		}
	}
	
	static public class T3<t1, t2, t3> extends T2<t1, t2> {
		public T3(t1 v1, t2 v2, t3 v3) {
			super(v1, v2);
			objects.add(v3);
		}
		
		@SuppressWarnings("unchecked")
		public t3 v3(){
			return (t3) objects.get(2);
		}
	}
}
