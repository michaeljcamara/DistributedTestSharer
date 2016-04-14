package server;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;
import org.apache.commons.net.ftp.FTPSServerSocketFactory;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite.SuiteClasses;

import delegator.DelegatorInterface; 

public class CustomServer extends UnicastRemoteObject implements CustomServerInterface{

	private static String host, ftpClassDir, ftpJarDir, ftpRootDir, userName, userPassword;
	private static int registryPort, ftpServerPort;
	
	protected CustomServer() throws RemoteException {
		super();
//		System.setSecurityManager(new SecurityManager());
	}
	
	public void updateClassLoader() {
		try {
			FTPClient client = new FTPClient();

			client.connect(host, ftpServerPort);		
			client.login(userName, userPassword);

			CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://" + userName + ":" + userPassword + "@" + host + ":" + ftpServerPort + "/" + ftpClassDir));

			// Add all files in the ftp lib directory directly to classpath
			FTPFile[] jarFiles = client.listFiles(ftpJarDir);

			for(int i = 0; i < jarFiles.length; i++) {
				CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://" + userName + ":" + userPassword + "@" + host + ":" + ftpServerPort + "/" + ftpJarDir + jarFiles[i].getName()));
			}	
		} catch(Exception e){e.printStackTrace();}
	}

	public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {

		
//		System.setProperty("java.system.class.loader", "server.CustomClassLoader"); 
		
			host = "192.168.0.103";
//			host = "141.195.23.234";
			registryPort = 12345;
			ftpServerPort = 12346;

			System.setProperty("java.security.policy", "rmi.policy");
			System.setSecurityManager(new SecurityManager());
			
			ftpClassDir = "resources/bin/";
			ftpJarDir = "resources/lib/";
			ftpRootDir = "resources/";
			userName = "user";
			userPassword = "user";


			DelegatorInterface delegator = (DelegatorInterface) Naming.lookup("//" + host + ":" + registryPort + "/Delegator");
			CustomServer server = new CustomServer();
			delegator.rebindServer((CustomServerInterface) server);
	}
	
	public Result runTest(File testFile) {
				
		System.out.println("**BEGINNING runTest()**");
		
		try{
		FTPClient client = new FTPClient();
		
		client.connect(host, ftpServerPort);		
		client.login(userName, userPassword);

//		client.changeToParentDirectory();
//		System.out.println("WD: " + client.printWorkingDirectory());
//		System.out.println("ChangeDir?: " + client.changeWorkingDirectory("bin/"));
//		System.out.println("WD: " + client.printWorkingDirectory());
//		client.setFileType(FTP.BINARY_FILE_TYPE); //LOCAL?
//		client.setFileType(FTP.LOCAL_FILE_TYPE);
		
		CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://" + userName + ":" + userPassword + "@" + host + ":" + ftpServerPort + "/" + ftpClassDir));
		
		// Add all files in the ftp lib directory directly to classpath
//		FTPFile[] jarFiles = client.listFiles("lib/");
		FTPFile[] jarFiles = client.listFiles(ftpJarDir);
		
		for(int i = 0; i < jarFiles.length; i++) {
			System.out.println(jarFiles[i].getName());
			CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://" + userName + ":" + userPassword + "@" + host + ":" + ftpServerPort + "/" + ftpJarDir + jarFiles[i].getName()));
		}
				
		// TODO: Need to recursively traverse lib directory and add all individual jar files to the class loader
		// TODO: Need to keep host name, username/password as user configurable (e.g. specify at runtime)
		// TODO: Ultimately try changing from ftp to ftps
		
//		CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://user:user@" + host + ":" + port + "/"));
//		CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://user:user@" + host + ":" + port));
//		CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://user:user@" + host + ":" + port + "/config/"));
//		CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://user:user@" + host + ":" + port + "/bin/config/"));
//		CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://user:user@" + host + ":" + port + "/"));
//		CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://user:user@" + host + ":" + port + "/config/locations.properties"));
		
		//**THIS MIGHT THROW CASTING ERROR!!!! STILL need to use this later
//		CustomClassLoader a = (CustomClassLoader)((URLClassLoader) URLClassLoader.getSystemClassLoader());
		
		for(URL url : ((URLClassLoader) URLClassLoader.getSystemClassLoader()).getURLs()) {
			System.out.println(url);
		}
		
//		for(Entry<Object, Object> p : System.getProperties().entrySet()) {
//			System.out.println((String)p.getKey() + " ||| " + (String)p.getValue());
//		}
		
//		System.setProperty("user.dir", "ftp://user:user@" + host + ":" + port + "/");
//		System.setProperty("java.class.path", "ftp://user:user@" + host + ":" + port + "/bin/");
		
//		System.setProperty("user.dir", ftpRootDir);
		
		FileInputStream in = new FileInputStream(testFile);
		byte[] classBytes = new byte[(int) testFile.length()];
		in.read(classBytes);	
		
		System.out.println(classBytes.getClass());
		System.out.println(classBytes.getClass().getName());
		
		SimpleClassLoader loader = new SimpleClassLoader();
//		CustomClassLoader loader = (CustomClassLoader) URLClassLoader.getSystemClassLoader();
//		Class convertedClass = CustomClassLoader.convertToClass(testFile.getName(), classBytes, 0, classBytes.length, null);
		
//		Class convertedClass = CustomClassLoader.findClassWithSystemClassLoader("testsuite.CorrectnessTest");
//		Class convertedClass = CustomClassLoader.findClassWithSystemClassLoader("org.schemaanalyst.unittest.AllTests");

		
		//Get classes in suite
//		try {
//			Class<?> convertedClass = loader.convertToClass(classBytes);
//			SuiteClasses suiteAnnotation = convertedClass.getAnnotation(SuiteClasses.class);
//			if(suiteAnnotation != null) {
//				Class<?>[] classesInSuite = suiteAnnotation.value();
//
//				for(Class<?> c : classesInSuite) {
//					System.out.println(c.getName());
//					
//					Result result = JUnitCore.runClasses(c);
//					System.out.println("Result: ");
//					System.out.println(result);
//					
//					// Indicate overall results of testing 
//					System.out.println("Total tests run: " + result.getRunCount());
//					System.out.println("Number of successes: " + (result.getRunCount() - result.getFailureCount()));
//					System.out.println("Number of failures: " + result.getFailureCount());
//
//					// List all failures that have been generated 
//					for (Failure failure : result.getFailures()) {
////						System.out.println("\t" + failure.toString());
////						System.out.println("MESSAGE: " + failure.getMessage());
////						System.out.println("HEADER: " + failure.getTestHeader());
////						System.out.println("TRACE: " + failure.getTrace());
////						System.out.println("DESCRIPTION: " + failure.getDescription());
//						System.out.println("EXCEPTION" + failure.getException());
//					}
//					
//				}
//			}
//
//			System.out.println("# CLASSES IN SUITE: " + suiteAnnotation.value().length);
//		}
//		catch(Exception e){e.printStackTrace();}
		
		
		
		Class convertedClass = loader.convertToClass(classBytes);		
		System.out.println("ConvertedClass: " + convertedClass);
		System.out.println("convertedClass CL: " + convertedClass.getClassLoader().toString());
		System.out.println("system CL: " + CustomClassLoader.getSystemClassLoader().toString());

		System.out.println("Current dir: " + System.getProperty("user.dir"));
		
		System.out.println("FTP WD: " + client.printWorkingDirectory());
		Result result = JUnitCore.runClasses(convertedClass);
		System.out.println("Result: ");
		System.out.println(result);
		
		// Indicate overall results of testing 
		System.out.println("Total tests run: " + result.getRunCount());
		System.out.println("Number of successes: " + (result.getRunCount() - result.getFailureCount()));
		System.out.println("Number of failures: " + result.getFailureCount());

		// List all failures that have been generated 
		for (Failure failure : result.getFailures()) {
//			System.out.println("\t" + failure.toString());
//			System.out.println("MESSAGE: " + failure.getMessage());
//			System.out.println("HEADER: " + failure.getTestHeader());
//			System.out.println("TRACE: " + failure.getTrace());
//			System.out.println("DESCRIPTION: " + failure.getDescription());
			System.out.println("EXCEPTION" + failure.getException());
		}
		
		System.out.println("============================");
		
//		RunNotifier notifierb = new RunNotifier();
//		BlockJUnit4ClassRunner b = new BlockJUnit4ClassRunner(convertedClass);
//		b.run(notifierb);
//		RunListener listener = new RunListener();
//		listener.
		
		
		client.disconnect();
		
//		return result;
		return null;
		
		
		
		
//		client.storeFile("testFile.file", new FileInputStream("testFile.file"));

//		-Djava.security.policy=rmi.policy
//		System.setSecurityManager(new SecurityManager());
		
		}
		catch(Exception e) {e.printStackTrace(); System.out.println("EXCEPTION!!!");return null;}
		}
	
	public Result runTest(Class testClass) {
		
		System.out.println("**BEGINNING runTest(class)**");
		
		try{
//		FTPClient client = new FTPClient();
//		
//		client.connect(host, ftpServerPort);		
//		client.login(userName, userPassword);
		
//		CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://" + userName + ":" + userPassword + "@" + host + ":" + ftpServerPort + "/" + ftpClassDir));
//		
//		// Add all files in the ftp lib directory directly to classpath
//		FTPFile[] jarFiles = client.listFiles(ftpJarDir);
//		
//		for(int i = 0; i < jarFiles.length; i++) {
//			CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://" + userName + ":" + userPassword + "@" + host + ":" + ftpServerPort + "/" + ftpJarDir + jarFiles[i].getName()));
//		}		

		Result result = JUnitCore.runClasses(testClass);
		System.out.println("Result: ");
		System.out.println(result);
		
		// Indicate overall results of testing 
		System.out.println("Total tests run: " + result.getRunCount());
		System.out.println("Number of successes: " + (result.getRunCount() - result.getFailureCount()));
		System.out.println("Number of failures: " + result.getFailureCount());

		// List all failures that have been generated 
		for (Failure failure : result.getFailures()) {
			System.out.println("EXCEPTION" + failure.getException());
		}
		
		System.out.println("============================");
		
//		client.disconnect();
		
		return result;
//		return null;
		}
		catch(Exception e) {e.printStackTrace(); System.out.println("EXCEPTION!!!");return null;}
		}
}
