package org.jboss.handler;

import java.util.List;

import org.apache.log4j.Logger;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

public class StartExceptionFrameworkHandler implements ActionHandler {

	private static final long serialVersionUID = 1L;
	
	/** Context variable keys for storing exception information. */
	public static final String EXCEPTION_ORIGINATING_NODE_NAME_KEY = "EXCEPTION_ORIGINATING_NODE_NAME";
	public static final String EXCEPTION_ORIGINATING_PROCESS_NAME_KEY = "EXCEPTION_ORIGINATING_PROCESS_NAME";
	public static final String EXCEPTION_ORIGINATING_NODE_TRANSITION_LIST_KEY = "EXCEPTION_ORIGINATING_NODE_TRANSITION_LIST";
	public static final String EXCEPTION_ORIGINATING_PROCESS_ID_KEY = "EXCEPTION_ORIGINATING_PROCESS_ID";
	public static final String EXCEPTION_FRAMEWORK_PROCESS_ID_KEY = "EXCEPTION_FRAMEWORK_PROCESS_ID";

	/** Exception framework process name. */
	public static final String EXCEPTION_FRAMEWORK_PROCESS_NAME_KEY = "Exception Framework";
	
	/** Logging facility. */
	private static final Logger LOGGER = Logger.getLogger(StartExceptionFrameworkHandler.class);

	/** Holds our process instance. */
	private ProcessInstance processInstance;

	/**
	 * Handler starts the exception framework.
	 */
	public final void execute(ExecutionContext executionContext) throws Exception {

    	getLogger().info("Start StartExceptionFrameworkHanlder.");

        try {
        	// get new instance of exception framework and start processing!
        	List<ProcessDefinition> listProcs = executionContext.getJbpmContext().getGraphSession().findAllProcessDefinitions();

        	if (getLogger().isDebugEnabled()) {
	        	for (ProcessDefinition processDefinition : listProcs) {
					getLogger().debug("Process definition is: " + processDefinition + " with version: " + processDefinition.getVersion() + ".");
				}
        	}
        	processInstance = executionContext.getJbpmContext().newProcessInstance(EXCEPTION_FRAMEWORK_PROCESS_NAME_KEY);
        	if (getLogger().isDebugEnabled()) {
        		if (processInstance == null) {
        			getLogger().debug("Process instance of exception framework was not created.");
        		}
        		getLogger().debug("Passing the process instance of exception framework to be signaled.");
        	}
        	
        	// push the exception framework process id into the context.
        	executionContext.setVariable(EXCEPTION_FRAMEWORK_PROCESS_ID_KEY, processInstance.getRootToken().getId());
        	
        	// push originating stuff.
        	processInstance.getContextInstance().setVariable(EXCEPTION_ORIGINATING_NODE_NAME_KEY, executionContext.getVariable(EXCEPTION_ORIGINATING_NODE_NAME_KEY));
        	processInstance.getContextInstance().setVariable(EXCEPTION_ORIGINATING_PROCESS_NAME_KEY, executionContext.getVariable(EXCEPTION_ORIGINATING_PROCESS_NAME_KEY));
        	processInstance.getContextInstance().setVariable(EXCEPTION_ORIGINATING_PROCESS_ID_KEY, executionContext.getVariable(EXCEPTION_ORIGINATING_PROCESS_ID_KEY));
        	processInstance.getContextInstance().setVariable(EXCEPTION_ORIGINATING_NODE_TRANSITION_LIST_KEY, executionContext.getVariable(EXCEPTION_ORIGINATING_NODE_TRANSITION_LIST_KEY));

        	if (getLogger().isDebugEnabled()) {
        		getLogger().debug("Set variable " + EXCEPTION_ORIGINATING_NODE_NAME_KEY + " for process instance with value :" + executionContext.getVariable(EXCEPTION_ORIGINATING_NODE_NAME_KEY));
        		getLogger().debug("Set variable " + EXCEPTION_ORIGINATING_PROCESS_NAME_KEY + " for process instance with value :" + executionContext.getVariable(EXCEPTION_ORIGINATING_PROCESS_NAME_KEY));
        		getLogger().debug("Set variable " + EXCEPTION_ORIGINATING_PROCESS_ID_KEY + " for process instance with value :" + executionContext.getVariable(EXCEPTION_ORIGINATING_PROCESS_ID_KEY));
        		getLogger().debug("Set variable " + EXCEPTION_ORIGINATING_NODE_TRANSITION_LIST_KEY + " for process instance with value :" + executionContext.getVariable(EXCEPTION_ORIGINATING_NODE_TRANSITION_LIST_KEY));
        	}

        	
        	processInstance.signal();			
	    } catch (Exception ex) {
	       	 if (getLogger().isInfoEnabled()) {
	             getLogger().info(("Exception caught during StartExceptionFrameworkHandler."), ex);
	         }
	    }

	    getLogger().info("Ended StartExceptionFrameworkHandler.");
    }
	
	protected String getCurrentAction() {
	       return "starting exception framework process";
	}

	protected Logger getLogger() {
		return LOGGER;
	}

}
