package tlb.balancer;

import org.junit.Before;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.Finder;
import org.restlet.Restlet;
import tlb.TlbConstants;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Mockito.mock;
import static tlb.RestletTestUtil.getRoutePatternsAndResources;

public class TlbClientTest {
    private TlbClient app;

    @Before
    public void setUp() {
        Context context = mock(Context.class);
        app = new TlbClient(context);
    }

    @Test
    public void shouldHaveRouteForBalancing() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/balance"));
        Restlet restlet = routeMaping.get("/balance");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(BalancerResource.class.getName()));
    }

    @Test
    public void shouldHaveRouteForReportingSuiteTime() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/suite_time"));
        Restlet restlet = routeMaping.get("/suite_time");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(SuiteTimeReporter.class.getName()));
    }

    @Test
    public void shouldHaveRouteForReportingSuiteResult() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/suite_result"));
        Restlet restlet = routeMaping.get("/suite_result");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(SuiteResultReporter.class.getName()));
    }

    @Test
    public void shouldHaveRouteForControl() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/control/{query}"));
        Restlet restlet = routeMaping.get(String.format("/control/{%s}", TlbConstants.Balancer.QUERY));
        assertThat(((Finder)restlet).getTargetClass().getName(), is(ControlResource.class.getName()));
    }

    @Test
    public void shouldExposeAssertionThatAllPartitionsExecuted() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/assert_all_partitions_executed"));
        Restlet restlet = routeMaping.get("/assert_all_partitions_executed");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(AllPartitionsExecutedAssertion.class.getName()));
    }
}
