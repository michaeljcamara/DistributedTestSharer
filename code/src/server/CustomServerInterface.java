package server;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

import org.junit.runner.Result;

/** Remote interface to allow binding to Java RMI registry*/
public interface CustomServerInterface extends Remote {

	public Result runTest(File testFile) throws RemoteException;

	public Result runTest(Class testClass) throws RemoteException;

	public void updateClassLoader() throws RemoteException;

	public String ping() throws RemoteException;

}