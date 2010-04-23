package org.jboss.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.util.ProcessDefinitionReference;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.GraphSession;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the Abstract Action Handler. 
 */
public class TestForNodeExceptionHandling {

	/** The global jBPM configuration. */
    private static JbpmConfiguration jbpmConf = JbpmConfiguration.getInstance();
    
    /** Variables for Tasks. */
    private static final String ACTOR_ID = "EXPERT";
	private static final String TASK_NAME = "Technical dropout";
	private static final Set<String> POOLED_ACTORS = null;
	private static final String SWIMLANE = null;
	
	public static final String ORIGINATING_PROCESS_NAME = "Originator Process";
	public static final String EXCEPTION_FRAMEWORK_PROCESS_NAME = "Exception Framework";

	/** 
	 * Fix the standard logging setup to work better within the IDE console,
	 * just comment out to use and COMMENT OUT BEFORE CHECKIN.
	 */
	@BeforeClass
	public static final void oneTimeSetupExceptionFrameworkTesting() {
		// Turn on for logging in console to be within my IDE window size.
        //((PatternLayout) ((Appender) Logger.getRootLogger().getAllAppenders().nextElement()).getLayout()).setConversionPattern("%r [%t] %p %c %x -%n%n  %m%n%n");
        

        // Start the JobExecutor for async continuations within the test environment.
        jbpmConf.startJobExecutor();

        // Initialize logging.
        BasicConfigurator.configure();
        // - Remove the superfluous appender.
        Logger.getRootLogger().removeAppender((Appender) Logger.getRootLogger().getAllAppenders().nextElement());
        // Adjust levels:
        Logger.getRootLogger().setLevel(Level.ERROR);
		Logger.getLogger("org.jboss").setLevel(Level.DEBUG);
		Logger.getLogger("org.jbpm.graph").setLevel(Level.DEBUG);

		// Deploy my exception framework too.
		try {
			deployExceptionFramework();			
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

    /**
     * Called once after all test runs, stops the JobExecutor.
     * 
     * @throws Exception When something unexpected goes wrong.
     */
    @AfterClass
    public static final void oneTimeTearDown() throws Exception {
        // End the JobExecutor thread.
        jbpmConf.getJobExecutor().stop();
    }
    
	/**
	 * The main test we want to run here to throw an exception in the Node of our
	 * test process and handle it through our Exception Framework.
	 * 
	 * @throws Exception
	 */
	@Test
    public void testForNodeExceptionHandling() throws Exception {
         // deploy process first.
        JbpmContext jbpmCtx = jbpmConf.createJbpmContext();
        try {
            // Create an instance of the jBPM process.
            ProcessDefinition procDef = getTestProcess().parseAndDeployProcess();
            ProcessInstance procInst = new ProcessInstance(procDef);
            jbpmCtx.save(procInst);
        } finally {
            // Close the transaction.
            jbpmCtx.close();
        }
        
        // signal process to start.
        jbpmCtx = jbpmConf.createJbpmContext();
		try {
			long processInstanceId = getProcessId(ORIGINATING_PROCESS_NAME);
			ProcessInstance procInst = jbpmCtx.getProcessInstance(processInstanceId);
			assertThat("No exception process instance returned.", procInst, is(not(nullValue())));
			procInst.signal();
		} finally {
			jbpmCtx.close();
		}

        Thread.sleep(5000);
        
        // In Human Task and need to end which will cause the flow to signal back
        // to the originating process.
        endTask();
		
		// Test that originating process is in end state.
        jbpmCtx = jbpmConf.createJbpmContext();
		try {
			long processInstanceId = getProcessId(ORIGINATING_PROCESS_NAME);
			ProcessInstance procInst = jbpmCtx.getProcessInstance(processInstanceId);
			assertThat("No exception process instance returned.", procInst, is(not(nullValue())));
			assertThat("Exception process not ended.", procInst.hasEnded(), is(true));
		} finally {
			jbpmCtx.close();
		}
		
        // Test that it is in end state now for exception framework.
		jbpmCtx = jbpmConf.createJbpmContext();
		try {
			long processInstanceId = getProcessId(EXCEPTION_FRAMEWORK_PROCESS_NAME);
			ProcessInstance procInst = jbpmCtx.getProcessInstance(processInstanceId);
			assertThat("No exception process instance returned.", procInst, is(not(nullValue())));
			assertThat("Exception process in unexpected state.", procInst.hasEnded(), is(true));
		} finally {
			jbpmCtx.close();
		}
    }

	/**
	 * Gives you the test process definition.
	 */
    protected ProcessDefinitionReference getTestProcess() throws Exception {
        String testProcDef = "<process-definition xmlns=\"urn:jbpm.org:jpdl-3.1\" name=\"" + ORIGINATING_PROCESS_NAME + "\">"
        	+ "<start-state name=\"start\">"
        	+ "<transition to=\"node1\" />"
        	+ "</start-state>"
        	+ "<node name=\"node1\" async=\"true\">"
        	+ "<action class=\"org.jboss.handler.ExceptionThrowingHandler\" />"
        	+ "<transition to=\"end\" name=\"node_to_end\" />"
        	+ "</node>"
        	+ "<end-state name=\"end\" />"
    		+ "</process-definition>";
        return new ProcessDefinitionReference(testProcDef);
    }

    private static void deployExceptionFramework() throws Exception {
		// Open the transaction.
		JbpmContext jbpmCtx = jbpmConf.createJbpmContext();
		try {
			// Create an instance of the jBPM exception framework process.
			String frameworkXml = "process/processdefinition.xml";
			ProcessDefinition procDef = ProcessDefinition.parseXmlResource(frameworkXml);
			jbpmCtx.deployProcessDefinition(procDef);
		} finally {
			// Close the transaction.
			jbpmCtx.close();
		}
    }
    
    private void endTask() {
		// Open the transaction.
		JbpmContext jbpmCtx = jbpmConf.createJbpmContext();
		 try {
			 long processInstanceId = getProcessId(EXCEPTION_FRAMEWORK_PROCESS_NAME);
			 ProcessInstance procInst = jbpmCtx.getProcessInstance(processInstanceId);
			 assertThat("No exception process instance returned.", procInst, is(not(nullValue())));

			 Collection<TaskInstance> taskInsts = procInst.getTaskMgmtInstance().getUnfinishedTasks(procInst.getRootToken());
			 assertThat("No task instances found.", taskInsts, is(not(nullValue())));
		
	    	 TaskInstance taskInst = getIndicatedTaskInstance(taskInsts, ACTOR_ID, TASK_NAME, POOLED_ACTORS, SWIMLANE);		
			 if (taskInst != null) {
				 // End the task instance.
				 taskInst.end();
			 }
		 } finally {
			 // Tear down the pojo persistence context.
		     jbpmCtx.close();
		 }		
    }
    
    
	private long getProcessId (String processName) {
		long procId;
		
		// Open the transaction.
		JbpmContext jbpmCtx = jbpmConf.createJbpmContext();
		 try {
			 // Get our process instance back.
			 GraphSession graphSession = jbpmCtx.getGraphSession();
		     ProcessDefinition processDefinition = graphSession.findLatestProcessDefinition(processName);
		     assertThat("No process definition found.", processDefinition, is(not(nullValue())));

		     // Now, we search for all process instances of this process definition.
		     List processInstances = graphSession.findProcessInstances(processDefinition.getId());
		     ProcessInstance procInst = (ProcessInstance) processInstances.get(0);
		     procId = procInst.getId();
		 } finally {
			 // Tear down the pojo persistence context.
		     jbpmCtx.close();
		 }			

		 return procId;
	}
	
    private TaskInstance getIndicatedTaskInstance(Collection<TaskInstance> taskInsts, final String actorId, final String taskName,
            final Set<String> pooledActors, final String swimlane) {
        TaskInstance indicated = null;
        for (TaskInstance taskInst : taskInsts) {
            if ((actorId == null || actorId.equals(taskInst.getActorId()))
                    && (taskName == null || taskName.equals(taskInst.getTask().getName()))
                    && (pooledActors == null || CollectionUtils.isSubCollection(pooledActors, taskInst.getPooledActors()))
                    && (swimlane == null || swimlane.equals(taskInst.getSwimlaneInstance().getName()))) {
                indicated = taskInst;
                break;
            }
        }
        return indicated;
    }
 
}
