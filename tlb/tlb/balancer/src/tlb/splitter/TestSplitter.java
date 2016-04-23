package tlb.splitter;

import tlb.TlbSuiteFile;

import java.util.List;

/**
 * @understands making sub-sets(partitions) of a given test-suite
 */
public interface TestSplitter {
    List<TlbSuiteFile> filterSuites(List<TlbSuiteFile> fileResources, String moduleName);
}
