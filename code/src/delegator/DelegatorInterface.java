package delegator;

import java.rmi.Remote;
import java.rmi.RemoteException;

import server.CustomServerInterface;

public interface DelegatorInterface extends Remote {

	public void rebindServer(CustomServerInterface remoteObject) throws RemoteException;

	public String ping() throws RemoteException;
}
