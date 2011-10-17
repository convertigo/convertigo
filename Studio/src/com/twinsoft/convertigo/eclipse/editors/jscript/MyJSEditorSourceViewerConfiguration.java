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

package com.twinsoft.convertigo.eclipse.editors.jscript;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

/**
 * This class overrides the JSSourceViewerConfiguration only to be able to specify a custom
 * Completion PRocessor
 * 
 * @author opic
 *
 */
public class MyJSEditorSourceViewerConfiguration extends TextSourceViewerConfiguration {

	private IContentAssistant fContentAssistant = null;

	/**
	 * 
	 */
	public MyJSEditorSourceViewerConfiguration() {
		super();
	}

	public MyJSEditorSourceViewerConfiguration(IPreferenceStore store) {
		super(store);
	}

	/**
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentAssistant(ISourceViewer)
	 */
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		if (fContentAssistant == null) {
			// Ensure that only one assistant is ever returned.  Creating a second assistant
			// that is added to a viewer can cause odd key-eating by the wrong one.
			ContentAssistant contentAssistant = new ContentAssistant();
			contentAssistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
			IContentAssistProcessor contentAssistantProcessor = new MyJScriptCompletionProcessor();
			contentAssistant.setContentAssistProcessor(contentAssistantProcessor, IDocument.DEFAULT_CONTENT_TYPE);
			contentAssistant.enableAutoActivation(true);
			contentAssistant.setAutoActivationDelay(500);
			contentAssistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
			contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
			contentAssistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
			fContentAssistant = contentAssistant;
		}
		return fContentAssistant;
	}
	
}
