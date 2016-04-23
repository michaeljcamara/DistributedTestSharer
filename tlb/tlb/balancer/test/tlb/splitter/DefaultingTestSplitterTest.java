package tlb.splitter;

import org.junit.Test;
import tlb.TlbConstants;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.utils.SystemEnvironment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.fail;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class DefaultingTestSplitterTest {

    private static final String moduleName = "module_foo";

    @Test
    public void shouldAttemptCriterionSpecifiedInOrder() throws Exception{
        TestSplitter criteria = defaultingCriteriaWith("tlb.splitter.test.UnusableSplitter1:tlb.splitter.test.UnusableSplitter2:tlb.splitter.test.LastSelectingSplitter");

        TlbSuiteFile foo = new TlbSuiteFileImpl("foo");
        TlbSuiteFile bar = new TlbSuiteFileImpl("bar");
        final List<TlbSuiteFile> suiteFiles = Arrays.asList(foo, bar);
        List<TlbSuiteFile> filteredResources = criteria.filterSuites(suiteFiles, moduleName);
        assertThat(filteredResources.size(), is(1));
        assertThat(filteredResources, hasItem(bar));
    }

    @Test
    public void shouldAcceptSpacesBetweenCriterionNamesSpecified() throws Exception{
        TestSplitter criteria = defaultingCriteriaWith("tlb.splitter.test.UnusableSplitter1   :   tlb.splitter.test.UnusableSplitter2 :   tlb.splitter.test.LastSelectingSplitter");

        TlbSuiteFile foo = new TlbSuiteFileImpl("foo");
        TlbSuiteFile bar = new TlbSuiteFileImpl("bar");

        List<TlbSuiteFile> filteredResources = criteria.filterSuites(Arrays.asList(foo, bar), moduleName);
        assertThat(filteredResources.size(), is(1));
        assertThat(filteredResources, hasItem(bar));
    }

    private TestSplitter defaultingCriteriaWith(String criterion) {
        Map<String, String> envMap = new HashMap<String, String>();
        envMap.put(TlbConstants.TLB_PREFERRED_SPLITTERS, criterion);
        SystemEnvironment env = new SystemEnvironment(envMap);
        return new DefaultingTestSplitter(env);
    }

    @Test
    public void shouldBombIfNoCriteriaCanBeUsedSuccessfully() throws Exception{
        TestSplitter criteria = defaultingCriteriaWith("tlb.splitter.test.UnusableSplitter1:tlb.splitter.test.UnusableSplitter2");

        TlbSuiteFile foo = new TlbSuiteFileImpl("foo");
        TlbSuiteFile bar = new TlbSuiteFileImpl("bar");
        try {
            criteria.filterSuites(Arrays.asList(foo, bar), moduleName);
            fail("should have raised exception as no usable criteria specified");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("None of [tlb.splitter.test.UnusableSplitter1, tlb.splitter.test.UnusableSplitter2] could successfully split the test suites."));
        }
    }
}
