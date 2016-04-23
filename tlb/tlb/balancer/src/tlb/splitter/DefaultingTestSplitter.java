package tlb.splitter;

import org.apache.log4j.Logger;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.factory.TlbBalancerFactory;
import tlb.utils.SystemEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @understands choosing criteria in order of preference
 */
public class DefaultingTestSplitter extends AbstractTestSplitter {
    private static final Logger logger = Logger.getLogger(DefaultingTestSplitter.class.getName());
    public static final SystemEnvironment.EnvVar TLB_PREFERRED_SPLITTERS = new SystemEnvironment.DefaultedEnvVar(TlbConstants.TLB_PREFERRED_SPLITTERS, TimeBasedTestSplitter.class.getCanonicalName() + ":" + CountBasedTestSplitter.class.getCanonicalName());

    private ArrayList<TestSplitter> criterion;

    public DefaultingTestSplitter(SystemEnvironment env) {
        super(env);
        criterion = new ArrayList<TestSplitter>();
        String[] criteriaNames = criteriaNames(env);
        for (String criteriaName : criteriaNames) {
            TestSplitter splitter = TlbBalancerFactory.getCriteria(criteriaName, env);
            criterion.add(splitter);
        }
    }

    public List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> fileResources, String moduleName) {
        for (TestSplitter criteria : criterion) {
            try {
                List<TlbSuiteFile> subset = criteria.filterSuites(fileResources, moduleName);
                logger.info(String.format("Used %s to balance.", criteria.getClass().getCanonicalName()));
                return subset;
            } catch (Exception e) {
                logger.warn(String.format("Could not use %s for balancing because: %s.", criteria.getClass().getCanonicalName(), e.getMessage()), e);
                continue;
            }
        }
        throw new IllegalStateException(String.format("None of %s could successfully split the test suites.", Arrays.asList(criteriaNames(env))));
    }

    private String[] criteriaNames(SystemEnvironment env) {
        return env.val(TLB_PREFERRED_SPLITTERS).split("\\s*:\\s*");
    }
}
