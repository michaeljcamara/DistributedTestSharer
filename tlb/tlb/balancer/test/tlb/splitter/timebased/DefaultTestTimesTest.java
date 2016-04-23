package tlb.splitter.timebased;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.domain.SuiteTimeEntry;
import tlb.service.Server;
import tlb.splitter.TimeBasedTestSplitter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tlb.TestUtil.convertToPlatformSpecificPath;

public class DefaultTestTimesTest {

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
    public void shouldDistributeUnknownTestsBasedOnAverageTime() throws Exception{
        when(server.totalPartitions()).thenReturn(2);
        when(server.getLastRunTestTimes()).thenReturn(testTimes());

        TlbSuiteFile a1 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A1.class"));
        TlbSuiteFile a2 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A2.class"));
        TlbSuiteFile a3 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A3.class"));
        TlbSuiteFile a4 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A4.class"));
        TlbSuiteFile a5 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A5.class"));
        TlbSuiteFile a6 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A6.class"));
        TlbSuiteFile a7 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A7.class"));
        TlbSuiteFile a8 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A8.class"));
        TlbSuiteFile a9 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A9.class"));
        TlbSuiteFile a10 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A10.class"));
        TlbSuiteFile a11 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A11.class"));
        TlbSuiteFile a12 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A12.class"));
        TlbSuiteFile a13 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A13.class"));
        TlbSuiteFile a14 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A14.class"));
        TlbSuiteFile a15 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A15.class"));
        TlbSuiteFile a16 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A16.class"));
        TlbSuiteFile a17 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/A17.class"));
        TlbSuiteFile b1 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/B1.class"));
        TlbSuiteFile b2 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/B2.class"));
        TlbSuiteFile b3 = new TlbSuiteFileImpl(convertToPlatformSpecificPath("c/f/B3.class"));
        List<TlbSuiteFile> resources = Arrays.asList(a1, a2, a3, a4, a5, a6, a7, a8, a9,  a10, a11, a12, a13, a14, a15, a16, a17, b1,b2, b3);

        logFixture.startListening();
        when(server.partitionNumber()).thenReturn(1);
        TimeBasedTestSplitter criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-1"));
        List<TlbSuiteFile> filteredResources = criteria.filterSuites(resources, moduleName);

        assertThat(filteredResources.size(), is(5));
        assertThat(filteredResources, hasItems(a9, a14, a16, a7, a17));
        logFixture.assertHeard("Encountered 3 new files which don't have historical time data, used average time [ 118.5 ] to balance");

        when(server.partitionNumber()).thenReturn(2);
        criteria = new TimeBasedTestSplitter(server, TestUtil.initEnvironment("job-2"));
        filteredResources = criteria.filterSuites(resources, moduleName);
        assertThat(filteredResources.size(), is(15));
        assertThat(filteredResources, hasItems(a1, a2, a3, a4, a5, a6, a8, a10, a11, a12, a13, a15, b1, b2, b3));
    }

    private List<SuiteTimeEntry> testTimes() {
        List<SuiteTimeEntry> entries = new ArrayList<SuiteTimeEntry>();
        entries.add(new SuiteTimeEntry(new File("c/f/A1.class").getPath(), 2l));
        entries.add(new SuiteTimeEntry(new File("c/f/A2.class").getPath(), 5l));
        entries.add(new SuiteTimeEntry(new File("c/f/A3.class").getPath(), 1l));
        entries.add(new SuiteTimeEntry(new File("c/f/A4.class").getPath(), 4l));
        entries.add(new SuiteTimeEntry(new File("c/f/A5.class").getPath(), 3l));
        entries.add(new SuiteTimeEntry(new File("c/f/A6.class").getPath(), 30l));
        entries.add(new SuiteTimeEntry(new File("c/f/A7.class").getPath(), 31l));
        entries.add(new SuiteTimeEntry(new File("c/f/A8.class").getPath(), 32l));
        entries.add(new SuiteTimeEntry(new File("c/f/A9.class").getPath(), 990l));
        entries.add(new SuiteTimeEntry(new File("c/f/A10.class").getPath(), 300l));
        entries.add(new SuiteTimeEntry(new File("c/f/A11.class").getPath(), 200l));
        entries.add(new SuiteTimeEntry(new File("c/f/A12.class").getPath(), 100l));
        entries.add(new SuiteTimeEntry(new File("c/f/A13.class").getPath(), 110l));
        entries.add(new SuiteTimeEntry(new File("c/f/A13.class").getPath(), 101l));
        entries.add(new SuiteTimeEntry(new File("c/f/A14.class").getPath(), 101l));
        entries.add(new SuiteTimeEntry(new File("c/f/A15.class").getPath(), 101l));
        entries.add(new SuiteTimeEntry(new File("c/f/A16.class").getPath(), 98l));
        entries.add(new SuiteTimeEntry(new File("c/f/A17.class").getPath(), 23l));
        entries.add(new SuiteTimeEntry(new File("c/f/A18.class").getPath(), 66l));
        entries.add(new SuiteTimeEntry(new File("c/f/A19.class").getPath(), 72l));
        return entries;
    }
}
