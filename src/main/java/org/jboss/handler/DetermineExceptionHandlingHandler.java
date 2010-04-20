package org.jboss.handler;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;

/**
 * Handler for deciding whether it is applicable to automatically have the
 * Exception throwing node retry its business.
 */
public class DetermineExceptionHandlingHandler implements DecisionHandler {
	/** Serial version UID. */
	private static final long serialVersionUID = 1L;

	/** Logging facility. */
	private static final Logger LOGGER = Logger
			.getLogger(DetermineExceptionHandlingHandler.class);

	private static final String DROPOUT_TRANSITION_NAME = "No";
	private static final String RETRY_TRANSITION_NAME = "Yes";


	protected Logger getLogger() {
		return LOGGER;
	}

	public String decide(ExecutionContext executionContext) throws Exception {
		// TODO Auto-generated method stub
		// check for our path of execution in exception handling.
		String transition = null;
		if (executionContext.getContextInstance().getVariable("take_exception_transtion") != null) {
			getLogger().debug("Determined that context variable with name: " 
					+ executionContext.getContextInstance().getVariable("take_exception_transtion")
					+ " exists!");
			transition = (String) executionContext.getContextInstance().getVariable("take_exception_transtion");
		}
		
		// TODO: Determine from configuration data whether the Exception that is
		// handled here is applicable to be retried automatically.
		
		if ( StringUtils.isNotBlank(transition) && StringUtils.equals(transition, "retry")) { 
			getLogger().debug("Determine Exception Handling Handler choosing transition: " + RETRY_TRANSITION_NAME);
			return RETRY_TRANSITION_NAME; 
		} 
		
		getLogger().debug("Determine Exception Handling Handler choosing transition: " + DROPOUT_TRANSITION_NAME);
		return DROPOUT_TRANSITION_NAME;
	}
}
