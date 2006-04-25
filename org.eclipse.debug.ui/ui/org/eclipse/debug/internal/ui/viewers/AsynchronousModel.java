/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.elements.adapters.AsynchronousDebugLabelAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.IAsynchronousContentAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.IAsynchronousLabelAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.IAsynchronousRequestMonitor;
import org.eclipse.debug.internal.ui.viewers.provisional.IChildrenRequestMonitor;
import org.eclipse.debug.internal.ui.viewers.provisional.ILabelRequestMonitor;
import org.eclipse.debug.internal.ui.viewers.provisional.IModelProxy;
import org.eclipse.debug.internal.ui.viewers.provisional.IModelProxyFactoryAdapter;
import org.eclipse.debug.internal.ui.viewers.provisional.IPresentationContext;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * Model for an asynchronous viewer
 * 
 * @since 3.2
 */
public abstract class AsynchronousModel {
	
	private ModelNode fRoot; // root node
	private Map fElementToNodes = new HashMap(); // map of element to corresponding tree nodes (list)
	private Map fModelProxies = new HashMap(); // map of installed model proxies, by element
	private AsynchronousViewer fViewer; // viewer this model works for
    private boolean fDisposed = false; // whether disposed
    
    // debug flags
	public static boolean DEBUG_MODEL = false;
	
	static {
		DEBUG_MODEL = DebugUIPlugin.DEBUG && "true".equals( //$NON-NLS-1$
		 Platform.getDebugOption("org.eclipse.debug.ui/debug/viewers/model")); //$NON-NLS-1$
	}    
	
	/**
	 * List of requests currently being performed.
	 */
	private List fPendingUpdates = new ArrayList();
	
	/**
	 * List of pending viewer udpates
	 */
	private List fViewerUpdates = new ArrayList();

	/**
	 * Constructs a new empty tree model
	 * 
	 * @param viewer associated viewer
	 */
	public AsynchronousModel(AsynchronousViewer viewer) {
		fViewer = viewer;
		if (DEBUG_MODEL) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("MODEL CREATED for: "); //$NON-NLS-1$
			buffer.append(fViewer);
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(this);
			buffer.append(")"); //$NON-NLS-1$
			DebugUIPlugin.debug(buffer.toString());
		}
	}
	
	/**
	 * Initializes this model. Called once after creation.
	 * 
	 * @param root root element or <code>null</code>
	 * @param widget root widget/control
	 */
	public void init(Object root) {
		if (root != null) {
			fRoot = new ModelNode(null, root);
			mapElement(root, fRoot);
		}		
	}
	
	protected AsynchronousViewer getViewer() {
		return fViewer;
	}
	
	/**
	 * Disposes this model
	 */
	public synchronized void dispose() {
		if (DEBUG_MODEL) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("MODEL DISPOSED for: "); //$NON-NLS-1$
			buffer.append(fViewer);
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(this);
			buffer.append(")"); //$NON-NLS-1$
			DebugUIPlugin.debug(buffer.toString());
		}
        fDisposed = true;
        cancelPendingUpdates();
		disposeAllModelProxies();
		ModelNode rootNode = getRootNode();
		if (rootNode != null) {
			rootNode.dispose();
		}
		fElementToNodes.clear();
	}
    
    /**
     * Returns whether this model has been disposed
     */
    public synchronized boolean isDisposed() {
        return fDisposed;
    }
    
    /**
     * Cancels all pending update requests.
     */
    protected synchronized void cancelPendingUpdates() {
        Iterator updates = fPendingUpdates.iterator();
        while (updates.hasNext()) {
            IAsynchronousRequestMonitor update = (IAsynchronousRequestMonitor) updates.next();
            updates.remove();
            update.setCanceled(true);
        }
        fPendingUpdates.clear();
    }    
	
	/**
	 * Installs the model proxy for the given element into this viewer
	 * if not already installed.
	 * 
	 * @param element element to install an update policy for
	 */
	public synchronized void installModelProxy(Object element) {
		if (!fModelProxies.containsKey(element)) {
			IModelProxyFactoryAdapter modelProxyFactory = getModelProxyFactoryAdapter(element);
			if (modelProxyFactory != null) {
				final IModelProxy proxy = modelProxyFactory.createModelProxy(element, getPresentationContext());
				if (proxy != null) {
					fModelProxies.put(element, proxy);
					Job job = new Job("Model Proxy installed notification job") {//$NON-NLS-1$
						protected IStatus run(IProgressMonitor monitor) {
							if (!monitor.isCanceled()) {
								proxy.init(getPresentationContext());
								getViewer().modelProxyAdded(proxy);
								proxy.installed();
							}
							return Status.OK_STATUS;
						} 
					};
					job.setSystem(true);
					job.schedule();
				}
			}
		}
	}	
	
	/**
	 * Uninstalls the model proxy installed for the given element, if any.
	 * 
	 * @param element
	 */
	protected synchronized void disposeModelProxy(Object element) {
		IModelProxy proxy = (IModelProxy) fModelProxies.remove(element);
		if (proxy != null) {
			getViewer().modelProxyRemoved(proxy);
			proxy.dispose();
		}
	}	
	
	/**
	 * Unintalls all model proxies installed for this model
	 */
	private void disposeAllModelProxies() {
	    synchronized(fModelProxies) {
	        Iterator updatePolicies = fModelProxies.values().iterator();
	        while (updatePolicies.hasNext()) {
	            IModelProxy proxy = (IModelProxy)updatePolicies.next();
	            getViewer().modelProxyRemoved(proxy);	            
	            proxy.dispose();
	        }
	        
	        fModelProxies.clear();
	    }
	}	
	
	/**
	 * Returns the presentation this model is installed in
	 * 
	 * @return
	 */
	protected IPresentationContext getPresentationContext() {
		return fViewer.getPresentationContext();
	}
	
	/**
	 * Returns the model proxy factory for the given element of <code>null</code> if none.
	 * 
	 * @param element element to retrieve adapters for
	 * @return model proxy factory adapter or <code>null</code>
	 */
	protected IModelProxyFactoryAdapter getModelProxyFactoryAdapter(Object element) {
		IModelProxyFactoryAdapter adapter = null;
		if (element instanceof IModelProxyFactoryAdapter) {
			adapter = (IModelProxyFactoryAdapter) element;
		} else if (element instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) element;
			adapter = (IModelProxyFactoryAdapter) adaptable.getAdapter(IModelProxyFactoryAdapter.class);
		}
		return adapter;
	}	
	
	/**
	 * Maps the given element to the given node.
	 * 
	 * @param element
	 * @param node
	 */
	protected synchronized void mapElement(Object element, ModelNode node) {
		ModelNode[] nodes = getNodes(element);
        node.remap(element);
		if (nodes == null) {
			fElementToNodes.put(element, new ModelNode[] { node});
		} else {
			for (int i = 0; i < nodes.length; i++) {
				if (nodes[i] == node) {
					return;
				}
			}
			ModelNode[] old = nodes;
			ModelNode[] newNodes = new ModelNode[old.length + 1];
			System.arraycopy(old, 0, newNodes, 0, old.length);
			newNodes[old.length] = node;
			fElementToNodes.put(element, newNodes);
		}
        installModelProxy(element);
	}
    
    /**
     * Unmaps the given node from its element and widget.
     * 
     * @param node
     */
    protected synchronized void unmapNode(ModelNode node) {
        Object element = node.getElement();
        ModelNode[] nodes = (ModelNode[]) fElementToNodes.get(element);
        if (nodes == null) {
            return;
        }
        if (nodes.length == 1) {
            fElementToNodes.remove(element);
            disposeModelProxy(element);
        } else {
            for (int i = 0; i < nodes.length; i++) {
                ModelNode node2 = nodes[i];
                if (node2 == node) {
                    ModelNode[] newNodes= new ModelNode[nodes.length - 1];
                    System.arraycopy(nodes, 0, newNodes, 0, i);
                    if (i < newNodes.length) {
                        System.arraycopy(nodes, i + 1, newNodes, i, newNodes.length - i);
                    }
                    fElementToNodes.put(element, newNodes);
                }
            }
        }
    }
	
	/**
	 * Returns the nodes in this model for the given element or
     * <code>null</code> if none.
	 * 
	 * @param element model element
	 * @return associated nodes or <code>null</code>
	 */
	public synchronized ModelNode[] getNodes(Object element) {
		return (ModelNode[]) fElementToNodes.get(element);
	}
	
	/**
	 * Returns the root node or <code>null</code>
	 * 
	 * @return the root node or <code>null</code>
	 */
	public ModelNode getRootNode() {
		return fRoot;
	}
	
	/**
	 * Cancels any conflicting updates for children of the given item, and
	 * schedules the new update.
	 * 
	 * @param update the update to schedule
	 */
	protected void requestScheduled(IAsynchronousRequestMonitor update) {
		AsynchronousRequestMonitor absUpdate = (AsynchronousRequestMonitor) update;
		synchronized (fPendingUpdates) {
			Iterator updates = fPendingUpdates.listIterator();
			while (updates.hasNext()) {
				AsynchronousRequestMonitor pendingUpdate = (AsynchronousRequestMonitor) updates.next();
				if (absUpdate.contains(pendingUpdate)) {
					updates.remove();
					pendingUpdate.setCanceled(true);
				}
			}
			fPendingUpdates.add(update);
		}
	}	
	
	/**
	 * Removes the update from the pending updates list.
	 * 
	 * @param update
	 */
	protected void requestComplete(IAsynchronousRequestMonitor update) {
		synchronized (fPendingUpdates) {
			fPendingUpdates.remove(update);
		}
	}
	
	/**
	 * An viewer update has been scheduled due to the following update request.
	 * 
	 * @param update
	 */
	protected void viewerUpdateScheduled(IAsynchronousRequestMonitor update) {
		// synch viewer updates and pending updates on same lock - fPendingUpdates
		synchronized (fPendingUpdates) {
			fViewerUpdates.add(update);
		}
	}
	
	/**
	 * Returns the result of running the given elements through the
	 * viewers filters.
	 * 
	 * @param parent parent element
	 * @param elements the elements to filter
	 * @return only the elements which all filters accept
	 */
	protected Object[] filter(Object parent, Object[] elements) {
		ViewerFilter[] filters = getViewer().getFilters();
		if (filters != null) {
			ArrayList filtered = new ArrayList(elements.length);
			for (int i = 0; i < elements.length; i++) {
				boolean add = true;
				for (int j = 0; j < filters.length; j++) {
					add = filters[j].select(getViewer(), parent, elements[i]);
					if (!add)
						break;
				}
				if (add)
					filtered.add(elements[i]);
			}
			return filtered.toArray();
		}
		return elements;
	}
	
	/**
	 * Refreshes the given node.
	 * 
	 * @param node
	 */
	protected void updateLabel(ModelNode node) {
		Object element = node.getElement();
		IAsynchronousLabelAdapter adapter = getLabelAdapter(element);
		if (adapter != null) {
			ILabelRequestMonitor labelUpdate = new LabelRequestMonitor(node, this);
			requestScheduled(labelUpdate);
			adapter.retrieveLabel(element, getPresentationContext(), labelUpdate);
		}		
	}
	
	/**
	 * Returns the label adapter for the given element or <code>null</code> if none.
	 * 
	 * @param element element to retrieve adapter for
	 * @return presentation adapter or <code>null</code>
	 */
	protected IAsynchronousLabelAdapter getLabelAdapter(Object element) {
		IAsynchronousLabelAdapter adapter = null;
		if (element instanceof IAsynchronousLabelAdapter) {
			adapter = (IAsynchronousLabelAdapter) element;
		} else if (element instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) element;
			adapter = (IAsynchronousLabelAdapter) adaptable.getAdapter(IAsynchronousLabelAdapter.class);
		}
		// if no adapter, use default (i.e. model presentation)
		if (adapter == null) {
			return new AsynchronousDebugLabelAdapter();
		}
		return adapter;
	}	
	
    /**
     * Returns the tree element adapter for the given element or
     * <code>null</code> if none.
     * 
     * @param element
     *            element to retrieve adapter for
     * @return presentation adapter or <code>null</code>
     */
    protected IAsynchronousContentAdapter getContentAdapter(Object element) {        
        IAsynchronousContentAdapter adapter = null;
        if (element instanceof IAsynchronousContentAdapter) {
            adapter = (IAsynchronousContentAdapter) element;
        } else if (element instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) element;
            adapter = (IAsynchronousContentAdapter) adaptable.getAdapter(IAsynchronousContentAdapter.class);
        }
        return adapter;
    }	
    
    /**
     * Updates the children of the given node.
     * 
     * @param parent
     *            node of which to update children
     */
    public void updateChildren(ModelNode parent) {
        Object element = parent.getElement();
        IAsynchronousContentAdapter adapter = getContentAdapter(element);
        if (adapter != null) {
            IChildrenRequestMonitor update = new ChildrenRequestMonitor(parent, this);
            requestScheduled(update);
            adapter.retrieveChildren(element, getPresentationContext(), update);
        }
    }    
	
	/**
	 * Update this model's viewer preserving its selection.
	 * 
	 * @param update
	 */
	protected void preservingSelection(Runnable update) {
		getViewer().preservingSelection(update);
	}

	/**
	 * The viewer updated associated with a request is compelte.
	 * 
	 * @param monitor
	 */
	protected void viewerUpdateComplete(IAsynchronousRequestMonitor monitor) {
		// synch viewer updates and pending updates on same lock - fPendingUpdates
		synchronized (fPendingUpdates) {
			fViewerUpdates.remove(monitor);
		}
		getViewer().updateComplete(monitor);
	}
	
	/**
	 * An update request was cancelled
	 * 
	 * @param monitor
	 */
	protected void requestCanceled(AsynchronousRequestMonitor monitor) {
		synchronized (fPendingUpdates) {
			fPendingUpdates.remove(monitor);
		}
	}
	
	/**
	 * Whether any updates are still in progress in the model or against the viewer.
	 * 
	 * @return
	 */
	protected boolean hasPendingUpdates() {
		synchronized (fViewerUpdates) {
			return !fPendingUpdates.isEmpty() || !fViewerUpdates.isEmpty();
		}
	}
	
    /**
     * Asynchronous update for add/set children request.
     * 
     * @param parent
     * @param element
     */
	protected abstract void add(ModelNode parent, Object element);
	
	/**
	 * Notification from children request monitor
	 * 
	 * @param parentNode parent node
	 * @param kids list of model elements
	 */
	protected void setChildren(final ModelNode parentNode, List kids) {
		
        final Object[] children = filter(parentNode.getElement(), kids.toArray());
        final AsynchronousViewer viewer = getViewer();
        ViewerSorter sorter = viewer.getSorter();
        if (sorter != null) {
        	sorter.sort(viewer, children);
        }
        
        ModelNode[] prevKids = null;
        ModelNode[] newChildren = null;
        ModelNode[] unmap = null; 
        
        synchronized (this) {
        	if (isDisposed()) {
                return;
            }
        	prevKids = parentNode.getChildrenNodes();
            if (prevKids == null) {
            	newChildren = new ModelNode[children.length];
            	for (int i = 0; i < children.length; i++) {
    				ModelNode node = new ModelNode(parentNode, children[i]);
    				mapElement(children[i], node);
    				newChildren[i] = node;
    			}
            	parentNode.setChildren(newChildren);
            } else {
            	newChildren = new ModelNode[children.length];
            	unmap = new ModelNode[prevKids.length];
            	for (int i = 0; i < prevKids.length; i++) {
					unmap[i] = prevKids[i];	
				}
            	for (int i = 0; i < children.length; i++) {
					Object child = children[i];
					boolean found = false;
					for (int j = 0; j < prevKids.length; j++) {
						ModelNode prevKid = prevKids[j];
						if (prevKid != null && child.equals(prevKid.getElement())) {
							newChildren[i] = prevKid;
							prevKids[j] = null;
							found = true;
							break;
						}
					}
					if (!found) {
						newChildren[i] = new ModelNode(parentNode, child);
						mapElement(child, newChildren[i]);
					}
				}
            	for (int i = 0; i < prevKids.length; i++) {
            		ModelNode kid = prevKids[i];
            		if (kid != null) {
            			kid.dispose();
            			unmapNode(kid);
            		}
            	}
    	        parentNode.setChildren(newChildren);
            }
            if (DEBUG_MODEL) {
            	DebugUIPlugin.debug("CHILDREN CHANGED: " + parentNode); //$NON-NLS-1$
            	DebugUIPlugin.debug(toString());
            }
        }
        
        //update viewer outside the lock
    	final ModelNode[] finalUnmap = unmap; 
        preservingSelection(new Runnable() {
            public void run() {
            	if (finalUnmap != null) {
	            	for (int i = 0; i < finalUnmap.length; i++) {
						viewer.unmapNode(finalUnmap[i]);
					}
            	}
            	viewer.nodeChildrenChanged(parentNode);
            }
        });        	        

	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		if (fRoot != null) {
			buf.append("ROOT: "); //$NON-NLS-1$
			append(buf, fRoot, 0);
		} else {
			buf.append("ROOT: null"); //$NON-NLS-1$
		}
		return buf.toString();
	}
	
	private void append(StringBuffer buf, ModelNode node, int level) {
		for (int i = 0; i < level; i++) {
			buf.append('\t');
		}
		buf.append(node);
		buf.append('\n');
		ModelNode[] childrenNodes = node.getChildrenNodes();
		if (childrenNodes != null) {
			for (int i = 0; i < childrenNodes.length; i++) {
				append(buf, childrenNodes[i], level + 1);
			}
		}
	}
}
