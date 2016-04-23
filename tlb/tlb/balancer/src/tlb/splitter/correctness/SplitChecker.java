package tlb.splitter.correctness;

import tlb.TlbSuiteFile;
import tlb.splitter.TestSplitter;

import java.util.List;

/**
 * @understands split correctness checking around balancer
 */
public abstract class SplitChecker implements TestSplitter {
    private final TestSplitter splitter;

    protected SplitChecker(final TestSplitter splitter) {
        this.splitter = splitter;
    }

    public final List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> files, String moduleName) {
        universalSet(files, moduleName);
        List<TlbSuiteFile> filteredFiles = splitter.filterSuites(files, moduleName);
        subSet(filteredFiles, moduleName);
        return filteredFiles;
    }

    abstract public void universalSet(List<TlbSuiteFile> fileResources, String moduleName);

    abstract public void subSet(List<TlbSuiteFile> fileResources, String moduleName);
}
