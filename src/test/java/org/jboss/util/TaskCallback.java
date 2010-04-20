package org.jboss.util;

import org.jbpm.graph.exe.ExecutionContext;

/** Interface for implementation of Task performing during tests. */
public interface TaskCallback {
	/**
	 * Called to allow for performing work to mimic a manual task.
	 * 
	 * @param executionContext
	 *            The jBPM execution context for the transaction in which the
	 *            work can be done.
	 */
	void performTask(ExecutionContext executionContext);
}
