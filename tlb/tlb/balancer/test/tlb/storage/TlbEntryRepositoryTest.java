package tlb.storage;

import org.junit.After;
import tlb.TestUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import tlb.domain.NameNumberEntry;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TlbEntryRepositoryTest {
    private TestUtil.LogFixture logFixture;
    private File tmpDir;

    @Before
    public void setUp() {
        logFixture = new TestUtil.LogFixture();
        tmpDir = TestUtil.createTmpDir();
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.forceDelete(tmpDir);
    }

    @Test
    public void shouldLogWhenLoadingOrPersistingCachableData() throws Exception{
        File file = new File(tmpDir, "foo");
        try {
            FileUtils.forceDelete(file);
        } catch (IOException e) {
            //ignore, file may not be there!
        }

        TlbEntryRepository cruise = new TlbEntryRepository(new File(new File(tmpDir.getAbsolutePath()), "foo"));

        logFixture.startListening();
        cruise.appendLine("hello world\n");
        logFixture.assertHeard(String.format("Wrote line [ hello world\n ] to %s", file.getAbsolutePath()));
        cruise.appendLine("hacking is fun\n");
        logFixture.assertHeard(String.format("Wrote line [ hacking is fun\n ] to %s", file.getAbsolutePath()));
        cruise.appendLine("foo bar baz quux\n");
        logFixture.assertHeard(String.format("Wrote line [ foo bar baz quux\n ] to %s", file.getAbsolutePath()));
        cruise.loadLines();
        logFixture.assertHeard(String.format("Cached 3 lines from %s, the last of which was [ foo bar baz quux ]", file.getAbsolutePath()));
    }

    @Test
    public void shouldLogWhenPersistingMultiLineCacheableData() throws Exception{
        File file = new File(tmpDir, "foo");
        try {
            FileUtils.forceDelete(file);
        } catch (IOException e) {
            //ignore, file may not be there!
        }

        TlbEntryRepository cruise = new TlbEntryRepository(new File(new File(tmpDir.getAbsolutePath()), "foo"));

        logFixture.startListening();
        cruise.appendLines(Arrays.asList(new NameNumberEntry("foo", 10), new NameNumberEntry("bar", 20), new NameNumberEntry("baz", 30), new NameNumberEntry("quux", 40)));
        logFixture.assertHeard(String.format("Wrote 4 lines with first line [ foo: 10 ] and last line [ quux: 40 ] to %s", file.getAbsolutePath()));
        cruise.loadLines();
        logFixture.assertHeard(String.format("Cached 4 lines from %s, the last of which was [ quux: 40 ]", file.getAbsolutePath()));
    }


    @Test
    public void shouldNotFailToCleanupWhenDataFileDoesNotExist() throws IOException {
        File tmpDir = TestUtil.createTmpDir();
        try {
            TlbEntryRepository repo = new TlbEntryRepository(new File(tmpDir, "foo_bar_baz"));
            assertThat(repo.getFile().exists(), is(false));
            repo.cleanup();
            assertThat(repo.getFile().exists(), is(false));
        } finally {
            FileUtils.deleteQuietly(tmpDir);
        }
    }

    @Test
    public void shouldCleanupTheRepositoryDirectory() throws IOException {
        File repoFile = new File(TestUtil.createTmpDir(), "foo_bar_baz");
        boolean createdNewFile = repoFile.createNewFile();
        assertThat(createdNewFile, is(true));
        TlbEntryRepository repo = new TlbEntryRepository(repoFile);
        assertThat(repo.getFile().exists(), is(true));
        repo.cleanup();
        assertThat(repo.getFile().exists(), is(false));
    }
    
    @Test
    public void shouldRecordNumberOfLinesInFileHeader() {
        TlbEntryRepository repo = new TlbEntryRepository(new File(new File(tmpDir.getAbsolutePath()), "foo"));
        repo.appendLine("hello world");
        assertThat(repo.lineCount(), is(1));
        repo.appendLine("second");
        repo.appendLine("third");
        assertThat(repo.lineCount(), is(3));
    }

    @Test
    public void shouldLoadLastLine() {
        TlbEntryRepository repo = new TlbEntryRepository(new File(new File(tmpDir.getAbsolutePath()), "foo"));
        repo.appendLine("foo\n");
        assertThat(repo.loadLastLine(), is("foo"));
        repo.appendLine("bar\n");
        assertThat(repo.loadLastLine(), is("bar"));
        repo.appendLine("baz\n");
        repo.appendLine("quux\n");
        assertThat(repo.loadLastLine(), is("quux"));
    }
}
