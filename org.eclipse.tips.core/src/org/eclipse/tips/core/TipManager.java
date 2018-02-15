/****************************************************************************
 * Copyright (c) 2017, 2018 Remain Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Wim Jongman <wim.jongman@remainsoftware.com> - initial API and implementation
 *****************************************************************************/
package org.eclipse.tips.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.tips.core.internal.LogUtil;

/**
 * An abstract implementation of ITipManager with additional control API. While
 * the rest of the framework must work with ITipManager, this class provides API
 * to open the dialog and do low level housekeeping that is of no concern to
 * external participants (Tip and TipProvider).
 *
 */
public abstract class TipManager implements ITipManager {

	private Map<String, TipProvider> fProviders = new HashMap<>();
	private Map<Integer, List<String>> fProviderPrio = new TreeMap<>();
	protected boolean fOpen;
	private boolean fServeReadTips = false;
	private TipProviderListenerManager fListenerManager = new TipProviderListenerManager();
	private boolean fIsDiposed;

	/**
	 * Instantiates a new TipManager.
	 */
	public TipManager() {
	}

	/**
	 * Gets the provider with the specified ID.
	 *
	 * @param providerID
	 *            the id of the provider to fetch
	 * @return the provider with the specified ID or null if no such provider
	 *         exists.
	 * @see TipProvider#getID()
	 */
	public TipProvider getProvider(String providerID) {
		checkDisposed();
		return fProviders.get(providerID);
	}

	/**
	 * Binds the passed provider to this manager. Implementations should override,
	 * call super and the asynchronously call the
	 * {@link TipProvider#loadNewTips(org.eclipse.core.runtime.IProgressMonitor)}
	 * method.
	 *
	 * @param provider
	 *            the {@link TipProvider} to register.
	 *
	 * @return this
	 */
	@Override
	public ITipManager register(TipProvider provider) {
		checkDisposed();
		log(LogUtil.info("Registering provider: " + provider.getID() + " : " + provider.getDescription()));
		provider.setManager(this);
		addToMaps(provider, new Integer(getPriority(provider)));
		provider.getListenerManager().addProviderListener(
				myProvider -> getListenerManager().notifyListeners(TipProviderListener.EVENT_READY, myProvider));
		return this;
	}

	private void checkDisposed() {
		if (isDisposed()) {
			throw new RuntimeException("This TipManager is disposed.");
		}

	}

	/**
	 * Calculates the priority that this provider has in the Tips framework. The
	 * {@link TipProvider#getExpression()} was purposed to aid in the calculation of
	 * the priority.
	 *
	 * @param provider
	 *            the provider
	 * @return the priority, lower is higher, never negative.
	 */
	public abstract int getPriority(TipProvider provider);

	private synchronized void addToMaps(TipProvider pProvider, Integer pPriorityHint) {
		removeFromMaps(pProvider);
		addToProviderMaps(pProvider, pPriorityHint);
		addToPriorityMap(pProvider, pPriorityHint);
	}

	private void addToPriorityMap(TipProvider provider, Integer priorityHint) {
		if (!fProviderPrio.get(priorityHint).contains(provider.getID())) {
			if (!fProviderPrio.get(priorityHint).contains(provider.getID())) {
				fProviderPrio.get(priorityHint).add(provider.getID());
			}
		}
	}

	private void addToProviderMaps(TipProvider provider, Integer priorityHint) {
		fProviders.put(provider.getID(), provider);
		if (fProviderPrio.get(priorityHint) == null) {
			fProviderPrio.put(priorityHint, new ArrayList<>());
		}
	}

	private void removeFromMaps(TipProvider provider) {
		if (fProviders.containsKey(provider.getID())) {
			for (Map.Entry<Integer, List<String>> entry : fProviderPrio.entrySet()) {
				entry.getValue().remove(provider.getID());
			}
			fProviders.remove(provider.getID());
		}
	}

	/**
	 * The returned list contains providers ready to serve tips and is guaranteed to
	 * be in a prioritised order according the implementation of this manager.
	 *
	 * @return the prioritised list of ready providers with tips in an immutable
	 *         list.
	 */
	public List<TipProvider> getProviders() {
		checkDisposed();
		if (fProviders == null) {
			return Collections.emptyList();
		}
		ArrayList<TipProvider> result = new ArrayList<>();
		for (Map.Entry<Integer, List<String>> entry : fProviderPrio.entrySet()) {
			for (String id : entry.getValue()) {
				if (fProviders.get(id).isReady()) {
					result.add(fProviders.get(id));
				}
			}
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * Determines if the Tips framework must run at startup. The default
	 * implementation returns true, subclasses should probably override this.
	 *
	 * @return true if the Tips framework should run at startup.
	 * @see TipManager#setRunAtStartup(boolean)
	 */
	public boolean isRunAtStartup() {
		checkDisposed();
		return true;
	}

	/**
	 * Determines if the Tips framework must run at startup.
	 *
	 * @param shouldRun
	 *            true if the tips should be displayed at startup, false otherwise.
	 *
	 * @return this
	 *
	 * @see #isRunAtStartup()
	 */
	public abstract TipManager setRunAtStartup(boolean shouldRun);

	/**
	 * Opens the Tip of the Day dialog.
	 *
	 * @param startUp
	 *            When called from a startup situation, true must be passed for
	 *            <code>pStartup</code>. If in a manual starting situation, false
	 *            must be passed. This enables the manager to decide to skip opening
	 *            the dialog at startup (e.g., no new tip items).
	 *
	 * @return this
	 *
	 * @see #isOpen()
	 */
	public abstract TipManager open(boolean startUp);

	/**
	 * The default implementation disposes of this manager and all the TipProviders
	 * when the dialog is disposed. Subclasses may override but must call super.
	 */
	public void dispose() {
		checkDisposed();
		try {
			for (TipProvider provider : fProviders.values()) {
				try {
					provider.dispose();
				} catch (Exception e) {
					log(LogUtil.error(e));
				}
			}
		} finally {
			fProviders.clear();
			fProviderPrio.clear();
			fIsDiposed = true;
		}
	}

	/**
	 * @return returns true if the tips are currently being displayed in some way.
	 */
	public boolean isOpen() {
		checkDisposed();
		return fOpen;
	}

	/**
	 * Indicates whether read tips must be served or not. Subclasses could override,
	 * to save the state somewhere, but must call super.
	 *
	 * @param serveRead
	 *            true of read tips may be served by the {@link TipProvider}s
	 * @return this
	 * @see TipManager#mustServeReadTips()
	 */
	public TipManager setServeReadTips(boolean serveRead) {
		checkDisposed();
		fServeReadTips = serveRead;
		return this;
	}

	/**
	 * Indicates whether already read tips must be served or not.
	 *
	 * @return true or false
	 * @see #setServeReadTips(boolean)
	 */
	@Override
	public boolean mustServeReadTips() {
		checkDisposed();
		return fServeReadTips;
	}

	/**
	 * Gets the listener manager so that interested parties can subscribe to the
	 * events of this provider.
	 *
	 * @return the listener manager, never null.
	 */
	public TipProviderListenerManager getListenerManager() {
		return fListenerManager;
	}

	@Override
	public boolean isDisposed() {
		return fIsDiposed;
	}
}