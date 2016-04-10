package delegator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.junit.runner.Result;

import server.CustomServer;
import server.CustomServerInterface;

public class Delegator extends UnicastRemoteObject implements DelegatorInterface{

	protected Delegator() throws RemoteException {
		super();
	}

	@Override
	public void uploadResources(File file) throws RemoteException {

		System.out.println(file);
		System.out.println(file.getPath());
		System.out.println(file.getName());
		System.out.println(file.toURI().toString());
		System.out.println(file.length());

		try {
			FileInputStream in = new FileInputStream(file);
			byte[] fileBytes = new byte[(int) file.length()];
			in.read(fileBytes);

			FileOutputStream out = new FileOutputStream("C:/FileZilla/bin/" + file.getName());
			out.write(fileBytes);

			//			FTPClient client = new FTPClient();
			//			client.connect(InetAddress.getByName("127.0.0.1"), 81);
			//			client.login("user", "user");
			//			client.storeFile("testFileOnFTPServer.file", in);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Result uploadTestCases(File file) throws RemoteException {

		try{
			Registry registry = LocateRegistry.getRegistry(1099);

			String[] serverNames = registry.list();
			CustomServerInterface[] serverObjects = new CustomServerInterface[serverNames.length - 1];
			for(int i = 0; i < serverObjects.length; i++) {
				Object serverObject = registry.lookup(serverNames[i]);
				if(serverObject instanceof CustomServerInterface) {
					serverObjects[i] = (CustomServerInterface) registry.lookup(serverNames[i]);
					System.out.println(serverNames[i]);
				}
			}
			
			System.out.println(serverObjects.length);
			
			Result result = serverObjects[0].runTest(file);
			return result;
		}
		catch(Exception e) {return null;}
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
		System.out.println("IN DELEGATOR--REBIND.  Before rebind:");
		Registry registry = LocateRegistry.getRegistry(1099);
		
		for(String s : registry.list()) {
			System.out.println(s);
		}
		
		registry.rebind("CustomServer_" + registry.list().length, remoteObject);
		
		System.out.println("AFTER REBIND: ");
		for(String s : registry.list()) {
			System.out.println(s);
		}
		}
		catch(Exception e) {e.printStackTrace();}
		
	}
}