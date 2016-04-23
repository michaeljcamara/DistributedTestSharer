package tlb.splitter.test;

import tlb.TlbSuiteFile;
import tlb.splitter.AbstractTestSplitter;
import tlb.utils.SystemEnvironment;

import java.util.List;

public class UnusableSplitter2 extends AbstractTestSplitter {
    public UnusableSplitter2(SystemEnvironment env) {
        super(env);
    }

    public List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> fileResources, String moduleName) {
        throw new RuntimeException("Unusable criteira #2 won't work!");
    }
}