package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.rmi.Naming;
import java.util.EnumSet;
import java.util.stream.Stream;

import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import delegator.DelegatorInterface;

public class CustomClient {

	public static void main(String[] args) {
		try	{
									
//			String host = "192.168.0.103";
//			String host = "141.195.226.138";
			String host = "141.195.23.54";
			String port = "12345";
			System.setProperty("java.security.policy", "rmi.policy");
//			System.setSecurityManager(new SecurityManager());

			DelegatorInterface delegator = (DelegatorInterface) Naming.lookup("//" + host + ":" + port + "/Delegator");
			
//			File resourceFile = new File("sentByClient/SimpleResource.class");
			File resourceFile = new File("resources/");
			
			delegator.uploadResources(resourceFile);
			
//			File testFile = new File("sentByClient/SimpleTest.class");
//			File testFile = new File("sentByClient/AllTests.class");
//			File testFile = new File("sentByClient/CorrectnessTest.class");
			
//			Result result = delegator.uploadTestCases(testFile);
			Result result = delegator.runTests();
			
			// Indicate overall results of testing 
			System.out.println("Total tests run: " + result.getRunCount());
			System.out.println("Number of successes: " + (result.getRunCount() - result.getFailureCount()));
			System.out.println("Number of failures: " + result.getFailureCount());
	
			// List all failures that have been generated 
			for (Failure failure : result.getFailures()) {
				System.out.println("\t" + failure.toString());
			}

		}
		catch(Exception e) {e.printStackTrace();}
	}
}


//Path curDir = (new File("")).toPath();
//Path targetPath = (new File("C:/FileZilla/rel/").toPath());
////FileOutputStream out = new FileOutputStream(new File("tempDir/"));
//
//System.out.println(curDir.getNameCount());
////System.out.println(curDir.toUri());
//
//Stream<Path> fileTree = Files.walk(Paths.get("resources/"), Integer.MAX_VALUE);
//
//for(Path path : (Path[]) fileTree.toArray(Path[]::new)) {
////	System.out.println(path.getParent());
////	
////	System.out.println(path.getNameCount() + " " + path.toUri());
//////	path.relativize(curDir);
//////	System.out.println(path.toUri());
////	
//////	Path newPath = path.subpath(0, path.getNameCount()-1);
////	
////	int numDirs = path.getNameCount();
////	Path newPath = curDir;
////	if(numDirs > 1) {
////		newPath = path.subpath(0, numDirs - 1);
////	}
////	
////	System.out.println(newPath.toUri());
////	System.out.println();
//	
////	Files.copy(path, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
////	Files.copy(path.getParent(), out);
//	if(path.getNameCount() > 1) {
////		File file = path.getParent().toFile();
//		File file = path.toFile();
//		
//		System.out.println(file);
//		System.out.println(file.toURI());
//		if(file.isDirectory()) {
//			System.out.println();
//		}
//		
//		FileInputStream in = new FileInputStream(file);
//		byte[] fileBytes = new byte[(int) file.length()];
//		in.read(fileBytes);
////		FileOutputStream out = new FileOutputStream("tempDir/" + file);
//		FileOutputStream out = new FileOutputStream("C:/Users/Michael/Documents/CMPSC/CMPSC441/Eclipse/TestProject/tempDir/" + file);
//		out.write(fileBytes);
//		
//	}
//}