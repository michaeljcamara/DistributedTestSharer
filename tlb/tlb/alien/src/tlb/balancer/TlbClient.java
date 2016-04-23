package tlb.balancer;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.Router;
import tlb.TlbConstants;

/**
 * @understands restlet tlb application for client that runs locally and offloads actual balancing logic from the client 
 */
public class TlbClient extends Application {
    public static final String SPLITTER = "SPLITTER";
    public static final String ORDERER = "ORDERER";
    public static final String TALK_TO_SERVICE = "TYPE_OF_SERVER";
    public static final String APP_COMPONENT = "APP_COMPONENT";

    public TlbClient(Context context) {
        super(context);
    }

    @Override
    public Restlet createRoot() {
        Router router = new Router(getContext());

        router.attach("/balance", BalancerResource.class);
        router.attach("/suite_time", SuiteTimeReporter.class);
        router.attach("/suite_result", SuiteResultReporter.class);
        router.attach(String.format("/control/{%s}", TlbConstants.Balancer.QUERY), ControlResource.class);
        router.attach("/assert_all_partitions_executed", AllPartitionsExecutedAssertion.class);

        return router;
    }
}