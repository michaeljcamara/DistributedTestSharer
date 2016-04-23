package tlb.splitter;

import org.apache.log4j.Logger;
import tlb.TlbConstants;

import java.util.List;


import tlb.TlbSuiteFile;
import tlb.service.Server;
import tlb.service.TalksToServer;
import tlb.utils.SystemEnvironment;

import static tlb.TlbConstants.TlbServer.*;
import static tlb.utils.SystemEnvironment.EnvVar;

/**
 * @understands the criteria for splitting a given test suite across jobs from the same family
 */
public abstract class JobFamilyAwareSplitter extends AbstractTestSplitter implements TalksToServer {
    public static TestSplitter MATCH_ALL_FILE_SET = new JobFamilyAwareSplitter(null) {
        @Override
        public List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> fileResources, String moduleName) {
            return fileResources;
        }

        protected List<TlbSuiteFile> subset(List<TlbSuiteFile> fileResources) {
            throw new RuntimeException("Should never reach here");
        }
    };
    protected Server server;

    private static final Logger logger = Logger.getLogger(JobFamilyAwareSplitter.class.getName());
    protected int totalPartitions;

    public JobFamilyAwareSplitter(SystemEnvironment env) {
        super(env);
    }

    public List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> fileResources, String moduleName) {
        logger.info(String.format("got total of %s files to balance", fileResources.size()));

        totalPartitions = server.totalPartitions();
        logger.info(String.format("total jobs to distribute load [ %s ]", totalPartitions));
        if (totalPartitions <= 1) {
            return fileResources;
        }

        List<TlbSuiteFile> subset = subset(fileResources);
        logger.info(String.format("assigned total of %s files to [ %s ]", subset.size(), server.partitionIdentifier()));
        server.publishSubsetSize(subset.size());
        return subset;
    }

    protected abstract List<TlbSuiteFile> subset(List<TlbSuiteFile> fileResources);

    public void talksToServer(Server service) {
       this.server = service;
    }

    protected boolean isLast(int totalPartitions, int index) {
        return (index + 1) == totalPartitions;
    }

    protected boolean isFirst(int index) {
        return (index == 0);
    }
}
