package server;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import delegator.DelegatorInterface; 

public class CustomServer extends UnicastRemoteObject implements CustomServerInterface{

	protected CustomServer() throws RemoteException {
		super();
	}

	public static void main(String[] args) {
		try {
		String host = "192.168.0.103";
		String port = "1099";
		
//		Registry registry = LocateRegistry.getRegistry(host, port);
//		registry.rebind("CustomServer_" + (registry.list().length + 1), new CustomServer());
//		DelegatorInterface delegator = (DelegatorInterface) registry.lookup("Delegator");

		DelegatorInterface delegator = (DelegatorInterface) Naming.lookup("//" + host + ":" + port + "/Delegator");
		
		CustomServer server = new CustomServer();
//		delegator.rebindServer((Remote) server);
		delegator.rebindServer((CustomServerInterface) server);
//		System.out.println(delegator.ping());
		
		}
		catch(Exception e){e.printStackTrace();}
		
	}
	
	public Result runTest(File testFile) {
		
		System.out.println("TEST");
		try{
		FTPClient client = new FTPClient();
		
		client.connect(InetAddress.getByName("192.168.0.103"), 81);
		
		client.login("user", "user");
				
		// TODO: Need to recursively traverse lib directory and add all individual jar files to
		// the class loader
		// TODO: Need to keep host name, username/password as user configurable (e.g. specify at runtime)
		// TODO: Ultimately try changing from ftp to ftps
		
		// >Without user credentials
//		CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://127.0.0.1:81/bin/"));
		
		// >With user credentials (username = "user", password = "user")
		CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://user:user@192.168.0.103:81/bin/"));
		
		// Add all files in the ftp lib directory directly to classpath
		FTPFile[] jarFiles = client.listFiles("lib/");
		for(int i = 0; i < jarFiles.length; i++) {
			System.out.println(jarFiles[i].getName());
			CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://user:user@192.168.0.103:81/lib/" + jarFiles[i].getName()));
		}
		
//		boolean test1 = client.retrieveFile("bin/testsuite/CorrectnessTest.class", new FileOutputStream("C:/FileZilla/CorrectnessTest.class"));
//		boolean test1 = client.retrieveFile("bin/org/schemaanalyst/unittest/faultlocalization/TestCalculator.class", new FileOutputStream("C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestProject/TestCalculator.class"));
//		boolean test1 = client.retrieveFile("bin/org/schemaanalyst/unittest/AllTests.class", new FileOutputStream("C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestProject/AllTests.class"));
		
//		File testClass = new File("C:/FileZilla/CorrectnessTest.class");
//		File testClass = new File("C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestProject/TestCalculator.class");
		
//		File testClass = new File("C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestProject/AllTests.class");
		
		
		
		FileInputStream in = new FileInputStream(testFile);
		byte[] classBytes = new byte[(int) testFile.length()];
		in.read(classBytes);		
		
		SimpleClassLoader loader = new SimpleClassLoader();
		Class convertedClass = loader.convertToClass(classBytes);		
		System.out.println("ConvertedClass: " + convertedClass);

		Result result = JUnitCore.runClasses(convertedClass);
		
//		// Indicate overall results of testing 
//		System.out.println("Total tests run: " + r.getRunCount());
//		System.out.println("Number of successes: " + (r.getRunCount() - r.getFailureCount()));
//		System.out.println("Number of failures: " + r.getFailureCount());
//
//		// List all failures that have been generated 
//		for (Failure failure : r.getFailures()) {
//			System.out.println("\t" + failure.toString());
//		}
		
		client.disconnect();
		
		return result;
		
		
		
		
//		client.storeFile("testFile.file", new FileInputStream("testFile.file"));

//		-Djava.security.policy=rmi.policy
//		System.setSecurityManager(new SecurityManager());
		
		}
		catch(Exception e) {e.printStackTrace(); return null;}
		}
}
