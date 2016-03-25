package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

public class DecoratingColumnLabelProvider extends ColumnLabelProvider {
	
	DecoratingLabelProvider provider;
	
	public DecoratingColumnLabelProvider(ILabelProvider provider, ILabelDecorator decorator) {
		this.provider = new DecoratingLabelProvider(provider, decorator);		
	}

	@Override
	public Font getFont(Object element) {
		return provider.getFont(element);
	}

	@Override
	public Color getBackground(Object element) {
		return provider.getBackground(element);
	}

	@Override
	public Color getForeground(Object element) {
		return provider.getForeground(element);
	}

	@Override
	public Image getImage(Object element) {
		return provider.getImage(element);
	}

	@Override
	public String getText(Object element) {
		return provider.getText(element);
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		super.addListener(listener);
		provider.addListener(listener);
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return provider.isLabelProperty(element, property);
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		super.removeListener(listener);
		provider.removeListener(listener);
	}

}
