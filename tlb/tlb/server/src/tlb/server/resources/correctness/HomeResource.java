package tlb.server.resources.correctness;

import org.restlet.Context;
import org.restlet.Finder;
import org.restlet.Route;
import org.restlet.Router;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.*;
import org.restlet.util.RouteList;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Map;

/**
 * @understands rendering tlb-server home page to show help for new users
 */
public class HomeResource extends Resource {

    private static final String SEPERATOR = ":";
    private static final String PAD = " ";
    private static final String TAB = PAD + PAD + PAD + PAD;
    private static final String ENDL = "\n";

    private static final String HELP_PREFIX = "\n\nThis is Test Load Balancer(TLB) server. \n\nPlease refer to [ http://test-load-balancer.github.com ] for details, help and documentation.\n\n\n" +
            "Resources supported by TLB server:\n\n";

    private static final String HELP_SUFFIX = "\n\nNOTE: Most of these resources are used by TLB components internally, and are listed here only for debugging/verification purpose. " +
            "These are internal API(s) and may be changed/removed by TLB developers in any future release.\n";

    private static final String ENV_VARS_PREFIX = "\n\nEnvironment variables configured for this TLB server instance:\n\n";

    private static final String JVM_ARGS_PREFIX = "\n\n\nJVM arguments for this process: ";

    private static final char WRAPPER_CH = '\'';

    private static final String THREAD_DETAILS = "\n\n\nThreads";

    private static final String PAREN_START = "(";
    private static final String PAREN_END = ")";

    private static final String COUNT = "Count: ";
    private static final String TIME = "Time: ";
    public static final String GC_DETAILS = "\n\n\nGC details:\n\n";

    public HomeResource(Context context, Request request, Response response) {
        super(context, request, response);
        getVariants().add(new Variant(MediaType.TEXT_PLAIN));
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        RouteList routes = ((Router) this.getApplication().getRoot()).getRoutes();
        StringBuilder b = new StringBuilder(HELP_PREFIX);
        for (Route route : routes) {
            String pattern = route.getTemplate().getPattern();
            String resourceName = ((Finder) route.getNext()).getTargetClass().getSimpleName();
            b.append(TAB).append(pattern).append(PAD).append(SEPERATOR).append(PAD).append(resourceName).append(ENDL);
        }
        b.append(HELP_SUFFIX);

        b.append(JVM_ARGS_PREFIX).append(ManagementFactory.getRuntimeMXBean().getInputArguments()).append(ENDL);

        b.append(GC_DETAILS);
        List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
            b.append(TAB).append(garbageCollectorMXBean.getName()).append(TAB).append(COUNT).append(garbageCollectorMXBean.getCollectionCount()).append(TAB).append(TIME).append(garbageCollectorMXBean.getCollectionTime()).append(ENDL);
        }

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] allThreadIds = threadMXBean.getAllThreadIds();
        b.append(THREAD_DETAILS).append(PAREN_START).append(threadMXBean.getThreadCount()).append(PAREN_END).append(SEPERATOR).append(ENDL).append(ENDL);
        for (ThreadInfo threadInfo : threadMXBean.getThreadInfo(allThreadIds)) {
            b.append(TAB).append(threadInfo).append(ENDL);
        }

        b.append(ENV_VARS_PREFIX);
        for (Map.Entry<String, String> var : System.getenv().entrySet()) {
            b.append(TAB).append(var.getKey()).append(PAD).append(SEPERATOR).append(PAD).append(WRAPPER_CH).append(var.getValue()).append(WRAPPER_CH).append(ENDL);
        }

        return new StringRepresentation(b.toString());
    }
}
