package tlb.splitter;

import org.apache.log4j.Logger;
import tlb.TlbSuiteFile;
import tlb.domain.SuiteTimeEntry;
import tlb.service.Server;
import tlb.service.TalksToServer;
import tlb.splitter.timebased.Bucket;
import tlb.splitter.timebased.TestFile;
import tlb.utils.CollectionUtils;
import tlb.utils.SystemEnvironment;

import java.util.*;

import static tlb.utils.CollectionUtils.mean;
import static tlb.utils.CollectionUtils.sortedCopy;


/**
 * @understands criteria for splitting tests based on time taken
 */
public class TimeBasedTestSplitter extends JobFamilyAwareSplitter implements TalksToServer {
    private static final Logger logger = Logger.getLogger(TimeBasedTestSplitter.class.getName());
    private static final String NO_HISTORICAL_DATA = "no historical test time data, aborting attempt to balance based on time";

    public TimeBasedTestSplitter(Server server, SystemEnvironment env) {
        this(env);
        talksToServer(server);
    }

    public TimeBasedTestSplitter(SystemEnvironment env) {
        super(env);
    }

    protected List<TlbSuiteFile> subset(List<TlbSuiteFile> fileResources) {
        List<TestFile> testFiles = testFiles(fileResources);
        Bucket thisBucket = buckets(testFiles);

        return resourcesFrom(thisBucket);
    }

    private Bucket buckets(List<TestFile> testFiles) {
        Bucket thisBucket = null;
        List<Bucket> buckets = new ArrayList<Bucket>();
        int thisPartition = server.partitionNumber();
        for(int i = 1; i <= totalPartitions; i++) {
            Bucket bucket = new Bucket(i);
            if (i == thisPartition) thisBucket = bucket;
            buckets.add(bucket);
        }

        assignToBuckets(testFiles, buckets);

        logger.info("Current bucket is partitions number: " + thisPartition);
        logger.info("Assigned the tests to buckets in the following way:");

        for (Bucket bucket : buckets) {
            logger.info("Bucket number " + bucket.index() + " has following files:\n" + bucket.files() + "\n");
        }

        return thisBucket;
    }

    private void assignToBuckets(List<TestFile> testFiles, List<Bucket> buckets) {
        for (TestFile testFile : testFiles) {
            buckets.get(0).add(testFile);
            Collections.sort(buckets);
        }
    }

    private List<TestFile> testFiles(List<TlbSuiteFile> fileResources) {
        List<SuiteTimeEntry> suiteTimeEntries = server.getLastRunTestTimes();
        if (suiteTimeEntries.isEmpty()) {
            logger.warn(NO_HISTORICAL_DATA);
            throw new IllegalStateException(NO_HISTORICAL_DATA);
        }
        Map<String, TlbSuiteFile> fileNameToResource = new HashMap<String, TlbSuiteFile>();
        Set<String> currentFileNames = new HashSet<String>();
        logger.info(String.format("Historical test time data has entries for %s suites", suiteTimeEntries.size()));
        logger.info("The historical suite entries are: \n" + suiteTimeEntries + "\n");
        logger.info("The files to be balanced are: \n" + fileResources + "\n");

        for (TlbSuiteFile fileResource : fileResources) {
            String name = fileResource.getName();
            currentFileNames.add(name);
            fileNameToResource.put(name, fileResource);
        }

        List<TestFile> testFiles = new ArrayList<TestFile>();

        for (SuiteTimeEntry suiteTimeEntry : suiteTimeEntries) {
            String fileName = suiteTimeEntry.getName();
            double time = suiteTimeEntry.getTime();
            if (currentFileNames.remove(fileName)) testFiles.add(new TestFile(fileNameToResource.get(fileName), time));
        }

        logger.info(String.format("%s entries of historical test time data found relevant", testFiles.size()));

        addNewTests(fileNameToResource, currentFileNames, testFiles, suiteTimeEntries);

        Collections.sort(testFiles);

        return testFiles;
    }

    private void addNewTests(Map<String, TlbSuiteFile> fileNameToResource, Set<String> currentFileNames, List<TestFile> testFiles, List<SuiteTimeEntry> suiteTimeEntries) {
        if (currentFileNames.isEmpty()) return;

        double newTime = calculateNewTime(groupSimilarEntries(suiteTimeEntries), suiteTimeEntries.size());
        logger.info(String.format("Encountered %s new files which don't have historical time data, used average time [ %s ] to balance", currentFileNames.size(), newTime));

        for (String newFile : currentFileNames) {
            testFiles.add(new TestFile(fileNameToResource.get(newFile), newTime));
        }
    }

    private List<List<Long>> groupSimilarEntries(List<SuiteTimeEntry> suiteTimeEntries) {
        List<List<Long>> groupedTimes = new ArrayList<List<Long>>();
        List<Long> latestGroup = new ArrayList<Long>();
        groupedTimes.add(latestGroup);
        for (SuiteTimeEntry entry : sortedCopy(suiteTimeEntries)) {
            long time = entry.getTime();
            if (latestGroup.size() > 1 && !comparableDifferences(latestGroup, time)) {
                latestGroup = new ArrayList<Long>();
                groupedTimes.add(latestGroup);
            }
            latestGroup.add(time);
        }
        return groupedTimes;
    }

    private double calculateNewTime(List<List<Long>> groupedTimes, int size) {
        double time = 0.0;
        for (List<Long> groupedTime : groupedTimes) {
            time += (((double) groupedTime.size()) / size) * mean(groupedTime);
        }
        return time;
    }

    private boolean comparableDifferences(List<Long> groupedTimes, long time) {
        double mean = meanDifference(groupedTimes);
        Long largest = groupedTimes.get(groupedTimes.size() - 1);
        long diff = time - largest;
        return  diff > .7 * mean && diff < 1.3 * mean || diff < .1 * largest;
    }

    private double meanDifference(List<Long> groupedTimes) {
        long sum = 0;
        for (int i = 1; i < groupedTimes.size(); i++) {
            sum += groupedTimes.get(i) - groupedTimes.get(i - 1);
        }
        return ((double)sum  / (groupedTimes.size() - 1));
    }

    private List<TlbSuiteFile> resourcesFrom(Bucket bucket) {
        ArrayList<TlbSuiteFile> resources = new ArrayList<TlbSuiteFile>();
        for (TlbSuiteFile file : bucket.files()) {
            resources.add(file);
        }
        return resources;
    }

}
