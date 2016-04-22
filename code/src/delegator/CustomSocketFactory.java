package delegator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Remote;
import java.rmi.server.RMISocketFactory;

public class CustomSocketFactory extends RMISocketFactory implements Remote {

    private int registryPort;

    public CustomSocketFactory(int registryPort) {
        this.registryPort = registryPort;
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {

        System.out.println("creating server socket on " + registryPort);

        if (port == 0) {
        	System.out.println("Port = 0 in CREATEserverSOCKET");
            return new ServerSocket(12345);
          }
        
        return new ServerSocket(12345);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        
        System.out.println("creating socket to host : " + host + " on port " + registryPort);
//        return new Socket(host, registryPort);
        
        if(port == 0) System.out.println("Port = 0 in CREATESOCKET");
        
        return new Socket(host, port);
    }
}