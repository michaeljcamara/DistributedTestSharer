package tlb.server.resources;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import tlb.TlbConstants;
import tlb.domain.SuiteTimeEntry;
import tlb.server.repo.EntryRepo;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SuiteTimeRepo;
import tlb.utils.SystemEnvironment;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VersionedSuiteTimeResourceTest {
    private VersionedSuiteTimeResource suiteTimeResource;
    protected HashMap<String, Object> attributeMap;
    protected EntryRepoFactory factory;
    protected SuiteTimeRepo repo;
    private Context context;
    private Request request;

    @Before
    public void setUp() throws ClassNotFoundException, IOException {
        context = new Context();
        request = mock(Request.class);
        factory = mock(EntryRepoFactory.class);
        context.setAttributes(Collections.singletonMap(TlbConstants.Server.REPO_FACTORY, (Object) factory));
        attributeMap = new HashMap<String, Object>();
        attributeMap.put(TlbConstants.Server.REQUEST_NAMESPACE, "namespace");
        attributeMap.put(TlbConstants.Server.LISTING_VERSION, "version");
        when(request.getAttributes()).thenReturn(attributeMap);
        repo = mock(SuiteTimeRepo.class);
        when(factory.createSuiteTimeRepo("namespace", "version")).thenReturn(repo);
        suiteTimeResource = new VersionedSuiteTimeResource(context, request, mock(Response.class));
    }

    @Test
    public void shouldUseSuiteTimeRepo() throws IOException, ClassNotFoundException {
        EntryRepo repo = suiteTimeResource.getRepo(factory, "namespace");
        assertThat((SuiteTimeRepo) repo, sameInstance(this.repo));
    }

    @Test
    public void shouldAllowPostRequests() {
        assertThat(suiteTimeResource.allowPost(), is(false));
    }

    @Test
    public void shouldNotAllowPutRequests() {
        assertThat(suiteTimeResource.allowPut(), is(false));
    }

    @Test
    public void shouldNotSupportParsingOfEntry_AsAddingToVersionedRepoIsNotPermitted() throws ResourceException, IOException {
        try {
            suiteTimeResource.parseEntry(new StringRepresentation("foo.bar.Baz: 120"));
            fail("should not have parsed entry, as mutation of versioned data is not allowed");
        } catch (Exception e) {
            assertThat(e, is(UnsupportedOperationException.class));
        }
    }

    @Test
    public void shouldNotSupportParsingOfEntries_AsAddingToVersionedRepoIsNotPermitted() throws ResourceException, IOException {
        try {
            suiteTimeResource.parseEntries(new StringRepresentation("foo.bar.Baz: 120\nquux.baz.Bang: 105\n"));
            fail("should not have parsed entry, as mutation of versioned data is not allowed");
        } catch (Exception e) {
            assertThat(e, is(UnsupportedOperationException.class));
        }
    }

    @Test
    public void shouldRenderAllRecordsForGivenNamespaceAndVersion() throws ResourceException, IOException, ClassNotFoundException {
        attributeMap.put(TlbConstants.Server.LISTING_VERSION, "foo");
        factory = new EntryRepoFactory(new SystemEnvironment(new HashMap<String, String>()));
        context.setAttributes(Collections.singletonMap(TlbConstants.Server.REPO_FACTORY, (Object) factory));
        SuiteTimeRepo latestFooRepo = factory.createSuiteTimeRepo("namespace", EntryRepoFactory.LATEST_VERSION);
        latestFooRepo.update(new SuiteTimeEntry("foo.bar.Baz", 10));
        latestFooRepo.update(new SuiteTimeEntry("foo.bar.Quux", 20));

        suiteTimeResource = new VersionedSuiteTimeResource(context, request, mock(Response.class));
        Representation actualRepresentation = suiteTimeResource.represent(new Variant(MediaType.TEXT_PLAIN));
        assertThat(actualRepresentation.getText(), containsString("foo.bar.Baz: 10\n"));
        assertThat(actualRepresentation.getText(), containsString("foo.bar.Quux: 20\n"));
    }
}
