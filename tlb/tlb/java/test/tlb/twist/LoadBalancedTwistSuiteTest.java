package tlb.twist;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import tlb.TlbFileResource;
import tlb.splitter.AbstractTestSplitter;
import tlb.splitter.TestSplitter;
import tlb.utils.SuiteFileConvertor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoadBalancedTwistSuiteTest {

    private static final String moduleName = "module_foo";

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File("destination"));
        FileUtils.deleteDirectory(new File("folder"));
    }

    @Test
    public void shouldCopyComputedSubsetForRunning() throws Exception {
        TestSplitter criteria = mock(AbstractTestSplitter.class);

        File folder = folder("folder");

        List<TlbFileResource> resources = scenarioResource(folder, 1, 2);
        final SuiteFileConvertor convertor = new SuiteFileConvertor();
        when(criteria.filterSuites(any(List.class), eq(moduleName))).thenReturn(convertor.toTlbSuiteFiles(resources));

        LoadBalancedTwistSuite suite = new LoadBalancedTwistSuite(criteria);

        suite.balance(folder.getAbsolutePath(), "destination", moduleName);

        File destination = new File("destination");
        assertThat(destination.exists(), is(true));
        assertThat(destination.isDirectory(), is(true));
        assertThat(FileUtils.listFiles(destination, null, false).size(), is(2));
    }

    @Test
    public void shouldCopyCSVAssociatedWithAScenario() throws Exception {
        TestSplitter criteria = mock(AbstractTestSplitter.class);

        File folder = folder("folder");
        List<TlbFileResource> resources = scenarioResource(folder, 1, 2);
        scenarioCSV(folder, 1);
        final SuiteFileConvertor convertor = new SuiteFileConvertor();
        when(criteria.filterSuites(any(List.class), eq(moduleName))).thenReturn(convertor.toTlbSuiteFiles(resources));

        LoadBalancedTwistSuite suite = new LoadBalancedTwistSuite(criteria);

        suite.balance(folder.getAbsolutePath(), "destination", moduleName);

        File destination = new File("destination");
        assertThat(destination.exists(), is(true));
        assertThat(destination.isDirectory(), is(true));
        assertThat(FileUtils.listFiles(destination, null, false).size(), is(3));
    }

    @Test
    public void shouldNotCopyCSVIfNotAssociatedWithAFilteredScenario() throws Exception {
        TestSplitter criteria = mock(AbstractTestSplitter.class);

        File folder = folder("folder");
        List<TlbFileResource> resources = scenarioResource(folder, 1, 2);
        scenarioCSV(folder, 10);
        final SuiteFileConvertor convertor = new SuiteFileConvertor();
        when(criteria.filterSuites(any(List.class), eq(moduleName))).thenReturn(convertor.toTlbSuiteFiles(resources));

        LoadBalancedTwistSuite suite = new LoadBalancedTwistSuite(criteria);

        suite.balance(folder.getAbsolutePath(), "destination", moduleName);

        File destination = new File("destination");
        assertThat(destination.exists(), is(true));
        assertThat(destination.isDirectory(), is(true));
        assertThat(FileUtils.listFiles(destination, null, false).size(), is(2));
    }

    private void scenarioCSV(File folder, int name) throws IOException {
        File file = new File(folder.getAbsolutePath(), "base" + name + ".scn.csv");
        file.createNewFile();
        file.deleteOnExit();
    }

    private File folder(String name) {
        File folder = new File(name);
        folder.mkdir();
        folder.deleteOnExit();
        return folder;
    }

    private List<TlbFileResource> scenarioResource(File folder, int... names) throws IOException {
        List<TlbFileResource> resources = new ArrayList<TlbFileResource>();
        for (int name : names) {
            File file = new File(folder.getAbsolutePath(), "base" + name + ".scn");
            file.createNewFile();
            file.deleteOnExit();
            resources.add(new SceanrioFileResource(file));
        }
        return resources;
    }
}
