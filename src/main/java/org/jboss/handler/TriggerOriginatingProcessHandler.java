package org.jboss.handler;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

public class TriggerOriginatingProcessHandler implements ActionHandler {

	private static final long serialVersionUID = 1L;
	
	/** Context variable keys for storing exception information. */
	public static final String EXCEPTION_ORIGINATING_PROCESS_ID_KEY = "EXCEPTION_ORIGINATING_PROCESS_ID";
	public static final String EXCEPTION_ORIGINATING_NODE_LEAVE_TRANSITION_KEY = "EXCEPTION_ORIGINATING_NODE_LEAVE_TRANSITION";
	
	private String transitionName;
	
	private ProcessInstance originatorProcInst;

	/** Logging facility. */
	private static final Logger LOGGER = Logger.getLogger(TriggerOriginatingProcessHandler.class);
	
	public final void execute(ExecutionContext executionContext) throws Exception {
	    getLogger().info("Start TriggerOriginatingProcessHandler.");

        try {
        	JbpmConfiguration jbpmConf = JbpmConfiguration.getInstance();
    		JbpmContext jbpmCtx = jbpmConf.createJbpmContext();
    		 try {   			 
    			 // Get our process instance back.
    			 originatorProcInst = jbpmCtx.getProcessInstance((Long) executionContext.getVariable(EXCEPTION_ORIGINATING_PROCESS_ID_KEY));

	   	       	if (getLogger().isDebugEnabled()) {
		        	if (originatorProcInst == null) {
		        		getLogger().debug("Process instance of exception framework was not created.");
		        	} 
		        } 	

	    		// Determine from the context variable (see ContextConstants) which transition to take 
	    	    // when we go back to the originating node. It should always be set to something, but we
	    	    // will take the default if it is null and log it as an error.
	    		transitionName = (String) executionContext.getVariable(EXCEPTION_ORIGINATING_NODE_LEAVE_TRANSITION_KEY);
	    		
	    		if (StringUtils.isBlank(transitionName)) {
	    			// Leave the node through the default transition, nothing to do just log this as an error as
	    			// should never have to happen.
                    if (getLogger().isDebugEnabled()) {
                    	getLogger().debug("Leaving transition not set in the context, leaving over the default transition.");
                    }
	    		} else {
	    			// Leave the node through the given transition we need to get out of the context variable if it exists, 
	    			// so checking it.
	    			if (originatorProcInst.getRootToken().getNode().hasLeavingTransition(transitionName)) {
	                    if (getLogger().isDebugEnabled()) {
	                        getLogger().debug("Leaving node over the given transition " + transitionName);
	                    }	                    
	    			} else {
	    				// given non-existing transition, take default and log error.
		    			getLogger().error("Leaving transition not set in the context, leaving over the default transition.");
		    			transitionName = null;
	    			}
                }
	    		
	    		if (StringUtils.isBlank(transitionName)) {
	    			originatorProcInst.getRootToken().signal();
	    		} else {
	    			originatorProcInst.getRootToken().signal(transitionName);
	    		}
	    		executionContext.leaveNode();
    		 } finally {
    			 jbpmCtx.close();
    		 }			
 	    } catch (Exception ex) {
	       	 if (getLogger().isInfoEnabled()) {
	             getLogger().info(("Exception caught during TriggerOriginatingProcessHandler."), ex);
	         }
	    }
	    getLogger().info("Ended TriggerOriginatingProcessHandler.");
    }
	
 
	protected String getCurrentAction() {
	       return "signaling originating process";
	}

	protected Logger getLogger() {
		return LOGGER;
	}
}
