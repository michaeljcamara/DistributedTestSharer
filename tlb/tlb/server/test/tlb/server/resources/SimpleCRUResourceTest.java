package tlb.server.resources;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.Entry;
import tlb.domain.SubsetSizeEntry;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SubsetSizeRepo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class SimpleCRUResourceTest {
    private SimpleCRUResource simpleCRUResource;
    private Context context;
    private EntryRepoFactory repoFactory;
    private Request request;
    private HashMap<String,Object> attributeMap;
    private SubsetSizeRepo repo;
    private TestUtil.LogFixture logFixture;

    static class TestSimpleCRUResource extends SimpleCRUResource<SubsetSizeRepo> {
        public TestSimpleCRUResource(Context context, Request request, Response response) {
            super(context, request, response);
        }

        @Override
        protected SubsetSizeRepo getRepo(EntryRepoFactory repoFactory, String namespace) throws IOException, ClassNotFoundException {
            return repoFactory.createSubsetRepo(namespace, EntryRepoFactory.LATEST_VERSION);
        }

        @Override
        protected Entry parseEntry(Representation entity) throws IOException {
            return new SubsetSizeEntry(Integer.parseInt(entity.getText()));
        }

        @Override
        protected List<SubsetSizeEntry> parseEntries(Representation entity) throws IOException {
            return SubsetSizeEntry.parse(entity.getText());
        }
    }

    @Before
    public void setUp() throws ClassNotFoundException, IOException {
        context = new Context();
        repoFactory = mock(EntryRepoFactory.class);
        repo = mock(SubsetSizeRepo.class);
        context.setAttributes(Collections.singletonMap(TlbConstants.Server.REPO_FACTORY, (Object) repoFactory));
        request = mock(Request.class);
        when(request.getOriginalRef()).thenReturn(new Reference("http://foo:7019/quux/foo/bar"));
        attributeMap = new HashMap<String, Object>();
        attributeMap.put(TlbConstants.Server.REQUEST_NAMESPACE, "identifier");
        when(request.getAttributes()).thenReturn(attributeMap);
        when(repoFactory.createSubsetRepo("identifier", EntryRepoFactory.LATEST_VERSION)).thenReturn(repo);
        simpleCRUResource = new TestSimpleCRUResource(context, request, mock(Response.class));
        logFixture = new TestUtil.LogFixture();
    }

    @Test
    public void shouldAcceptPlainText() {
        assertThat(simpleCRUResource.getVariants().get(0).getMediaType(), is(MediaType.TEXT_PLAIN));
    }

    @Test
    public void shouldRenderAllRecordsForGivenNamespace() throws ResourceException, IOException {
        when(repo.list()).thenReturn(Arrays.asList(new SubsetSizeEntry(10), new SubsetSizeEntry(12), new SubsetSizeEntry(15)));
        Representation actualRepresentation = simpleCRUResource.represent(new Variant(MediaType.TEXT_PLAIN));
        assertThat(actualRepresentation.getText(), is("10\n12\n15\n"));
        verify(repo).list();
    }
        
    @Test
    public void shouldThrowExceptionRaisedByRepoWhileListing() throws ResourceException, IOException, ClassNotFoundException {
        @SuppressWarnings({"ThrowableInstanceNeverThrown"}) final RuntimeException listingException = new RuntimeException("test exception");
        when(repo.list()).thenThrow(listingException);
        try {
            simpleCRUResource.represent(new Variant(MediaType.TEXT_PLAIN));
            fail("list exception should have been propagated");
        } catch(Exception e) {
            assertThat((RuntimeException) e.getCause(), sameInstance(listingException));
        }
    }

    @Test
    public void shouldAddRecords() throws ResourceException {
        StringRepresentation representation = new StringRepresentation("14");
        simpleCRUResource.acceptRepresentation(representation);
        verify(repo).add(new SubsetSizeEntry(14));
    }
    
    @Test
    public void shouldPropagateExceptionIfAddFails() throws ResourceException, IOException {
        Representation representation = mock(Representation.class);
        @SuppressWarnings({"ThrowableInstanceNeverThrown"}) IOException exception = new IOException("test exception");
        when(representation.getText()).thenThrow(exception);
        logFixture.startListening();
        try {
            simpleCRUResource.acceptRepresentation(representation);
            fail("should have propagated exception");
        } catch (Exception e) {
            assertThat((IOException) e.getCause(), sameInstance(exception));
        }
        logFixture.assertHeardException(exception);
        logFixture.assertHeard("addition of representation failed for ");
        logFixture.stopListening();
    }

    @Test
    public void shouldPropagateExceptionIfUpdateFails() throws ResourceException, IOException {
        Representation representation = mock(Representation.class);
        IOException exception = new IOException("test exception");
        when(representation.getText()).thenThrow(exception);
        logFixture.startListening();
        try {
            simpleCRUResource.storeRepresentation(representation);
            fail("should have propagated exception");
        } catch (Exception e) {
            assertThat((IOException) e.getCause(), sameInstance(exception));
        }
        logFixture.assertHeardException(exception);
        logFixture.assertHeard("update of representation failed for ");
        logFixture.stopListening();
    }

    @Test
    public void shouldUpdateRecords() throws ResourceException {
        StringRepresentation representation = new StringRepresentation("14\n20\n3\n");
        simpleCRUResource.storeRepresentation(representation);
        verify(repo).updateAll(Arrays.asList(new SubsetSizeEntry(14), new SubsetSizeEntry(20), new SubsetSizeEntry(3)));
    }

    @Test
    public void shouldSupportTextPlain() {
        assertThat(simpleCRUResource.getVariants().get(0).getMediaType(), is(MediaType.TEXT_PLAIN));
    }

    @Test
    public void shouldLogExceptionMessageIfFailsToGetRepo() throws ClassNotFoundException, IOException {
        repoFactory = mock(EntryRepoFactory.class);
        when(repoFactory.createSubsetRepo("identifier", EntryRepoFactory.LATEST_VERSION)).thenThrow(new IOException("test exception"));
        context.setAttributes(Collections.singletonMap(TlbConstants.Server.REPO_FACTORY, (Object) repoFactory));
        logFixture.startListening();
        try {
            simpleCRUResource = new TestSimpleCRUResource(context, request, mock(Response.class));
            fail("should have bubbled up exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("java.io.IOException: test exception"));
        }
        logFixture.stopListening();
        logFixture.assertHeardException(new IOException("test exception"));
        logFixture.assertHeard("Failed to get repo for '/quux/foo/bar'");
    }
}
