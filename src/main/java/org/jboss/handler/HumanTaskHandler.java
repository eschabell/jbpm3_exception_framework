package org.jboss.handler;

import org.apache.log4j.Logger;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Handler for preparing before and following up after a manual human task.
 */
public class HumanTaskHandler extends AbstractExceptionActionHandler {
	/** Serial version UID. */
	private static final long serialVersionUID = 1L;

	/** Logging facility. */
	private static final Logger LOGGER = Logger.getLogger(HumanTaskHandler.class);

	// Transition names.
	private static final String RETRY_TRANSITION_NAME = "Retry";

	public void doPerformAction(ExecutionContext executionContext) throws Exception {
		if (Event.EVENTTYPE_NODE_LEAVE.equals(executionContext.getEvent().getEventType()) && RETRY_TRANSITION_NAME.equals(executionContext.getTransition().getName())) {
			getLogger().debug("Have EVENTTYPE_NODE_LEAVE so updating retry transistions before going further!");
			getLogger().debug("Picked up event type " + executionContext.getEvent().getEventType() + " and transition name is " + executionContext.getTransition().getName());
			updateRetryTransition(executionContext);
		}
	}

	/**
	 * Point the retry transition to the Node from which the Exception
	 * originated.
	 * 
	 * @param executionContext The current jBPM context.
	 */
	private void updateRetryTransition(ExecutionContext executionContext) {
		String name = (String) executionContext.getVariable(EXCEPTION_ORIGINATING_NODE_NAME_KEY);
		Node originatingNode = executionContext.getProcessDefinition().getNode(name);
		getLogger().debug("updateRetryTranisition EXCEPTION_ORIGINATING_NODE_NAME_KEY is: " + name);
		if (originatingNode == null) {
			String msg = "No originating node available for retry after exception handling.";
			LOGGER.error(msg);
			throw new IllegalStateException(msg);
		}

		executionContext.getNode().getLeavingTransition(RETRY_TRANSITION_NAME).setTo(originatingNode);
	}

	protected Logger getLogger() {
		return LOGGER;
	}
}
