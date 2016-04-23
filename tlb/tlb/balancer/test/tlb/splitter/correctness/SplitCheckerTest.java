package tlb.splitter.correctness;

import org.junit.Test;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.splitter.TestSplitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.*;

public class SplitCheckerTest {

    @Test
    public void shouldCallCorrectnessCheckOperationsWithCorrespondingModuleName() {
        TestSplitter splitter = mock(TestSplitter.class);

        SplitChecker splitChecker = spy(new TestSplitChecker(splitter));

        List<TlbSuiteFile> given = new ArrayList<TlbSuiteFile>();

        given.add(new TlbSuiteFileImpl("foo"));
        given.add(new TlbSuiteFileImpl("bar"));
        given.add(new TlbSuiteFileImpl("baz"));

        when(splitter.filterSuites(given, "module_baz")).thenReturn(Arrays.asList((TlbSuiteFile) new TlbSuiteFileImpl("baz"), new TlbSuiteFileImpl("foo")));

        assertThat(splitChecker.filterSuites(given, "module_baz"), is(Arrays.asList((TlbSuiteFile) new TlbSuiteFileImpl("baz"), new TlbSuiteFileImpl("foo"))));
        verify(splitChecker).universalSet(Arrays.asList((TlbSuiteFile) new TlbSuiteFileImpl("foo"), new TlbSuiteFileImpl("bar"), new TlbSuiteFileImpl("baz")), "module_baz");
        verify(splitChecker).subSet(Arrays.asList((TlbSuiteFile) new TlbSuiteFileImpl("baz"), new TlbSuiteFileImpl("foo")), "module_baz");

        when(splitter.filterSuites(given, "foo_module")).thenReturn(Arrays.asList((TlbSuiteFile) new TlbSuiteFileImpl("bar"), new TlbSuiteFileImpl("baz")));

        assertThat(splitChecker.filterSuites(given, "foo_module"), is(Arrays.asList((TlbSuiteFile) new TlbSuiteFileImpl("bar"), new TlbSuiteFileImpl("baz"))));
        verify(splitChecker).universalSet(Arrays.asList((TlbSuiteFile) new TlbSuiteFileImpl("foo"), new TlbSuiteFileImpl("bar"), new TlbSuiteFileImpl("baz")), "foo_module");
        verify(splitChecker).subSet(Arrays.asList((TlbSuiteFile) new TlbSuiteFileImpl("bar"), new TlbSuiteFileImpl("baz")), "foo_module");
    }

    private static class TestSplitChecker extends SplitChecker {
        public TestSplitChecker(TestSplitter splitter) {
            super(splitter);
        }

        @Override
        public void universalSet(List<TlbSuiteFile> fileResources, String moduleName) {

        }

        @Override
        public void subSet(List<TlbSuiteFile> fileResources, String moduleName) {

        }
    }
}
