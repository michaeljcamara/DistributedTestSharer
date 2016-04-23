package tlb.splitter.correctness;

import tlb.TlbSuiteFile;
import tlb.splitter.TestSplitter;

import java.util.List;

/**
 * @understands split-check functionality disabled state
 */
public class NoOp extends SplitChecker {
    public NoOp(TestSplitter splitter) {
        super(splitter);
    }

    @Override
    public final void universalSet(List<TlbSuiteFile> fileResources, String moduleName) {
    }

    @Override
    public final void subSet(List<TlbSuiteFile> fileResources, String moduleName) {
    }
}
