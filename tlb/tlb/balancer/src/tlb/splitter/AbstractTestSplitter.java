package tlb.splitter;

import tlb.TlbConstants;
import tlb.utils.SystemEnvironment;

/**
 * @understands the criteria for splitting a given test suite 
 */
public abstract class AbstractTestSplitter implements TestSplitter {
    protected final SystemEnvironment env;
    public static final SystemEnvironment.EnvVar TLB_SPLITTER = new SystemEnvironment.DefaultedEnvVar(TlbConstants.TLB_SPLITTER, DefaultingTestSplitter.class.getCanonicalName());

    protected AbstractTestSplitter(SystemEnvironment env) {
        this.env = env;
    }
}
