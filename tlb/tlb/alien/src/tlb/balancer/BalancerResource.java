package tlb.balancer;

import org.apache.log4j.Logger;
import org.restlet.data.*;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.orderer.TestOrderer;
import tlb.service.Server;
import tlb.splitter.TestSplitter;
import org.restlet.Context;
import org.restlet.resource.*;
import tlb.splitter.correctness.IncorrectBalancingException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static tlb.TlbConstants.Correctness.CORRECTNESS_CHECK_NOT_AVAILABLE;
import static tlb.TlbConstants.Correctness.CORRECTNESS_VALIDATION_FAILED;


/**
 * @understands subseting and ordering of set of suite names given
 */
public class BalancerResource extends Resource {
    private static final Logger logger = Logger.getLogger(BalancerResource.class.getName());

    private final TestOrderer orderer;
    private final TestSplitter splitter;
    private final Server server;

    public BalancerResource(Context context, Request request, Response response) {
        super(context, request, response);
        getVariants().add(new Variant(MediaType.TEXT_PLAIN));
        orderer = (TestOrderer) context.getAttributes().get(TlbClient.ORDERER);
        splitter = (TestSplitter) context.getAttributes().get(TlbClient.SPLITTER);
        server = (Server) context.getAttributes().get(TlbClient.TALK_TO_SERVICE);
    }

    @Override
    public void acceptRepresentation(Representation representation) throws ResourceException {
        List<TlbSuiteFile> suiteFiles = null;
        try {
            suiteFiles = TlbSuiteFileImpl.parse(representation.getText());
        } catch (IOException e) {
            final String message = "failed to read request";
            logger.warn(message, e);
            throw new RuntimeException(message, e);
        }
        String moduleName = RequestUtil.moduleName(getRequest());
        List<TlbSuiteFile> suiteFilesSubset = null;
        try {
            suiteFilesSubset = splitter.filterSuites(suiteFiles, moduleName);
        } catch (IncorrectBalancingException e) {
            setExceptionInResponse(e, new Status(Status.CLIENT_ERROR_EXPECTATION_FAILED, e, CORRECTNESS_VALIDATION_FAILED));
            return;
        } catch (UnsupportedOperationException e) {
            setExceptionInResponse(e, new Status(Status.SERVER_ERROR_NOT_IMPLEMENTED, e, CORRECTNESS_CHECK_NOT_AVAILABLE));
            return;
        }
        Collections.sort(suiteFilesSubset, orderer);
        server.publishSubsetSize(suiteFilesSubset.size());
        final StringBuilder builder = new StringBuilder();
        for (TlbSuiteFile suiteFile : suiteFilesSubset) {
            builder.append(suiteFile.dump());
        }
        getResponse().setEntity(new StringRepresentation(builder));
    }

    private void setExceptionInResponse(RuntimeException e, final Status status) {
        getResponse().setStatus(status);
        getResponse().setEntity(new StringRepresentation(e.getMessage()));
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
