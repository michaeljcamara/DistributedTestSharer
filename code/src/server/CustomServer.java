package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import delegator.CustomSocketFactory;
import delegator.Delegator;
import delegator.DelegatorInterface;

public class CustomServer extends UnicastRemoteObject implements CustomServerInterface{

	private static String host, ftpClassDir, ftpJarDir, ftpRootDir, userName, userPassword;
	private static int registryPort, ftpServerPort;
	private static FTPClient client;
	
	protected CustomServer() throws RemoteException {
		super(12345);
	}
	
	public void updateClassLoader() {
		
		System.out.println("BEGIN UPDATECLASSLOADER");
		try {
			client = new FTPClient();
			System.out.println("BEGIN UPDATECLASSLOADER1");
			client.connect(host, ftpServerPort);		
			System.out.println("BEGIN UPDATECLASSLOADER2");
			client.login(userName, userPassword);
			
			System.out.println(client.getReplyCode());
			System.out.println(client.getLocalAddress());
			System.out.println(client.getLocalPort());
			System.out.println(client.getRemotePort());
			System.out.println(client.printWorkingDirectory());
			System.out.println(client.getKeepAlive());
			
			//change user.dir ->
			
			System.out.println("BEGIN UPDATECLASSLOADER3");

			CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://" + userName + ":" + userPassword + "@" + host + ":" + ftpServerPort + "/" + ftpClassDir));
			System.out.println("BEGIN UPDATECLASSLOADER4");
			// Add all files in the ftp lib directory directly to classpath
			
			System.out.println(ftpJarDir);
//			String[] jarFiles = client.listNames(ftpJarDir);
			FTPFile[] jarFiles = client.listFiles(ftpJarDir);
			System.out.println("got names");
			client.setKeepAlive(true);
			
//			FTPFile[] jarFiles = client.listFiles(ftpJarDir);
			System.out.println("BEGIN UPDATECLASSLOADER5");
			for(int i = 0; i < jarFiles.length; i++) {
				CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://" + userName + ":" + userPassword + "@" + host + ":" + ftpServerPort + "/" + ftpJarDir + jarFiles[i].getName()));
//				CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://" + userName + ":" + userPassword + "@" + host + ":" + ftpServerPort + "/" + ftpJarDir + jarFiles[i]));
//				System.out.println("BEGIN UPDATECLASSLOADER6");
				System.out.println(jarFiles[i].getName());
			}
			
			
		} catch(Exception e){e.printStackTrace();}
		
		System.out.println("END UPDATECLASSLOADER");
	}

	public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {

		
			System.setProperty("java.system.class.loader", "server.CustomClassLoader"); 
		
//			host = "192.168.0.101";
			host = "141.195.226.180";
			registryPort = 12345;
			ftpServerPort = 12346;

			System.setProperty("java.security.policy", "rmi.policy");
			System.setProperty("java.rmi.server.disableHttp", "false");
//			System.setProperty("java.rmi.server.hostname", "141.195.226.138");
			
//			System.setProperty("jav.rmi.server.codebase", "/home/c/camaram/cs441s2015/cs441s2016-fp-team1/code/bin/");
//			System.setProperty("java.rmi.server.hostname", "141.195.23.54");
//			System.setSecurityManager(new SecurityManager());
//			
			ftpClassDir = "resources/bin/";
			ftpJarDir = "resources/lib/";
			ftpRootDir = "resources/";
			userName = "user";
			userPassword = "user";

//			try {
//				RMISocketFactory.setSocketFactory(new CustomSocketFactory(registryPort));
//			} catch (Exception e) {
//				e.printStackTrace();
//			}

//			Registry registry = LocateRegistry.getRegistry(host, registryPort, new CustomSocketFactory(registryPort));
			Registry registry = LocateRegistry.getRegistry(host, registryPort);
			System.out.println("GOT REGISTRY");
			for(String a : registry.list()) {
				System.out.println("Bound name: " + a);
			}
			
			System.out.println(registry);

//			DelegatorInterface delegator = (DelegatorInterface) Naming.lookup("//" + host + ":" + registryPort + "/Delegator");
			DelegatorInterface delegator = (DelegatorInterface) registry.lookup("Delegator");			
			CustomServer server = new CustomServer();
			
			Registry registry2 = LocateRegistry.createRegistry(12345);
	        registry2.rebind("Delegator", delegator);
			
			System.out.println(delegator);
			delegator.rebindServer((CustomServerInterface) server);
//			System.out.println(delegator.getClass());
			System.out.println(delegator.ping());
			
	        
			
	}
	
	public String ping() throws RemoteException {return "==========PING FROM SERVER==============";}
	
	public Result runTest(File testFile) {
				
		System.out.println("**BEGINNING runTest()**");
		
		try{
		
		FileInputStream in = new FileInputStream(testFile);
		byte[] classBytes = new byte[(int) testFile.length()];
		in.read(classBytes);	
		
		SimpleClassLoader loader = new SimpleClassLoader();
//		CustomClassLoader loader = (CustomClassLoader) URLClassLoader.getSystemClassLoader();
//		Class convertedClass = CustomClassLoader.convertToClass(testFile.getName(), classBytes, 0, classBytes.length, null);
		
//		Class convertedClass = CustomClassLoader.findClassWithSystemClassLoader("testsuite.CorrectnessTest");
//		Class convertedClass = CustomClassLoader.findClassWithSystemClassLoader("org.schemaanalyst.unittest.AllTests");
		
//		CustomClassLoader loader2 = new CustomClassLoader(((URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs());
//		SimpleClassLoader loader2 = new SimpleClassLoader(); 
//		FileOutputStream out = new FileOutputStream("temp.file");
//		InputStream in = client.retrieveFileStream(ftpClassDir + "testSuite/CorrectnessTest.class");
//		System.out.println(in);
//		System.out.println(client.getReplyCode());
//		byte[] testBytes2 = new byte[(int) testFile.length()];
//		for(byte b : testBytes2) System.out.println(b);
//		System.out.println(in.read(testBytes2));
		
		
		Class convertedClass = loader.convertToClass(classBytes);
//		Class convertedClass = loader.convertToClass(testBytes2);
		System.out.println("ConvertedClass: " + convertedClass);
		System.out.println("convertedClass CL: " + convertedClass.getClassLoader().toString());	
		
		Result result = JUnitCore.runClasses(convertedClass);
		
		// Indicate overall results of testing 
		System.out.println("Total tests run: " + result.getRunCount());
		System.out.println("Number of successes: " + (result.getRunCount() - result.getFailureCount()));
		System.out.println("Number of failures: " + result.getFailureCount());

		// List all failures that have been generated 
		for (Failure failure : result.getFailures()) {
//			System.out.println("EXCEPTION" + failure.getException());
			System.out.println("EXCEPTION" + failure.getException());
			System.out.println("TRACE: " + failure.getTrace());
			System.out.println("MESSAGE: " + failure.getMessage());
			System.out.println("HEADER: " + failure.getTestHeader());
			System.out.println("DESCRIPTION: " + failure.getDescription());
		}
		
		System.out.println("============================");
		
		return result;
//		return null;
		
		}
		catch(Exception e) {e.printStackTrace(); System.out.println("EXCEPTION!!!");return null;}
		}
	
	public Result runTest(Class testClass) {
		
		System.out.println("**BEGINNING runTest(class)**");
//		for(URL url : ((URLClassLoader) URLClassLoader.getSystemClassLoader()).getURLs()) {
//			System.out.println(url);
//		}		
		
		try{

//		CustomClassLoader loader2 = new CustomClassLoader(((URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs());
//		SimpleClassLoader loader2 = new SimpleClassLoader(); 
		
//		Class convertedClass = loader2.convertToClass(testClass);
		
//		System.out.println(convertedClass.getComponentType());
//		System.out.println(convertedClass.getName());
//		System.out.println(convertedClass.getSimpleName());
//		System.out.println(convertedClass.getTypeName());
//		System.out.println(convertedClass.getPackage());
//		System.out.println(convertedClass.getSuperclass());
		
		System.out.println(testClass.getName());
		
//		System.setProperty("user.dir", "ftp://user:user@" + host + ":" + ftpServerPort + "/resources/");
//		System.setProperty("user.dir", "ftp://user:user@" + host + ":" + ftpServerPort + "/resources/");
//		System.setProperty("user.dir", "C:/FileZilla/resources");
//		System.setProperty("java.class.path", "bin;lib/*;C:/FileZilla/resources/config");
		
		
		Result result = JUnitCore.runClasses(testClass);
		
		// Indicate overall results of testing 
		System.out.println("Total tests run: " + result.getRunCount());
		System.out.println("Number of successes: " + (result.getRunCount() - result.getFailureCount()));
		System.out.println("Number of failures: " + result.getFailureCount());

		// List all failures that have been generated 
		for (Failure failure : result.getFailures()) {
			System.out.println("EXCEPTION" + failure.getException());
			System.out.println("TRACE: " + failure.getTrace());
			System.out.println("MESSAGE: " + failure.getMessage());
			System.out.println("HEADER: " + failure.getTestHeader());
			System.out.println("DESCRIPTION: " + failure.getDescription());
		}
		
		System.out.println("============================");
		
		return result;
		}
		catch(Exception e) {e.printStackTrace(); System.out.println("EXCEPTION!!!");return null;}
		}
	
	
	public Result runTest(byte[] testBytes) {
		
		System.out.println("**BEGINNING runTest(BYTES)**");
		
		try{
			
			for(URL url : ((URLClassLoader) URLClassLoader.getSystemClassLoader()).getURLs()) {
				System.out.println(url);
			}
			
//			Thread.sleep(1000);
			
//			((URLClassLoader) URLClassLoader.getSystemClassLoader()).defineClass("aa", testBytes, 0, testBytes.length-1);
//			ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
//			Class<ClassLoader> classLoaderClass = ClassLoader.class;
//			
//			for(Method m : classLoaderClass.getDeclaredMethods()) {
//				System.out.print(m.getName() + " ");
//
//				for(Class c : m.getParameterTypes()) {
//					System.out.print(c.getName() + " ");
//				}
//
//				System.out.println(", return: " + m.getReturnType().getName());
//
//			}
//
//			Method method = classLoaderClass.getDeclaredMethod("defineClass", new Class[]{byte[].class, int.class, int.class}); 
//			method.setAccessible(true); 
//			System.out.println("CUSTOMMETHOD: " + method.getName());
//			for(Class c : method.getParameterTypes()) {
//				System.out.print(c.getName() + " ");
//			}
//			System.out.println(", return: " + method.getReturnType().getName());
//			Class convertedClass =  (Class) method.invoke(systemClassLoader, new Object[]{testBytes, 0, testBytes.length-1});
			
//		FTPClient client = new FTPClient();
//		
//		System.out.println("1");
//		client.connect(host, ftpServerPort);
//		System.out.println("1");
//		client.login(userName, userPassword);
//		System.out.println("1");
				
//		System.setProperty("user.dir", "ftp://user:user@" + host + ":" + ftpServerPort + "/");
//		System.setProperty("java.class.path", "ftp://user:user@" + host + ":" + port + "/bin/");
//		System.setProperty("user.dir", ftpRootDir);
		
		
//		CustomClassLoader loader = (CustomClassLoader) URLClassLoader.getSystemClassLoader();
//		Class convertedClass = CustomClassLoader.convertToClass(testFile.getName(), classBytes, 0, classBytes.length, null);
		
//		Class convertedClass = CustomClassLoader.findClassWithSystemClassLoader("testsuite.CorrectnessTest");
//		Class convertedClass = CustomClassLoader.findClassWithSystemClassLoader("org.schemaanalyst.unittest.AllTests");
		
//		client.retrieveFile("resources/tests/AllTests.class", local)
			
//			CustomClassLoader loader2 = new CustomClassLoader(ClassLoader.getSystemClassLoader());
			CustomClassLoader loader2 = new CustomClassLoader(((URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs());			
			
			FileOutputStream out = null;
			client.retrieveFile(ftpClassDir + "testSuite/CorrectnessTest.class", out);
			byte[] testBytes2 = new byte[]{};
			out.write(testBytes2);
			Class convertedClass = loader2.convertToClass(testBytes2);
			
			System.out.println(convertedClass.getComponentType());
			System.out.println(convertedClass.getName());
			System.out.println(convertedClass.getSimpleName());
			System.out.println(convertedClass.getTypeName());
			System.out.println(convertedClass.getPackage());
			System.out.println(convertedClass.getSuperclass());
			
			
		

		
//		SimpleClassLoader loader = new SimpleClassLoader();
//		Class convertedClass = loader.convertToClass(testBytes);
//		Class convertedClass = loader2.convertToClass(testBytes);
		System.out.println("ConvertedClass: " + convertedClass);
		System.out.println("convertedClass CL: " + convertedClass.getClassLoader().toString());
		System.out.println("system CL: " + CustomClassLoader.getSystemClassLoader().toString());

		System.out.println("Current dir: " + System.getProperty("user.dir"));
		
		Result result = JUnitCore.runClasses(convertedClass);
		System.out.println("Result: ");
		System.out.println(result);
		
		// Indicate overall results of testing 
		System.out.println("Total tests run: " + result.getRunCount());
		System.out.println("Number of successes: " + (result.getRunCount() - result.getFailureCount()));
		System.out.println("Number of failures: " + result.getFailureCount());

		// List all failures that have been generated 
		for (Failure failure : result.getFailures()) {
			System.out.println("EXCEPTION" + failure.getException());
			System.out.println("\t" + failure.toString());
			System.out.println("MESSAGE: " + failure.getMessage());
			System.out.println("HEADER: " + failure.getTestHeader());
//			System.out.println("TRACE: " + failure.getTrace());
			System.out.println("DESCRIPTION: " + failure.getDescription());
		}
		
		System.out.println("============================");
				
//		client.disconnect();
		
		
//		return result;
		return null;

		}
		catch(Exception e) {e.printStackTrace(); System.out.println("EXCEPTION!!!");System.out.println(e.getCause());return null;}
		}	
	
}
