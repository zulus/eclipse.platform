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
package org.eclipse.tips.manual.tests;

import java.net.MalformedURLException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tips.core.ITipManager;
import org.eclipse.tips.core.JsonTestProvider;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipManager;
import org.eclipse.tips.core.TipProvider;
import org.eclipse.tips.ui.internal.TipDialog;
import org.eclipse.tips.ui.internal.util.ResourceManager;

/**
 * Class to manage the tip providers and start the tip of the day UI.
 */
public class SleakTipManager extends TipManager {

	private static SleakTipManager instance = new SleakTipManager();

	public static void main(String[] args) throws MalformedURLException {
		instance.register(new JsonTestProvider());
		if (!instance.getProviders().isEmpty()) {
			instance.open(true);
		}
	}

	/**
	 * @return the tip manager instance.
	 */
	public static SleakTipManager getInstance() {
		return instance;
	}

	private SleakTipManager() {
	}

	/**
	 * For resource leak detection rename this method to open and run the IDE. Won't
	 * work on Linux because GTK cannot handle multiple displays.
	 */
	@Override
	public TipManager open(boolean pStart) {

		Thread t = new Thread(() -> {

			DeviceData data = new DeviceData();
			data.tracking = true;
			Display display = new Display(data);
			Shell shell = new Shell(display);
			shell.setLayout(new FillLayout());
			new Sleak().open();
			TipDialog tipDialog = new TipDialog(shell, SleakTipManager.this, TipDialog.DEFAULT_STYLE);
			tipDialog.addDisposeListener(pE -> dispose());
			tipDialog.open();
			shell.pack();
			shell.open();
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
			display.dispose();
		});

		t.start();
		return this;

	}

	@Override
	public ITipManager register(TipProvider provider) {
		super.register(provider);
		load(provider);
		return this;
	}

	private void load(TipProvider pProvider) {
		pProvider.loadNewTips(new NullProgressMonitor());
	}

	@Override
	public boolean isRead(Tip pTip) {
		return false;
	}

	@Override
	public TipManager setAsRead(Tip pTip) {
		return this;
	}

	protected synchronized SleakTipManager setNewTips(boolean pNewTips) {
		return this;
	}

	@Override
	public void dispose() {
		ResourceManager.dispose();
		super.dispose();
	}

	@Override
	public TipManager setRunAtStartup(boolean pShouldRun) {
		return this;
	}

	@Override
	public ITipManager log(IStatus pStatus) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPriority(TipProvider pProvider) {
		return 0;
	}
}