package tlb.balancer;

import com.noelios.restlet.http.HttpConstants;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.restlet.util.WrapperResponse;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.orderer.TestOrderer;
import tlb.service.Server;
import tlb.splitter.TestSplitter;
import tlb.splitter.correctness.ValidationResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static junit.framework.Assert.fail;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class AllPartitionsExecutedAssertionTest {

    private AllPartitionsExecutedAssertion assertionResource;
    protected Request request;
    protected TestUtil.LogFixture logFixture;
    protected Response response;
    protected Representation representation;
    private Server talkToService;
    private Response mockResponse;
    private HashMap<String,Object> attrs;

    @Before
    public void setUp() throws ClassNotFoundException, IOException {
        Context context = new Context();

        request = mock(Request.class);
        attrs = new HashMap<String, Object>();
        attrs.put(HttpConstants.ATTRIBUTE_HEADERS, new Form());
        when(request.getAttributes()).thenReturn(attrs);

        final HashMap<String, Object> ctxMap = new HashMap<String, Object>();
        talkToService = mock(Server.class);
        ctxMap.put(TlbClient.TALK_TO_SERVICE, talkToService);
        context.setAttributes(ctxMap);

        mockResponse = mock(Response.class);
        response = new WrapperResponse(mockResponse) {
            @Override
            public void setEntity(Representation entity) {
                representation = entity;
            }
        };
        assertionResource = new AllPartitionsExecutedAssertion(context, request, response);
        logFixture = new TestUtil.LogFixture();
    }

    @Test
    public void shouldCompleteAssertionRequestSuccessfully_ifAllPartitionsExecuted() throws ResourceException, IOException {
        attrs.put(HttpConstants.ATTRIBUTE_HEADERS, new Form(Arrays.asList(new Parameter(TlbConstants.Balancer.TLB_MODULE_NAME_HEADER, "some-module"))));
        when(talkToService.verifyAllPartitionsExecutedFor("some-module")).thenReturn(new ValidationResult(ValidationResult.Status.OK, "all partitions executed indeed!"));

        Representation representation = assertionResource.represent(new Variant(MediaType.TEXT_PLAIN));
        assertThat(representation.getText(), is("all partitions executed indeed!"));
        verify(talkToService).verifyAllPartitionsExecutedFor("some-module");
        verifyNoMoreInteractions(talkToService);
    }

    @Test
    public void shouldFailAssertionRequest_ifAllPartitionsDidNotExecute() throws ResourceException, IOException {
        attrs.put(HttpConstants.ATTRIBUTE_HEADERS, new Form(Arrays.asList(new Parameter(TlbConstants.Balancer.TLB_MODULE_NAME_HEADER, "some-other-module"))));
        when(talkToService.verifyAllPartitionsExecutedFor("some-other-module")).thenReturn(new ValidationResult(ValidationResult.Status.FAILED, "alert: some partitions didn't execute!"));

        Representation representation = assertionResource.represent(new Variant(MediaType.TEXT_PLAIN));
        assertThat(representation.getText(), is("alert: some partitions didn't execute!"));
        verify(mockResponse).setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED);
        verify(talkToService).verifyAllPartitionsExecutedFor("some-other-module");
        verifyNoMoreInteractions(talkToService);
    }

    @Test
    public void shouldFailAssertionRequest_ifCompletenessAssertionIsNotSupported() throws ResourceException, IOException {
        attrs.put(HttpConstants.ATTRIBUTE_HEADERS, new Form(Arrays.asList(new Parameter(TlbConstants.Balancer.TLB_MODULE_NAME_HEADER, "some-other-module"))));
        when(talkToService.verifyAllPartitionsExecutedFor("some-other-module")).thenThrow(new UnsupportedOperationException("go server doesn't have correctness-check feature, use tlb server."));

        Representation representation = assertionResource.represent(new Variant(MediaType.TEXT_PLAIN));
        assertThat(representation.getText(), is("go server doesn't have correctness-check feature, use tlb server."));
        verify(mockResponse).setStatus(Status.SERVER_ERROR_NOT_IMPLEMENTED);
        verify(talkToService).verifyAllPartitionsExecutedFor("some-other-module");
        verifyNoMoreInteractions(talkToService);
    }
    
    @Test
    public void shouldAcceptPlainText() {
        assertThat(assertionResource.getVariants().get(0).getMediaType(), is(MediaType.TEXT_PLAIN));
    }

    @Test
    public void shouldNotAllowGet() {
        assertThat(assertionResource.allowGet(), is(true));
    }

    @Test
    public void shouldNotAllowRequestsWithMethodOtherThan_GET() {
        assertThat(assertionResource.allowPost(), is(false));
        assertThat(assertionResource.allowPut(), is(false));
        assertThat(assertionResource.allowDelete(), is(false));
        assertThat(assertionResource.allowOptions(), is(false));
        assertThat(assertionResource.allowHead(), is(false));
    }
}
