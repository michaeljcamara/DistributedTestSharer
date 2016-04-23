package tlb.service;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.domain.SuiteResultEntry;
import tlb.domain.SuiteTimeEntry;
import tlb.service.http.HttpAction;
import tlb.utils.FileUtil;
import tlb.utils.SystemEnvironment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static junit.framework.Assert.fail;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static tlb.TestUtil.fileContents;
import static tlb.TlbConstants.Go;
import static tlb.TlbConstants.TLB_TMP_DIR;

@RunWith(Theories.class)
public class GoServerTest {
    private GoServer server;
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
    public void shouldReturnTheListOfJobsIntheGivenStage() throws Exception {
        SystemEnvironment environment = initEnvironment("http://test.host:8153/cruise");
        assertCanFindJobsFrom("http://test.host:8153/cruise", environment);
    }

    @Test
    public void shouldIdentifyPartitionCorrectly() throws Exception {
        Map<String, String> envMap = initEnvMap("http://test.host:8153/go");
        envMap.put(TlbConstants.Go.GO_JOB_NAME, "firefox-2");
        SystemEnvironment environment = new SystemEnvironment(envMap);
        try {
            HttpAction action = mock(HttpAction.class);

            when(action.get("http://test.host:8153/go/pipelines/pipeline-foo/26/stage-foo-bar/1.xml")).thenReturn(TestUtil.fileContents("resources/stage_detail_with_jobs_in_random_order.xml"));
            stubJobDetails(action);

            server = new GoServer(environment, action);
            assertThat(server.partitionIdentifier(), is("job: 'firefox-2', partition: 2/3"));
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    @Test
    public void shouldUnderstandPartitionsForPearJobsIdentifiedByNumber() throws Exception{
        Map<String, String> envMap = initEnvMap("http://test.host:8153/go");
        envMap.put(TlbConstants.Go.GO_JOB_NAME, "firefox-2");
        SystemEnvironment environment = new SystemEnvironment(envMap);
        try {
            HttpAction action = mock(HttpAction.class);

            when(action.get("http://test.host:8153/go/pipelines/pipeline-foo/26/stage-foo-bar/1.xml")).thenReturn(TestUtil.fileContents("resources/stage_detail_with_jobs_in_random_order.xml"));
            stubJobDetails(action);

            server = new GoServer(environment, action);
            assertThat(server.getJobs(), is(Arrays.asList("firefox-3", "rails", "firefox-1", "smoke", "firefox-2")));
            assertThat(server.pearJobs(), is(Arrays.asList("firefox-1", "firefox-2", "firefox-3")));
            assertThat(server.totalPartitions(), is(3));
            assertThat(server.partitionNumber(), is(2));
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    @Test
    public void shouldUnderstandPartitionsForPearJobsIdentifiedOnUUID() throws Exception{
        Map<String, String> envMap = initEnvMap("http://test.host:8153/go");
        envMap.put(Go.GO_JOB_NAME, "firefox-bbcdef12-1234-1234-1234-abcdef123456");
        SystemEnvironment environment = new SystemEnvironment(envMap);

        try {
            HttpAction action = mock(HttpAction.class);

            when(action.get("http://test.host:8153/go/pipelines/pipeline-foo/26/stage-foo-bar/1.xml")).thenReturn(TestUtil.fileContents("resources/stage_detail_with_jobs_in_random_order.xml"));
            when(action.get("http://test.host:8153/go/api/jobs/140.xml")).thenReturn(TestUtil.fileContents("resources/job_details_140_UUID.xml"));
            when(action.get("http://test.host:8153/go/api/jobs/139.xml")).thenReturn(TestUtil.fileContents("resources/job_details_139.xml"));
            when(action.get("http://test.host:8153/go/api/jobs/141.xml")).thenReturn(TestUtil.fileContents("resources/job_details_141_UUID.xml"));
            when(action.get("http://test.host:8153/go/api/jobs/142.xml")).thenReturn(TestUtil.fileContents("resources/job_details_142_UUID.xml"));
            when(action.get("http://test.host:8153/go/api/jobs/143.xml")).thenReturn(TestUtil.fileContents("resources/job_details_143.xml"));

            server = new GoServer(environment, action);
            assertThat(server.getJobs(), is(Arrays.asList("firefox-cbcdef12-1234-1234-1234-abcdef123456", "rails", "firefox-abcdef12-1234-1234-1234-abcdef123456", "smoke", "firefox-bbcdef12-1234-1234-1234-abcdef123456")));
            assertThat(server.pearJobs(), is(Arrays.asList("firefox-abcdef12-1234-1234-1234-abcdef123456", "firefox-bbcdef12-1234-1234-1234-abcdef123456", "firefox-cbcdef12-1234-1234-1234-abcdef123456")));
            assertThat(server.totalPartitions(), is(3));
            assertThat(server.partitionNumber(), is(2));
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    @Test
    public void shouldWorkWithUrlHavingPathWithTrailingSlash() throws Exception {
        SystemEnvironment environment = initEnvironment("https://test.host:8154/go/");
        assertCanFindJobsFrom("https://test.host:8154/go", environment);
    }

    @Test
    public void shouldUpdateCruiseArtifactWithTestTimeUsingPUT() throws Exception {
        SystemEnvironment environment = initEnvironment("http://test.host:8153/go");
        try {
            HttpAction action = mock(HttpAction.class);
            String data = "com.thoughtworks.tlb.TestSuite: 12\n";
            String url = "http://test.host:8153/go/files/pipeline-foo/pipeline-foo-26/stage-foo-bar/1/job-baz/" + GoServer.TEST_TIME_FILE;

            FileUtil fileUtil = new FileUtil(environment);
            SmoothingServerTest.clearCachingFiles(fileUtil);
            GoServer cruise = new GoServer(environment, action);
            cruise.subsetSizeRepository.appendLine("1\n");

            logFixture.startListening();
            cruise.testClassTime("com.thoughtworks.tlb.TestSuite", 12);
            logFixture.assertHeard("recording run time for suite com.thoughtworks.tlb.TestSuite");
            logFixture.assertHeard("Posting test run times for suite with size 1 to the server.");
            verify(action).put(url, data);
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    @Test
    public void shouldAppendToSubsetSizeArtifactForMultipleCalls() throws Exception{
        SystemEnvironment environment = initEnvironment("http://test.host:8153/go");
        try {
            HttpAction action = mock(HttpAction.class);
            when(action.put("http://test.host:8153/go/files/pipeline-foo/pipeline-foo-26/stage-foo-bar/1/job-baz/tlb/subset_size", "10")).thenReturn("File tlb/subset_size was appended successfully");
            when(action.put("http://test.host:8153/go/files/pipeline-foo/pipeline-foo-26/stage-foo-bar/1/job-baz/tlb/subset_size", "20")).thenReturn("File tlb/subset_size was appended successfully");
            when(action.put("http://test.host:8153/go/files/pipeline-foo/pipeline-foo-26/stage-foo-bar/1/job-baz/tlb/subset_size", "25")).thenReturn("File tlb/subset_size was appended successfully");
            GoServer toCruise = new GoServer(environment, action);
            FileUtil fileUtil = new FileUtil(environment);
            SmoothingServerTest.clearCachingFiles(fileUtil);
            logFixture.startListening();
            toCruise.publishSubsetSize(10);
            logFixture.assertHeard("Posting balanced subset size as 10 to cruise server");
            List<String> times = new ArrayList<String>();
            times.add("10");
            assertThat(toCruise.subsetSizeRepository.loadLines(), is(times));
            toCruise.publishSubsetSize(20);
            logFixture.assertHeard("Posting balanced subset size as 20 to cruise server");
            times.add("20");
            assertThat(toCruise.subsetSizeRepository.loadLines(), is(times));
            toCruise.publishSubsetSize(25);
            logFixture.assertHeard("Posting balanced subset size as 25 to cruise server");
            times.add("25");
            assertThat(toCruise.subsetSizeRepository.loadLines(), is(times));
            verify(action).put("http://test.host:8153/go/files/pipeline-foo/pipeline-foo-26/stage-foo-bar/1/job-baz/tlb/subset_size", "10\n");
            verify(action).put("http://test.host:8153/go/files/pipeline-foo/pipeline-foo-26/stage-foo-bar/1/job-baz/tlb/subset_size", "20\n");
            verify(action).put("http://test.host:8153/go/files/pipeline-foo/pipeline-foo-26/stage-foo-bar/1/job-baz/tlb/subset_size", "25\n");
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    @Test
    public void shouldUpdateCruiseArtifactWithTestTimeUsingPUTOnlyOnTheLastSuite() throws Exception {
        SystemEnvironment env = initEnvironment("http://test.host:8153/go");
        FileUtil fileUtil = new FileUtil(env);
        try {
            HttpAction action = mock(HttpAction.class);
            String data = "com.thoughtworks.tlb.TestSuite: 12\n" +
                    "com.thoughtworks.tlb.TestTimeBased: 15\n" +
                    "com.thoughtworks.tlb.TestCountBased: 10\n" +
                    "com.thoughtworks.tlb.TestCriteriaSelection: 30\n" +
                    "com.thougthworks.tlb.SystemEnvTest: 8\n";
            String url = "http://test.host:8153/go/files/pipeline-foo/pipeline-foo-26/stage-foo-bar/1/job-baz/" + GoServer.TEST_TIME_FILE;

            GoServer cruise = new GoServer(env, action);
            SmoothingServerTest.clearCachingFiles(fileUtil);
            cruise.subsetSizeRepository.appendLine("5\n");
            cruise.testClassTime("com.thoughtworks.tlb.TestSuite", 12);
            SmoothingServerTest.assertCacheState(env, 1, "com.thoughtworks.tlb.TestSuite: 12", cruise.testTimesRepository);
            cruise.testClassTime("com.thoughtworks.tlb.TestTimeBased", 15);
            SmoothingServerTest.assertCacheState(env, 2, "com.thoughtworks.tlb.TestTimeBased: 15", cruise.testTimesRepository);
            cruise.testClassTime("com.thoughtworks.tlb.TestCountBased", 10);
            SmoothingServerTest.assertCacheState(env, 3, "com.thoughtworks.tlb.TestCountBased: 10", cruise.testTimesRepository);
            cruise.testClassTime("com.thoughtworks.tlb.TestCriteriaSelection", 30);
            SmoothingServerTest.assertCacheState(env, 4, "com.thoughtworks.tlb.TestCriteriaSelection: 30", cruise.testTimesRepository);

            when(action.put(url, data)).thenReturn("File tlb/test_time.properties was appended successfully");


            cruise.testClassTime("com.thougthworks.tlb.SystemEnvTest", 8);
            assertThat(fileUtil.getUniqueFile(cruise.jobLocator).exists(), is(false));

            verify(action).put(url, data);
        } finally {
            SmoothingServerTest.clearCachingFiles(fileUtil);
        }
    }

    @Test
    public void shouldUpdateCruiseArtifactWithFailedTestListUsingPUTOnlyOnTheLastSuite() throws Exception {
        SystemEnvironment env = initEnvironment("http://test.host:8153/go");
        try {
            HttpAction action = mock(HttpAction.class);
            String data = "com.thoughtworks.tlb.FailedSuiteOne\n" +
                    "com.thoughtworks.tlb.FailedSuiteTwo\n" +
                    "com.thoughtworks.tlb.FailedSuiteThree\n";
            String url = "http://test.host:8153/go/files/pipeline-foo/pipeline-foo-26/stage-foo-bar/1/job-baz/" + GoServer.FAILED_TESTS_FILE;

            GoServer cruise = new GoServer(env, action);
            SmoothingServerTest.clearCachingFiles(new FileUtil(env));
            cruise.subsetSizeRepository.appendLine("3\n10\n6\n");
            cruise.testClassTime("com.thoughtworks.tlb.PassingSuite", 10l);
            cruise.testClassFailure("com.thoughtworks.tlb.PassingSuite", false);
            SmoothingServerTest.assertCacheState(env, 1, "com.thoughtworks.tlb.PassingSuite: false", cruise.failedTestsRepository);
            cruise.testClassFailure("com.thoughtworks.tlb.FailedSuiteOne", true);
            SmoothingServerTest.assertCacheState(env, 2, "com.thoughtworks.tlb.FailedSuiteOne: true", cruise.failedTestsRepository);
            cruise.testClassFailure("com.thoughtworks.tlb.FailedSuiteTwo", true);
            SmoothingServerTest.assertCacheState(env, 3, "com.thoughtworks.tlb.FailedSuiteTwo: true", cruise.failedTestsRepository);
            cruise.testClassFailure("com.thoughtworks.tlb.PassingSuiteTwo", false);
            SmoothingServerTest.assertCacheState(env, 4, "com.thoughtworks.tlb.PassingSuiteTwo: false", cruise.failedTestsRepository);
            cruise.testClassFailure("com.thoughtworks.tlb.FailedSuiteThree", true);
            SmoothingServerTest.assertCacheState(env, 5, "com.thoughtworks.tlb.FailedSuiteThree: true", cruise.failedTestsRepository);

            when(action.put(url, data)).thenReturn("File tlb/failed_tests was appended successfully");

            cruise.testClassFailure("com.thoughtworks.tlb.PassingSuiteThree", false);

            assertThat(cruise.failedTestsRepository.getFile().exists(), is(false));
            assertThat(cruise.subsetSizeRepository.getFile().exists(), is(true));
            assertThat(cruise.testTimesRepository.getFile().exists(), is(true));
            assertThat(cruise.oldTestTimesRepo.getFile().exists(), is(true));
            //should not clear files as test time post(which happens after this) needs it

            verify(action).put(url, data);
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(env));
        }
    }

    @Test
    public void shouldUpdateCruiseArtifactWithTestTimeUsingPUTOnlyOnTheLastSuiteAccordingToLastSubsetSizeEntry() throws Exception {
        SystemEnvironment env = initEnvironment("http://test.host:8153/go");
        FileUtil fileUtil = new FileUtil(env);
        try {
            HttpAction action = mock(HttpAction.class);
            String data = "com.thoughtworks.tlb.TestSuite: 12\n" +
                    "com.thoughtworks.tlb.TestCriteriaSelection: 30\n" +
                    "com.thougthworks.tlb.SystemEnvTest: 8\n";
            String url = "http://test.host:8153/go/files/pipeline-foo/pipeline-foo-26/stage-foo-bar/1/job-baz/" + GoServer.TEST_TIME_FILE;

            GoServer cruise = new GoServer(env, action);
            SmoothingServerTest.clearCachingFiles(fileUtil);
            cruise.subsetSizeRepository.appendLine("5\n10\n3\n");
            cruise.testClassTime("com.thoughtworks.tlb.TestSuite", 12);
            SmoothingServerTest.assertCacheState(env, 1, "com.thoughtworks.tlb.TestSuite: 12", cruise.testTimesRepository);
            cruise.testClassTime("com.thoughtworks.tlb.TestCriteriaSelection", 30);
            SmoothingServerTest.assertCacheState(env, 2, "com.thoughtworks.tlb.TestCriteriaSelection: 30", cruise.testTimesRepository);

            when(action.put(url, data)).thenReturn("File tlb/test_time.properties was appended successfully");


            cruise.testClassTime("com.thougthworks.tlb.SystemEnvTest", 8);
            assertThat(fileUtil.getUniqueFile(cruise.jobLocator).exists(), is(false));

            verify(action).put(url, data);
        } finally {
            SmoothingServerTest.clearCachingFiles(fileUtil);
        }
    }

    @Test
    public void shouldUpdateCruiseArtifactWithSmoothenedTestTimes() throws Exception {
        Map<String, String> envMap = initEnvMap("http://test.host:8153/go");
        envMap.put(TlbConstants.Go.GO_JOB_NAME, "firefox-2");
        envMap.put(TlbConstants.TLB_SMOOTHING_FACTOR.key, "0.5");
        SystemEnvironment env = new SystemEnvironment(envMap);
        FileUtil fileUtil = new FileUtil(env);

        try {
            HttpAction action = mock(HttpAction.class);

            when(action.get("http://test.host:8153/go/pipelines/pipeline-foo/26/stage-foo-bar/1.xml")).thenReturn(TestUtil.fileContents("resources/stage_detail_with_jobs_in_random_order.xml"));
            stubJobDetails(action);

            server = new GoServer(env, action);

            when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml")).thenReturn(fileContents("resources/stages_p1.xml"));

            when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml?before=23")).thenReturn(fileContents("resources/stages_p2.xml"));
            when(action.get("http://test.host:8153/go/api/stages/3.xml")).thenReturn(fileContents("resources/stage_detail.xml"));

            when(action.get("http://test.host:8153/go/files/pipeline/1/stage/1/firefox-1/tlb/test_time.properties")).thenReturn(fileContents("resources/test_time_1.properties"));
            when(action.get("http://test.host:8153/go/files/pipeline/1/stage/1/firefox-2/tlb/test_time.properties")).thenReturn(fileContents("resources/test_time_2.properties"));
            when(action.get("http://test.host:8153/go/files/pipeline/1/stage/1/firefox-3/tlb/test_time.properties")).thenReturn("");

            String data = "com.thoughtworks.cruise.one.One: 55\n" +
                    "com.thoughtworks.cruise.two.Two: 30\n" +
                    "com.thoughtworks.cruise.three.Three: 20\n";
            String url = "http://test.host:8153/go/files/pipeline-foo/pipeline-foo-26/stage-foo-bar/1/firefox-2/" + GoServer.TEST_TIME_FILE;

            GoServer cruise = new GoServer(env, action);
            SmoothingServerTest.clearCachingFiles(fileUtil);
            cruise.subsetSizeRepository.appendLine("5\n10\n3\n");
            cruise.testClassTime("com.thoughtworks.cruise.one.One", 100);
            SmoothingServerTest.assertCacheState(env, 1, "com.thoughtworks.cruise.one.One: 55", cruise.testTimesRepository);
            cruise.testClassTime("com.thoughtworks.cruise.two.Two", 40);
            SmoothingServerTest.assertCacheState(env, 2, "com.thoughtworks.cruise.two.Two: 30", cruise.testTimesRepository);

            when(action.put(url, data)).thenReturn("File tlb/test_time.properties was appended successfully");


            cruise.testClassTime("com.thoughtworks.cruise.three.Three", 10);
            assertThat(fileUtil.getUniqueFile(cruise.jobLocator).exists(), is(false));

            verify(action).put(url, data);
        } finally {
            SmoothingServerTest.clearCachingFiles(fileUtil);
        }
    }

    @Test
    public void shouldPublishSubsetSizeAsALineAppendedToJobArtifact() throws Exception{
        SystemEnvironment environment = initEnvironment("http://test.host:8153/go");
        try {
            HttpAction action = mock(HttpAction.class);
            when(action.put("http://test.host:8153/go/files/pipeline-foo/pipeline-foo-26/stage-foo-bar/1/job-baz/tlb/subset_size", "10\n")).thenReturn("File tlb/subset_size was appended successfully");
            Server toService = new GoServer(environment, action);
            toService.publishSubsetSize(10);
            verify(action).put("http://test.host:8153/go/files/pipeline-foo/pipeline-foo-26/stage-foo-bar/1/job-baz/tlb/subset_size", "10\n");
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    @Test
    public void shouldFindFailedTestsFromTheLastRunStage() throws Exception{
        HttpAction action = mock(HttpAction.class);
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml")).thenReturn(TestUtil.fileContents("resources/stages_p1.xml"));
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml?before=23")).thenReturn(TestUtil.fileContents("resources/stages_p2.xml"));
        when(action.get("http://test.host:8153/go/api/stages/3.xml")).thenReturn(TestUtil.fileContents("resources/stage_detail.xml"));
        stubJobDetails(action);
        when(action.get("http://test.host:8153/go/files/pipeline/1/stage/1/firefox-1/tlb/failed_tests")).thenReturn(TestUtil.fileContents("resources/failed_tests_1.properties"));
        when(action.get("http://test.host:8153/go/files/pipeline/1/stage/1/firefox-2/tlb/failed_tests")).thenReturn(TestUtil.fileContents("resources/failed_tests_2.properties"));
        SystemEnvironment environment = initEnvironment("http://test.host:8153/go");
        try {
            GoServer service = new GoServer(environment, action);
            List<SuiteResultEntry> failedTestEntries = service.getLastRunFailedTests(Arrays.asList("firefox-1", "firefox-2"));
            List<String> failedTests = failedTestNames(failedTestEntries);
            Collections.sort(failedTests);
            assertThat(failedTests, is(Arrays.asList("com.thoughtworks.cruise.AnotherFailedTest", "com.thoughtworks.cruise.FailedTest", "com.thoughtworks.cruise.YetAnotherFailedTest")));
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    @Test
    public void shouldNotFailWhenFailedTestsNotAvailableInLastRun() throws Exception{
        HttpAction action = mock(HttpAction.class);
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml")).thenReturn(TestUtil.fileContents("resources/stages_p1.xml"));
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml?before=23")).thenReturn(TestUtil.fileContents("resources/stages_p2.xml"));
        when(action.get("http://test.host:8153/go/api/stages/3.xml")).thenReturn(TestUtil.fileContents("resources/stage_detail.xml"));
        stubJobDetails(action);
        when(action.get("http://test.host:8153/go/files/pipeline/1/stage/1/firefox-1/tlb/failed_tests")).thenThrow(new RuntimeException("Something went wrong"));
        when(action.get("http://test.host:8153/go/files/pipeline/1/stage/1/firefox-2/tlb/failed_tests")).thenThrow(new RuntimeException("Something else went wrong"));
        SystemEnvironment environment = initEnvironment("http://test.host:8153/go");
        try {
            GoServer service = new GoServer(environment, action);
            List<SuiteResultEntry> failedTestEntries = null;
            try {
                failedTestEntries = service.getLastRunFailedTests(Arrays.asList("firefox-1", "firefox-2"));
                assertThat(failedTestEntries.isEmpty(), is(true));
            } catch (Exception e) {
                fail("should not have failed in absence of failed tests data, but failed with exception => " + e.getMessage());
            }

            Exception exception = new RuntimeException("something went really wrong!");
            when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml?before=23")).thenThrow(exception);
            logFixture.startListening();
            try {
                failedTestEntries = service.getLastRunFailedTests(Arrays.asList("firefox-1", "firefox-2"));
                assertThat(failedTestEntries.isEmpty(), is(true));
            } catch (Exception e) {
                fail("should not have failed in absence of failed tests data, but failed with exception => " + e.getMessage());
            }
            logFixture.stopListening();
            logFixture.assertHeard("Couldn't find tests that failed in the last run");
            logFixture.assertHeardException(exception);
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    private List<String> failedTestNames(List<SuiteResultEntry> failedTestEntries) {
        ArrayList<String> failedTestNames = new ArrayList<String>();
        for (SuiteResultEntry failedTestEntry : failedTestEntries) {
            failedTestNames.add(failedTestEntry.getName());
        }
        return failedTestNames;
    }


    @Test
    public void shouldFindTestTimesFromLastRunStage() throws Exception{
        HttpAction action = mock(HttpAction.class);
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml")).thenReturn(fileContents("resources/stages_p1.xml"));
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml?before=23")).thenReturn(fileContents("resources/stages_p2.xml"));
        when(action.get("http://test.host:8153/go/api/stages/3.xml")).thenReturn(fileContents("resources/stage_detail.xml"));
        stubJobDetails(action);
        when(action.get("http://test.host:8153/go/files/pipeline/1/stage/1/firefox-1/tlb/test_time.properties")).thenReturn(fileContents("resources/test_time_1.properties"));
        when(action.get("http://test.host:8153/go/files/pipeline/1/stage/1/firefox-2/tlb/test_time.properties")).thenReturn(fileContents("resources/test_time_2.properties"));
        SystemEnvironment environment = initEnvironment("http://test.host:8153/go");
        try {
            GoServer service = new GoServer(environment, action);
            List<SuiteTimeEntry> runTimes = service.getLastRunTestTimes(Arrays.asList("firefox-1", "firefox-2"));
            List<SuiteTimeEntry> expected = new ArrayList<SuiteTimeEntry>();
            expected.add(new SuiteTimeEntry("com.thoughtworks.cruise.one.One", 10l));
            expected.add(new SuiteTimeEntry("com.thoughtworks.cruise.two.Two", 20l));
            expected.add(new SuiteTimeEntry("com.thoughtworks.cruise.three.Three", 30l));
            expected.add(new SuiteTimeEntry("com.thoughtworks.cruise.four.Four", 40l));
            expected.add(new SuiteTimeEntry("com.thoughtworks.cruise.five.Five", 50l));
            assertThat(runTimes, is(expected));
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    @Test
    public void shouldFindTestTimesFromLastRunStageWhenDeepDownFeedLinks() throws Exception{
        HttpAction action = mock(HttpAction.class);

        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml")).thenReturn(TestUtil.fileContents("resources/stages_p1.xml"));
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml?before=23")).thenReturn(TestUtil.fileContents("resources/stages_p2.xml"));
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml?before=19")).thenReturn(TestUtil.fileContents("resources/stages_p3.xml"));
        when(action.get("http://test.host:8153/go/api/stages/2.xml")).thenReturn(TestUtil.fileContents("resources/stage_detail.xml"));

        stubJobDetails(action);
        when(action.get("http://test.host:8153/go/files/pipeline/1/stage/1/firefox-1/tlb/test_time.properties")).thenReturn(fileContents("resources/test_time_1.properties"));
        when(action.get("http://test.host:8153/go/files/pipeline/1/stage/1/firefox-2/tlb/test_time.properties")).thenReturn(fileContents("resources/test_time_2_with_new_lines.properties"));
        Map<String, String> envMap = initEnvMap("http://test.host:8153/go");
        envMap.put(TlbConstants.Go.GO_PIPELINE_NAME, "pipeline-foo");
        envMap.put(TlbConstants.Go.GO_STAGE_NAME, "stage-foo-quux");
        SystemEnvironment environment = new SystemEnvironment(envMap);

        try {
            GoServer service = new GoServer(environment, action);
            List<SuiteTimeEntry> runTimes = service.getLastRunTestTimes(Arrays.asList("firefox-1", "firefox-2"));
            List<SuiteTimeEntry> expected = new ArrayList<SuiteTimeEntry>();
            expected.add(new SuiteTimeEntry("com.thoughtworks.cruise.one.One", 10l));
            expected.add(new SuiteTimeEntry("com.thoughtworks.cruise.two.Two", 20l));
            expected.add(new SuiteTimeEntry("com.thoughtworks.cruise.three.Three", 30l));
            expected.add(new SuiteTimeEntry("com.thoughtworks.cruise.four.Four", 40l));
            expected.add(new SuiteTimeEntry("com.thoughtworks.cruise.five.Five", 50l));
            assertThat(runTimes, is(expected));
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    @Test
    public void failWhenCantFindTestTimesFromLastRunStage() throws Exception{
        HttpAction action = mock(HttpAction.class);
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml")).thenReturn(fileContents("resources/stages_p3.xml"));
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml?before=17")).thenReturn(fileContents("resources/stages_p4.xml"));
        when(action.get("http://test.host:8153/go/api/stages/3.xml")).thenReturn(fileContents("resources/stage_detail.xml"));
        stubJobDetails(action);
        when(action.get("http://test.host:8153/go/files/pipeline/1/stage/1/firefox-1/tlb/test_time.properties")).thenReturn(fileContents("resources/test_time_1.properties"));
        when(action.get("http://test.host:8153/go/files/pipeline/1/stage/1/firefox-2/tlb/test_time.properties")).thenReturn(fileContents("resources/test_time_2.properties"));
        SystemEnvironment environment = initEnvironment("http://test.host:8153/go");
        try {
            GoServer service = new GoServer(environment, action);
            try {
                service.getLastRunTestTimes(Arrays.asList("firefox-1", "firefox-2"));
                fail("should have failed as a historical stage run does not exist");
            } catch (Exception e) {
                assertThat(e, is(NullPointerException.class));
            }
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    @Test
    public void shouldStopAtDefinedDepthWhenTryingToFindHistoricalStageInstance() throws IOException, URISyntaxException {
        HttpAction action = mock(HttpAction.class);
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml")).thenReturn(fileContents("resources/stages_p1.xml"));
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml?before=23")).thenReturn(fileContents("resources/stages_p2.xml"));
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml?before=19")).thenReturn(fileContents("resources/stages_p1.xml"));
        Map<String, String> map = initEnvMap("http://test.host:8153/go");
        map.put(TlbConstants.Go.GO_PIPELINE_NAME, "pipeline-foo");
        map.put(TlbConstants.Go.GO_PIPELINE_LABEL, "pipeline-foo-3");
        map.put(TlbConstants.Go.GO_PIPELINE_COUNTER, "3");
        map.put(TlbConstants.Go.GO_STAGE_NAME, "fun-stage");
        SystemEnvironment environment = new SystemEnvironment(map);
        GoServer service;
        try {
            service = new GoServer(environment, action);
            try {
                service.getLastRunTestTimes(Arrays.asList("firefox-1", "firefox-2"));
                fail("should have failed as historical stage run does not exist in defined depth");
            } catch (IllegalStateException e) {
                assertThat(e, is(IllegalStateException.class));
                assertThat(e.getMessage(), is("Couldn't find a historical run for stage in '10' pages of stage feed."));
            }
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }

        map.put(TlbConstants.Go.GO_STAGE_FEED_MAX_SEARCH_DEPTH.key, "17");
        environment = new SystemEnvironment(map);
        try {
            service = new GoServer(environment, action);
            try {
                service.getLastRunTestTimes(Arrays.asList("firefox-1", "firefox-2"));
                fail("should have failed as historical stage run does not exist in defined depth");
            } catch (IllegalStateException e) {
                assertThat(e, is(IllegalStateException.class));
                assertThat(e.getMessage(), is("Couldn't find a historical run for stage in '17' pages of stage feed."));
            }
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }

        map.put(TlbConstants.Go.GO_STAGE_FEED_MAX_SEARCH_DEPTH.key, "2");
        when(action.get("http://test.host:8153/go/api/pipelines/pipeline-foo/stages.xml?before=19")).thenThrow(new AssertionError("requested more pages than permitted"));
        environment = new SystemEnvironment(map);
        try {
            service = new GoServer(environment, action);
            try {
                service.getLastRunTestTimes(Arrays.asList("firefox-1", "firefox-2"));
                fail("should have failed as historical stage run does not exist in defined depth");
            } catch (IllegalStateException e) {
                assertThat(e, is(IllegalStateException.class));
                assertThat(e.getMessage(), is("Couldn't find a historical run for stage in '2' pages of stage feed."));
            }
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    public static interface MethodInvocation {
        void invoke(Server server);
    }

    @DataPoint public static final MethodInvocation UNIVERSAL_SET_REPORTING_CALL = new MethodInvocation() {
        public void invoke(Server server) {
            server.validateUniversalSet(new ArrayList<TlbSuiteFile>(), "foo");
        }
    };

    @DataPoint public static final MethodInvocation SUB_SET_REPORTING_CALL = new MethodInvocation() {
        public void invoke(Server server) {
            server.validateSubSet(new ArrayList<TlbSuiteFile>(), "foo");
        }
    };

    @DataPoint public static final MethodInvocation ALL_PARTITIONS_EXECUTED_CALL = new MethodInvocation() {
        public void invoke(Server server) {
            server.verifyAllPartitionsExecutedFor("foo");
        }
    };


    @Theory
    public void shouldFailAndInformUserAboutCorrectnessCheckUnavailability_whileUsingGoServerSupport(MethodInvocation invocation) {
        Map<String, String> map = initEnvMap("http://test.host:8153/go");
        SystemEnvironment environment = new SystemEnvironment(map);
        try {
            server = new GoServer(environment, mock(HttpAction.class));

            UnsupportedOperationException e = null;
            try {
                invocation.invoke(server);
                Assert.fail("should not have allowed correctness check invocation while working against go server.");
            } catch (UnsupportedOperationException ex) {
                e = ex;
            }
            assertThat(e.getMessage(), is("Correctness-check feature is only available when working against TLB server. Go server support does not include correctness-checking yet."));
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    private SystemEnvironment initEnvironment(String url) {
        return new SystemEnvironment(initEnvMap(url));
    }

    private Map<String, String> initEnvMap(String url) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(Go.GO_SERVER_URL, url);
        map.put(TlbConstants.Go.GO_PIPELINE_NAME, "pipeline-foo");
        map.put(TlbConstants.Go.GO_PIPELINE_LABEL, "pipeline-foo-26");
        map.put(TlbConstants.Go.GO_JOB_NAME, "job-baz");
        map.put(TlbConstants.Go.GO_STAGE_NAME, "stage-foo-bar");
        map.put(Go.GO_STAGE_COUNTER, "1");
        map.put(TlbConstants.Go.GO_PIPELINE_COUNTER, "26");
        map.put(TLB_TMP_DIR, System.getProperty("java.io.tmpdir"));
        return map;
    }

    private void assertCanFindJobsFrom(String baseUrl, SystemEnvironment environment) throws IOException, URISyntaxException {
        try {
            HttpAction action = mock(HttpAction.class);

            when(action.get(baseUrl + "/pipelines/pipeline-foo/26/stage-foo-bar/1.xml")).thenReturn(fileContents("resources/stage_detail.xml"));
            stubJobDetails(action);

            server = new GoServer(environment, action);
            logFixture.startListening();
            assertThat(server.getJobs(), is(Arrays.asList("firefox-1", "firefox-2", "firefox-3", "rails", "smoke")));
            logFixture.assertHeard("jobs found [firefox-1, firefox-2, firefox-3, rails, smoke]");
        } finally {
            SmoothingServerTest.clearCachingFiles(new FileUtil(environment));
        }
    }

    private void stubJobDetails(HttpAction action) throws IOException, URISyntaxException {
        when(action.get("http://test.host:8153/go/api/jobs/140.xml")).thenReturn(fileContents("resources/job_details_140.xml"));
        when(action.get("http://test.host:8153/go/api/jobs/139.xml")).thenReturn(fileContents("resources/job_details_139.xml"));
        when(action.get("http://test.host:8153/go/api/jobs/141.xml")).thenReturn(fileContents("resources/job_details_141.xml"));
        when(action.get("http://test.host:8153/go/api/jobs/142.xml")).thenReturn(fileContents("resources/job_details_142.xml"));
        when(action.get("http://test.host:8153/go/api/jobs/143.xml")).thenReturn(fileContents("resources/job_details_143.xml"));
    }
}
