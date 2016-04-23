package tlb.server.resources.correctness;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import tlb.TlbConstants;
import tlb.domain.PartitionIdentifier;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.PartitionRecordRepo;
import tlb.server.repo.SetRepo;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VerifyPartitionCompletenessResourceTest {
    private Context context;
    private Request request;
    private VerifyPartitionCompletenessResource resource;
    private EntryRepoFactory repoFactory;
    private PartitionRecordRepo repo;
    private Response response;
    private Representation representationGiven;
    private HashMap<String, Object> reqAttrMap;


    @Before
    public void setUp() throws IOException {
        context = new Context();
        request = mock(Request.class);
        repoFactory = mock(EntryRepoFactory.class);
        repo = new PartitionRecordRepo();
        repo.setIdentifier("foo-bar-baz");
        context.setAttributes(Collections.singletonMap(TlbConstants.Server.REPO_FACTORY, (Object) repoFactory));
        reqAttrMap = new HashMap<String, Object>();
        reqAttrMap.put(TlbConstants.Server.REQUEST_NAMESPACE, "family_name");
        reqAttrMap.put(TlbConstants.Server.LISTING_VERSION, "version-string");
        reqAttrMap.put(TlbConstants.Server.MODULE_NAME, "my-module");
        when(request.getAttributes()).thenReturn(reqAttrMap);
        when(repoFactory.createPartitionRecordRepo("family_name", "version-string", "my-module")).thenReturn(repo);
        response = mock(Response.class);

        representationGiven = null;

        resource = new VerifyPartitionCompletenessResource(context, request, response);
    }

    @Test
    public void shouldAllowGetRequests() {
        assertThat(resource.allowGet(), is(true));
    }

    @Test
    public void shouldReturn_OK_whenAllPartitionsHaveRun() throws ResourceException, IOException {
        repo.subsetReceivedFromPartition(new PartitionIdentifier(1, 3));
        repo.subsetReceivedFromPartition(new PartitionIdentifier(3, 3));
        repo.subsetReceivedFromPartition(new PartitionIdentifier(2, 3));
        Representation represent = resource.represent(new Variant(MediaType.TEXT_PLAIN));
        verify(response).setStatus(Status.SUCCESS_OK);
        assertThat(represent.getText(), is("All partitions executed.\n"));
    }

    @Test
    public void shouldReturn_ExpectationFailedError_whenSomePartitionsHave_NOT_Run() throws ResourceException, IOException {
        repo.subsetReceivedFromPartition(new PartitionIdentifier(3, 5));
        repo.subsetReceivedFromPartition(new PartitionIdentifier(2, 5));
        repo.subsetReceivedFromPartition(new PartitionIdentifier(5, 5));
        Representation represent = resource.represent(new Variant(MediaType.TEXT_PLAIN));
        verify(response).setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED);
        assertThat(represent.getText(), is("- [1, 4] of total 5 partition(s)(for module my-module) were not executed. This violates collective exhaustion. Please check your partition configuration for potential mismatch in total-partitions value and actual 'number of partitions' configured and check your build process triggering mechanism for failures.\n"));
    }
}
