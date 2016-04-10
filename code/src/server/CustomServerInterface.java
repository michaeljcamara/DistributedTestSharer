package server;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

import org.junit.runner.Result;

public interface CustomServerInterface extends Remote {

	public Result runTest(File testFile) throws RemoteException;
	
}