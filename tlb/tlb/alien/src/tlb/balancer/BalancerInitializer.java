package tlb.balancer;

import tlb.TlbConstants;
import tlb.factory.TlbBalancerFactory;
import tlb.factory.TlbFactory;
import tlb.orderer.TestOrderer;
import tlb.server.ServerInitializer;
import tlb.splitter.AbstractTestSplitter;
import tlb.utils.SystemEnvironment;
import org.restlet.Context;
import org.restlet.Restlet;

import java.util.HashMap;

/**
 * @understands initializing Balancer tlb restlet app
 */
public class BalancerInitializer extends ServerInitializer {
    private final SystemEnvironment env;

    public BalancerInitializer(SystemEnvironment env) {
        this.env = env;
    }

    @Override
    protected int appPort() {
        return Integer.parseInt(env.val(TlbConstants.Balancer.TLB_BALANCER_PORT));
    }

    @Override
    public Restlet application() {
        HashMap<String, Object> appMap = new HashMap<String, Object>();
        appMap.put(TlbClient.SPLITTER, TlbBalancerFactory.getCriteria(env.val(AbstractTestSplitter.TLB_SPLITTER), env));
        appMap.put(TlbClient.ORDERER, TlbBalancerFactory.getOrderer(env.val(TestOrderer.TLB_ORDERER), env));
        appMap.put(TlbClient.TALK_TO_SERVICE, TlbFactory.getTalkToService(env));
        appMap.put(TlbClient.APP_COMPONENT, init());
        Context applicationContext = new Context();
        applicationContext.setAttributes(appMap);
        return new TlbClient(applicationContext);
    }
}
