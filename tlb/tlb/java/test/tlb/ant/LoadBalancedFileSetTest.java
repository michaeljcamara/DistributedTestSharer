package tlb.ant;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbFileResource;
import tlb.TlbSuiteFile;
import tlb.orderer.TestOrderer;
import tlb.splitter.CountBasedTestSplitter;
import tlb.splitter.JobFamilyAwareSplitter;
import tlb.utils.FileUtil;
import tlb.utils.SuiteFileConvertor;
import tlb.utils.SystemEnvironment;

import java.io.File;
import java.util.*;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tlb.TlbConstants.Go.GO_SERVER_URL;
import static tlb.TlbConstants.TLB_SPLITTER;

public class LoadBalancedFileSetTest {
    private LoadBalancedFileSet fileSet;
    private File projectDir;
    private File tmpDir;

    @Before
    public void setUp() throws Exception {
        SystemEnvironment env = new SystemEnvironment(new HashMap<String, String>());
        tmpDir = new File(new FileUtil(env).tmpDir());
        fileSet = new LoadBalancedFileSet(env);
        projectDir = TestUtil.createTmpDir();
        initFileSet(fileSet);
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(projectDir);
        FileUtils.deleteQuietly(tmpDir);
    }

    private void initFileSet(FileSet fileSet) {
        fileSet.setDir(projectDir);
        fileSet.setProject(new Project());
    }

    @Test
    public void shouldReturnAListOfFilesWhichMatchAGivenMatcher() {
        TestUtil.createFileInFolder(projectDir, "excluded");
        File included = TestUtil.createFileInFolder(projectDir, "included");

        JobFamilyAwareSplitter criteria = mock(JobFamilyAwareSplitter.class);
        TlbFileResource fileResource = new JunitFileResource(included);
        final SuiteFileConvertor convertor = new SuiteFileConvertor();
        when(criteria.filterSuites(any(List.class), eq("module_foo"))).thenReturn(convertor.toTlbSuiteFiles(Arrays.asList(fileResource)));

        fileSet = new LoadBalancedFileSet(criteria, TestOrderer.NO_OP);
        fileSet.setDir(projectDir);
        fileSet.setModuleName("module_foo");
        initFileSet(fileSet);
        Iterator files = fileSet.iterator();

        assertThat(files.hasNext(), is(true));
        assertThat(((FileResource) files.next()).getFile(), is(included));
        assertThat(files.hasNext(), is(false));
    }

    @Test
    public void shouldReturnAListOfFilesInOrderReturnedByReorderer() {
        TestUtil.createFileInFolder(projectDir, "excluded");
        JunitFileResource resourceOne = new JunitFileResource(TestUtil.createFileInFolder(projectDir, "C"));
        JunitFileResource resourceTwo = new JunitFileResource(TestUtil.createFileInFolder(projectDir, "B"));
        JunitFileResource resourceThree = new JunitFileResource(TestUtil.createFileInFolder(projectDir, "A"));

        JobFamilyAwareSplitter criteria = mock(JobFamilyAwareSplitter.class);

        final SuiteFileConvertor convertor = new SuiteFileConvertor();
        when(criteria.filterSuites(any(List.class), eq("default-module"))).thenReturn(convertor.toTlbSuiteFiles(Arrays.asList((TlbFileResource) resourceOne, resourceTwo, resourceThree)));//should use default module when module-name is not set

        fileSet = new LoadBalancedFileSet(criteria, new TestOrderer(new SystemEnvironment()) {
            public int compare(TlbSuiteFile o1, TlbSuiteFile o2) {
                return 1;
            }
        });
        fileSet.setDir(projectDir);
        initFileSet(fileSet);
        Iterator files = fileSet.iterator();

        assertThat(files.hasNext(), is(true));
        List<FileResource> resources = new ArrayList<FileResource>();

        while (files.hasNext()) {
            resources.add((FileResource) files.next());
        }
        List<FileResource> expectedResourcesOrder = new ArrayList<FileResource>();
        expectedResourcesOrder.add(resourceThree.getFileResource());
        expectedResourcesOrder.add(resourceTwo.getFileResource());
        expectedResourcesOrder.add(resourceOne.getFileResource());
        assertThat(resources, is(expectedResourcesOrder));
    }

    @Test
    public void shouldUseSystemPropertyToInstantiateCriteria() throws IllegalAccessException {
        SystemEnvironment env = initEnvironment("tlb.splitter.CountBasedTestSplitter");
        try {
            fileSet = new LoadBalancedFileSet(env);
            fileSet.setDir(projectDir);
            assertThat(TestUtil.deref("splitter", fileSet.getSplitterCriteria()), instanceOf(CountBasedTestSplitter.class));
        } finally {
            FileUtils.deleteQuietly(new File(new FileUtil(env).tmpDir()));
        }
    }

    private SystemEnvironment initEnvironment(String strategyName) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(TLB_SPLITTER, strategyName);
        map.put(GO_SERVER_URL, "https://localhost:8154/cruise");
        return new SystemEnvironment(map);
    }
}
