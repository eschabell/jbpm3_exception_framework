package org.jboss.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jbpm.JbpmConfiguration;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * Container for a process definition reference, with the possibility to include
 * a list of these containers for the subprocesses.
 */
public class ProcessDefinitionReference {
	private static final Logger LOGGER = Logger.getLogger(ProcessDefinitionReference.class);

	private static final JbpmConfiguration JBPM_CONF = JbpmConfiguration.getInstance();

	/** The String containing an XML process definition or the name of a parsable resource. */
	private String procDefString;

	/** The list of subprocesses. */
	private List<ProcessDefinitionReference> subprocs = new ArrayList<ProcessDefinitionReference>();

	/**
	 * Constructor for process definition <code>String</code>s.
	 * 
	 * @param procDefString
	 *            The process definition, in one of the following formats:
	 *            <ul>
	 *            <li>An XML format string containing the definition.
	 *            <li>The file pathname for the process archive resource (ending
	 *            in '.par') containing the definition.
	 *            <li>The file pathname for the resource containing the
	 *            definition.
	 *            </ul>
	 */
	public ProcessDefinitionReference(final String procDefString) {
		this.procDefString = procDefString;
	}

	/**
	 * Chainable convenience method for adding subprocesses.
	 * 
	 * @param ref The reference object for the subprocess definition to be added.
	 * @return A reference to the process to which the subprocess was just added.
	 */
	public ProcessDefinitionReference addSubproc(ProcessDefinitionReference ref) {
		subprocs.add(ref);
		return this;
	}

	/**
	 * @return The reference to the top-level process definition, which is
	 *         deployed along with all of its nested subprocesses.
	 */
	public ProcessDefinition parseAndDeployProcess() {
		// First do the subprocesses (recursively).
		for (ProcessDefinitionReference subProcRef : subprocs) {
			subProcRef.parseAndDeployProcess();
		}

		// Then do this process itself.
		// - Parse.
		ProcessDefinition procDef = null;
		if (procDefString.endsWith("</process-definition>")) {
			// It's a process definition XML String.
			procDef = ProcessDefinition.parseXmlString(procDefString);
		} else if (procDefString.endsWith(".par")) {
			// It's a process archive.
			try {
				procDef = ProcessDefinition.parseParResource(procDefString);
			} catch (IOException ioEx) {
				LOGGER.error("Cannot parse .par file as a resource.", ioEx);
			}
		} else {
			// Assume it's a definition in a resource on the classpath.
			procDef = ProcessDefinition.parseXmlResource(procDefString);
		}
		// - Deploy.
		if (procDef != null) {
			JBPM_CONF.getCurrentJbpmContext().deployProcessDefinition(procDef);
		}

		return procDef;
	}
}
