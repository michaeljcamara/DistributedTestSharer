package tlb.utils;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.FileResource;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsSame;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tlb.TlbFileResource;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.ant.JunitFileResource;
import tlb.twist.SceanrioFileResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;

public class SuiteFileConvertorTest {
    protected SuiteFileConvertor convertor;
    protected ArrayList<TlbFileResource> resources;
    protected JunitFileResource bazTestResource;
    protected JunitFileResource fileTestResource;
    protected SceanrioFileResource fooScn;
    protected SceanrioFileResource barScn;
    protected SceanrioFileResource bazScn;
    protected JunitFileResource fooTest;

    @Before
    public void setUp() {
        convertor = new SuiteFileConvertor();
        resources = new ArrayList<TlbFileResource>();
        fooScn = new SceanrioFileResource(new File("foo.scn"));
        resources.add(fooScn);
        barScn = new SceanrioFileResource(new File("bar.scn"));
        resources.add(barScn);
        bazScn = new SceanrioFileResource(new File("baz.scn"));
        resources.add(bazScn);
        fooTest = new JunitFileResource(new File("FooTest.class"));
        resources.add(fooTest);
        final Project project = new Project();
        bazTestResource = new JunitFileResource(new FileResource(project, "BazTest.class"));
        resources.add(bazTestResource);
        fileTestResource = new JunitFileResource(new FileResource(new File("dir"), "FileTest.class"));
        resources.add(fileTestResource);
    }

    @Test
    public void shouldConvertAListOfTlbFileResourceToTlbSuiteFileInOrder() {
        List<TlbSuiteFile> suiteFiles = convertor.toTlbSuiteFiles(resources);
        Assert.assertThat(suiteFiles.size(), Is.is(6));
        Assert.assertThat(suiteFiles.get(0).getName(), is(fooScn.getName()));
        Assert.assertThat(suiteFiles.get(1).getName(), is(barScn.getName()));
        Assert.assertThat(suiteFiles.get(2).getName(), is(bazScn.getName()));
        Assert.assertThat(suiteFiles.get(3).getName(), is(fooTest.getName()));
        Assert.assertThat(suiteFiles.get(4).getName(), is(bazTestResource.getName()));
        Assert.assertThat(suiteFiles.get(5).getName(), is(fileTestResource.getName()));
    }
    
    @Test
    public void shouldNotAllowConvertingToTlbSuiteFilesTwice() {
        convertor.toTlbSuiteFiles(resources);
        try {
            convertor.toTlbSuiteFiles(resources);
            Assert.fail("should not have allowed overwriting");
        } catch (IllegalStateException e) {
            Assert.assertThat(e.getMessage(), Is.is("overwriting of suite resource list is not allowed, new instance should be used"));
        }
    }

    @Test
    public void shouldFetchCorrespondingFileResourcesForSubsetOfTestSuiteFilesInOrder() {
        convertor.toTlbSuiteFiles(resources);
        final List<TlbSuiteFile> files = new ArrayList<TlbSuiteFile>();
        files.add(new TlbSuiteFileImpl(bazScn.getName()));
        files.add(new TlbSuiteFileImpl(fileTestResource.getName()));
        files.add(new TlbSuiteFileImpl(fooTest.getName()));
        files.add(new TlbSuiteFileImpl(fooScn.getName()));
        List<TlbFileResource> filteredSet = convertor.toTlbFileResources(files);
        Assert.assertThat(filteredSet.size(), Is.is(4));
        Assert.assertThat(filteredSet.get(0), IsSame.sameInstance((TlbFileResource) bazScn));
        Assert.assertThat(filteredSet.get(1), IsSame.sameInstance((TlbFileResource) fileTestResource));
        Assert.assertThat(filteredSet.get(2), IsSame.sameInstance((TlbFileResource) fooTest));
        Assert.assertThat(filteredSet.get(3), IsSame.sameInstance((TlbFileResource) fooScn));
    }
}
