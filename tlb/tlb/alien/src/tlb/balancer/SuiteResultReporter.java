package tlb.balancer;

import org.apache.log4j.Logger;
import tlb.domain.SuiteResultEntry;
import tlb.service.Server;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import java.io.IOException;
import java.util.List;


/**
 * @understands posting the suite result entry to server
 */
public class SuiteResultReporter extends Resource {
    private static final Logger logger = Logger.getLogger(SuiteResultReporter.class.getName());

    protected Server server;

    public SuiteResultReporter(Context context, Request request, Response response) {
        super(context, request, response);
        getVariants().add(new Variant(MediaType.TEXT_PLAIN));
        server = (Server) context.getAttributes().get(TlbClient.TALK_TO_SERVICE);
    }

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        try {
            final List<SuiteResultEntry> entries = SuiteResultEntry.parse(entity.getText());
            for (SuiteResultEntry entry : entries) {
                server.testClassFailure(entry.getName(), entry.hasFailed());
            }
        } catch (IOException e) {
            logger.warn(String.format("could not report test result: '%s'", e.getMessage()), e);
            throw new RuntimeException(e);
        }
    }
    @Override
    public boolean allowGet() {
        return false;
    }

    @Override
    public boolean allowPost() {
        return true;
    }
}
