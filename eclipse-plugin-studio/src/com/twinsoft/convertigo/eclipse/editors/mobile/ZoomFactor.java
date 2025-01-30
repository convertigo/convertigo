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

package com.twinsoft.convertigo.eclipse.editors.mobile;

import com.teamdev.jxbrowser.zoom.ZoomLevel;

enum ZoomFactor {
	z150(ZoomLevel.P_150, 1.5),
	z125(ZoomLevel.P_125, 1.25),
	z110(ZoomLevel.P_110, 1.1),
	z100(ZoomLevel.P_100, 1),
	z90(ZoomLevel.P_90, 0.90),
	z80(ZoomLevel.P_80, 0.80),
	z75(ZoomLevel.P_75, 0.75),
	z67(ZoomLevel.P_67, 0.67),
	z50(ZoomLevel.P_50, 0.5),
	z33(ZoomLevel.P_33, 1f/3),
	z25(ZoomLevel.P_25, 0.25);
	
	private ZoomLevel jx;
	private double swt;
	
	ZoomFactor(ZoomLevel jx, double swt) {
		this.jx = jx;
		this.swt = swt;
	}
	
	ZoomLevel zoomLevel() {
		return jx;
	}
	
	int swt(int viewport) {
		return (int) Math.round(viewport * swt);
	}
	
	ZoomFactor in() {
		return ZoomFactor.values()[Math.max(0, this.ordinal() - 1)];
	}
	
	ZoomFactor out() {
		return ZoomFactor.values()[Math.min(ZoomFactor.values().length - 1, this.ordinal() + 1)];
	}

	int percent() {
		int percent = 100;
		try {
			percent = Integer.parseInt(name().substring(1));
		} catch (Exception e) {
			// TODO: handle exception
		}
		return percent;
	}
	
	static ZoomFactor get(int percent) {
		try {
			return ZoomFactor.valueOf("z" + percent);
		} catch (Exception e) {
			return ZoomFactor.z100;
		}
	}
}
