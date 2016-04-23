package delegator;

import java.io.File;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.runner.Result;

import server.CustomServerInterface;

public class TestAgent extends Thread implements Runnable{

	private Class<?> testClass;
	private static ConcurrentLinkedQueue<Result> resultQueue;
	private CustomServerInterface server;
	private File testFile;
	
	public TestAgent(CustomServerInterface server, ConcurrentLinkedQueue<Result> resultQueueRef) {
		this.server = server;
		if(resultQueue == null) {
			resultQueue = resultQueueRef;
		}
	}
	
	public TestAgent(TestAgent agent, Class<?> testClass) {
		server = agent.getServer();
		resultQueue = agent.getResultQueue();
		this.testClass = testClass;
	}
	
	public TestAgent(TestAgent agent, File testFile) {
		server = agent.getServer();
		resultQueue = agent.getResultQueue();
		this.testFile = testFile;
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
	
	@Override
	public void run() {
		try {
			if(testFile == null) {
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
	
	public static synchronized void addResult (Result result) {
		resultQueue.add(result);
	}
	
	

}
