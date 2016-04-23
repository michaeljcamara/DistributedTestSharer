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

	private static String host, ftpClassDir, ftpJarDir, ftpRootDir, userName, userPassword;

	private static int registryPort, ftpServerPort;

	private static FTPClient client;

	protected CustomServer() throws RemoteException {
		super();
	}

	public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {

		System.setProperty("java.system.class.loader", "server.CustomClassLoader");

		// host = "192.168.0.103";
		host = args[0];
		System.out.println("Using host address: " + host);
		registryPort = 12345;
		ftpServerPort = 12346;
		ftpClassDir = "resources/bin/";
		ftpJarDir = "resources/lib/";
		ftpRootDir = "resources/";
		userName = "user";
		userPassword = "user";

		System.setProperty("java.security.policy", "rmi.policy");
		// System.setProperty("java.rmi.server.disableHttp", "false");

		DelegatorInterface delegator = (DelegatorInterface) Naming.lookup("//" + host + ":" + registryPort + "/Delegator");
		// DelegatorInterface delegator = (DelegatorInterface) registry.lookup("Delegator");
		CustomServer server = new CustomServer();

		System.out.println(delegator.ping());
		delegator.rebindServer((CustomServerInterface) server);
	}

	public void updateClassLoader() {

		System.out.println("BEGIN UPDATECLASSLOADER");
		try {
			client = new FTPClient();
			client.connect(host, ftpServerPort);
			client.login(userName, userPassword);
			client.setKeepAlive(true);

			CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://" + userName + ":" + userPassword + "@" + host + ":" + ftpServerPort + "/" + ftpClassDir));
			System.out.println(ftpJarDir);

			FTPFile[] jarFiles = client.listFiles(ftpJarDir);

			for (int i = 0; i < jarFiles.length; i++) {
				CustomClassLoader.addURLToSystemClassLoader(new URL("ftp://" + userName + ":" + userPassword + "@" + host + ":" + ftpServerPort + "/" + ftpJarDir + jarFiles[i].getName()));
			}

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

	private void createResources(FTPFile file, String parentDir) {
		try {
			String currentDir = System.getProperty("user.dir");
			if (file.isDirectory()) {
				// File targetFile = new File(currentDir + "/" + parentDir + file.getName() + "/");
				File targetFile = new File(parentDir + file.getName() + "/");
				targetFile.mkdir();
				for (FTPFile subDir : client.listFiles(file.getName())) {
					createResources(subDir, parentDir + file.getName() + "/");
				}
			}
			else {

				// boolean done = client.retrieveFile(parentDir + file.getName(), new
				// FileOutputStream(currentDir + "/" + parentDir + file.getName()));
				boolean done = client.retrieveFile(parentDir + file.getName(), new FileOutputStream(parentDir + file.getName()));
				// System.out.println("Done?: " + done);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String ping() throws RemoteException {
		return "==========PING FROM SERVER==============";
	}

	public Result runTest(Class testClass) {

		System.out.println("**Running test: " + testClass.getName());

		try {

			Result result = JUnitCore.runClasses(testClass);

			// Indicate overall results of testing
			// System.out.println("Total tests run: " + result.getRunCount());
			// System.out.println("Number of successes: " + (result.getRunCount() - result.getFailureCount()));
			// System.out.println("Number of failures: " + result.getFailureCount());
			//
			// // List all failures that have been generated
			// for (Failure failure : result.getFailures()) {
			// System.out.println("EXCEPTION" + failure.getException());
			// System.out.println("TRACE: " + failure.getTrace());
			// System.out.println("MESSAGE: " + failure.getMessage());
			// System.out.println("HEADER: " + failure.getTestHeader());
			// System.out.println("DESCRIPTION: " + failure.getDescription());
			// }

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Result runTest(File testFile) {

		System.out.println("**Running test: " + testFile.getName());

		try {
			FileInputStream in = new FileInputStream(testFile);
			byte[] classBytes = new byte[(int) testFile.length()];
			in.read(classBytes);

			SimpleClassLoader loader = new SimpleClassLoader();
			Class convertedClass = loader.convertToClass(classBytes);
			System.out.println("ConvertedClass: " + convertedClass);

			Result result = JUnitCore.runClasses(convertedClass);

			// Indicate overall results of testing
			// System.out.println("Total tests run: " + result.getRunCount());
			// System.out.println("Number of successes: " + (result.getRunCount() - result.getFailureCount()));
			// System.out.println("Number of failures: " + result.getFailureCount());

			// List all failures that have been generated
			// for (Failure failure : result.getFailures()) {
			// System.out.println("EXCEPTION" + failure.getException());
			// System.out.println("TRACE: " + failure.getTrace());
			// System.out.println("MESSAGE: " + failure.getMessage());
			// System.out.println("HEADER: " + failure.getTestHeader());
			// System.out.println("DESCRIPTION: " + failure.getDescription());
			// }

			return result;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}

// public Result runTest(byte[] testBytes) {
//
// System.out.println("**BEGINNING runTest(BYTES)**");
//
// try {
//
// for (URL url : ((URLClassLoader) URLClassLoader.getSystemClassLoader()).getURLs()) {
// System.out.println(url);
// }
//
// // Thread.sleep(1000);
//
// // ((URLClassLoader) URLClassLoader.getSystemClassLoader()).defineClass("aa", testBytes,
// // 0, testBytes.length-1);
// // ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
// // Class<ClassLoader> classLoaderClass = ClassLoader.class;
// //
// // for(Method m : classLoaderClass.getDeclaredMethods()) {
// // System.out.print(m.getName() + " ");
// //
// // for(Class c : m.getParameterTypes()) {
// // System.out.print(c.getName() + " ");
// // }
// //
// // System.out.println(", return: " + m.getReturnType().getName());
// //
// // }
// //
// // Method method = classLoaderClass.getDeclaredMethod("defineClass", new
// // Class[]{byte[].class, int.class, int.class});
// // method.setAccessible(true);
// // System.out.println("CUSTOMMETHOD: " + method.getName());
// // for(Class c : method.getParameterTypes()) {
// // System.out.print(c.getName() + " ");
// // }
// // System.out.println(", return: " + method.getReturnType().getName());
// // Class convertedClass = (Class) method.invoke(systemClassLoader, new
// // Object[]{testBytes, 0, testBytes.length-1});
//
// // FTPClient client = new FTPClient();
// //
// // System.out.println("1");
// // client.connect(host, ftpServerPort);
// // System.out.println("1");
// // client.login(userName, userPassword);
// // System.out.println("1");
//
// // System.setProperty("user.dir", "ftp://user:user@" + host + ":" + ftpServerPort +
// // "/");
// // System.setProperty("java.class.path", "ftp://user:user@" + host + ":" + port +
// // "/bin/");
// // System.setProperty("user.dir", ftpRootDir);
//
// // CustomClassLoader loader = (CustomClassLoader) URLClassLoader.getSystemClassLoader();
// // Class convertedClass = CustomClassLoader.convertToClass(testFile.getName(),
// // classBytes, 0, classBytes.length, null);
//
// // Class convertedClass =
// // CustomClassLoader.findClassWithSystemClassLoader("testsuite.CorrectnessTest");
// // Class convertedClass =
// // CustomClassLoader.findClassWithSystemClassLoader("org.schemaanalyst.unittest.AllTests");
//
// // client.retrieveFile("resources/tests/AllTests.class", local)
//
// // CustomClassLoader loader2 = new
// // CustomClassLoader(ClassLoader.getSystemClassLoader());
// CustomClassLoader loader2 = new CustomClassLoader(((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs());
//
// FileOutputStream out = null;
// client.retrieveFile(ftpClassDir + "testSuite/CorrectnessTest.class", out);
// byte[] testBytes2 = new byte[] {};
// out.write(testBytes2);
// Class convertedClass = loader2.convertToClass(testBytes2);
//
// System.out.println(convertedClass.getComponentType());
// System.out.println(convertedClass.getName());
// System.out.println(convertedClass.getSimpleName());
// System.out.println(convertedClass.getTypeName());
// System.out.println(convertedClass.getPackage());
// System.out.println(convertedClass.getSuperclass());
//
// // SimpleClassLoader loader = new SimpleClassLoader();
// // Class convertedClass = loader.convertToClass(testBytes);
// // Class convertedClass = loader2.convertToClass(testBytes);
// System.out.println("ConvertedClass: " + convertedClass);
// System.out.println("convertedClass CL: " + convertedClass.getClassLoader().toString());
// System.out.println("system CL: " + CustomClassLoader.getSystemClassLoader().toString());
//
// System.out.println("Current dir: " + System.getProperty("user.dir"));
//
// Result result = JUnitCore.runClasses(convertedClass);
// System.out.println("Result: ");
// System.out.println(result);
//
// // Indicate overall results of testing
// System.out.println("Total tests run: " + result.getRunCount());
// System.out.println("Number of successes: " + (result.getRunCount() - result.getFailureCount()));
// System.out.println("Number of failures: " + result.getFailureCount());
//
// // List all failures that have been generated
// for (Failure failure : result.getFailures()) {
// System.out.println("EXCEPTION" + failure.getException());
// System.out.println("\t" + failure.toString());
// System.out.println("MESSAGE: " + failure.getMessage());
// System.out.println("HEADER: " + failure.getTestHeader());
// // System.out.println("TRACE: " + failure.getTrace());
// System.out.println("DESCRIPTION: " + failure.getDescription());
// }
//
// System.out.println("============================");
//
// // client.disconnect();
//
// // return result;
// return null;
//
// } catch (Exception e) {
// e.printStackTrace();
// System.out.println("EXCEPTION!!!");
// System.out.println(e.getCause());
// return null;
// }
// }
// }