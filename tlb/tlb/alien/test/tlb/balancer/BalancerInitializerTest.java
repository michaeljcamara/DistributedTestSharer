package tlb.balancer;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Component;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.orderer.FailedFirstOrderer;
import tlb.service.TlbServer;
import tlb.splitter.CountBasedTestSplitter;
import tlb.utils.FileUtil;
import tlb.utils.SystemEnvironment;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static tlb.TestUtil.updateEnv;

public class BalancerInitializerTest {
    protected BalancerInitializer initializer;
    private HashMap<String, String> systemEnv;
    private SystemEnvironment env;

    @Before
    public void setUp() {
        systemEnv = new HashMap<String, String>();
        env = new SystemEnvironment(systemEnv);
        initializer = new BalancerInitializer(env);
    }

    @Test
    public void shouldCreateApplicationContextWithNecessaryObjects() throws NoSuchFieldException, IllegalAccessException {
        updateEnv(env, TlbConstants.TLB_SPLITTER, CountBasedTestSplitter.class.getCanonicalName());
        updateEnv(env, TlbConstants.TLB_ORDERER, FailedFirstOrderer.class.getCanonicalName());
        updateEnv(env, TlbConstants.TYPE_OF_SERVER.key, TlbServer.class.getCanonicalName());
        updateEnv(env, TlbConstants.Balancer.TLB_BALANCER_PORT.key, "614");
        updateEnv(env, TlbConstants.TlbServer.TLB_BASE_URL, "http://foo.bar.com:7019");

        try {
            ConcurrentMap<String,Object> map = initializer.application().getContext().getAttributes();
            assertThat(TestUtil.deref("splitter", map.get(TlbClient.SPLITTER)), is(CountBasedTestSplitter.class));
            assertThat(map.get(TlbClient.ORDERER), is(FailedFirstOrderer.class));
            assertThat(map.get(TlbClient.TALK_TO_SERVICE), is(TlbServer.class));
            assertThat(map.get(TlbClient.APP_COMPONENT), is(Component.class));
            assertThat(map.get(TlbClient.APP_COMPONENT), sameInstance((Object) initializer.init()));
        } finally {
            FileUtils.deleteQuietly(new File(new FileUtil(env).tmpDir()));
        }
    }

    @Test
    public void shouldInitializeTlbToRunOnConfiguredPort() throws NoSuchFieldException, IllegalAccessException {
        updateEnv(env, TlbConstants.Balancer.TLB_BALANCER_PORT.key, "4321");
        assertThat(initializer.appPort(), is(4321));
    }
}
