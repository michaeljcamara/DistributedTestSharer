package tlb.server;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.util.RouteList;
import org.restlet.util.ServerList;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class ServerInitializerTest {
    protected TestServerInitializer serverInitializer;

    class TestServerInitializer extends ServerInitializer {

        @Override
        protected Restlet application() {
            return app;
        }

        @Override
        protected int appPort() {
            return 614;
        }
    }

    protected Component component;
    protected Restlet app;

    @Before
    public void setUp() {
        app = mock(Restlet.class);
        serverInitializer = new TestServerInitializer();
        component = serverInitializer.init();
    }

    @Test
    public void shouldInitializeTlbToRunOnConfiguredPort() {
        ServerList servers = component.getServers();
        assertThat(servers.size(), is(1));
        assertThat(servers.get(0).getPort(), is(614));
        assertThat(servers.get(0).getProtocols().size(), is(1));
        assertThat(servers.get(0).getProtocols().get(0), is(Protocol.HTTP));
    }

    @Test
    public void shouldReturnApplicationReturnedByInit() {
        RouteList routeList = component.getDefaultHost().getRoutes();
        assertThat(routeList.size(), is(1));
        Restlet application = routeList.get(0).getNext();
        assertThat(application, sameInstance(app));
    }
    
    @Test
    public void shouldCacheComponentInitialization() {
        assertThat(serverInitializer.init(), sameInstance(component));
    }

}
