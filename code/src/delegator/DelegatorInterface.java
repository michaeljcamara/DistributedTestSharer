package delegator;

import java.rmi.Remote;
import java.rmi.RemoteException;

import server.CustomServerInterface;

/** Remote interface to allow binding to Java RMI registry*/
public interface DelegatorInterface extends Remote {
	public void rebindServer(CustomServerInterface remoteObject) throws RemoteException;

	public String ping() throws RemoteException;
}
