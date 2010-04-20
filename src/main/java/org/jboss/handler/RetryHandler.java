package org.jboss.handler;

import org.apache.log4j.Logger;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Handler for retrying a node that threw an Exception before, after the waiting
 * period configured for the specific situation.
 */
public class RetryHandler extends AbstractExceptionActionHandler {
	/** Serial version UID. */
	private static final long serialVersionUID = 1L;

	/** Logging facility. */
	private static final Logger LOGGER = Logger.getLogger(RetryHandler.class);

	public void doPerformAction(ExecutionContext executionContext) throws Exception {
		// TODO: Determine the waiting period from the configuration.

		// TODO: Wait the applicable period.
		
		// Check whether a leaving transition is available.
		Transition transition = executionContext.getNode().getDefaultLeavingTransition();
		getLogger().debug("Default leaving transition from RetryHandler is: " + transition);
		if (transition == null) {
			// Create a transition.
			transition = new Transition();
			executionContext.getNode().addLeavingTransition(transition);
			getLogger().debug("Leaving transition from RetryHandler has been set to: " + transition);
		}
		// Set the transition to the originating node.
		Node originatingNode = executionContext.getProcessDefinition().getNode(EXCEPTION_ORIGINATING_NODE_NAME_KEY);
		if (originatingNode == null) {
			String msg = "No originating node available for retry after exception handling.";
			LOGGER.error(msg);
			throw new IllegalStateException(msg);
		}
		getLogger().debug("Transition from RetryHandler is being set to: " + originatingNode);
		transition.setTo(originatingNode);		
	}

	protected Logger getLogger() {
		return LOGGER;
	}
}
