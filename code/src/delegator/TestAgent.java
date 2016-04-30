package delegator;

import java.io.File;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.runner.Result;

import server.CustomServerInterface;

public class TestAgent extends Thread implements Runnable {

	private Class<?> testClass;
	private static ConcurrentLinkedQueue<Result> resultQueue;
	private CustomServerInterface server;
	private File testFile;

	/**
	 * Constructor for assigning the server used by this agent and the shared result queue that all results will be added to.
	 */
	public TestAgent(CustomServerInterface server, ConcurrentLinkedQueue<Result> resultQueueRef) {
		this.server = server;
		if (resultQueue == null) {
			resultQueue = resultQueueRef;
		}
	}

	/**
	 * Constructor for assigning an existing agent a new test class. 
	 * This constructor is needed, since thread will "die" and not be usable after executing run() method once.
	 */
	public TestAgent(TestAgent agent, Class<?> testClass) {
		server = agent.getServer();
		resultQueue = agent.getResultQueue();
		this.testClass = testClass;
	}

	/**
	 * Constructor for assigning an existing agent a new test file. 
	 * This constructor is needed, since thread will "die" and not be usable after executing run() method once.
	 */
	public TestAgent(TestAgent agent, File testFile) {
		server = agent.getServer();
		resultQueue = agent.getResultQueue();
		this.testFile = testFile;
	}

	/**
	 * The run method is executed whenever the super method from Thread, start(), is executed. 
	 * It will run the appropriate runTest() method for the CustomServer, depending on whether it has a test file or test class assigned
	 */
	@Override
	public void run() {
		try {
			if (testFile == null) {
				Result result = server.runTest(testClass);
				addResult(result);
			}
			else {
				Result result = server.runTest(testFile);
				addResult(result);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Synchronized method to allow multiple, different agents to add to the same result queue.
	 */
	public static synchronized void addResult(Result result) {
		resultQueue.add(result);
	}

	private ConcurrentLinkedQueue<Result> getResultQueue() {
		return resultQueue;
	}

	private CustomServerInterface getServer() {
		return server;
	}

	public void setTest(Class<?> testClass) {
		this.testClass = testClass;
	}
}
