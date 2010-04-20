package org.jboss.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * Ensures this is a singleton implementation of the ThreadPool for service calls. 
 */
public enum ThreadPoolSingleton {
    /** The Threadpool singleton instance. */
    INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(ThreadPoolSingleton.class);

    private final Integer numberThreads = 10;
    private Executor exec = new HandleableThreadPoolExecutor();

    private UncaughtExceptionHandler handler;

    /**
     * This is what we do!
     * 
     * @param command
     *            The runnable command.
     */
    public void executeServiceCall(Runnable command) {
        exec.execute(command);
    }

    /**
     * @param ueh
     *            A handler that is going to catch the uncaught exceptions instead of the default handler (e.g. for testing
     *            purposes).
     */
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler ueh) {
        handler = ueh;
    }

    /**
     * ThreadPoolExecutor that allows for setting an <code>UncaughtExceptionHandler</code> to each thread that is used to execute a
     * given <code>Runnable</code>.
     */
    private class HandleableThreadPoolExecutor extends ThreadPoolExecutor {
        /** Default constructor that creates a 'fixed thread pool'. */
        HandleableThreadPoolExecutor() {
            super(numberThreads, numberThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            setRejectedExecutionHandler(new DefaultRejectedExecutionHandler());
        }

        /** {@inheritDoc} */
        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Starting task " + r + " in thread " + t);
            }
            if (handler != null) {
                t.setUncaughtExceptionHandler(handler);
            }
        }
    }

    /**
     * Default implementation for the <code>RejectedExecutionHandler</code>.
     */
    private static class DefaultRejectedExecutionHandler implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor tpe) {
            LOGGER.error(tpe + " unable to handle task " + r);
            throw new RejectedExecutionException();
        }
    }
}
