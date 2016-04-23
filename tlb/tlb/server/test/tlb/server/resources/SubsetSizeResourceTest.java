package tlb.server.resources;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.StringRepresentation;
import tlb.TlbConstants;
import tlb.domain.SubsetSizeEntry;
import tlb.server.repo.EntryRepo;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SubsetSizeRepo;

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

public class SubsetSizeResourceTest {
    private SubsetSizeResource subsetSizeResource;

    @Before
    public void setUp() throws ClassNotFoundException, IOException {
        Context context = new Context();
        EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        SubsetSizeRepo repo = mock(SubsetSizeRepo.class);
        context.setAttributes(Collections.singletonMap(TlbConstants.Server.REPO_FACTORY, (Object) repoFactory));
        Request request = mock(Request.class);
        HashMap<String, Object> attributeMap = new HashMap<String, Object>();
        attributeMap.put(TlbConstants.Server.REQUEST_NAMESPACE, "family_name");
        when(request.getAttributes()).thenReturn(attributeMap);
        when(repoFactory.createSubsetRepo(TlbConstants.Server.REQUEST_NAMESPACE, EntryRepoFactory.LATEST_VERSION)).thenReturn(repo);
        subsetSizeResource = new SubsetSizeResource(context, request, mock(Response.class));
    }

    @Test
    public void shouldAllowPostRequests() {
        assertThat(subsetSizeResource.allowPost(), is(true));
    }

    @Test
    public void shouldNotAllowPutRequests() {
        assertThat(subsetSizeResource.allowPut(), is(false));
    }

    @Test
    public void shouldUseSuiteTimeRepo() throws ClassNotFoundException, IOException {
        EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        SubsetSizeRepo expectedRepo = mock(SubsetSizeRepo.class);
        when(repoFactory.createSubsetRepo("namespace", EntryRepoFactory.LATEST_VERSION)).thenReturn(expectedRepo);
        EntryRepo repo = subsetSizeResource.getRepo(repoFactory, "namespace");
        assertThat((SubsetSizeRepo) repo, sameInstance(expectedRepo));
    }

    @Test
    public void shouldParseResultEntries() throws IOException {
        List<SubsetSizeEntry> entry = subsetSizeResource.parseEntries(new StringRepresentation("115\n12\n19\n"));
        assertThat(entry, is(Arrays.asList(new SubsetSizeEntry(115), new SubsetSizeEntry(12), new SubsetSizeEntry(19))));
    }
    
    @Test
    public void shouldParseResultEntry() throws IOException {
        SubsetSizeEntry entry = (SubsetSizeEntry) subsetSizeResource.parseEntry(new StringRepresentation("115"));
        assertThat(entry, is(new SubsetSizeEntry(115)));
    }
}