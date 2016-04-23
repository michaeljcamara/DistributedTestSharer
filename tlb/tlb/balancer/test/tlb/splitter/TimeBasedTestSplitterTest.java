package tlb.splitter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import static tlb.TestUtil.convertToPlatformSpecificPath;
import tlb.domain.SuiteTimeEntry;
import tlb.service.Server;
import tlb.utils.SystemEnvironment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeBasedTestSplitterTest {

    private Server server;
    private TestUtil.LogFixture logFixture;

    private static final String moduleName = "module_quux";

    @Before
    public void setUp() throws Exception {
        server = mock(Server.class);
        logFixture = new TestUtil.LogFixture();
    }

    @After
    public void tearDown() {
        logFixture.stopListening();
    }

    @Test
    public void shouldConsumeAllTestsWhenNoJobsToBalanceWith() {
        when(server.totalPartitions()).thenReturn(1);
        when(server.partitionNumber()).thenReturn(1);

        SystemEnvironment env = new SystemEnvironment();

        TlbSuiteFile first = new TlbSuiteFileImpl("first");
        TlbSuiteFile second = new TlbSuiteFileImpl("second");
        TlbSuiteFile third = new TlbSuiteFileImpl("third");
        List<TlbSuiteFile> resources = Arrays.asList(first, second, third);

        TimeBasedTestSplitter criteria = new TimeBasedTestSplitter(server, env);
        logFixture.startListening();
        assertThat(criteria.filterSuites(resources, moduleName), is(Arrays.asList(first, second, third)));
        logFixture.assertHeard("total jobs to distribute load [ 1 ]");
    }

    @Test
    public void shouldSplitTestsBasedOnTimeForTwoJob() {
        when(server.totalPartitions()).thenReturn(2);

        List<SuiteTimeEntry> entries = testTimes();
        when(server.getLastRunTestTimes()).thenReturn(entries);

        TlbSuiteFile first = new TlbSuiteFileImpl(convertToPlatformSpecificPath("com/foo/First.class"));
        TlbSuiteFile second = new TlbSuiteFileImpl(convertToPlatformSpecificPath("com/foo/Second.class"));
        TlbSuiteFile third = new TlbSuiteFileImpl(convertToPlatformSpecificPath("com/bar/Third.class"));
        TlbSuiteFile fourth = new TlbSuiteFileImpl(convertToPlatformSpecificPath("foo/baz/Fourth.class"));
        TlbSuiteFile fifth = new TlbSuiteFileImpl(convertToPlatformSpecificPath("foo/bar/Fourth.class"));
        List<TlbSuiteFile> resources = Arrays.asList(first, second, third, fourth, fifth);
        when(server.partitionNumber()).thenReturn(1);

        TimeBasedTestSplitter criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-1"));
        assertThat(criteria.filterSuites(resources, moduleName), is(Arrays.asList(second, first, third)));

        when(server.partitionNumber()).thenReturn(2);
        criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-2"));
        assertThat(criteria.filterSuites(resources, moduleName), is(Arrays.asList(fourth, fifth)));
    }

    @Test
    public void shouldBombWhenNoTestTimeDataAvailable() {
        when(server.totalPartitions()).thenReturn(4);

        when(server.getLastRunTestTimes()).thenReturn(new ArrayList<SuiteTimeEntry>());

        TlbSuiteFile first = new TlbSuiteFileImpl("com/foo/First.class");
        TlbSuiteFile second = new TlbSuiteFileImpl("com/foo/Second.class");
        TlbSuiteFile third = new TlbSuiteFileImpl("com/bar/Third.class");
        TlbSuiteFile fourth = new TlbSuiteFileImpl("foo/baz/Fourth.class");
        TlbSuiteFile fifth = new TlbSuiteFileImpl("foo/bar/Fourth.class");
        List<TlbSuiteFile> resources = Arrays.asList(first, second, third, fourth, fifth);

        when(server.partitionNumber()).thenReturn(1);
        TimeBasedTestSplitter criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-1"));
        logFixture.startListening();
        assertAbortsForNoHistoricalTimeData(resources, criteria);

        when(server.partitionNumber()).thenReturn(2);
        criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-2"));
        assertAbortsForNoHistoricalTimeData(resources, criteria);

        when(server.partitionNumber()).thenReturn(3);
        criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-3"));
        assertAbortsForNoHistoricalTimeData(resources, criteria);

        when(server.partitionNumber()).thenReturn(4);
        criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-4"));
        assertAbortsForNoHistoricalTimeData(resources, criteria);
    }

    private void assertAbortsForNoHistoricalTimeData(List<TlbSuiteFile> resources, TimeBasedTestSplitter criteria) {
        try {
            criteria.filterSuites(resources, moduleName);
            fail("should have aborted, as no historical test time data was given");
        } catch (Exception e) {
            String message = "no historical test time data, aborting attempt to balance based on time";
            logFixture.assertHeard(message);
            logFixture.clearHistory();
            assertThat(e.getMessage(), is(message));
        }
    }

    @Test
    public void shouldSplitTestsBasedOnTimeForFourJobs() {
        when(server.totalPartitions()).thenReturn(4);
        when(server.getLastRunTestTimes()).thenReturn(testTimes());

        TlbSuiteFile first = new TlbSuiteFileImpl(convertToPlatformSpecificPath("com/foo/First.class"));
        TlbSuiteFile second = new TlbSuiteFileImpl(convertToPlatformSpecificPath("com/foo/Second.class"));
        TlbSuiteFile third = new TlbSuiteFileImpl(convertToPlatformSpecificPath("com/bar/Third.class"));
        TlbSuiteFile fourth = new TlbSuiteFileImpl(convertToPlatformSpecificPath("foo/baz/Fourth.class"));
        TlbSuiteFile fifth = new TlbSuiteFileImpl(convertToPlatformSpecificPath("foo/bar/Fourth.class"));
        List<TlbSuiteFile> resources = Arrays.asList(first, second, third, fourth, fifth);

        when(server.partitionNumber()).thenReturn(1);
        TimeBasedTestSplitter criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-1"));
        assertThat(criteria.filterSuites(resources, moduleName), is(Arrays.asList(second)));

        when(server.partitionNumber()).thenReturn(2);
        criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-2"));
        assertThat(criteria.filterSuites(resources, moduleName), is(Arrays.asList(fourth)));

        when(server.partitionNumber()).thenReturn(3);
        criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-3"));
        assertThat(criteria.filterSuites(resources, moduleName), is(Arrays.asList(fifth)));

        when(server.partitionNumber()).thenReturn(4);
        criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-4"));
        assertThat(criteria.filterSuites(resources, moduleName), is(Arrays.asList(first, third)));
    }

    @Test
    public void shouldDistributeUnknownTestsBasedOnAverageTime() throws Exception{
        when(server.totalPartitions()).thenReturn(2);
        when(server.getLastRunTestTimes()).thenReturn(testTimes());

        TlbSuiteFile first = new TlbSuiteFileImpl(convertToPlatformSpecificPath("com/foo/First.class"));
        TlbSuiteFile second = new TlbSuiteFileImpl(convertToPlatformSpecificPath("com/foo/Second.class"));
        TlbSuiteFile third = new TlbSuiteFileImpl(convertToPlatformSpecificPath("com/bar/Third.class"));
        TlbSuiteFile fourth = new TlbSuiteFileImpl(convertToPlatformSpecificPath("foo/baz/Fourth.class"));
        TlbSuiteFile fifth = new TlbSuiteFileImpl(convertToPlatformSpecificPath("foo/bar/Fourth.class"));
        TlbSuiteFile firstNew = new TlbSuiteFileImpl(convertToPlatformSpecificPath("foo/quux/First.class"));
        TlbSuiteFile secondNew = new TlbSuiteFileImpl(convertToPlatformSpecificPath("foo/quux/Second.class"));
        List<TlbSuiteFile> resources = Arrays.asList(first, second, third, fourth, fifth, firstNew, secondNew);

        when(server.partitionNumber()).thenReturn(1);
        TimeBasedTestSplitter criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-1"));
        logFixture.startListening();
        List<TlbSuiteFile> filteredResources = criteria.filterSuites(resources, moduleName);
        assertThat(filteredResources.size(), is(4));
        assertThat(filteredResources, hasItems(second, firstNew, first, third));

        when(server.partitionNumber()).thenReturn(2);
        criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-2"));
        filteredResources = criteria.filterSuites(resources, moduleName);
        logFixture.assertHeard("Encountered 2 new files which don't have historical time data, used average time [ 3.0 ] to balance", 2);
        assertThat(filteredResources.size(), is(3));
        assertThat(filteredResources, hasItems(fourth, fifth, secondNew));
    }

    @Test
    public void shouldIgnoreDeletedTests() throws Exception{
        when(server.totalPartitions()).thenReturn(2);
        when(server.getLastRunTestTimes()).thenReturn(testTimes());
        when(server.partitionIdentifier()).thenReturn("foo bar");

        TlbSuiteFile first = new TlbSuiteFileImpl(convertToPlatformSpecificPath("com/foo/First.class"));
        TlbSuiteFile second = new TlbSuiteFileImpl(convertToPlatformSpecificPath("com/foo/Second.class"));
        TlbSuiteFile third = new TlbSuiteFileImpl(convertToPlatformSpecificPath("com/bar/Third.class"));

        List<TlbSuiteFile> resources = Arrays.asList(second, first, third);

        when(server.partitionNumber()).thenReturn(1);
        TimeBasedTestSplitter criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job"));
        logFixture.startListening();

        assertThat(criteria.filterSuites(resources, moduleName), is(Arrays.asList(second)));
        logFixture.assertHeard("got total of 3 files to balance");
        logFixture.assertHeard("total jobs to distribute load [ 2 ]");
        logFixture.assertHeard("Historical test time data has entries for 5 suites");
        logFixture.assertHeard("3 entries of historical test time data found relevant");
        logFixture.assertHeard("assigned total of 1 files to [ foo bar ]");

        when(server.partitionNumber()).thenReturn(2);
        criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job"));

        assertThat(criteria.filterSuites(resources, moduleName), is(Arrays.asList(first, third)));
        logFixture.assertHeard("got total of 3 files to balance", 2);
        logFixture.assertHeard("total jobs to distribute load [ 2 ]", 2);
        logFixture.assertHeard("Historical test time data has entries for 5 suites", 2);
        logFixture.assertHeard("3 entries of historical test time data found relevant", 2);
        logFixture.assertHeard("assigned total of 2 files to [ foo bar ]");
    }

    private List<SuiteTimeEntry> testTimes() {
        List<SuiteTimeEntry> entries = new ArrayList<SuiteTimeEntry>();
        entries.add(new SuiteTimeEntry(new File("com/foo/First.class").getPath(), 2l));
        entries.add(new SuiteTimeEntry(new File("com/foo/Second.class").getPath(), 5l));
        entries.add(new SuiteTimeEntry(new File("com/bar/Third.class").getPath(), 1l));
        entries.add(new SuiteTimeEntry(new File("foo/baz/Fourth.class").getPath(), 4l));
        entries.add(new SuiteTimeEntry(new File("foo/bar/Fourth.class").getPath(), 3l));
        return entries;
    }
}
