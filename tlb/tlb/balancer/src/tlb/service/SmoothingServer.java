package tlb.service;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import tlb.TlbConstants;
import tlb.domain.SuiteResultEntry;
import tlb.domain.SuiteTimeEntry;
import tlb.storage.TlbEntryRepository;
import tlb.utils.FileUtil;
import tlb.utils.SystemEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @understands smoothing test data before posting it to the service
 */
public abstract class SmoothingServer implements Server {
    private static final Logger logger = Logger.getLogger(SmoothingServer.class.getName());
    static final String OLD_TEST_TIMES_REPO_FILE = "old_test_times";
    static final String SUBSET_SIZE_REPO_FILE = "subset_size";
    static final String TEST_TIMES_REPO_FILE = "test_times";
    static final String FAILED_TESTS_REPO_FILE = "failed_tests";
    private FileUtil fileUtil;
    Integer subsetSize;

    final TlbEntryRepository testTimesRepository;
    final TlbEntryRepository oldTestTimesRepo;
    final TlbEntryRepository subsetSizeRepository;
    final TlbEntryRepository failedTestsRepository;

    private static class PassThroughSuiteEntry extends SuiteTimeEntry {
        public PassThroughSuiteEntry() {
            super(null, -1l);
        }

        @Override
        public SuiteTimeEntry smoothedWrt(SuiteTimeEntry newDataPoint, double alpha) {
            return newDataPoint;
        }
    }

    protected final SystemEnvironment environment;

    protected SmoothingServer(SystemEnvironment environment) {
        this.environment = environment;
        fileUtil = new FileUtil(this.environment);
        oldTestTimesRepo = new TlbEntryRepository(fileUtil.getUniqueFile(OLD_TEST_TIMES_REPO_FILE));
        subsetSizeRepository = new TlbEntryRepository(fileUtil.getUniqueFile(SUBSET_SIZE_REPO_FILE));
        testTimesRepository = new TlbEntryRepository(fileUtil.getUniqueFile(TEST_TIMES_REPO_FILE));
        failedTestsRepository = new TlbEntryRepository(fileUtil.getUniqueFile(FAILED_TESTS_REPO_FILE));
        subsetSize = null;
    }

    protected int subsetSize() {
        if (subsetSize == null) {
            subsetSize = Integer.parseInt(subsetSizeRepository.loadLastLine());
        }
        return subsetSize;
    }

    public void publishSubsetSize(int size) {
        String line = String.format("%s\n", size);
        subsetSizeRepository.appendLine(line);
        logger.info(String.format("Posting balanced subset size as %s to cruise server", size));
        postSubsetSizeToServer(size);
    }

    protected abstract void postSubsetSizeToServer(int line);

    protected abstract void postTestTimesToServer(String body);

    public void testClassFailure(String className, boolean hasFailed) {
        failedTestsRepository.appendLine(new SuiteResultEntry(className, hasFailed).dump());

        if (subsetSize() == failedTestsRepository.lineCount()) {
            List<String> runTests = failedTestsRepository.loadLines();
            List<SuiteResultEntry> resultEntries = SuiteResultEntry.parse(runTests);
            postFailedTestsToServer(resultEntries);
            cleanupRepo(failedTestsRepository);
            cleanupCachingFilesIfNoOtherReposExist();
        }
    }

    protected abstract void postFailedTestsToServer(List<SuiteResultEntry> failures);

    public abstract List<SuiteTimeEntry> fetchLastRunTestTimes();

    public void processedTestClassTime(String className, long time) {
        logger.info(String.format("recording run time for suite %s", className));
        testTimesRepository.appendLine(new SuiteTimeEntry(className, time).dump());

        if (subsetSize() == testTimesRepository.lineCount()) {
            String body = testTimesRepository.loadBody();
            logger.info(String.format("Posting test run times for suite with size %s to the server.", subsetSize()));
            postTestTimesToServer(body);
            cleanupRepo(testTimesRepository);
            cleanupRepo(oldTestTimesRepo);
            cleanupCachingFilesIfNoOtherReposExist();
        }
    }

    public void testClassTime(String className, long time) {
        SuiteTimeEntry entry = entryFor(className);
        entry = entry.smoothedWrt(new SuiteTimeEntry(className, time), smoothingFactor());
        processedTestClassTime(entry.getName(), entry.getTime());
    }

    private double smoothingFactor() {
        return Double.parseDouble(environment.val(TlbConstants.TLB_SMOOTHING_FACTOR));
    }

    private SuiteTimeEntry entryFor(String className) {
        for (SuiteTimeEntry suiteTimeEntry : getLastRunTestTimes()) {
            if (suiteTimeEntry.getName().equals(className)) {
                return suiteTimeEntry;
            }
        }
        return new PassThroughSuiteEntry();
    }

    private void deleteCachingFilesDir() {
        FileUtils.deleteQuietly(new File(fileUtil.tmpDir()));
    }

    private void cleanupCachingFilesIfNoOtherReposExist() {
        boolean anyRepoExists;
        synchronized (this) {
            anyRepoExists = testTimesRepository.exists() || oldTestTimesRepo.exists() || failedTestsRepository.exists();
        }
        if (anyRepoExists) {
            if (logger.isDebugEnabled()) {
                logger.debug("not cleaning up cache dir as some repos still exist");
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("clearing out the cache dir");
            }
            cleanupRepo(subsetSizeRepository);
            deleteCachingFilesDir();
        }
    }

    private void cleanupRepo(TlbEntryRepository repository) {
        try {
            repository.cleanup();
        } catch (IOException e) {
            logger.warn("could not delete cache file: " + e.getMessage(), e);
        }
    }

    public List<SuiteTimeEntry> getLastRunTestTimes() {
        if (!oldTestTimesRepo.getFile().exists()) {
            cacheOldSuiteTimeEntries();
        }
        return SuiteTimeEntry.parse(oldTestTimesRepo.loadLines());
    }

    private void cacheOldSuiteTimeEntries() {
        List<SuiteTimeEntry> suiteTimeEntries = null;
        try {
            suiteTimeEntries = fetchLastRunTestTimes();
        } catch (Exception e) {
            logger.warn(String.format("could not load test times for smoothing.: '%s'", e.getMessage()), e);
            suiteTimeEntries = new ArrayList<SuiteTimeEntry>();
        }
        oldTestTimesRepo.appendLines(suiteTimeEntries);
    }
}
