package tlb;

import tlb.server.ServerInitializer;
import tlb.server.TlbServerInitializer;
import tlb.utils.SystemEnvironment;

/**
 * @understands launching tlb server
 */
public class ServerApp {
    public static void main(String[] args) {
        try {
            new TlbServerInitializer(new SystemEnvironment()).init().start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
