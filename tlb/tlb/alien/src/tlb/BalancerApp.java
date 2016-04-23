package tlb;

import tlb.balancer.BalancerInitializer;
import tlb.server.ServerInitializer;
import tlb.utils.SystemEnvironment;

/**
 * @understands launching balancer server for a partition
 */
public class BalancerApp {
    public static void main(String[] args) {
        try {
            new BalancerInitializer(new SystemEnvironment()).init().start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
