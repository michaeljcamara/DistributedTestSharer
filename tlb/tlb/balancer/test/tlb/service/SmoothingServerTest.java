package tlb.service;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.Times;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.domain.SuiteResultEntry;
import tlb.domain.SuiteTimeEntry;
import tlb.splitter.correctness.ValidationResult;
import tlb.storage.TlbEntryRepository;
import tlb.utils.FileUtil;
import tlb.utils.SystemEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static tlb.TestUtil.deref;
import static tlb.TestUtil.updateEnv;

public class SmoothingServerTest {
    private SystemEnvironment env;
    private SmoothingServer server;
    private ArrayList<SuiteTimeEntry> fetchedEntries;
    private TestUtil.LogFixture logFixture;
    private SmoothingServer delegate;

    public static void clearCachingFiles(FileUtil fileUtil) {
        FileUtils.deleteQuietly(new File(fileUtil.tmpDir()));
    }

    public static void assertCacheState(SystemEnvironment env, int lineCount, String lastLine, TlbEntryRepository repository) throws IOException {
        List<String> cache = repository.loadLines();
        assertThat(cache.size(), is(lineCount));
        if (! cache.isEmpty()) {
            assertThat(cache.get(lineCount - 1), is(lastLine));
        }
    }

    @Before
    public void setUp() throws IllegalAccessException, IOException {
        fetchedEntries = new ArrayList<SuiteTimeEntry>();
        HashMap<String, String> variables = new HashMap<String, String>();
        variables.put(TlbConstants.TLB_SMOOTHING_FACTOR.key, "0.05");
        env = new SystemEnvironment(variables);

        delegate = mock(SmoothingServer.class);
        server = new DelegatingSmoothingServer(delegate, env);
        FileUtils.deleteQuietly(new File(new FileUtil(env).tmpDir()));
        File file = testTimeCacheRepo().getFile();
        if (file.exists()) {
            FileUtils.forceDelete(file);
        }

        logFixture = new TestUtil.LogFixture();
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(new File(new FileUtil(env).tmpDir()));
    }

    private TlbEntryRepository testTimeCacheRepo() throws IllegalAccessException {
        return server.oldTestTimesRepo;
    }

    @Test
    public void shouldSmoothenUsingSetSmoothingFactor() throws NoSuchFieldException, IllegalAccessException {
        server.subsetSizeRepository.appendLine("2\n");

        when(delegate.fetchLastRunTestTimes()).thenReturn(Arrays.asList(new SuiteTimeEntry("foo/bar/Baz.class", 12l)));
        server.testClassTime("foo/bar/Baz.class", 102l);
        server.testClassTime("foo/bar/Quux.class", 19l);
        verify(delegate).postTestTimesToServer("foo/bar/Baz.class: 17\nfoo/bar/Quux.class: 19\n");
    }

    @Test
    public void shouldNotFailWhenHasNoHistory() {//should just skip smoothing
        server.subsetSizeRepository.appendLine("2\n");

        final RuntimeException exception = new RuntimeException("failed for some reason");
        when(delegate.fetchLastRunTestTimes()).thenThrow(exception);
        logFixture.startListening();
        server.testClassTime("foo/baz/Bam.class", 35l);
        server.testClassTime("foo/baz/Boom.class", 10l);
        logFixture.stopListening();
        verify(delegate).postTestTimesToServer("foo/baz/Bam.class: 35\nfoo/baz/Boom.class: 10\n");
        logFixture.assertHeard("could not load test times for smoothing.: 'failed for some reason'");
        logFixture.assertHeardException(exception);
    }

    @Test
    public void shouldCacheTestRunTimes() {
        server.subsetSizeRepository.appendLine("3\n");

        when(delegate.fetchLastRunTestTimes()).thenReturn(Arrays.asList(new SuiteTimeEntry("foo/bar/Baz.class", 12l), new SuiteTimeEntry("quux/bang/Boom.class", 15l)));
        server.testClassTime("foo/baz/Quux.class", 77l);
        verify(delegate, new Times(1)).fetchLastRunTestTimes();
        server.testClassTime("quux/bang/Boom.class", 18l);
        server.testClassTime("foo/bar/Baz.class", 90l);

        verify(delegate).postTestTimesToServer("foo/baz/Quux.class: 77\nquux/bang/Boom.class: 15\nfoo/bar/Baz.class: 16\n");
        verify(delegate, new Times(1)).fetchLastRunTestTimes();
    }

    @Test
    public void shouldCleanupAllRepos_IncludingCachingDir_AfterPostingAllFeedback_WhenTestTimeIsPostedFirst() throws IOException {
        String tmpDir = new FileUtil(env).tmpDir();

        server.subsetSizeRepository.appendLine("2\n");
        assertCacheState(env, 1, "2", server.subsetSizeRepository);
        when(delegate.fetchLastRunTestTimes()).thenReturn(Arrays.asList(new SuiteTimeEntry("foo/bar/Baz.class", 12l)));

        server.testClassTime("foo/bar/Baz.class", 102l);
        assertCacheState(env, 1, "foo/bar/Baz.class: 12", server.oldTestTimesRepo);
        assertCacheState(env, 1, "foo/bar/Baz.class: 12", server.oldTestTimesRepo);
        assertCacheState(env, 1, "foo/bar/Baz.class: 17", server.testTimesRepository);
        server.testClassFailure("foo/bar/Baz.class", true);
        assertCacheState(env, 1, "foo/bar/Baz.class: true", server.failedTestsRepository);

        server.testClassTime("foo/bar/Quux.class", 19l);

        verify(delegate).postTestTimesToServer("foo/bar/Baz.class: 17\nfoo/bar/Quux.class: 19\n");
        verify(delegate, never()).postFailedTestsToServer(any(List.class));
        assertThat(server.oldTestTimesRepo.getFile().exists(), is(false));
        assertThat(server.testTimesRepository.getFile().exists(), is(false));
        assertCacheState(env, 1, "2", server.subsetSizeRepository);
        assertCacheState(env, 1, "foo/bar/Baz.class: true", server.failedTestsRepository);

        server.testClassFailure("foo/bar/Quux.class", false);
        verify(delegate).postFailedTestsToServer(Arrays.asList(new SuiteResultEntry("foo/bar/Baz.class", true), new SuiteResultEntry("foo/bar/Quux.class", false)));
        assertThat(server.oldTestTimesRepo.getFile().exists(), is(false));
        assertThat(server.testTimesRepository.getFile().exists(), is(false));
        assertThat(server.subsetSizeRepository.getFile().exists(), is(false));
        assertThat(server.failedTestsRepository.getFile().exists(), is(false));
        assertThat(new File(tmpDir).exists(), is(false));
    }

    @Test
    public void shouldCleanupAllRepos_IncludingCachingDir_AfterPostingAllFeedback_WhenTestResultIsPostedFirst() throws IOException {
        String tmpDir = new FileUtil(env).tmpDir();

        server.subsetSizeRepository.appendLine("2\n");
        assertCacheState(env, 1, "2", server.subsetSizeRepository);


        server.testClassFailure("foo/bar/Baz.class", true);
        assertCacheState(env, 1, "foo/bar/Baz.class: true", server.failedTestsRepository);


        when(delegate.fetchLastRunTestTimes()).thenReturn(Arrays.asList(new SuiteTimeEntry("foo/bar/Baz.class", 12l)));
        server.testClassTime("foo/bar/Baz.class", 102l);
        assertCacheState(env, 1, "foo/bar/Baz.class: 12", server.oldTestTimesRepo);
        assertCacheState(env, 1, "foo/bar/Baz.class: 17", server.testTimesRepository);

        server.testClassFailure("foo/bar/Quux.class", false);

        verify(delegate).postFailedTestsToServer(Arrays.asList(new SuiteResultEntry("foo/bar/Baz.class", true), new SuiteResultEntry("foo/bar/Quux.class", false)));
        verify(delegate, never()).postTestTimesToServer(any(String.class));
        assertThat(server.failedTestsRepository.getFile().exists(), is(false));
        assertCacheState(env, 1, "2", server.subsetSizeRepository);
        assertCacheState(env, 1, "foo/bar/Baz.class: 17", server.testTimesRepository);
        assertCacheState(env, 1, "foo/bar/Baz.class: 12", server.oldTestTimesRepo);

        server.testClassTime("foo/bar/Quux.class", 19l);
        verify(delegate).postTestTimesToServer("foo/bar/Baz.class: 17\nfoo/bar/Quux.class: 19\n");
        assertThat(server.oldTestTimesRepo.getFile().exists(), is(false));
        assertThat(server.testTimesRepository.getFile().exists(), is(false));
        assertThat(server.subsetSizeRepository.getFile().exists(), is(false));
        assertThat(server.failedTestsRepository.getFile().exists(), is(false));
        assertThat(new File(tmpDir).exists(), is(false));
    }

    @Test
    public void shouldAssumeNoOpSmoothingFactorWhenNotGiven() throws NoSuchFieldException, IllegalAccessException {
        server.subsetSizeRepository.appendLine("1\n");

        when(delegate.fetchLastRunTestTimes()).thenReturn(Arrays.asList(new SuiteTimeEntry("foo/bar/Baz.class", 12l)));
        File oldFile = new File(new FileUtil(env).tmpDir());
        try {
            updateEnv(env, TlbConstants.TLB_SMOOTHING_FACTOR.key, null);
            server.testClassTime("foo/bar/Baz.class", 102l);
            verify(delegate).postTestTimesToServer("foo/bar/Baz.class: 102\n");
        } finally {
            FileUtils.deleteQuietly(oldFile);//because the old value may lead to a directory being left behind(teardown will not clean it up because environment here is being mutated.
            //teardown will get the new location anyway
        }
    }

    private static class DelegatingSmoothingServer extends SmoothingServer {

        private final SmoothingServer delegate;

        public DelegatingSmoothingServer(SmoothingServer delegate, final SystemEnvironment env) {
            super(env);
            this.delegate = delegate;
        }

        @Override
        protected void postSubsetSizeToServer(int line) {
            delegate.postSubsetSizeToServer(line);
        }

        @Override
        protected void postTestTimesToServer(String body) {
            delegate.postTestTimesToServer(body);
        }

        @Override
        protected void postFailedTestsToServer(List<SuiteResultEntry> failures) {
            delegate.postFailedTestsToServer(failures);
        }

        @Override
        public List<SuiteTimeEntry> fetchLastRunTestTimes() {
            return delegate.fetchLastRunTestTimes();
        }

        public List<SuiteResultEntry> getLastRunFailedTests() {
            return delegate.getLastRunFailedTests();
        }

        public int partitionNumber() {
            return delegate.partitionNumber();
        }

        public int totalPartitions() {
            return delegate.totalPartitions();
        }

        public ValidationResult validateUniversalSet(List<TlbSuiteFile> universalSet, String moduleName) {
            throw new UnsupportedOperationException("not implemented yet");
        }

        public ValidationResult validateSubSet(List<TlbSuiteFile> subSet, String moduleName) {
            throw new UnsupportedOperationException("not implemented yet");
        }

        public ValidationResult verifyAllPartitionsExecutedFor(String moduleName) {
            throw new UnsupportedOperationException("not implemented yet");
        }

        public String partitionIdentifier() {
            return delegate.partitionIdentifier();
        }
    }
}
