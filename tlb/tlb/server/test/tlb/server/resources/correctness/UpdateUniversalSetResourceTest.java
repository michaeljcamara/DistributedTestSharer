package tlb.server.resources.correctness;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import tlb.TlbConstants;
import tlb.domain.SuiteNamePartitionEntry;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SetRepo;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class UpdateUniversalSetResourceTest {
    private Context context;
    private Request request;
    private UpdateUniversalSetResource resource;
    private EntryRepoFactory repoFactory;
    private SetRepo repo;
    private Response response;
    private Representation representationGiven;
    private HashMap<String,Object> reqAttrMap;

    @Before
    public void setUp() throws IOException {
        context = new Context();
        request = mock(Request.class);
        repoFactory = mock(EntryRepoFactory.class);
        repo = new SetRepo();
        repo.setIdentifier("foo-bar-baz");
        context.setAttributes(Collections.singletonMap(TlbConstants.Server.REPO_FACTORY, (Object) repoFactory));
        reqAttrMap = new HashMap<String, Object>();
        reqAttrMap.put(TlbConstants.Server.REQUEST_NAMESPACE, "family_name");
        reqAttrMap.put(TlbConstants.Server.LISTING_VERSION, "version-string");
        reqAttrMap.put(TlbConstants.Server.MODULE_NAME, "my-module");
        when(request.getAttributes()).thenReturn(reqAttrMap);
        when(repoFactory.createUniversalSetRepo("family_name", "version-string", "my-module")).thenReturn(repo);
        response = mock(Response.class);

        representationGiven = null;

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                representationGiven = (Representation) invocationOnMock.getArguments()[0];
                return null;
            }
        }).when(response).setEntity(any(Representation.class));

        resource = new UpdateUniversalSetResource(context, request, response);
    }

    @Test
    public void shouldAllow_ONLY_Post() {
        assertThat(resource.allowPost(), is(true));
        assertThat(resource.allowPut(), is(false));
        assertThat(resource.allowGet(), is(false));
        assertThat(resource.allowHead(), is(false));
        assertThat(resource.allowDelete(), is(false));
    }

    @Test
    public void shouldCreateUniversalSetRepo_whenGivenTheListingByFirstPartition() throws ResourceException, IOException {
        resource.acceptRepresentation(new StringRepresentation("foo.bar.Baz.class\nbar.baz.Bang.class\nbaz.bang.Quux.class"));
        assertThat(repo.list().size(), is(3));
        assertThat(repo.list(), hasItems(new SuiteNamePartitionEntry("foo.bar.Baz.class"), new SuiteNamePartitionEntry("bar.baz.Bang.class"), new SuiteNamePartitionEntry("baz.bang.Quux.class")));
        verify(response).setStatus(Status.SUCCESS_CREATED);
    }

    @Test
    public void shouldMatchUniversalSetRepo_whenGivenTheListingByAnyPartitionAfterFirst() throws ResourceException, IOException {
        String suites = "foo.bar.Baz.class\nbar.baz.Bang.class\nbaz.bang.Quux.class";
        synchronized (repo) {
            StringReader stringReader = new StringReader(suites);
            repo.loadAndMarkDirty(stringReader);
        }
        List<SuiteNamePartitionEntry> listBefore2ndPartitionPosting = new ArrayList<SuiteNamePartitionEntry>(repo.list());

        resource.acceptRepresentation(new StringRepresentation("foo.bar.Baz.class\nbaz.bang.Quux.class\nbar.baz.Bang.class"));

        assertThat(repo.list().size(), is(3));
        assertThat(repo.list(), hasItems(new SuiteNamePartitionEntry("foo.bar.Baz.class"), new SuiteNamePartitionEntry("bar.baz.Bang.class"), new SuiteNamePartitionEntry("baz.bang.Quux.class")));

        List<SuiteNamePartitionEntry> listAfter2ndPartitionPosting = new ArrayList<SuiteNamePartitionEntry>(repo.list());

        Collections.sort(listBefore2ndPartitionPosting, new SuiteNamePartitionEntry.SuiteNameCountEntryComparator());
        Collections.sort(listAfter2ndPartitionPosting, new SuiteNamePartitionEntry.SuiteNameCountEntryComparator());
        for (int i = 0; i < listBefore2ndPartitionPosting.size(); i++) {
             assertThat(listBefore2ndPartitionPosting.get(i), sameInstance(listAfter2ndPartitionPosting.get(i)));
        }//assert nothing changed in server's copy

        verify(response).setStatus(Status.SUCCESS_OK);
    }

    @Test
    public void shouldGenerateError_whenUniversalSetRepoIsNotMatched_ForAnyPartitionAfterFirst() throws ResourceException, IOException {
        String suites = "foo.bar.Baz.class\nbar.baz.Bang.class\nbaz.bang.Quux.class";
        synchronized (repo) {
            StringReader stringReader = new StringReader(suites);
            repo.loadAndMarkDirty(stringReader);
        }
        List<SuiteNamePartitionEntry> listBeforeBadPartitionPosting = new ArrayList<SuiteNamePartitionEntry>(repo.list());

        resource.acceptRepresentation(new StringRepresentation("foo.bar.Baz.class\nbaz.bang.Quux.class"));

        assertThat(repo.list().size(), is(3));
        assertThat(repo.list(), hasItems(new SuiteNamePartitionEntry("foo.bar.Baz.class"), new SuiteNamePartitionEntry("bar.baz.Bang.class"), new SuiteNamePartitionEntry("baz.bang.Quux.class")));

        List<SuiteNamePartitionEntry> listAfterBadPartitionPosting = new ArrayList<SuiteNamePartitionEntry>(repo.list());

        Collections.sort(listBeforeBadPartitionPosting, new SuiteNamePartitionEntry.SuiteNameCountEntryComparator());
        Collections.sort(listAfterBadPartitionPosting, new SuiteNamePartitionEntry.SuiteNameCountEntryComparator());
        for (int i = 0; i < listBeforeBadPartitionPosting.size(); i++) {
             assertThat(listBeforeBadPartitionPosting.get(i), sameInstance(listAfterBadPartitionPosting.get(i)));
        }

        verify(response).setStatus(Status.CLIENT_ERROR_CONFLICT);
        assertThat(representationGiven.getText(), is("Expected universal set was [bar.baz.Bang.class, baz.bang.Quux.class, foo.bar.Baz.class] but given [baz.bang.Quux.class, foo.bar.Baz.class].\n"));
    }

    @Test
    public void shouldGenerateError_whenUniversalSetRepoDefinitionByASubsequentPartition_EndsBeforeExpected() throws ResourceException, IOException {
        String suites = "foo.bar.Baz.class\nbar.baz.Bang.class\nbaz.bang.Quux.class";
        synchronized (repo) {
            StringReader stringReader = new StringReader(suites);
            repo.loadAndMarkDirty(stringReader);
        }
        List<SuiteNamePartitionEntry> listBeforeBadPartitionPosting = new ArrayList<SuiteNamePartitionEntry>(repo.list());

        resource.acceptRepresentation(new StringRepresentation("baz.bang.Quux.class\nbar.baz.Bang.class"));

        assertThat(repo.list().size(), is(3));
        assertThat(repo.list(), hasItems(new SuiteNamePartitionEntry("foo.bar.Baz.class"), new SuiteNamePartitionEntry("bar.baz.Bang.class"), new SuiteNamePartitionEntry("baz.bang.Quux.class")));

        List<SuiteNamePartitionEntry> listAfterBadPartitionPosting = new ArrayList<SuiteNamePartitionEntry>(repo.list());

        Collections.sort(listBeforeBadPartitionPosting, new SuiteNamePartitionEntry.SuiteNameCountEntryComparator());
        Collections.sort(listAfterBadPartitionPosting, new SuiteNamePartitionEntry.SuiteNameCountEntryComparator());
        for (int i = 0; i < listBeforeBadPartitionPosting.size(); i++) {
             assertThat(listBeforeBadPartitionPosting.get(i), sameInstance(listAfterBadPartitionPosting.get(i)));
        }

        verify(response).setStatus(Status.CLIENT_ERROR_CONFLICT);
        assertThat(representationGiven.getText(), is("Expected universal set was [bar.baz.Bang.class, baz.bang.Quux.class, foo.bar.Baz.class] but given [bar.baz.Bang.class, baz.bang.Quux.class].\n"));
    }

    @Test
    public void shouldCreateUniversalSetRepos_namespacedByDifferentModules_whenGivenTheListingByRespectiveFirstPartitions() throws ResourceException, IOException {
        resource.acceptRepresentation(new StringRepresentation("foo.bar.Baz.class\nbar.baz.Bang.class\nbaz.bang.Quux.class"));
        assertThat(repo.list().size(), is(3));
        assertThat(repo.list(), hasItems(new SuiteNamePartitionEntry("foo.bar.Baz.class"), new SuiteNamePartitionEntry("bar.baz.Bang.class"), new SuiteNamePartitionEntry("baz.bang.Quux.class")));
        verify(response).setStatus(Status.SUCCESS_CREATED);

        reset(response);

        repo = new SetRepo();
        repo.setIdentifier("foo-bar-quux");
        reqAttrMap.put(TlbConstants.Server.MODULE_NAME, "my-other-module");
        when(request.getAttributes()).thenReturn(reqAttrMap);
        when(repoFactory.createUniversalSetRepo("family_name", "version-string", "my-other-module")).thenReturn(repo);
        resource = new UpdateUniversalSetResource(context, request, response);

        resource.acceptRepresentation(new StringRepresentation("bar.baz.Bang.class\nbaz.bang.SomeOther.class"));
        assertThat(repo.list().size(), is(2));
        assertThat(repo.list(), hasItems(new SuiteNamePartitionEntry("bar.baz.Bang.class"), new SuiteNamePartitionEntry("baz.bang.SomeOther.class")));
        verify(response).setStatus(Status.SUCCESS_CREATED);
    }
}
