package tlb.server.resources;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.StringRepresentation;
import tlb.TlbConstants;
import tlb.domain.SuiteTimeEntry;
import tlb.server.repo.EntryRepo;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SuiteTimeRepo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SuiteTimeResourceTest {
    private SuiteTimeResource suiteTimeResource;

    @Before
    public void setUp() throws ClassNotFoundException, IOException {
        Context context = new Context();
        Request request = mock(Request.class);
        EntryRepoFactory factory = mock(EntryRepoFactory.class);
        context.setAttributes(Collections.singletonMap(TlbConstants.Server.REPO_FACTORY, (Object) factory));
        HashMap<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(TlbConstants.Server.REQUEST_NAMESPACE, "namespace");
        when(request.getAttributes()).thenReturn(attributeMap);
        suiteTimeResource = new SuiteTimeResource(context, request, mock(Response.class));
    }

    @Test
    public void shouldAllowPostRequests() {
        assertThat(suiteTimeResource.allowPost(), is(false));
    }

    @Test
    public void shouldNotAllowPutRequests() {
        assertThat(suiteTimeResource.allowPut(), is(true));
    }

    @Test
    public void shouldParseSuitTimeEntry() throws IOException {
        final SuiteTimeEntry entry = (SuiteTimeEntry) suiteTimeResource.parseEntry(new StringRepresentation("foo.bar.Baz: 135"));
        assertThat(entry, is(new SuiteTimeEntry("foo.bar.Baz", 135)));
    }

    @Test
    public void shouldParseSuitTimeEntries() throws IOException {
        final List<SuiteTimeEntry> entry = suiteTimeResource.parseEntries(new StringRepresentation("foo.bar.Baz: 135\nfoo.baz.Quux: 27\nfoo.quux.Bang: 129\n"));
        assertThat(entry, is(Arrays.asList(new SuiteTimeEntry("foo.bar.Baz", 135), new SuiteTimeEntry("foo.baz.Quux", 27), new SuiteTimeEntry("foo.quux.Bang", 129))));
    }

    @Test
    public void shouldUseSuiteTimeRepo() throws IOException, ClassNotFoundException {
        EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        SuiteTimeRepo expectedRepo = mock(SuiteTimeRepo.class);
        when(repoFactory.createSuiteTimeRepo("namespace", EntryRepoFactory.LATEST_VERSION)).thenReturn(expectedRepo);
        EntryRepo repo = suiteTimeResource.getRepo(repoFactory, "namespace");
        assertThat((SuiteTimeRepo)repo, sameInstance(expectedRepo));
    }
}
