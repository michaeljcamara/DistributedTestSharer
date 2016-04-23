package tlb.server.resources;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.StringRepresentation;
import tlb.TlbConstants;
import tlb.domain.SuiteResultEntry;
import tlb.server.repo.EntryRepo;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SuiteResultRepo;

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

public class SuiteResultResourceTest {
    private SuiteResultResource suiteResultResource;

    @Before
    public void setUp() throws ClassNotFoundException, IOException {
        Context context = new Context();
        Request request = mock(Request.class);
        EntryRepoFactory factory = mock(EntryRepoFactory.class);
        context.setAttributes(Collections.singletonMap(TlbConstants.Server.REPO_FACTORY, (Object) factory));
        HashMap<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(TlbConstants.Server.REQUEST_NAMESPACE, "namespace");
        when(request.getAttributes()).thenReturn(attributeMap);
        suiteResultResource = new SuiteResultResource(context, request, mock(Response.class));
    }

    @Test
    public void shouldAllowPostRequests() {
        assertThat(suiteResultResource.allowPost(), is(false));
    }

    @Test
    public void shouldNotAllowPutRequests() {
        assertThat(suiteResultResource.allowPut(), is(true));
    }
    
    @Test
    public void shouldUseSuiteResultRepo() throws IOException, ClassNotFoundException {
        EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        SuiteResultRepo expectedRepo = mock(SuiteResultRepo.class);
        when(repoFactory.createSuiteResultRepo("namespace", EntryRepoFactory.LATEST_VERSION)).thenReturn(expectedRepo);
        EntryRepo repo = suiteResultResource.getRepo(repoFactory, "namespace");
        assertThat((SuiteResultRepo)repo, sameInstance(expectedRepo));
    }

    @Test
    public void shouldParseSuitResultEntry() throws IOException {
        final SuiteResultEntry entry = (SuiteResultEntry) suiteResultResource.parseEntry(new StringRepresentation("foo.bar.Baz: true"));
        assertThat(entry, is(new SuiteResultEntry("foo.bar.Baz", true)));
    }

    @Test
    public void shouldParseSuitResultEntries() throws IOException {
        final List<SuiteResultEntry> entry = suiteResultResource.parseEntries(new StringRepresentation("foo.bar.Baz: true\nfoo.baz.Quux: false\nfoo.quux.Bang: true\n"));
        assertThat(entry, is(Arrays.asList(new SuiteResultEntry("foo.bar.Baz", true), new SuiteResultEntry("foo.baz.Quux", false), new SuiteResultEntry("foo.quux.Bang", true))));
    }
}
