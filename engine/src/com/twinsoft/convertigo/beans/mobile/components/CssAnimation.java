/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.beans.mobile.components;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.twinsoft.convertigo.engine.util.EnumUtils;

public enum CssAnimation {
	bounce,
	flash,
	pulse,
	rubberBand,
	shake,
	headShake,
	swing,
	tada,
	wobble,
	jello,
	bounceIn,
	bounceInDown,
	bounceInLeft,
	bounceInRight,
	bounceInUp,
	bounceOut,
	bounceOutDown,
	bounceOutLeft,
	bounceOutRight,
	bounceOutUp,
	fadeIn,
	fadeInDown,
	fadeInDownBig,
	fadeInLeft,
	fadeInLeftBig,
	fadeInRight,
	fadeInRightBig,
	fadeInUp,
	fadeInUpBig,
	fadeOut,
	fadeOutDown,
	fadeOutDownBig,
	fadeOutLeft,
	fadeOutLeftBig,
	fadeOutRight,
	fadeOutRightBig,
	fadeOutUp,
	fadeOutUpBig,
	flipInX,
	flipInY,
	flipOutX,
	flipOutY,
	lightSpeedIn,
	lightSpeedOut,
	rotateIn,
	rotateInDownLeft,
	rotateInDownRight,
	rotateInUpLeft,
	rotateInUpRight,
	rotateOut,
	rotateOutDownLeft,
	rotateOutDownRight,
	rotateOutUpLeft,
	rotateOutUpRight,
	hinge,
	jackInTheBox,
	rollIn,
	rollOut,
	zoomIn,
	zoomInDown,
	zoomInLeft,
	zoomInRight,
	zoomInUp,
	zoomOut,
	zoomOutDown,
	zoomOutLeft,
	zoomOutRight,
	zoomOutUp,
	slideInDown,
	slideInLeft,
	slideInRight,
	slideInUp,
	slideOutDown,
	slideOutLeft,
	slideOutRight,
	slideOutUp,
	delay_2s("delay-2s"),
	delay_3s("delay-3s"),
	delay_4s("delay-4s"),
	delay_5s("delay-5s"),
	slow,
	slower,
	fast,
	faster
	;
	
	CssAnimation() {
		
	}
	
	String name;
	CssAnimation(String name) {
		this.name = name;
	}
	
	
	@Override
	public String toString() {
		return this.name != null ? this.name : this.name();
	}


	public static String[] names() {
		List<String> names = Arrays.asList(EnumUtils.toStrings(CssAnimation.class));
		Collections.sort(names, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return s1.compareTo(s2);
			}				
		} );
		return names.toArray(new String[names.size()]);
	}
}
