/**********************************************************************
 * Copyright (c) 2004 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Common Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.internal.jobs;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobStatus;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Standard implementation of the IJobStatus interface.
 */
public class JobStatus extends Status implements IJobStatus {
	private Job job;
	/**
	 * Creates a new job status.
	 * @param severity
	 * @param code
	 * @param job
	 * @param message
	 * @param exception
	 */
	public JobStatus(int severity, int code, Job job, String message, Throwable exception) {
		super(severity, Platform.PI_RUNTIME, code, message, exception);
		this.job = job;
	}
	/**
	 * Creates a new job status with no interesting error code or exception.
	 * @param severity
	 * @param job
	 * @param message
	 */
	public JobStatus(int severity, Job job, String message) {
		super(severity, Platform.PI_RUNTIME, 1, message, null);
		this.job = job;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobStatus#getJob()
	 */
	public Job getJob() {
		return job;
	}
}