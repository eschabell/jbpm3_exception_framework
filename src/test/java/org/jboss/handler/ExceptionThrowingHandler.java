package org.jboss.handler;

import java.util.Date;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.instantiation.Delegation;
import org.jbpm.job.ExecuteActionJob;
import org.jbpm.msg.MessageService;
import org.jbpm.svc.ServiceFactory;
import org.jbpm.svc.Services;

public class ExceptionThrowingHandler implements ActionHandler {

	private static final long serialVersionUID = 1L;
	
	/** Context variable keys for storing exception information. */
	public static final String EXCEPTION_ORIGINATING_NODE_NAME_KEY = "EXCEPTION_ORIGINATING_NODE_NAME";
	public static final String EXCEPTION_ORIGINATING_PROCESS_NAME_KEY = "EXCEPTION_ORIGINATING_PROCESS_NAME";
	public static final String EXCEPTION_ORIGINATING_NODE_TRANSITION_LIST_KEY = "EXCEPTION_ORIGINATING_NODE_TRANSITION_LIST";
	public static final String EXCEPTION_ORIGINATING_PROCESS_ID_KEY = "EXCEPTION_ORIGINATING_PROCESS_ID";

	private boolean leaveNode = true;
	
	private String transitionName;
	
	private static String retryTransitionName = "Retry";
	
	/** Set to false to use custom exception framework. */
	private boolean useJbpmExceptionHandler = false;
	
	/** Conditional error throwing variable. **/
	private static final String EXCEPTION_THROWN = "EXCEPTION_THROWN"; 
	
	/** Logging facility. */
	private static final Logger LOGGER = Logger.getLogger(ExceptionThrowingHandler.class);
	

	/**
	 *  Our execution method where the work is done!
	 *  
	 *  @param executionContext Current execution context.
	 */
	public void execute(ExecutionContext executionContext) throws Exception {
		
		 if (getLogger().isDebugEnabled()) {
	            getLogger().debug("Starting jBPM custom exception framework.");
	        }
		
		ExecutionContext.pushCurrentContext(executionContext);
        
		try {
            try {
                // Perform the action.
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Performing action for process instance with id: " + executionContext.getProcessInstance().getId());
                }
                doPerformAction(executionContext);

                if (leaveNode) {
                    // Leave the node through the given transition.
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("Leaving node over " 
                        		+ (StringUtils.isBlank(transitionName) ? "default transition." : "transition: "
                                + transitionName));
                    }

                    // When leaving the node we can either have set a transition to take while in the doPerformAction via the
                    // setTransition method or leave it to the default transition, here we decide what has happened.
                    if (StringUtils.isBlank(transitionName)) {
                        executionContext.getNode().leave(executionContext);
                    } else {
                        executionContext.getNode().leave(executionContext, transitionName);
                    }
                }
            } catch (Exception ex) {
                if (useJbpmExceptionHandler) {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug(
                                "Using jBPMExceptionHandler for exception caught in process instance with id: "
                                        + executionContext.getProcessInstance().getId());
                    }
                    executionContext.getNode().raiseException(ex, executionContext);
                } else {
                    if (getLogger().isDebugEnabled()) {
                        getLogger()
                                .debug(
                                        "Using handleException (custom) for exception caught in process instance with id: "
                                                + executionContext.getProcessInstance().getId()
                                                + " and pushing exception to context with setException.");
                    }
                    executionContext.setException(ex);
                    
                    handleException(executionContext);           
                    if (getLogger().isDebugEnabled()) {
                    	getLogger().debug("Exception passed to framework, now we wait.");
                    }
                }
            }
        } catch (Exception ex) {
        	 if (getLogger().isInfoEnabled()) {
                 getLogger().info(("Exception caught during jBPM custom framework execution"), ex);
             }
        } finally {
            ExecutionContext.popCurrentContext(executionContext);
        }
	}
	
	
    /**
     * This method is used to handle exceptions.
     * 
     * @param executionContext Current execution context.
     */
	private void handleException(ExecutionContext executionContext) {
        getLogger().info("Start exception handling for custom jBPM exception framework.");

        // Save the current node in the context to make a retry possible.
        executionContext.setVariable(EXCEPTION_ORIGINATING_NODE_NAME_KEY, executionContext.getNode().getName());
        executionContext.setVariable(EXCEPTION_ORIGINATING_PROCESS_NAME_KEY, executionContext.getProcessDefinition().getName());
        executionContext.setVariable(EXCEPTION_ORIGINATING_PROCESS_ID_KEY, executionContext.getProcessInstance().getId());
        
        // Also save the transitions that we will want to provide a choice of (all outgoing transitions
        // plus a retry transition that loops back to the same node. First we add the loopback transition
        // to the existing list (note, adding to back of list, do not want it to become the default!).
        Transition retryTransition = new Transition();
        retryTransition.setName(retryTransitionName);
        retryTransition.setTo(executionContext.getNode());
        executionContext.getNode().addLeavingTransition(retryTransition);
        executionContext.setVariable(EXCEPTION_ORIGINATING_NODE_TRANSITION_LIST_KEY, executionContext.getNode().getLeavingTransitions());
        
        if (getLogger().isDebugEnabled()) {
        	getLogger().debug("The EXCEPTION_ORIGINATING_NODE_NAME_KEY is set to: " + executionContext.getNode().getName());
        	getLogger().debug("The EXCEPTION_ORIGINATING_PROCESS_NAME_KEY is set to: " + executionContext.getProcessDefinition().getName());
        	getLogger().debug("The EXCEPTION_ORIGINATING_PROCESS_ID_KEY is set to: " + executionContext.getProcessInstance().getId());
        	getLogger().debug("The following transitions have been pushed into the context: ");
        	int i = 0;
        	for (Iterator iterator = executionContext.getNode().getLeavingTransitions().iterator(); iterator.hasNext();) {
				Transition transition = (Transition) iterator.next();
				getLogger().debug("Transition " + i + " called " + transition.getName());
				i++;
			}
        } 
        
        // Pass the context off to the exception framework process.
        ServiceFactory serviceFactory = executionContext.getJbpmContext().getServiceFactory(Services.SERVICENAME_MESSAGE);
        if (serviceFactory != null) {
            MessageService messageService = (MessageService) serviceFactory.openService();
            Action action = new Action(new Delegation(StartExceptionFrameworkHandler.class.getName()));
            ExecuteActionJob job = new ExecuteActionJob(executionContext.getToken());
            job.setAction(action);
            job.setDueDate(new Date());
            
            // have to explicitly save the action context.
            executionContext.getJbpmContext().getSession().saveOrUpdate(action);
            messageService.send(job);
            
            if (getLogger().isDebugEnabled()) {
            	getLogger().debug("Scheduled job with jobExecuter and waiting for it to run...");
            }            
        }

        getLogger().info("Ended exception handling for jBPM custom exception framework.");
    }

	/**
	 * The action for this handler with the sole purpose of throwing an exception.
	 * 
	 * @param executionContext Current execution context.
	 * @throws Exception 
	 */
	public void doPerformAction(ExecutionContext executionContext) throws Exception {

		// ensuring that the exception is thrown only once.
		if (StringUtils.isEmpty((String) executionContext.getVariable(EXCEPTION_THROWN))) {
			// mark the exception as being thrown once and only once!
			if (getLogger().isDebugEnabled()) {
				LOGGER.debug("ExceptionThrowningHandler ==> throwing in the kitchen sink now...");
			}
				
			executionContext.setVariable(EXCEPTION_THROWN, "kitchen_sink");
			throw new IllegalStateException("Creating a problem.");
		}

		// reset the error variable.
		executionContext.setVariable(EXCEPTION_THROWN, null);
	}
	
	/** The logger. */
	protected Logger getLogger() {
		return LOGGER;
		
	}

}
