package tlb.utils;

import tlb.TestUtil;
import tlb.TlbConstants;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.core.Is.is;
import org.apache.commons.io.FileUtils;

import java.util.HashMap;
import java.io.File;
import java.io.IOException;

import com.googlecode.junit.ext.RunIf;
import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.checkers.OSChecker;

@RunWith(JunitExtRunner.class)
public class FileUtilTest {
    private FileUtil fileUtil;
    private TestUtil.LogFixture logFixture;
    private String javaTmpDir;
    private String overriddenTmpDir;
    private String overriddenTmpDirParent;
    private String javaTmpDirParent;

    @Before
    public void setUp() throws Exception {
        javaTmpDirParent = System.getProperty("java.io.tmpdir");
        javaTmpDir = new SystemEnvironment(new HashMap<String, String>()).tmpDir();
        overriddenTmpDirParent = new File(javaTmpDir + "/tlb_dir/foo/bar").getPath();

        logFixture = new TestUtil.LogFixture();

        HashMap<String, String> envMap = new HashMap<String, String>();

        envMap.put(TlbConstants.TLB_TMP_DIR, overriddenTmpDirParent);
        SystemEnvironment env = new SystemEnvironment(envMap);
        fileUtil = new FileUtil(env);
        overriddenTmpDir = new File(overriddenTmpDirParent + "/" + env.getDigest()).getPath();
        deleteOverriddenTmpDirIfExists();
    }

    private void deleteOverriddenTmpDirIfExists() throws IOException {
        File tmpDir = new File(overriddenTmpDir);
        if (tmpDir.exists()) {
            if (tmpDir.isDirectory()) {
                FileUtils.deleteDirectory(tmpDir);
            } else {
                FileUtils.forceDelete(tmpDir);
            }
        }
    }

    @After
    public void tearDown() throws IOException {
        logFixture.stopListening();
        deleteOverriddenTmpDirIfExists();
        FileUtils.deleteQuietly(new File(javaTmpDir));
    }

    @Test
    public void testClassFileRelativePath() {
        assertThat(fileUtil.classFileRelativePath("com.thoughtworks.cruise.Foo"), is("com/thoughtworks/cruise/Foo.class"));
    }

    @Test
    public void testGetsUniqueFileForGivenStringUnderTmpDir() {
        logFixture.startListening();
        File uniqueFile = fileUtil.getUniqueFile("foo_bar_baz");
        assertThat(uniqueFile.getParentFile().getAbsolutePath(), is(overriddenTmpDir));
        logFixture.assertHeard(String.format("unique file name foo_bar_baz translated to %s", uniqueFile.getAbsolutePath()));
    }

    @Test
    public void shouldDefaultTmpDirToSystemTmpDir() throws Exception{
        logFixture.startListening();
        assertThat(new File(fileUtil.tmpDir()).getAbsolutePath(), is(overriddenTmpDir));
        logFixture.assertNotHeard("defaulting");
        logFixture.assertHeard(String.format("using %s as tlb temp directory", overriddenTmpDirParent));
        logFixture.assertHeard(String.format("checking for existance of directory %s as tlb tmpdir", overriddenTmpDir));
        logFixture.assertHeard(String.format("directory %s doesn't exist, creating it now", overriddenTmpDir));
        assertThat(new File(fileUtil.tmpDir()).exists(), is(true));

        FileUtil util = new FileUtil(new SystemEnvironment(new HashMap<String, String>()));
        assertThat(util.tmpDir(), is(javaTmpDir));
        logFixture.assertHeard(String.format("defaulting tlb tmp directory to %s", javaTmpDirParent));
        logFixture.assertHeard(String.format("using %s as tlb temp directory", javaTmpDirParent));
        logFixture.assertHeard(String.format("checking for existance of directory %s as tlb tmpdir", javaTmpDir));
        logFixture.assertHeard(String.format("directory %s exists, creation not required", javaTmpDir));
    }

    @Test
    public void shouldFailIfFindsAFileInPlaceOfDirectoryForTmpDir() throws Exception{
        FileUtils.writeStringToFile(new File(overriddenTmpDir), "hello world");
        logFixture.startListening();
        try {
            fileUtil.tmpDir();
            fail("should have failed as a file exists in place of tmp dir");
        } catch (Exception e) {
            //ignore
        }
        logFixture.assertHeard(String.format("tlb tmp dir %s is a file, it must be a directory", overriddenTmpDir));
    }

    @Test
    public void shouldMakeTmpDirIfNonExistant() throws Exception{
        assertThat(new File(overriddenTmpDir).exists(), is(false));
        logFixture.startListening();
        fileUtil.tmpDir();
        logFixture.assertHeard(String.format("using %s as tlb temp directory", overriddenTmpDirParent));
        logFixture.assertHeard(String.format("checking for existance of directory %s as tlb tmpdir", overriddenTmpDir));
        logFixture.assertHeard(String.format("directory %s doesn't exist, creating it now", overriddenTmpDir));
        logFixture.assertHeard(String.format("created directory %s, which is to be used as tlb tmp dir.", overriddenTmpDir));
        File tmpDir = new File(overriddenTmpDir);
        assertThat(tmpDir.exists(), is(true));
        assertThat(tmpDir.isDirectory(), is(true));
    }

    @Test
    @RunIf(value = OSChecker.class, arguments = OSChecker.LINUX)
    public void shouldFailIfDirCouldNotBeCreated() throws Exception{
        HashMap<String, String> envMap = new HashMap<String, String>();
        String tlbTmpDirPath = new File("/var/lib/tlb_data").getPath();
        envMap.put(TlbConstants.TLB_TMP_DIR, tlbTmpDirPath);
        SystemEnvironment env = new SystemEnvironment(envMap);
        FileUtil util = new FileUtil(env);
        assertThat(new File(tlbTmpDirPath).exists(), is(false));
        logFixture.startListening();
        try {
            util.tmpDir();
            fail("should have bombed on failing to create tmp dir");
        } catch (Exception e) {
            //ignore
        }
        String tlbActualTmpDir = new File(tlbTmpDirPath + "/" + env.getDigest()).getPath();
        logFixture.assertHeard(String.format("using %s as tlb temp directory", tlbTmpDirPath));
        logFixture.assertHeard(String.format("checking for existance of directory %s as tlb tmpdir", tlbActualTmpDir));
        logFixture.assertHeard(String.format("directory %s doesn't exist, creating it now", tlbActualTmpDir));
        logFixture.assertHeard(String.format("could not create directory %s", tlbActualTmpDir));
        File tmpDir = new File(tlbTmpDirPath);
        assertThat(tmpDir.exists(), is(false));
    }

    @Test
    public void shouldStripTheExtention() {
        assertThat(FileUtil.stripExtension("foo.name"), is("foo"));
        assertThat(FileUtil.stripExtension("foo.name.ext"), is("foo.name"));
        assertThat(FileUtil.stripExtension("foo"), is("foo"));
    }
}
