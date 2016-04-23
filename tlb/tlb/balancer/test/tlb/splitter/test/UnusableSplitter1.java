package tlb.splitter.test;

import tlb.TlbSuiteFile;
import tlb.splitter.AbstractTestSplitter;
import tlb.utils.SystemEnvironment;

import java.util.List;

public class UnusableSplitter1 extends AbstractTestSplitter {
    public UnusableSplitter1(SystemEnvironment env) {
        super(env);
    }

    public List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> fileResources, String moduleName) {
        throw new RuntimeException("Unusable criteira #1 won't work!");
    }
}
