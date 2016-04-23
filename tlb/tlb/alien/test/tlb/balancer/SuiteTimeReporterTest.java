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

public class SuiteTimeReporterTest {
    protected SuiteTimeReporter reporter;
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
        reporter = new SuiteTimeReporter(context, mock(Request.class), mock(Response.class));
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
    public void shouldReportSuiteTimeOverToServiceImpl() throws ResourceException {
        reporter.acceptRepresentation(new StringRepresentation("com/foo/Foo.class: 103"));
        verify(toService).testClassTime("com/foo/Foo.class", 103l);
    }

    @Test
    public void shouldReportBatchedSuiteTimeReportingOverToServiceImpl() throws ResourceException {
        reporter.acceptRepresentation(new StringRepresentation("com/foo/Foo.class: 103\ncom/bar/Bar.class: 89\ncom/baz/Quux.class: 17\nfoo/bar/Baz.class: 134"));
        verify(toService).testClassTime("com/foo/Foo.class", 103l);
        verify(toService).testClassTime("com/bar/Bar.class", 89l);
        verify(toService).testClassTime("com/baz/Quux.class", 17l);
        verify(toService).testClassTime("foo/bar/Baz.class", 134l);
    }
    
    @Test
    public void shouldNotFailForBatchedSuiteTimeReportingWithNoEntries() throws ResourceException {
        reporter.acceptRepresentation(new StringRepresentation(""));
        verify(toService, never()).testClassTime(any(String.class), anyLong());
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
        logFixture.assertHeard("could not report test time: 'test exception'");
        logFixture.assertHeardException(exception);
    }
}
