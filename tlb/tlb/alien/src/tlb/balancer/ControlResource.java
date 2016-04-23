package tlb.balancer;

import org.apache.log4j.Logger;
import tlb.TlbConstants;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.*;



/**
 * @understands balancer process(as in a regular user process with PID)
 */
public class ControlResource extends Resource {
    private static final Logger logger = Logger.getLogger(ControlResource.class.getName());

    private Thread suicideThread;

    public static enum Query {
        status {
            @Override
            Representation act(ControlResource controlResource) {
                return new StringRepresentation("RUNNING");
            }
        },
        suicide {
            @Override
            Representation act(ControlResource controlResource) {
                controlResource.suicideThread().start();
                return new StringRepresentation("HALTING");
            }
        };
        abstract Representation act(ControlResource controlResource);
    }

    public ControlResource(Context context, Request request, Response response) {
        super(context, request, response);
        getVariants().add(new Variant(MediaType.TEXT_PLAIN));
    }

    Thread suicideThread() {
        final Component component = (Component) getContext().getAttributes().get(TlbClient.APP_COMPONENT);
        if (suicideThread == null) {
            suicideThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        component.stop();
                        System.exit(0);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        return suicideThread;
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        return Query.valueOf(query()).act(this);
    }

    private String query() {
        return (String) getRequest().getAttributes().get(TlbConstants.Balancer.QUERY);
    }
}