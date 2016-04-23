package tlb.splitter;

import org.junit.Before;
import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.service.GoServer;
import tlb.service.Server;
import tlb.service.TlbServer;
import tlb.utils.SystemEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CountBasedTestSplitterTest {
    private Server server;
    private TestUtil.LogFixture logFixture;

    private static final String moduleName = "module_foo";

    @Before
    public void setUp() throws Exception {
        server = mock(TlbServer.class);
        logFixture = new TestUtil.LogFixture();
    }

    @Test
    public void shouldConsumeAllTestsWhenNoJobsToBalanceWith() {
        when(server.totalPartitions()).thenReturn(1);
        when(server.partitionNumber()).thenReturn(1);

        SystemEnvironment env = TestUtil.initEnvironment("job-1");

        TlbSuiteFile first = new TlbSuiteFileImpl("first");
        TlbSuiteFile second = new TlbSuiteFileImpl("second");
        TlbSuiteFile third = new TlbSuiteFileImpl("third");
        List<TlbSuiteFile> resources = Arrays.asList(first, second, third);

        CountBasedTestSplitter countBasedTestSplitter = new CountBasedTestSplitter(server, env);
        CountBasedTestSplitter criteria = countBasedTestSplitter;
        assertThat(criteria.filterSuites(resources, moduleName), is(Arrays.asList(first, second, third)));
    }

    @Test
    public void shouldSplitTestsBasedOnSplitFactorForTheFirstJob() {
        when(server.totalPartitions()).thenReturn(2);
        when(server.partitionNumber()).thenReturn(1);
        when(server.partitionIdentifier()).thenReturn("job: 'job-1', version: 'build_no_19', partition: 1/2");

        SystemEnvironment env = TestUtil.initEnvironment("job-1");

        TlbSuiteFile first = new TlbSuiteFileImpl("first");
        TlbSuiteFile second = new TlbSuiteFileImpl("second");
        List<TlbSuiteFile> resources = Arrays.asList(first, second, new TlbSuiteFileImpl("third"), new TlbSuiteFileImpl("fourth"), new TlbSuiteFileImpl("fifth"));

        CountBasedTestSplitter criteria = new CountBasedTestSplitter(server, env);
        logFixture.startListening();
        assertThat(criteria.filterSuites(resources, moduleName), is(Arrays.asList(first, second)));
        logFixture.assertHeard("got total of 5 files to balance");
        logFixture.assertHeard("total jobs to distribute load [ 2 ]");
        logFixture.assertHeard("count balancing to approximately 2 files per job with 1 extra file to bucket");
        logFixture.assertHeard("assigned total of 2 files to [ job: 'job-1', version: 'build_no_19', partition: 1/2 ]");
    }

    @Test
    public void shouldSplitTestsBasedOnSplitFactorForTheSecondJob() {
        when(server.totalPartitions()).thenReturn(2);
        when(server.partitionNumber()).thenReturn(2);
        when(server.partitionIdentifier()).thenReturn("job: 'job-2', version: 'build_no_19', partition: 2/2");

        SystemEnvironment env = TestUtil.initEnvironment("job-2");

        TlbSuiteFile third = new TlbSuiteFileImpl("third");
        TlbSuiteFile fourth = new TlbSuiteFileImpl("fourth");
        TlbSuiteFile fifth = new TlbSuiteFileImpl("fifth");
        List<TlbSuiteFile> resources = Arrays.asList(new TlbSuiteFileImpl("first"), new TlbSuiteFileImpl("second"), third, fourth, fifth);

        CountBasedTestSplitter criteria = new CountBasedTestSplitter(server, env);
        logFixture.startListening();
        assertThat(criteria.filterSuites(resources, moduleName), is(Arrays.asList(third, fourth, fifth)));
        logFixture.assertHeard("got total of 5 files to balance");
        logFixture.assertHeard("total jobs to distribute load [ 2 ]");
        logFixture.assertHeard("count balancing to approximately 2 files per job with 1 extra file to bucket");
        logFixture.assertHeard("assigned total of 3 files to [ job: 'job-2', version: 'build_no_19', partition: 2/2 ]");
    }

    @Test
    public void shouldSplitTestsBalanced() {
        when(server.totalPartitions()).thenReturn(3);

        ArrayList<TlbSuiteFile> resources = new ArrayList<TlbSuiteFile>();

        for(int i = 0; i < 11; i++) {
            resources.add(new TlbSuiteFileImpl("base" + i));
        }

        logFixture.startListening();
        assertThat(criteria("job-foo", 1, 3).filterSuites(resources, moduleName), is(tlbFileResources(0, 1, 2)));
        logFixture.assertHeard("count balancing to approximately 3 files per job with 2 extra file to bucket");
        logFixture.assertHeard("assigned total of 3 files to [ job: 'job-foo', version: 'build_no_19', partition: 1/3 ]");

        assertThat(criteria("job-foo", 2, 3).filterSuites(resources, moduleName), is(tlbFileResources(3, 4, 5, 6)));
        logFixture.assertHeard("count balancing to approximately 3 files per job with 2 extra file to bucket", 2);
        logFixture.assertHeard("assigned total of 4 files to [ job: 'job-foo', version: 'build_no_19', partition: 2/3 ]");

        assertThat(criteria("job-foo", 3, 3).filterSuites(resources, moduleName), is(tlbFileResources(7, 8, 9, 10)));
        logFixture.assertHeard("count balancing to approximately 3 files per job with 2 extra file to bucket", 3);
        logFixture.assertHeard("assigned total of 4 files to [ job: 'job-foo', version: 'build_no_19', partition: 3/3 ]");
    }

    @Test
    public void shouldSplitTestsWhenTheSplitsAreMoreThanTests() {
        when(server.totalPartitions()).thenReturn(3);

        List<TlbSuiteFile> resources = tlbFileResources(0, 1);


        assertThat(criteria("job-1", 1, 3).filterSuites(resources, moduleName), is(tlbFileResources()));
        assertThat(criteria("job-2", 2, 3).filterSuites(resources, moduleName), is(tlbFileResources(0)));

        assertThat(criteria("job-3", 3, 3).filterSuites(resources, moduleName), is(tlbFileResources(1)));
    }

    @Test
    public void shouldSplitTestsWhenTheSplitsIsEqualToNumberOfTests() {
        when(server.totalPartitions()).thenReturn(3);

        List<TlbSuiteFile> resources = tlbFileResources(0, 1, 2);

        assertThat(criteria("job", 1, 3).filterSuites(resources, moduleName), is(tlbFileResources(0)));
        assertThat(criteria("job", 2, 3).filterSuites(resources, moduleName), is(tlbFileResources(1)));
        assertThat(criteria("job", 3, 3).filterSuites(resources, moduleName), is(tlbFileResources(2)));
    }

    @Test//to assertain it really works as expected
    public void shouldSplitTestsBalancedFor37testsAcross7Jobs() {
        when(server.totalPartitions()).thenReturn(7);

        int[] fileNumbers = new int[37];
        for(int i = 0; i < 37; i++) {
            fileNumbers[i] = i;
        }
        List<TlbSuiteFile> resources = tlbFileResources(fileNumbers);

        assertThat(criteria("job", 1, 7).filterSuites(resources, moduleName), is(tlbFileResources(0, 1, 2, 3, 4))); //2/7

        assertThat(criteria("job", 2, 7).filterSuites(resources, moduleName), is(tlbFileResources(5, 6, 7, 8, 9))); //4/7

        assertThat(criteria("job", 3, 7).filterSuites(resources, moduleName), is(tlbFileResources(10, 11, 12, 13, 14))); //6/7

        assertThat(criteria("job", 4, 7).filterSuites(resources, moduleName), is(tlbFileResources(15, 16, 17, 18, 19, 20))); //1/7

        assertThat(criteria("job", 5, 7).filterSuites(resources, moduleName), is(tlbFileResources(21, 22, 23, 24, 25))); //3/7

        assertThat(criteria("job", 6, 7).filterSuites(resources, moduleName), is(tlbFileResources(26, 27, 28, 29, 30))); //5/7

        assertThat(criteria("job", 7, 7).filterSuites(resources, moduleName), is(tlbFileResources(31, 32, 33, 34, 35, 36))); //7/7
    }

    @Test//to assertain it really works as expected
    public void shouldSplitTestsBalancedFor41testsAcross7Jobs() {
        when(server.totalPartitions()).thenReturn(7);

        int[] fileNumbers = new int[41];
        for(int i = 0; i < 41; i++) {
            fileNumbers[i] = i;
        }
        List<TlbSuiteFile> resources = tlbFileResources(fileNumbers);

        assertThat(criteria("job", 1, 7).filterSuites(resources, moduleName), is(tlbFileResources(0, 1, 2, 3, 4))); //6/7

        assertThat(criteria("job", 2, 7).filterSuites(resources, moduleName), is(tlbFileResources(5, 6, 7, 8, 9, 10))); //12/7 = 5/7

        assertThat(criteria("job", 3, 7).filterSuites(resources, moduleName), is(tlbFileResources(11, 12, 13, 14, 15, 16))); //18/7 = 4/7

        assertThat(criteria("job", 4, 7).filterSuites(resources, moduleName), is(tlbFileResources(17, 18, 19, 20, 21, 22))); //24/7 = 3/7

        assertThat(criteria("job", 5, 7).filterSuites(resources, moduleName), is(tlbFileResources(23, 24, 25, 26, 27, 28))); //30/7 = 2/7

        assertThat(criteria("job", 6, 7).filterSuites(resources, moduleName), is(tlbFileResources(29, 30, 31, 32, 33, 34))); //36/7 = 1/7

        assertThat(criteria("job", 7, 7).filterSuites(resources, moduleName), is(tlbFileResources(35, 36, 37, 38, 39, 40))); //42/7 = 7/7
    }

    @Test//to assertain it really works as expected
    public void shouldSplitTestsBalancedFor36testsAcross6Jobs() {
        when(server.totalPartitions()).thenReturn(6);

        int[] fileNumbers = new int[36];
        for(int i = 0; i < 36; i++) {
            fileNumbers[i] = i;
        }
        List<TlbSuiteFile> resources = tlbFileResources(fileNumbers);

        assertThat(criteria("job-1", 1, 6).filterSuites(resources, moduleName), is(tlbFileResources(0, 1, 2, 3, 4, 5)));

        assertThat(criteria("job-2", 2, 6).filterSuites(resources, moduleName), is(tlbFileResources(6, 7, 8, 9, 10, 11)));

        assertThat(criteria("job-3", 3, 6).filterSuites(resources, moduleName), is(tlbFileResources(12, 13, 14, 15, 16, 17)));

        assertThat(criteria("job-4", 4, 6).filterSuites(resources, moduleName), is(tlbFileResources(18, 19, 20, 21, 22, 23)));

        assertThat(criteria("job-5", 5, 6).filterSuites(resources, moduleName), is(tlbFileResources(24, 25, 26, 27, 28, 29)));

        assertThat(criteria("job-6", 6, 6).filterSuites(resources, moduleName), is(tlbFileResources(30, 31,  32, 33, 34, 35)));
    }

    private CountBasedTestSplitter criteria(String jobName, int partitionNumber, int totalPartitions) {
        when(server.partitionNumber()).thenReturn(partitionNumber);
        when(server.partitionIdentifier()).thenReturn(String.format("job: '%s', version: 'build_no_19', partition: %s/%s", jobName, partitionNumber, totalPartitions));
        return new CountBasedTestSplitter(server, TestUtil.initEnvironment(jobName));
    }
    
    private List<TlbSuiteFile> tlbFileResources(int... numbers) {
        List<TlbSuiteFile> resources = new ArrayList<TlbSuiteFile>();
        for (int number : numbers) {
            resources.add(new TlbSuiteFileImpl("base" + number));
        }
        return resources;
    }
}
