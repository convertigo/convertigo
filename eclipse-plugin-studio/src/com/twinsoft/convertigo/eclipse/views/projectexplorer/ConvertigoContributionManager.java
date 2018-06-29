package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManagerOverrides;

class ConvertigoContributionManager implements IContributionManagerOverrides {

	@Override
	public Boolean getEnabled(IContributionItem item) {
		return item.isEnabled();
	}

	@Override
	public Integer getAccelerator(IContributionItem item) {
		if (item instanceof ActionContributionItem) {
			System.out.println("getAccelerator " + item + " = " + ((ActionContributionItem) item).getAction().getAccelerator());
			return ((ActionContributionItem) item).getAction().getAccelerator();
		}
		System.out.println("getAccelerator " + item);
		return 0;
	}

	@Override
	public String getAcceleratorText(IContributionItem item) {
		if (item instanceof ActionContributionItem) {
			System.out.println("getAcceleratorText " + item + " = " + ((ActionContributionItem) item).getAction().getText());
			return ((ActionContributionItem) item).getAction().getText();
		}
		System.out.println("getAcceleratorText " + item);
		return null;
	}

	@Override
	public String getText(IContributionItem item) {
		if (item instanceof ActionContributionItem) {
			return ((ActionContributionItem) item).getAction().getText();
		}
		return item.getId();
	}

	@Override
	public Boolean getVisible(IContributionItem item) {
		if (!item.isVisible()) {
			return false;
		}
		String id = item.getId();
		String cn = item.getClass().getSimpleName();
		if (cn.equals("DynamicMenuContributionItem") || cn.equals("HandledContributionItem")) {
			return id.startsWith("org.eclipse.egit.ui");
		}
		if (cn.equals("MenuManager")) {
			return id.startsWith("egit.") || id.startsWith("team.");
		}
		if (cn.equals("PluginActionContributionItem")) {
			return id.startsWith("convertigo.") || id.contains("org.eclipse.team");
		}
		return true;
	}

}
