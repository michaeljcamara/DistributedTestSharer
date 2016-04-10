package client;

import java.io.File;
import java.rmi.Naming;

import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import delegator.DelegatorInterface;

public class CustomClient {

	public static void main(String[] args) {
		try	{

			String host = "192.168.0.103";
			String port = "1099";

			DelegatorInterface delegator = (DelegatorInterface) Naming.lookup("//" + host + ":" + port + "/Delegator");
			
			File resourceFile = new File("sentByClient/SimpleResource.class");
			
			delegator.uploadResources(resourceFile);
			
			File testFile = new File("sentByClient/SimpleTest.class");

			Result result = delegator.uploadTestCases(testFile);
			
			// Indicate overall results of testing 
			System.out.println("Total tests run: " + result.getRunCount());
			System.out.println("Number of successes: " + (result.getRunCount() - result.getFailureCount()));
			System.out.println("Number of failures: " + result.getFailureCount());
	
			// List all failures that have been generated 
			for (Failure failure : result.getFailures()) {
				System.out.println("\t" + failure.toString());
			}

		}
		catch(Exception e) {}
	}
}
