package tlb.balancer;

import tlb.TestUtil;
import tlb.service.Server;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;

import java.io.IOException;
import java.util.HashMap;

import static junit.framework.Assert.fail;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class SuiteResultReporterTest {
    protected SuiteResultReporter reporter;
    protected HashMap<String,Object> appCtx;
    protected Server toService;
    protected TestUtil.LogFixture logFixture;

    @Before
    public void setUp() {
        final Context context = new Context();
        appCtx = new HashMap<String, Object>();
        toService = mock(Server.class);
        appCtx.put(TlbClient.TALK_TO_SERVICE, toService);
        context.setAttributes(this.appCtx);
        reporter = new SuiteResultReporter(context, mock(Request.class), mock(Response.class));
        logFixture = new TestUtil.LogFixture();
    }

    @Test
    public void shouldRegisterPlainTextAsSupportedMimeType() {
        assertThat(reporter.getVariants().get(0).getMediaType(), is(MediaType.TEXT_PLAIN));
    }

    @Test
    public void shouldNotAllowGet() {
        assertThat(reporter.allowGet(), is(false));
    }

    @Test
    public void shouldAllowPostRequest() {
        assertThat(reporter.allowPost(), is(true));
    }

    @Test
    public void shouldReportSuiteResultToServiceImpl() throws ResourceException {
        reporter.acceptRepresentation(new StringRepresentation("foo/bar/Baz.class: true"));
        verify(toService).testClassFailure("foo/bar/Baz.class", true);
        reporter.acceptRepresentation(new StringRepresentation("foo/bar/Quux.class: false"));
        verify(toService).testClassFailure("foo/bar/Quux.class", false);
    }
    
    @Test
    public void shouldReportBatchedSuiteResultOverToServiceImpl() throws ResourceException {
        reporter.acceptRepresentation(new StringRepresentation("foo/bar/Baz.class: true\nbar/Baz.class: false\ncom/foo/bar/Bar.class: true\nfoo/bar/Quux.class: false\n"));
        verify(toService).testClassFailure("foo/bar/Baz.class", true);
        verify(toService).testClassFailure("bar/Baz.class", false);
        verify(toService).testClassFailure("com/foo/bar/Bar.class", true);
        verify(toService).testClassFailure("foo/bar/Quux.class", false);
    }

    @Test
    public void shouldNotFailWhileTryingToReportEmptySuiteResultOverToServiceImpl() throws ResourceException {
        reporter.acceptRepresentation(new StringRepresentation(""));
        verify(toService, never()).testClassFailure(any(String.class), anyBoolean());
    }

    @Test
    public void shouldLogAndRaiseUpIOExceptions() throws ResourceException, IOException {
        final Representation representation = mock(Representation.class);
        @SuppressWarnings({"ThrowableInstanceNeverThrown"}) final IOException exception = new IOException("test exception");
        when(representation.getText()).thenThrow(exception);
        logFixture.startListening();
        try {
            reporter.acceptRepresentation(representation);
            fail("should have exceptioned");
        } catch (RuntimeException e) {
            assertThat(e.getCause(), sameInstance((Throwable) exception));
        }
        logFixture.stopListening();
        logFixture.assertHeard("could not report test result: 'test exception'");
        logFixture.assertHeardException(exception);
    }
}
