package tlb.balancer;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import tlb.TlbConstants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import com.googlecode.junit.ext.RunIf;
import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.checkers.OSChecker;
import tlb.utils.FileUtil;
import tlb.utils.SystemEnvironment;

@RunWith(JunitExtRunner.class)
public class ControlResourceTest {
    protected Request req;
    protected Response res;
    protected ControlResource resource;
    protected HashMap<String, Object> attributes;
    protected Component component;

    @Before
    public void setUp() {
        req = mock(Request.class);
        res = mock(Response.class);
        attributes = new HashMap<String, Object>();
        when(req.getAttributes()).thenReturn(attributes);
        final Context context = new Context();
        final HashMap<String, Object> contextMap = new HashMap<String, Object>();
        component = mock(Component.class);
        contextMap.put(TlbClient.APP_COMPONENT, component);
        context.setAttributes(contextMap);
        resource = new ControlResource(context, req, res);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(component);
    }

    @Test
    public void shouldReturnRunningAsProcessStatus() throws ResourceException, IOException {
        attributes.put(TlbConstants.Balancer.QUERY, ControlResource.Query.status.toString());
        final Representation rep = resource.represent(new Variant(MediaType.TEXT_PLAIN));
        assertThat(rep.getText(), is("RUNNING"));
    }

    @Test
    public void shouldBombWhenQueryNotUnderstood() {
        attributes.put(TlbConstants.Balancer.QUERY, "foo");
        try {
            resource.represent(new Variant(MediaType.TEXT_PLAIN));
            fail("should have bombed, because 'foo' is not a known query");
        } catch (Exception e) {
            assertThat(e, is(IllegalArgumentException.class));
        }
    }

    @Test
    public void shouldAllowGet() {
        assertThat(resource.allowGet(), is(true));
    }

    @Test
    public void suicideThreadShouldBeCached() {
        assertThat(resource.suicideThread(), sameInstance(resource.suicideThread()));
    }

    @Test
    @RunIf(value = OSChecker.class, arguments = OSChecker.LINUX)
    public void shouldStopServer() throws InterruptedException, IOException {
        final String port = unpriviledgedPort();
        final File buildFile = new File("build.xml");
        if (!buildFile.exists()) {
            throw new RuntimeException("please execute this test from project root");
        }
        final Process process = new ProcessBuilder("ant", "run_balancer", "-Doffline=t", "-Drandom.balancer.port=" + port).start();
        Pumper pumperOut = new Pumper(process.getInputStream());
        Pumper pumperErr = new Pumper(process.getErrorStream());

        DefaultHttpClient client = new DefaultHttpClient();
        final HttpGet suicide = new HttpGet(String.format("http://localhost:%s/control/suicide", port));
        final HttpGet waitForStart = new HttpGet(String.format("http://localhost:%s/control/status", port));

        while (true) {
            try {
                HttpResponse execute = client.execute(waitForStart);
                if (EntityUtils.toString(execute.getEntity()).equals("RUNNING")) break;
            } catch (IOException e) {
                //ignore
            }
        }

        HttpResponse suicideCall = client.execute(suicide);
        assertThat(suicideCall.getStatusLine().getStatusCode(), is(200));
        assertThat(EntityUtils.toString(suicideCall.getEntity()), is("HALTING"));
        assertThat(process.waitFor(), is(0));
        pumperOut.stop();
        pumperErr.stop();
    }

    private class Pumper {
        private Thread thd;
        private boolean shdRun;

        protected Pumper(final InputStream stream) {
            shdRun = true;
            final Runnable runner = new Runnable() {
                public void run() {
                    while (shdRun) {
                        emptyStream(stream);
                    }
                }
            };
            thd = new Thread(runner);
            thd.start();
        }

        void stop() throws InterruptedException {
            shdRun = false;
            thd.join();
        }
    }

    private void emptyStream(InputStream stream) {
        try {
            final int n = stream.available();
            final byte[] bytes = new byte[n];
            if (n > 0) {
                stream.read(bytes);
                System.out.println(new String(bytes));
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private String unpriviledgedPort() {
        int unprivilegedPort = new Random(new Date().getTime()).nextInt(15);
        if (unprivilegedPort <= 1024) {
            unprivilegedPort += 1024;
        }
        return String.valueOf(unprivilegedPort);
    }
}
