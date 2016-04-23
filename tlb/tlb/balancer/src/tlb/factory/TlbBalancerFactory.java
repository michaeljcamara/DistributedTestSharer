package tlb.factory;

import tlb.TlbConstants;
import tlb.orderer.TestOrderer;
import tlb.splitter.AbstractTestSplitter;
import tlb.splitter.JobFamilyAwareSplitter;
import tlb.splitter.TestSplitter;
import tlb.splitter.correctness.SplitChecker;
import tlb.utils.SystemEnvironment;

/**
 * @understands creating balancer specific algorithm implementations
 */
public class TlbBalancerFactory {
    private static TlbFactory<TestSplitter> criteriaFactory;
    private static TlbFactory<SplitChecker> splitCheckerFactory;
    private static TlbFactory<TestOrderer> testOrderer;

    public static TestSplitter getCriteria(String criteriaName, SystemEnvironment environment) {
        if (criteriaFactory == null)
            criteriaFactory = new TlbFactory<TestSplitter>(TestSplitter.class, JobFamilyAwareSplitter.MATCH_ALL_FILE_SET, SystemEnvironment.class);
        TestSplitter splitter = criteriaFactory.getInstance(criteriaName, environment, environment);
        if (splitCheckerFactory == null)
            splitCheckerFactory = new TlbFactory<SplitChecker>(SplitChecker.class, null, TestSplitter.class);
        return splitCheckerFactory.getInstance(environment.val(TlbConstants.Correctness.SPLIT_CORRECTNESS_CHECKER), environment, splitter);
    }

    public static TestOrderer getOrderer(String ordererName, SystemEnvironment environment) {
        if (testOrderer == null)
            testOrderer = new TlbFactory<TestOrderer>(TestOrderer.class, TestOrderer.NO_OP, SystemEnvironment.class);
        return testOrderer.getInstance(ordererName, environment, environment);
    }
}
