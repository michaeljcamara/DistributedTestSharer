package tlb.splitter.test;

import tlb.TlbSuiteFile;
import tlb.splitter.AbstractTestSplitter;
import tlb.utils.SystemEnvironment;

import java.util.Arrays;
import java.util.List;

public class LastSelectingSplitter extends AbstractTestSplitter {
    public LastSelectingSplitter(SystemEnvironment env) {
        super(env);
    }

    public List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> fileResources, String moduleName) {
        return Arrays.asList(fileResources.get(fileResources.size() - 1));
    }
}