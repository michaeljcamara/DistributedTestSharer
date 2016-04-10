package delegator;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class CustomRegistry {

	public static void main(String args[]) throws RemoteException {

		Registry registry = LocateRegistry.createRegistry(1099);			

		Delegator delegator = new Delegator();

		registry.rebind("Delegator", delegator);
		//			
		//			Registry r = LocateRegistry.getRegistry("localhost");


	}

}