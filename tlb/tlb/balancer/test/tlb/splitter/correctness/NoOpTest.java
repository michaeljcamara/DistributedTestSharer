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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NoOpTest {
    @Test
    public void shouldNotDisturbUniversalSetOrSubset() {
        TestSplitter splitter = mock(TestSplitter.class);
        NoOp checker = new NoOp(splitter);

        ArrayList<TlbSuiteFile> given = new ArrayList<TlbSuiteFile>();
        TlbSuiteFile foo = new TlbSuiteFileImpl("foo");
        given.add(foo);
        TlbSuiteFile bar = new TlbSuiteFileImpl("bar");
        given.add(bar);
        TlbSuiteFile baz = new TlbSuiteFileImpl("baz");
        given.add(baz);
        when(splitter.filterSuites(given, "my-module")).thenReturn(Arrays.asList(baz, bar));

        List<TlbSuiteFile> filtered = checker.filterSuites(given, "my-module");

        assertThat(filtered, is(Arrays.asList(baz, bar)));
    }
}
