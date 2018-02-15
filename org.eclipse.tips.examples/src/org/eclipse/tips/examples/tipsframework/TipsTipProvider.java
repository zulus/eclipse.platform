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
package org.eclipse.tips.examples.tipsframework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.core.internal.LogUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class TipsTipProvider extends org.eclipse.tips.core.TipProvider {

	private TipImage fImage64, fImage48;

	@Override
	public TipImage getImage() {
		if (fImage48 == null) {
			Bundle bundle = FrameworkUtil.getBundle(getClass());
			try {
				fImage48 = new TipImage(bundle.getEntry("icons/48/tips.png")).setAspectRatio(1);
			} catch (IOException e) {
				getManager().log(LogUtil.info(getClass(), e));
			}
		}
		return fImage48;
	}

	@Override
	public synchronized IStatus loadNewTips(IProgressMonitor pMonitor) {
		SubMonitor subMonitor = SubMonitor.convert(pMonitor);
		subMonitor.beginTask("Loading Tips", -1);
		List<Tip> tips = new ArrayList<>();
		tips.add(new WelcomeTip(getID()));
		tips.add(new StartingTip(getID()));
		tips.add(new Navigate1Tip(getID()));
		tips.add(new Navigate2Tip(getID()));
		tips.add(new GithubTip(getID()));
		tips.add(new MatrixRainTip(getID()));
		setTips(tips);
		subMonitor.done();
		return Status.OK_STATUS;
	}

	@Override
	public String getDescription() {
		return "Tips about Tips";
	}

	@Override
	public String getID() {
		return getClass().getName();
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}
}