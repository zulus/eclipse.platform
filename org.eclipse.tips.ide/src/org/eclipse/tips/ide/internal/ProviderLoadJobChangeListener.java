/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.ide.internal;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipProvider;

/**
 *
 * Internal class to listen to async provider load completions.
 *
 */
public class ProviderLoadJobChangeListener extends JobChangeAdapter {

	private IDETipManager fManager;
	private TipProvider fProvider;

	public ProviderLoadJobChangeListener(IDETipManager manager, TipProvider provider) {
		fManager = manager;
		fProvider = provider;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If this provider has new tips then the {@link IDETipManager} gets a callback
	 * to update the UI.
	 *
	 * @see IDETipManager#setNewTips(boolean)
	 */
	@Override
	public void done(IJobChangeEvent event) {
		for (Tip tip : fProvider.getTips(false)) {
			if (!fManager.isRead(tip)) {
				fManager.setNewTips(true);
				return;
			}
		}
	}
}