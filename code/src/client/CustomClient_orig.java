//package client;
//
//import java.io.DataInputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.lang.reflect.Method;
//import java.net.URISyntaxException;
//import java.net.URL;
//import java.rmi.server.RMIClassLoader;
//import java.util.Enumeration;
//
//import org.apache.commons.net.ftp.FTPClient;
//import org.junit.runner.JUnitCore;
//import org.junit.runner.Result;
//import org.junit.runner.notification.Failure;
//
//
//public class CustomClient_orig {
//
//	public static void main(String[] args) throws IOException, ClassNotFoundException, URISyntaxException {
//		
//		File testClass = new File("C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestSuiteHere/bin/testsuite/CorrectnessTest.class");
//		
//		FileInputStream in = new FileInputStream(testClass);
//		
//		byte[] classBytes = new byte[(int) testClass.length()];
//		in.read(classBytes);
//		
////		for(byte s : classBytes) {
////		System.out.println(s);
////		}
//	
////		-Djava.security.policy=rmi.policy
//		System.setSecurityManager(new SecurityManager());
//		
//		CustomClassLoader loader = new CustomClassLoader();
//		
//		Class convertedClass = loader.convertToClass(classBytes);
//		
////		ClassLoader rmiLoader = RMIClassLoader.getClassLoader(new File("C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestSuiteHere/bin/").toURI().toURL().toString());
////		ClassLoader rmiLoader = RMIClassLoader.getClassLoader((new File("C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestSuiteHere/bin/")).toURI().toURL().getPath());
//		String f = (new File("C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestSuiteHere/bin/")).toURI().toURL().toString();
//		System.out.println(f);
////		rmiLoader.loadClass("solver/Requirement")
//		
//		//Wrong name: solver/Requirement
////		Class testing = RMIClassLoader.loadClass((new File("C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestSuiteHere/bin/solver")).toURI().toURL().toString(), "Requirement");
//		
////		Class testing = RMIClassLoader.loadClass((new File("C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestSuiteHere/bin/")).toURI().toURL().toString(), "solver.Requirement");
//
//		ClassLoader rmiLoader = RMIClassLoader.getClassLoader((new File("C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestSuiteHere/bin/")).toURI().toURL().toString());
//		
//		Class missingClass = rmiLoader.loadClass("solver.Requirement");
//		
////		Class missingClass = null;
////		System.out.println(missingClass.getClassLoader());
//		
//		System.out.println(loader.getSystemResource(""));
//		
//		// Create new class file in local filesystem in correct directory
////		File targetDir = new File (loader.getSystemResource("").getPath() + missingClass.getPackage().getName());
////		targetDir.mkdirs();
////		File targetClass = new File(targetDir, missingClass.getSimpleName());
////		targetClass.createNewFile();
//		
////		FileOutputStream fstream = new FileOutputStream(new File(loader.getSystemResource("").getPath() + missingClass.getName().replace('.', '/')));
//		FileOutputStream asads = new FileOutputStream(loader.getSystemResource("").getPath() + missingClass.getName().replace('.', '/'));
//		
//		System.out.println(loader.getSystemResource("").getPath() + missingClass.getPackage().getName());
//		System.out.println(loader.getSystemResource("").getPath() + missingClass.getName().replace('.', '/'));
////		File fa = new File(loader.getSystemResource("").getPath() + "/test.txt");
//		File fa = new File(loader.getSystemResource("").getPath() + missingClass.getName().replace('.', '/'));
//		System.out.println(fa);
//		fa.mkdir();
//		fa.createNewFile();
//		
//		
//		System.out.println("---------------------");
////		System.out.println(convertedClass);
////		System.out.println(convertedClass.getName());
//		Method[] methods = null;
//		try{
//			methods = convertedClass.getDeclaredMethods();
//		}catch(java.lang.NoClassDefFoundError e) {System.out.println(1);}
//		
//		System.out.println();
//		for(Class c : convertedClass.getDeclaredClasses()) {
//			System.out.println(c);
//		}
//		
////		for(Method m : convertedClass.getMethods()) {
////			System.out.println(m);
////		}
//		
//		System.out.println("---------------------");
//
//		
//		
//		try {
//			for(Method m : convertedClass.getDeclaredMethods()) {
//				System.out.println(m.getName());
//			}
//		}
//		catch(NoClassDefFoundError e) {
//			// solver/Requirement
//			String missingClassString = e.getLocalizedMessage();
//			System.out.println("missing class: " + missingClassString);
//						
//			URL url = loader.getResource(missingClassString);
//			System.out.println(url);
////			Class checkClass = loader.getClass(missingClass);
////			System.out.println(checkClass);
//			Class checkLoadedClass = loader.getLoadedClass(missingClassString);
//			System.out.println(checkLoadedClass);
//		}
//
//		// Run full test suite for correctness and performance
//		Result r = JUnitCore.runClasses(convertedClass);
//
//		// Indicate overall results of testing 
//		System.out.println("Total tests run: " + r.getRunCount());
//		System.out.println("Number of successes: " + (r.getRunCount() - r.getFailureCount()));
//		System.out.println("Number of failures: " + r.getFailureCount());
//
//		// List all failures that have been generated 
//		for (Failure failure : r.getFailures()) {
//			System.out.println("\t" + failure.toString());
//		}
//				
//				
//	}
//}
