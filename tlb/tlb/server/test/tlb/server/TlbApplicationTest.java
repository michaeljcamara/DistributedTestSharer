package tlb.server;

import org.junit.Before;
import org.junit.Test;
import org.restlet.*;
import tlb.server.resources.*;
import tlb.server.resources.correctness.HomeResource;
import tlb.server.resources.correctness.UpdateSubsetResource;
import tlb.server.resources.correctness.UpdateUniversalSetResource;
import tlb.server.resources.correctness.VerifyPartitionCompletenessResource;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Mockito.mock;
import static tlb.RestletTestUtil.getRoutePatternsAndResources;

public class TlbApplicationTest {
    private TlbApplication app;

    @Before
    public void setUp() {
        Context context = mock(Context.class);
        app = new TlbApplication(context);
    }

    @Test
    public void shouldHaveRouteForSubsetSize() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/{namespace}/subset_size"));
        Restlet restlet = routeMaping.get("/{namespace}/subset_size");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(SubsetSizeResource.class.getName()));
    }

    @Test
    public void shouldHaveRouteForSuiteTime() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/{namespace}/suite_time"));
        Restlet restlet = routeMaping.get("/{namespace}/suite_time");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(SuiteTimeResource.class.getName()));
    }
    
    @Test
    public void shouldHaveRouteForVersionedSuiteTime() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/{namespace}/suite_time/{listing_version}"));
        Restlet restlet = routeMaping.get("/{namespace}/suite_time/{listing_version}");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(VersionedSuiteTimeResource.class.getName()));
    }

    @Test
    public void shouldHaveRouteForSuiteResult() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/{namespace}/suite_result"));
        Restlet restlet = routeMaping.get("/{namespace}/suite_result");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(SuiteResultResource.class.getName()));
    }

    @Test
    public void shouldHaveAn_updateUniversalSet_RouteForCorrectnessCheck() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/{namespace}/correctness_check/{listing_version}/universal_set/{module_name}"));
        Restlet restlet = routeMaping.get("/{namespace}/correctness_check/{listing_version}/universal_set/{module_name}");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(UpdateUniversalSetResource.class.getName()));
    }

    @Test
    public void shouldHaveA_updateSubset_RouteForCorrectnessCheck() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/{namespace}/correctness_check/{listing_version}/{total_jobs}/{job_number}/sub_set/{module_name}"));
        Restlet restlet = routeMaping.get("/{namespace}/correctness_check/{listing_version}/{total_jobs}/{job_number}/sub_set/{module_name}");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(UpdateSubsetResource.class.getName()));
    }

    @Test
    public void shouldHaveARouteFor_partitionCompleteness_correctnessCheck() {
        HashMap<String, Restlet> routeMaping = getRoutePatternsAndResources(app);
        assertThat(routeMaping.keySet(), hasItem("/{namespace}/correctness_check/{listing_version}/verify_partition_completeness/{module_name}"));
        Restlet restlet = routeMaping.get("/{namespace}/correctness_check/{listing_version}/verify_partition_completeness/{module_name}");
        assertThat(((Finder)restlet).getTargetClass().getName(), is(VerifyPartitionCompletenessResource.class.getName()));
    }

    @Test
    public void shouldHaveARouteFor_homePage() {
        Route defaultRoute = ((Router) app.createRoot()).getDefaultRoute();
        String pattern = defaultRoute.getTemplate().getPattern();
        assertThat(pattern, is(""));
        assertThat(((Finder)defaultRoute.getNext()).getTargetClass().getName(), is(HomeResource.class.getName()));
    }

}
