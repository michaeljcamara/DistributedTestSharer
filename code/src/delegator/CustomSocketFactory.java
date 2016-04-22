package delegator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;

public class CustomSocketFactory extends RMISocketFactory {

    private int registryPort;

    public CustomSocketFactory(int registryPort) {
        this.registryPort = registryPort;
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {

        if (port == 0) {
            port = registryPort;
        }

        return new ServerSocket(port);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        if (port == 0) {
            port = registryPort;
        }

        return new Socket(host, port);
    }

}
