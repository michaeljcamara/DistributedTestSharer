package tlb;

import tlb.utils.SystemEnvironment;

/**
 * @understands TLB constants
 */
public interface TlbConstants {
    static final SystemEnvironment.EnvVar TYPE_OF_SERVER = new SystemEnvironment.DefaultedEnvVar("TYPE_OF_SERVER", "tlb.service.TlbServer");

    public static interface Go {
        static final String GO_SERVER_URL = "GO_SERVER_URL";
        static final String GO_PIPELINE_NAME = "GO_PIPELINE_NAME";
        static final String GO_STAGE_NAME = "GO_STAGE_NAME";
        static final String GO_JOB_NAME = "GO_JOB_NAME";
        static final String GO_STAGE_COUNTER = "GO_STAGE_COUNTER";
        static final String GO_PIPELINE_COUNTER = "GO_PIPELINE_COUNTER";
        static final String GO_PIPELINE_LABEL = "GO_PIPELINE_LABEL";
        static final String DEFAULT_STAGE_FEED_SEARCH_DEPTH = "10";
        static final SystemEnvironment.EnvVar GO_STAGE_FEED_MAX_SEARCH_DEPTH = new SystemEnvironment.DefaultedEnvVar("GO_STAGE_FEED_MAX_SEARCH_DEPTH", DEFAULT_STAGE_FEED_SEARCH_DEPTH);
    }

    public static interface TlbServer {
        static final String TLB_JOB_NAME = "TLB_JOB_NAME";
        static final String TLB_BASE_URL = "TLB_BASE_URL";
        static final String TLB_PARTITION_NUMBER = "TLB_PARTITION_NUMBER";
        static final String TLB_TOTAL_PARTITIONS = "TLB_TOTAL_PARTITIONS";
        static final String TLB_JOB_VERSION = "TLB_JOB_VERSION";
    }

    static final String PASSWORD = "TLB_PASSWORD";
    static final String USERNAME = "TLB_USERNAME";
    static final String TLB_SPLITTER = "TLB_SPLITTER";
    static final String TEST_SUBSET_SIZE_FILE = "tlb/subset_size";
    static final String TLB_PREFERRED_SPLITTERS = "TLB_PREFERRED_SPLITTERS";
    static final String TLB_TMP_DIR = "TLB_TMP_DIR";
    static final String TLB_ORDERER = "TLB_ORDERER";
    static final SystemEnvironment.EnvVar TLB_SMOOTHING_FACTOR = new SystemEnvironment.DefaultedEnvVar("TLB_SMOOTHING_FACTOR", "1.0");

    public static interface Balancer {
        static final SystemEnvironment.EnvVar TLB_BALANCER_PORT = new SystemEnvironment.DefaultedEnvVar("TLB_BALANCER_PORT", "8019");
        static final String QUERY = "query";
        static final String DEFAULT_MODULE_NAME = "default-module";
        static final String TLB_MODULE_NAME_HEADER = "X-Tlb-Module-Name";
    }

    public static interface Correctness {
        static final SystemEnvironment.EnvVar SPLIT_CORRECTNESS_CHECKER = new SystemEnvironment.DefaultedEnvVar("SPLIT_CORRECTNESS_CHECKER", "tlb.splitter.correctness.NoOp");

        static final String CORRECTNESS_CHECK_NOT_AVAILABLE = "Correctness check feature not available in current configuration";

        static final String CORRECTNESS_VALIDATION_FAILED = "Correctness validation failed";
        static final String NO_UNIVERSAL_SET_FOUND = "Failed to find corresponding universal set definition";
        static final String CURRENT_PARTITION_VIOLATES_CORRECTNESS_CHECK_FOR_SUBSET = "Choice of suites by the current partition violate correctness check with respect to other partitions";
        static final String CURRENT_PARTITION_POSTED_INCORRECT_UNIVERSAL_SET = "Current partition is partitioning a different set of test-suites than other partitions";
        static final String SOME_PARTITIONS_DID_NOT_EXECUTE = "One or more partitions didn't execute for job-name, job-version and module-name combination";
    }

    public static interface Server {
        static final String REPO_FACTORY = "repo_factory";
        static final String REQUEST_NAMESPACE = "namespace";
        static final String DEFAULT_SERVER_PORT = "7019";
        static final SystemEnvironment.EnvVar TLB_SERVER_PORT = new SystemEnvironment.DefaultedEnvVar("TLB_SERVER_PORT", DEFAULT_SERVER_PORT);
        static final String DEFAULT_TLB_DATA_DIR = "tlb_store";
        static final SystemEnvironment.EnvVar TLB_DATA_DIR = new SystemEnvironment.DefaultedEnvVar("TLB_DATA_DIR", DEFAULT_TLB_DATA_DIR);

        static final String LISTING_VERSION = "listing_version";
        static final String JOB_NUMBER = "job_number";
        static final String TOTAL_JOBS = "total_jobs";
        static final SystemEnvironment.EnvVar TLB_VERSION_LIFE_IN_DAYS = new SystemEnvironment.DefaultedEnvVar("TLB_VERSION_LIFE_IN_DAYS", "7");
        static final SystemEnvironment.EnvVar TLB_SYNC_TO_DISK_INTERVAL_IN_MINS = new SystemEnvironment.DefaultedEnvVar("TLB_SYNC_TO_DISK_INTERVAL_IN_MINS", "60");
        static final SystemEnvironment.EnvVar TLB_DATA_CACHE_SIZE = new SystemEnvironment.DefaultedEnvVar("TLB_DATA_CACHE_SIZE", "100");

        static final String MODULE_NAME = "module_name";
        static final String VERIFY_PARTITION_COMPLETENESS = "verify_partition_completeness";

        public static interface EntryRepoFactory {
            static final String SUBSET_SIZE = "subset_size";
            static final String SUITE_TIME = "suite_time";
            static final String SUITE_RESULT = "suite_result";
            static final String CORRECTNESS_CHECK = "correctness_check";
            static final String UNIVERSAL_SET = "universal_set";
            static final String PARTITION_RECORD = "partition_record";
            static final String SUB_SET = "sub_set";
        }
    }
}
