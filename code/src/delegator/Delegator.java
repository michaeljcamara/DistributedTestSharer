package delegator;

import java.io.File;
import java.io.FileInputStream;
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
import org.junit.runner.notification.Failure;
import org.junit.runners.Suite.SuiteClasses;

import server.CustomServerInterface;

public class Delegator extends UnicastRemoteObject implements DelegatorInterface {

	private static final long serialVersionUID = 7417123750947132852L;
	private static String host, ftpRootDir;
	private static int registryPort, ftpServerPort;
	private static LinkedList<File> testSuiteList, testSingleList;
	private static LinkedList<CustomServerInterface> serverList;

	public static void main(String[] args) throws RemoteException {

		// Prevent console logging output
		Logger.getRootLogger().removeAllAppenders();
		Logger.getRootLogger().addAppender(new NullAppender());

		// Set IP address to be used by the registry and FTP server on this node
		host = args[0];
		System.out.println("Using host address: " + host);

		// Set the port numbers to use for the registry and ftp server (must be different)
		registryPort = 12345;
		ftpServerPort = 12346;

		// Set the directory the FTP server will use as its home dir
		ftpRootDir = "ftpserver/";

		// Initialize empty lists for test cases and servers
		testSuiteList = new LinkedList<File>();
		testSingleList = new LinkedList<File>();
		serverList = new LinkedList<CustomServerInterface>();

		// Set the system-wide policy for RMI security (by default, grant all permissions)
		System.setProperty("java.security.policy", "rmi.policy");

		// Set the IP address to which a registry will be bound
		System.setProperty("java.rmi.server.hostname", host);

		// Create the Java RMI registry on this node, using the specified IP address and port
		createRegistry();

		// Create the FTP server that will host files
		createFTPServer();

		// Create the lists of test cases that will be sent to servers for execution
		createTestList();

		// Wait for user input before proceeding
		Scanner scan = new Scanner(System.in);
		System.out.println("== Press ENTER when all servers are connected ==");
		scan.nextLine();

		// Begin timing here for response time of systme
		long start = System.currentTimeMillis();

		// Update the servers' classpaths and establish connection with the FTP server
		updateServers();

		// Run all of the tests by sending them to servers;
		// Save them all in this list of results
		ConcurrentLinkedQueue<Result> results = runTests();

		// End timing
		long end = System.currentTimeMillis();
		long elapsed = end - start;

		// Output results from running tests in a formatted way
		int runs, successes, failures;
		runs = successes = failures = 0;

		for (Result result : results) {
			runs += result.getRunCount();
			failures += result.getFailureCount();

			for (Failure failure : result.getFailures()) {
				System.out.println("EXCEPTION" + failure.getException());
				System.out.println("TRACE: " + failure.getTrace());
				System.out.println("MESSAGE: " + failure.getMessage());
				System.out.println("HEADER: " + failure.getTestHeader());
				System.out.println("DESCRIPTION: " + failure.getDescription());
			}
		}
		successes = runs - failures;
		System.out.println("Total tests run: " + runs);
		System.out.println("Number of successes: " + successes);
		System.out.println("Number of failures: " + failures);
		System.out.println("Elapsed time: " + elapsed);
	}

	protected Delegator() throws RemoteException {
		super();
	}

	/**
	 * Create a Java RMI registry for storing a Delegator remote object and any CustomServer objects that connect with it.
	 * 
	 * @throws RemoteException
	 */
	private static void createRegistry() throws RemoteException {
		Registry registry = LocateRegistry.createRegistry(registryPort);
		Delegator delegator = new Delegator();
		registry.rebind("Delegator", delegator);
	}

	/**
	 * Create the Apache FTP server the will host files to be used by the CustomServers.
	 */
	private static void createFTPServer() {

		FtpServerFactory ftpServerFactory = new FtpServerFactory();

		// Create one user that will have access to this server.
		// Currently, the user will be considered an admin and have all permissions
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

		// Set the listener of the server to the indicated port and host
		ListenerFactory listenerFactory = new ListenerFactory();
		listenerFactory.setPort(ftpServerPort);
		listenerFactory.setServerAddress(host);
		listenerFactory.setIdleTimeout(999);

		// Set some connection properties, such as the number of concurrent logins
		ConnectionConfigFactory connectionFactory = new ConnectionConfigFactory();
		connectionFactory.setAnonymousLoginEnabled(true);
		connectionFactory.setMaxAnonymousLogins(999);
		connectionFactory.setMaxLogins(999);
		connectionFactory.setMaxLoginFailures(999);
		connectionFactory.setMaxThreads(999);
		ftpServerFactory.setConnectionConfig(connectionFactory.createConnectionConfig());

		// Adjust more connection settings to be as permissive as possible to the user
		DataConnectionConfigurationFactory dataFactory = new DataConnectionConfigurationFactory();
		dataFactory.setActiveEnabled(true);
		dataFactory.setActiveIpCheck(false);
		dataFactory.setActiveLocalAddress(host);
		dataFactory.setPassiveAddress(host);
		dataFactory.setIdleTime(999);
		listenerFactory.setDataConnectionConfiguration(dataFactory.createDataConnectionConfiguration());
		ftpServerFactory.addListener("default", listenerFactory.createListener());

		// Allow user direct access to the previously indicated file directory
		NativeFileSystemFactory fileFactory = new NativeFileSystemFactory();
		try {
			fileFactory.createFileSystemView(adminUser);
			fileFactory.setCreateHome(true);
		} catch (FtpException e1) {
			e1.printStackTrace();
		}
		ftpServerFactory.setFileSystem(fileFactory);

		// Create the server using these properties, then start it
		FtpServer ftpServer = ftpServerFactory.createServer();

		try {
			ftpServer.start();
		} catch (FtpException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create list of single tests (testSingleList) and test suites (testSuiteList). These are obtained from the ftpserver/resources/test_singles and ftpserver/resources/test_suites directories, respectively.
	 * 
	 * @throws RemoteException
	 */
	private static void createTestList() throws RemoteException {

		File file = new File("ftpserver/resources/");

		// Get the nested test_singles and test_suites directories, and
		// run the appropriate method to parse the given test type
		for (File subDir : file.listFiles()) {
			if (subDir.getName().equals("test_singles")) {
				createSingleTestList(subDir);
			}
			else if (subDir.getName().equals("test_suites")) {
				createTestSuiteList(subDir);
			}
		}
	}

	/**
	 * Create the testSingleList, which will contain all individual test cases. 
	 * This will recursively travel through the parent directory such that all subdirectories and files are considered.
	 * 
	 * @param file
	 *            The test_singles directory
	 */
	private static void createSingleTestList(File file) {

		System.out.println("begin create single tests");
		System.out.println(FilenameUtils.getExtension(file.getName()));

		// If file is a directory, then recursively call this method again
		if (file.isDirectory()) {
			for (File subDir : file.listFiles()) {
				createSingleTestList(subDir);
			}
		}
		// Only consider files in the directory that end in .class
		else if (FilenameUtils.getExtension(file.getName()).equals("class") && (!file.getName().contains("$"))) {
			testSingleList.add(file);
		}
		System.out.println("end create single tests");
	}

	/**
	 * Create the testSuiteList, which will contain all test suites. 
	 * This will recursively travel through the parent directory such that all subdirectories and files are considered.
	 * 
	 * @param file
	 *            The test_suites directory
	 */
	private static void createTestSuiteList(File file) {
		System.out.println("begin create suite tests");

		// If file is a directory, then recursively call this method again
		if (file.isDirectory()) {
			for (File subDir : file.listFiles()) {
				createTestSuiteList(subDir);
			}
		}
		// Only consider files in the directory that end in .class
		else if (FilenameUtils.getExtension(file.getName()).equals("class")) {
			testSuiteList.add(file);
		}
		System.out.println("end create suite tests");
	}

	/**
	 * This method is called by a CustomServer in order to bind that server to the registry on this node.
	 * 
	 * @param remoteObject
	 *            The reference to the remote CustomServer
	 */
	public void rebindServer(CustomServerInterface remoteObject) throws RemoteException {

		System.out.println("Starting to bind server");
		try {
			Registry registry = LocateRegistry.getRegistry(host, registryPort);
			registry.rebind("CustomServer_" + registry.list().length, remoteObject);

			// Add this server to the server list for easy access later
			serverList.add(remoteObject);

			// Announce the current list of objects in the registry
			System.out.println("CURRENTLY BOUND REMOTE OBJECTS: ");
			for (String s : registry.list()) {
				System.out.println(s);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Call each of the servers to update their classpaths and retrieve any necessary configuration files needed to run the tests
	 * 
	 * @throws RemoteException
	 */
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

	/**
	 * This controls all of the load sharing used for distributing tests to servers.
	 * 
	 * @return The list of Results obtained from running tests on the servers
	 * @throws RemoteException
	 */
	private static ConcurrentLinkedQueue<Result> runTests() throws RemoteException {

		try {

			// Use ConcurrentLinkedQueues to allow modification from multiple threads
			ConcurrentLinkedQueue<Result> resultQueue = new ConcurrentLinkedQueue<Result>();
			ConcurrentLinkedQueue<TestAgent> agents = new ConcurrentLinkedQueue<TestAgent>();

			// Add separate agent for each server
			for (CustomServerInterface server : serverList) {
				agents.add(new TestAgent(server, resultQueue));
			}

			// Class loader for converting bytes to Class object (and nothing else)
			SimpleClassLoader simpleLoader = new SimpleClassLoader();

			// Iterators for each test list
			Iterator<File> suiteIterator = testSuiteList.iterator();
			Iterator<File> singleIterator = testSingleList.iterator();

			// Iterate first through the list of single test cases
			while (singleIterator.hasNext()) {

				// Access (don't remove) the first test in the list
				File testFile = testSingleList.getFirst();

				// Remove the first agent in the queue
				TestAgent agent = agents.remove();

				// Consider agents that are not alive (ie not running tests)
				if (!agent.isAlive()) {

					// Point agent to new agent with the same server reference, but with the current test
					agent = new TestAgent(agent, testFile);

					// Have the agent execute the server's runTest method in its own thread
					agent.start();

					// Remove the test from the list
					singleIterator.next();
				}

				// Add the agent back to the queue
				agents.add(agent);
			}

			// Iterate next through all test suites
			while (suiteIterator.hasNext()) {

				// Convert the File from bytes, then to a Class
				FileInputStream in = new FileInputStream(suiteIterator.next());
				byte[] classBytes = new byte[(int) testSuiteList.getFirst().length()];
				in.read(classBytes);
				simpleLoader = new SimpleClassLoader();
				Class<?> convertedClass = simpleLoader.convertToClass(classBytes);

				// Get all test cases (classes) referenced in the test suite
				SuiteClasses suiteAnnotation = convertedClass.getAnnotation(SuiteClasses.class);
				Class<?>[] classesInSuite = suiteAnnotation.value();

				// Iterate through all test cases in the test suite
				for (int i = 0; i < classesInSuite.length;) {

					// Access (don't remove) the first test in the list
					Class<?> c = classesInSuite[i];

					// Remove the first agent in the queue
					TestAgent agent = agents.remove();

					// Consider agents that are not alive (ie not running tests)
					if (!agent.isAlive()) {
						System.out.println("Assigning test: " + c.getName());

						// Point agent to new agent with the same server reference, but with the current test
						agent = new TestAgent(agent, c);

						// Have the agent execute the server's runTest method in its own thread
						agent.start();

						// Move to the next test case in the list
						i++;
					}

					// Add the agent back to the queue
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

	/** Debugging method to show remote communication between nodes */
	public String ping() throws RemoteException {
		return "===PING!===";
	}
}