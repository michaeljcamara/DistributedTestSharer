package tlb.ant;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tlb.TestUtil;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JunitFileResourceTest {

    private File tmpDir;

    @Before
    public void setUp() {
        tmpDir = TestUtil.createTmpDir();
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(tmpDir);
    }

    @Test
    public void shouldReturnTheClassNameAsName() {
        Project project = new Project();

        String baseDir = tmpDir.getAbsolutePath();
        project.setBasedir(baseDir);
        JunitFileResource junitFileResource = new JunitFileResource(project, "foo/bar/Baz.class");
        junitFileResource.setBaseDir(new File(baseDir));
        assertThat(junitFileResource.getName(), is(new File("foo/bar/Baz.class").getPath()));
    }
}
