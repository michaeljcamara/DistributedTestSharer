package delegator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.io.FilenameUtils;
import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.ipfilter.MinaIpFilter;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import server.CustomServerInterface;
import server.SimpleClassLoader;

public class Delegator extends UnicastRemoteObject implements DelegatorInterface{
	
	private static String host, ftpRootDir;
	private static int registryPort, ftpServerPort;
	private static LinkedList<File> testList;
	private static LinkedList<CustomServerInterface> serverList;
	
	public static void main(String[] args) throws RemoteException {
		
		// Prevent console logging output
		Logger.getRootLogger().removeAllAppenders();
		Logger.getRootLogger().addAppender(new NullAppender());
		
		host = "192.168.0.103";
//		host = "141.195.23.234";
		registryPort = 12345;
		ftpServerPort = 12346;
		ftpRootDir = "C:/FileZilla/";
		testList = new LinkedList<File>();
		serverList = new LinkedList<CustomServerInterface>();
		
		System.setProperty("java.security.policy", "rmi.policy");
		System.setSecurityManager(new SecurityManager());
		System.setProperty("java.rmi.server.hostname", host);
//		System.setProperty("java.rmi.server.codebase", "ftp://C:/FileZilla/resources/bin/");
		System.setProperty("java.rmi.server.codebase", "ftp://" + "user" + ":" + "user"+ "@" + host + ":" + ftpServerPort + "/" + "resources/bin/");
		
		
		createRegistry();
		createFTPServer();
		
	}

	protected Delegator() throws RemoteException {
		super();
		System.setProperty("java.security.policy", "rmi.policy");
	}
	
	private void updateServers() throws RemoteException {
		for(CustomServerInterface server : serverList) {
			server.updateClassLoader();
		}
	}
	
	private static void createFTPServer() {
		
		FtpServerFactory ftpServerFactory = new FtpServerFactory();
		
	    UserManager userManager = ftpServerFactory.getUserManager();
	    BaseUser adminUser = new BaseUser();
	    adminUser.setName("user");
	    adminUser.setPassword("user");
	    adminUser.setEnabled(true);
	    ArrayList<Authority> authorities = new ArrayList<Authority>();
	    authorities.add(new WritePermission());
	    adminUser.setAuthorities(authorities);
	    adminUser.setHomeDirectory(ftpRootDir);
	    adminUser.setMaxIdleTime(999);
	    try {
			userManager.save(adminUser);
		} catch (FtpException e2) {
			e2.printStackTrace();
		}
	    ftpServerFactory.setUserManager(userManager);
	    
		ListenerFactory listenerFactory = new ListenerFactory();
		listenerFactory.setPort(ftpServerPort);
		listenerFactory.setServerAddress(host);
		listenerFactory.setIdleTimeout(999);
		
		ConnectionConfigFactory connectionFactory = new ConnectionConfigFactory();
		connectionFactory.setAnonymousLoginEnabled(true);
		connectionFactory.setMaxAnonymousLogins(999);
		connectionFactory.setMaxLogins(999);
		connectionFactory.setMaxLoginFailures(999);
		connectionFactory.setMaxThreads(999);
		ftpServerFactory.setConnectionConfig(connectionFactory.createConnectionConfig());
		
		DataConnectionConfigurationFactory dataFactory = new DataConnectionConfigurationFactory();
		dataFactory.setActiveEnabled(true);
		dataFactory.setActiveIpCheck(false);
		dataFactory.setActiveLocalAddress(host);
		dataFactory.setPassiveAddress(host);
		dataFactory.setIdleTime(999);
		listenerFactory.setDataConnectionConfiguration(dataFactory.createDataConnectionConfiguration());
		ftpServerFactory.addListener("default", listenerFactory.createListener());
		
		NativeFileSystemFactory fileFactory = new NativeFileSystemFactory();
		try {
			fileFactory.createFileSystemView(adminUser);
			fileFactory.setCreateHome(true);
//			fileFactory.setCaseInsensitive(true);
		} catch (FtpException e1) {
			e1.printStackTrace();
		}
		ftpServerFactory.setFileSystem(fileFactory);
		

		
		
		 
		FtpServer ftpServer = ftpServerFactory.createServer();
		
		try {
			ftpServer.start();
		} catch (FtpException e) {
			e.printStackTrace();
		}
	}
	
	private static void createRegistry() throws RemoteException {
		Registry registry = LocateRegistry.createRegistry(registryPort);
		Delegator delegator = new Delegator();
		registry.rebind("Delegator", delegator);
	}

	@Override
	public void uploadResources(File file) throws RemoteException {

		System.out.println(file);
		System.out.println(file.getPath());
		System.out.println(file.getName());
		System.out.println(file.toURI().toString());
		System.out.println(file.length());
		
//		LinkedList<File> testList = new LinkedList<File>();
		
		for(File subDir : file.listFiles()) {
			if(subDir.getName().equals("tests")) {
				createTestList(subDir);
			}
			else {
				createResources(subDir);
			}
		}
		
		// Update the servers' classpaths and establish connection with the FTP server
		updateServers();
	}
	
	private void createResources(File file) {
		try {
			if(file.isDirectory()) {
//				System.out.println("path " + file.toPath());
				File targetFile = new File("C:/FileZilla/" + file.toPath());
				targetFile.mkdirs();
				for(File subDir : file.listFiles()) {
					createResources(subDir);
				}
			}
			else {

//				System.out.println(file.getName());
//				System.out.println(file.toPath());
				FileInputStream in = new FileInputStream(file);
				byte[] fileBytes = new byte[(int) file.length()];
				in.read(fileBytes);

				FileOutputStream out = new FileOutputStream("C:/FileZilla/" + file.getPath());
				out.write(fileBytes);
			}
			

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	private void createTestList(File file, LinkedList<File> list) {
	private void createTestList(File file) {
		
		System.out.println(FilenameUtils.getExtension(file.getName()));
		
		if(file.isDirectory()) {
			for(File subDir : file.listFiles()) {
				createTestList(subDir);
			}
		}
		else if(FilenameUtils.getExtension(file.getName()).equals("class")){
			testList.add(file);
		}
	}

	@Override
//	public Result uploadTestCases(File file) throws RemoteException {
	public Result runTests() throws RemoteException {
		
		
		System.out.println("FIRST TEST: " + testList.getFirst());
//		for(File testFile : testList) {
//		System.out.println(testFile);
//	}
//	System.out.println("#Tests: " + testList.size());
						
		try{
//			Registry registry = LocateRegistry.getRegistry(registryPort);
//
//			String[] serverNames = registry.list();
//			CustomServerInterface[] serverObjects = new CustomServerInterface[serverNames.length - 1];
//			for(int i = 0; i < serverObjects.length; i++) {
//				Object serverObject = registry.lookup(serverNames[i]);
//				if(serverObject instanceof CustomServerInterface) {
//					serverObjects[i] = (CustomServerInterface) registry.lookup(serverNames[i]);
//					System.out.println(serverNames[i]);
//				}
//			}
			
//			System.out.println(serverObjects.length);
			System.setSecurityManager(new SecurityManager());
			
			SimpleClassLoader simpleLoader = new SimpleClassLoader();
			FileInputStream in = new FileInputStream(testList.getFirst());
			byte[] classBytes = new byte[(int) testList.getFirst().length()];
			in.read(classBytes);
			Class<?> convertedClass = simpleLoader.convertToClass(classBytes);
			SuiteClasses suiteAnnotation = convertedClass.getAnnotation(SuiteClasses.class);
			if(suiteAnnotation != null) {
				Class<?>[] classesInSuite = suiteAnnotation.value();

				for(Class<?> c : classesInSuite) {
					System.out.println(c.getName());
					serverList.get(0).runTest(c);
					serverList.get(1).runTest(c);
				}
			}

			System.out.println("# CLASSES IN SUITE: " + suiteAnnotation.value().length);
			
			
			
			
			
			
//			Result result = serverObjects[0].runTest(testList.removeFirst());
			
//			for(File testFile : testList) {
				
				
//				// Indicate overall results of testing 
//				System.out.println("Total tests run: " + result.getRunCount());
//				System.out.println("Number of successes: " + (result.getRunCount() - result.getFailureCount()));
//				System.out.println("Number of failures: " + result.getFailureCount());
//
//				// List all failures that have been generated 
//				for (Failure failure : result.getFailures()) {
//					System.out.println("\t" + failure.toString());
//				}
//			}
			
//			Iterator<File> iterator = testList.iterator();
//			while(iterator.hasNext()) {
//				serverObjects[0].runTest(iterator.next());
//			}
			
//			// Indicate overall results of testing 
//			System.out.println("Total tests run: " + result.getRunCount());
//			System.out.println("Number of successes: " + (result.getRunCount() - result.getFailureCount()));
//			System.out.println("Number of failures: " + result.getFailureCount());
//
//			// List all failures that have been generated 
//			for (Failure failure : result.getFailures()) {
//				System.out.println("\t" + failure.toString());
//			}
			
//			return result;
			return null;
			
		}
		catch(Exception e) {e.printStackTrace(); return null;}
	}
	
	public String ping() throws RemoteException {
		System.out.println("===PING!===");
		return "===PING!===";
	}
	
	@Override
//	public void rebindServer(Remote remoteObject) throws RemoteException {
	public void rebindServer(CustomServerInterface remoteObject) throws RemoteException {
		
		System.out.println("IN DELEGATOR--REBIND.  Before rebind:");
		
		try {
//		Registry registry = LocateRegistry.getRegistry(1099);
			Registry registry = LocateRegistry.getRegistry(12345);
		
		for(String s : registry.list()) {
			System.out.println(s);
		}
		
		registry.rebind("CustomServer_" + registry.list().length, remoteObject);
		
		serverList.add(remoteObject);
		
		System.out.println("AFTER REBIND: ");
		for(String s : registry.list()) {
			System.out.println(s);
		}
		}
		catch(Exception e) {e.printStackTrace();}
		
	}
}