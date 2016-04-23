package tlb.splitter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.service.GoServer;
import tlb.service.TlbServer;
import tlb.utils.SystemEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class JobFamilyAwareSplitterTest {
    private TestUtil.LogFixture logFixture;

    @Before
    public void setUp() {
        logFixture = new TestUtil.LogFixture();
    }

    @After
    public void tearDown() {
        logFixture.stopListening();
    }

    @Test
    public void testFilterShouldPublishNumberOfSuitesSelectedForRunning() {
        HashMap<String, String> envMap = new HashMap<String, String>();
        envMap.put(TlbConstants.Go.GO_JOB_NAME, "build-1");
        GoServer toCruise = mock(GoServer.class);
        when(toCruise.totalPartitions()).thenReturn(3);
        when(toCruise.partitionIdentifier()).thenReturn("foo bar baz quux");

        JobFamilyAwareSplitter criteria = new JobFamilyAwareSplitter(new SystemEnvironment(envMap)) {
            protected List<TlbSuiteFile> subset(List<TlbSuiteFile> fileResources) {
                TlbSuiteFile foo = new TlbSuiteFileImpl("foo");
                TlbSuiteFile bar = new TlbSuiteFileImpl("bar");
                return Arrays.asList(foo, bar);
            }
        };
        criteria.talksToServer(toCruise);
        logFixture.startListening();
        List<TlbSuiteFile> resources = criteria.filterSuites(new ArrayList<TlbSuiteFile>(), "module_bar");
        logFixture.assertHeard("got total of 0 files to balance");
        logFixture.assertHeard("total jobs to distribute load [ 3 ]");
        logFixture.assertHeard("assigned total of 2 files to [ foo bar baz quux ]");
        assertThat(resources.size(), is(2));
        verify(toCruise).publishSubsetSize(2);
    }
}
