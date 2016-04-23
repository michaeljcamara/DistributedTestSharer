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
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.io.FilenameUtils;
import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.junit.runner.Result;
import org.junit.runners.Suite.SuiteClasses;

import server.CustomServerInterface;

public class Delegator extends UnicastRemoteObject implements DelegatorInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7417123750947132852L;

	private static String host, ftpRootDir;

	private static int registryPort, ftpServerPort;

	// private static Registry registry;

	private static LinkedList<File> testSuiteList, testSingleList;

	private static LinkedList<CustomServerInterface> serverList;

	public static void main(String[] args) throws RemoteException {

		// Prevent console logging output
		Logger.getRootLogger().removeAllAppenders();
		Logger.getRootLogger().addAppender(new NullAppender());

		// host = "192.168.0.103";
		host = args[0];
		System.out.println("Using host address: " + host);
		registryPort = 12345;
		ftpServerPort = 12346;
		ftpRootDir = "ftpserver/";

		testSuiteList = new LinkedList<File>();
		testSingleList = new LinkedList<File>();
		serverList = new LinkedList<CustomServerInterface>();

		System.setProperty("java.security.policy", "rmi.policy");
		System.setProperty("java.rmi.server.hostname", host);
		System.setProperty("java.rmi.activation.port", "12345");

		createRegistry();
		createFTPServer();
		createTestList();

		Scanner scan = new Scanner(System.in);
		System.out.println("== Press ENTER when all servers are connected ==");
		scan.nextLine();

		// Update the servers' classpaths and establish connection with the FTP server
		updateServers();

		long start = System.currentTimeMillis();
        ConcurrentLinkedQueue<Result> results = runTests();
        long end = System.currentTimeMillis();
        long elapsed = end - start;

        int runs, successes, failures;
        runs = successes = failures = 0;

        for (Result result : results) {
            runs += result.getRunCount();
            failures += result.getFailureCount();
        }
        successes = runs - failures;

        System.out.println("Total tests run: " + runs);
        System.out.println("Number of successes: " + successes);
        System.out.println("Number of failures: " + failures);
        System.out.println("Elapsed time: " + elapsed);

		 //List all failures that have been generated
//		 for (Failure failure : result.getFailures()) {
//		 System.out.println("EXCEPTION" + failure.getException());
//		 System.out.println("TRACE: " + failure.getTrace());
//		 System.out.println("MESSAGE: " + failure.getMessage());
//		 System.out.println("HEADER: " + failure.getTestHeader());
//		 System.out.println("DESCRIPTION: " + failure.getDescription());
//		 }

	}

	protected Delegator() throws RemoteException {
		super();
	}

	private static void updateServers() throws RemoteException {
		try {
			System.out.println("Begin update servers");

			for (CustomServerInterface server : serverList) {
				server.updateClassLoader();
			}
			System.out.println("End update servers");
		} catch (Exception e) {
			e.printStackTrace();
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
			// fileFactory.setCaseInsensitive(true);
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

	private static void createTestList() throws RemoteException {

		File file = new File("ftpserver/resources/");

		for (File subDir : file.listFiles()) {
			if (subDir.getName().equals("test_singles")) {
				createSingleTestList(subDir);
			}
			else if (subDir.getName().equals("test_suites")) {
				createTestSuiteList(subDir);
			}
		}
	}

	private static void createResources(File file) {
		try {
			if (file.isDirectory()) {
				File targetFile = new File(ftpRootDir + file.toPath());
				targetFile.mkdirs();

				for (File subDir : file.listFiles()) {
					createResources(subDir);
				}
			}
			else {

				FileInputStream in = new FileInputStream(file);
				byte[] fileBytes = new byte[(int) file.length()];
				in.read(fileBytes);

				FileOutputStream out = new FileOutputStream(ftpRootDir + file.getPath());
				out.write(fileBytes);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void createSingleTestList(File file) {

		System.out.println("begin create single tests");
		System.out.println(FilenameUtils.getExtension(file.getName()));

		if (file.isDirectory()) {
			for (File subDir : file.listFiles()) {
				createSingleTestList(subDir);
			}
		}
		else if (FilenameUtils.getExtension(file.getName()).equals("class") && (!file.getName().contains("$"))) {
			testSingleList.add(file);
		}
		System.out.println("end create single tests");
	}

	private static void createTestSuiteList(File file) {
		System.out.println("begin create suite tests");

		if (file.isDirectory()) {
			for (File subDir : file.listFiles()) {
				createTestSuiteList(subDir);
			}
		}
		else if (FilenameUtils.getExtension(file.getName()).equals("class")) {
			testSuiteList.add(file);
		}
		System.out.println("end create suite tests");
	}

	private static ConcurrentLinkedQueue<Result> runTests() throws RemoteException {

		try {

			ConcurrentLinkedQueue<Result> resultQueue = new ConcurrentLinkedQueue<Result>();
			ConcurrentLinkedQueue<TestAgent> agents = new ConcurrentLinkedQueue<TestAgent>();

			// Add separate agent for each server
			for (CustomServerInterface server : serverList) {
				agents.add(new TestAgent(server, resultQueue));
			}

			SimpleClassLoader simpleLoader = new SimpleClassLoader();

			Iterator<File> suiteIterator = testSuiteList.iterator();
			Iterator<File> singleIterator = testSingleList.iterator();

			while (singleIterator.hasNext()) {
				// **TODO: Need to see if able to pass by File, or if need to convert to bytes first
				// FileInputStream in = new FileInputStream(testList.getFirst());
				// byte[] classBytes = new byte[(int) testList.getFirst().length()];
				// in.read(classBytes);

				File testFile = testSingleList.getFirst();
				TestAgent agent = agents.remove();

				if (!agent.isAlive()) {
					agent = new TestAgent(agent, testFile);
					agent.start();
					singleIterator.next();
				}

				agents.add(agent);
			}

			while (suiteIterator.hasNext()) {

				FileInputStream in = new FileInputStream(suiteIterator.next());
				byte[] classBytes = new byte[(int) testSuiteList.getFirst().length()];
				in.read(classBytes);
				simpleLoader = new SimpleClassLoader();
				Class<?> convertedClass = simpleLoader.convertToClass(classBytes);

				SuiteClasses suiteAnnotation = convertedClass.getAnnotation(SuiteClasses.class);
				Class<?>[] classesInSuite = suiteAnnotation.value();

				for (int i = 0; i < classesInSuite.length;) {
					Class<?> c = classesInSuite[i];
					TestAgent agent = agents.remove();

					if (!agent.isAlive()) {
						System.out.println("Assigning test: " + c.getName());
						agent = new TestAgent(agent, c);
						agent.start();
						i++;
					}

					agents.add(agent);
				}
			}

			// Wait until all agents have finished before returning result
			while (!agents.isEmpty()) {
				TestAgent agent = agents.peek();
				if (!agent.isAlive()) {
					agents.remove();
				}
			}

			return resultQueue;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String ping() throws RemoteException {
		return "===PING!===";
	}

	public void rebindServer(CustomServerInterface remoteObject) throws RemoteException {

		System.out.println("Starting to bind server");
		try {
			Registry registry = LocateRegistry.getRegistry(host, registryPort);
			System.out.println("got registry: " + registry);
			registry.rebind("CustomServer_" + registry.list().length, remoteObject);
			System.out.println("bound it");
			serverList.add(remoteObject);

			System.out.println("CURRENTLY BOUND REMOTE OBJECTS: ");
			for (String s : registry.list()) {
				System.out.println(s);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}