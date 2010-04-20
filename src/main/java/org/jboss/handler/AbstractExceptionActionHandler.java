package org.jboss.handler;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.util.ThreadPoolSingleton;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.State;
import org.jbpm.instantiation.ProcessClassLoader;
import org.jbpm.util.ClassLoaderUtil;

/**
 * Base class for ActionHandler which call services.
 */
public abstract class AbstractExceptionActionHandler implements ActionHandler {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    public static final String EXCEPTION_ORIGINATING_NODE_NAME_KEY = "EXCEPTION_ORIGINATING_NODE_NAME";
    
    private String transitionName;

    /** False means you are using our exception framework handling of exceptions. */
    private boolean useJbpmExceptionHandler = false;

    private boolean signalAfterException = false;

    protected final String getCurrentAction() {
        return "action handling";
    }

	/** Logging facility. */
	private static final Logger LOGGER = Logger.getLogger(AbstractExceptionActionHandler.class);

    
    public final void execute(ExecutionContext executionContext) throws Exception {

    	getLogger().info("Start AbstractExceptionActionHanlder.");

    	ExecutionContext.pushCurrentContext(executionContext);
		try {
			if (executionContext.getNode() instanceof State) {
				// Asynchronous handling.
				if (getLogger().isDebugEnabled()) {
					getLogger().debug("Scheduling async job [" + getClass().getName() + "] for process instance " + executionContext.getProcessInstance().getId());
				}

				// Dig into Hibernate here to accomplish transaction isolation.
				executionContext.getJbpmContext().getSession().getTransaction().registerSynchronization(
						new ServiceRunner(executionContext.getToken().getId(), getClass().getName()));
			} else {
				try {
					// Synchronous handling: Perform the action directly.
					getLogger().debug("Performing action on Node with id: " + executionContext.getProcessInstance().getId());
					doPerformAction(executionContext);
				} catch (Exception ex) {
					getLogger().error("Handler threw exception.", ex);
					getLogger().debug("Using jBPMExceptionHandler for exception caught on Node with id: " + executionContext.getProcessInstance().getId());
					executionContext.getNode().raiseException(ex, executionContext);
				}
			}
		} catch (Exception ex) {
       	 if (getLogger().isInfoEnabled()) {
             getLogger().info(("Exception caught during AbstractExceptionActionHandling."), ex);
         }
		} finally {
			ExecutionContext.popCurrentContext(executionContext);
		}

		getLogger().info("Ended AbstractExceptionActionHanlder.");
    }

    
    /**
     * Implement this method for processing a single node.
     * 
     * @param executionContext An execution context.
     * @throws Exception For all unexpected errors.
     */
    public abstract void doPerformAction(ExecutionContext executionContext) throws Exception;

    /**
     * Set this flag when it is required to have the process signalled 'normally' in case an exception is caught (and handled)
     * within the asynchronous part of the handling.
     * <p>
     * The default behaviour is that no signalling takes place; this puts the responsibility of the progress of the process in the
     * hands of the mechanism handling the exception.
     * 
     * @param signal Whether signalling is to take place after exception handling.
     */
    protected final void setSignalAfterException(final boolean signal) {
        signalAfterException = signal;
    }

    /**
     * Set the transition to be taken for an asynchronous execution.
     * 
     * @param transitionName The name of the transition.
     */
    protected void setTransition(String transitionName) {
        this.transitionName = transitionName;
    }

	/**
	 * Set whether the 'standard' jBPM <exception-handler> construct in the
	 * process definition is to be used for the exception handling for an
	 * asynchronous execution.
	 * <p>
	 * The default behaviour for this class is to handle it implicitly and
	 * redirect the flow towards an exception handling node, which is required
	 * to be present in the definition (and its name in the context).
	 * 
	 * @param useJbpmExceptionHandler
	 *            Indicating whether (or not) to use the jBPM mechanism. By
	 *            default this value is <code>false</code>.
	 */
	protected final void useJbpmExceptionHandler(
			final boolean useJbpmExceptionHandler) {
		this.useJbpmExceptionHandler = useJbpmExceptionHandler;
	}


	/**
	 * Inner class which performs the actual action handling for asynchronous
	 * State nodes.
	 */
	class ServiceRunner implements Runnable, Synchronization {
		private long tokenId;
		private String handlerClassName;

		public ServiceRunner(long tokenId, String handlerClassName) {
			this.tokenId = tokenId;
			this.handlerClassName = handlerClassName;
		}

		/** {@inheritDoc} */
		public void run() {
			JbpmContext jbpmCtx = JbpmConfiguration.getInstance().createJbpmContext();
			try {
				// Get the token in which the process is waiting.
				final Token token = jbpmCtx.getTokenForUpdate(tokenId);
				ExecutionContext executionContext = new ExecutionContext(token);

				// Perform the action in the concrete subclass.
				ExecutionContext.pushCurrentContext(executionContext);
				AbstractExceptionActionHandler currentHandler = null;
				boolean exceptionCaught = false;
				try {
					ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ProcessClassLoader>() {
								public ProcessClassLoader run() {
									return new ProcessClassLoader(ClassLoaderUtil.class.getClassLoader(), token.getProcessInstance().getProcessDefinition());
								}
							});
					currentHandler = (AbstractExceptionActionHandler) cl.loadClass(
							handlerClassName).newInstance();
					currentHandler.doPerformAction(executionContext);
				} catch (Exception ex) {
					getLogger().error("Handler threw exception.", ex);
					executionContext.getNode().raiseException(ex, executionContext);
					exceptionCaught = true;
				} finally {
					ExecutionContext.popCurrentContext(executionContext);
				}

				// Move the process along, if applicable.
				if (!exceptionCaught || !useJbpmExceptionHandler || signalAfterException) {
					if (!StringUtils.isBlank(transitionName) && executionContext.getNode().hasLeavingTransition(transitionName)) {
						token.signal(transitionName);
					} else {
						// Use the default transition.
						token.signal();
					}
				}
			} finally {
				jbpmCtx.close();
			}
		}

		/** Inherited method not used here. */
		public void beforeCompletion() {
			// Not used here.
		}

		public void afterCompletion(int status) {
			if (getLogger().isDebugEnabled()
			        && status != Status.STATUS_COMMITTED && status != Status.STATUS_ROLLEDBACK) {
					getLogger().debug("Attempting to start asynchronous action handling while tx status was " + status);
			}

			// The original transaction is completed, a new one can be started.
			ThreadPoolSingleton.INSTANCE.executeServiceCall(this);
		}
	}
	
	protected Logger getLogger() {
		return LOGGER;		
	}

}
