package delegator;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

import org.junit.runner.Result;

import server.CustomServer;
import server.CustomServerInterface;

public interface DelegatorInterface extends Remote {

	public void uploadResources(File file) throws RemoteException;
	
	public Result uploadTestCases(File file) throws RemoteException;
	
//	public void rebindServer(Remote remoteObject) throws RemoteException;
	public void rebindServer(CustomServerInterface remoteObject) throws RemoteException;
	
	public String ping() throws RemoteException;

	
}
