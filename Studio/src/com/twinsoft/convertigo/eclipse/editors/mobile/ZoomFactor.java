package com.twinsoft.convertigo.eclipse.editors.mobile;

public enum ZoomFactor {
	z150(2.22390108574154, 1.5),
	z125(1.22390108574154, 1.25),
	z110(0.5227586989, 1.1),
	z100(0, 1),
	z90(-0.5778829312, 0.90),
	z80(-1.223901086, 0.80),
	z75(-1.577882931, 0.75),
	z66(-2.223901086, 0.66),
	z50(-3.801784017, 0.5),
	z33(-6.025685103, 1f/3),
	z25(-7.603568034, 0.25);
	
	private double jx;
	private double swt;
	
	ZoomFactor(double jx, double swt) {
		this.jx = jx;
		this.swt = swt;
	}
	
	double zoomLevel() {
		return jx;
	}
	
	int swt(int viewport, double dpiFactor) {
		return (int) Math.round(viewport * swt * dpiFactor);
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
		return ZoomFactor.valueOf("z" + percent);
	}
}
