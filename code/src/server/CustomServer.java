package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import delegator.DelegatorInterface;
import delegator.SimpleClassLoader;

public class CustomServer extends UnicastRemoteObject implements CustomServerInterface {

	private static final long serialVersionUID = -3247691930817118343L;
	private static String host, ftpClassDir, ftpJarDir, ftpRootDir, userName, userPassword;
	private static int registryPort, ftpServerPort;
	private static FTPClient client;

	public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {

		// Immediately set the default class path to a new CustomClassLoader
		System.setProperty("java.system.class.loader", "server.CustomClassLoader");

		// Get the IP address for the delegator from the command line
		host = args[0];

		// Some nodes may need to assign their own rmi server hostname with their
		// ip address to allow communication with Delegator
		if (args.length > 1) {
			String thisRMIhost = args[1];
			System.setProperty("java.rmi.server.hostname", thisRMIhost);
		}

		// Initialize various vars
		System.out.println("Using host address: " + host);
		registryPort = 12345;
		ftpServerPort = 12346;
		ftpClassDir = "resources/bin/";
		ftpJarDir = "resources/lib/";
		ftpRootDir = "resources/";
		userName = "user";
		userPassword = "user";

		// Set a very permissive RMI policy
		System.setProperty("java.security.policy", "rmi.policy");

		// Retrieve a remote reference to the Delegator stored in the registry
		DelegatorInterface delegator = (DelegatorInterface) Naming.lookup("//" + host + ":" + registryPort + "/Delegator");

		// PING the delegator to ensure patent communication
		System.out.println(delegator.ping());

		// Bind an instance of a CustomServer to the Delegator's registry
		CustomServer server = new CustomServer();
		delegator.rebindServer((CustomServerInterface) server);
	}

	protected CustomServer() throws RemoteException {
		super();
	}

	/** Update this server's CustomClassLoader to points to the files on the Delegator,
	 * accessed through its FTP server
	 */
	public void updateClassLoader() {

		System.out.println("BEGIN UPDATECLASSLOADER");
		try {
			// Use the Apache Commons Net library to facilitate FTP communication
			client = new FTPClient();
			client.connect(host, ftpServerPort);
			client.login(userName, userPassword);
			client.setKeepAlive(true);

			// Set classpath to the class directory for the resources on the Delegator
			CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://" + userName + ":" + userPassword + "@" + host + ":" + ftpServerPort + "/" + ftpClassDir));

			// Set classpath to each jar file for the resources on the Delegator
			FTPFile[] jarFiles = client.listFiles(ftpJarDir);
			for (int i = 0; i < jarFiles.length; i++) {
				CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://" + userName + ":" + userPassword + "@" + host + ":" + ftpServerPort + "/" + ftpJarDir + jarFiles[i].getName()));
			}

			// Add any supplemental configuration files needed to run subsequent tests
			// (located immediately in the ftpserver/ directory on the Delegator node)
			FTPFile[] files = client.listFiles();
			for (int i = 0; i < files.length; i++) {

				FTPFile file = files[i];
				if (!file.getName().equals("resources")) {
					createResources(file, "");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("END UPDATECLASSLOADER");
	}

	/** This method will recursively go through the home directory from the FTP server
	 * and retrieve and recreate the directory structure for all files (NOT in the main
	 * resources directory; ie only supplemental, non-class files needed to execute the
	 * given test cases)
	 */
	private void createResources(FTPFile file, String parentDir) {
		try {
			if (file.isDirectory()) {
				File targetFile = new File(parentDir + file.getName() + "/");
				targetFile.mkdir();
				for (FTPFile subDir : client.listFiles(file.getName())) {
					createResources(subDir, parentDir + file.getName() + "/");
				}
			}
			else {
				client.retrieveFile(parentDir + file.getName(), new FileOutputStream(parentDir + file.getName()));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Execute the test case (formatted as a Class object) retrieved from the Delegator */
	public Result runTest(Class testClass) {

		System.out.println("**Running test: " + testClass.getName());
		try {
			// Run the test case using JUnit, storing and sending the result back to the Delegator
			Result result = JUnitCore.runClasses(testClass);

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/** Execute the test case (formatted as a File object) retrieved from the Delegator */
	public Result runTest(File testFile) {

		System.out.println("**Running test: " + testFile.getName());

		try {
			// Convert the File to bytes, and then to a Class object
			FileInputStream in = new FileInputStream(testFile);
			byte[] classBytes = new byte[(int) testFile.length()];
			in.read(classBytes);
			SimpleClassLoader loader = new SimpleClassLoader();
			Class convertedClass = loader.convertToClass(classBytes);

			// Run the test case using JUnit, storing and sending the result back to the Delegator
			Result result = JUnitCore.runClasses(convertedClass);

			return result;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/** Debugging method to ensure patent communication between server and delegator*/
	public String ping() throws RemoteException {
		return "==========PING FROM SERVER==============";
	}
}