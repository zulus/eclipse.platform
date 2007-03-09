/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ua.tests.cheatsheet.util;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;

public class NavigateAction extends Action implements ICheatSheetAction {

	private static void go(String newSheet) {
		final String finalNewSheet = newSheet;
		try {
			Display.getCurrent().asyncExec( new Runnable() {
				public void run() {
					OpenCheatSheetAction csAction = new OpenCheatSheetAction( finalNewSheet, true );
					csAction.run();
				}
			} );
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	public void run( String[] params, ICheatSheetManager manager ) {
		go( params[ 0 ] );
	}
}
