package delegator;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class CustomRegistry {

	public static void main(String args[]) throws RemoteException {
		
		int port = 12345; 
				
		System.setSecurityManager(new SecurityManager());
		Registry registry = LocateRegistry.createRegistry(port);

		Delegator delegator = new Delegator();

		registry.rebind("Delegator", delegator);
	}

}