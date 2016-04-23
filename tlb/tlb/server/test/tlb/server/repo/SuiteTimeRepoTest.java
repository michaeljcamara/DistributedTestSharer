package tlb.server.repo;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.SuiteTimeEntry;
import tlb.utils.SystemEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static tlb.domain.SuiteTimeEntry.parseSingleEntry;
import static tlb.server.repo.EntryRepoFactory.LATEST_VERSION;

public class SuiteTimeRepoTest {
    private SuiteTimeRepo repo;
    protected File tmpDir;
    private EntryRepoFactory factory;

    @Before
    public void setUp() throws Exception {
        tmpDir = TestUtil.createTmpDir();
        factory = new EntryRepoFactory(env());
        repo = factory.createSuiteTimeRepo("name", LATEST_VERSION);
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(tmpDir);
    }

    private SystemEnvironment env() {
        final HashMap<String, String> env = new HashMap<String, String>();
        env.put(TlbConstants.Server.TLB_DATA_DIR.key, tmpDir.getAbsolutePath());
        return new SystemEnvironment(env);
    }

    @Test
    public void shouldKeepVersionFrozenAcrossDumpAndReload() throws InterruptedException, ClassNotFoundException, IOException {
        repo.update(parseSingleEntry("foo.bar.Foo: 12"));
        repo.update(parseSingleEntry("foo.bar.Bar: 134"));
        factory.createSuiteTimeRepo("name", "foo");
        repo.update(parseSingleEntry("foo.bar.Baz: 15"));
        repo.update(parseSingleEntry("foo.bar.Bar: 18"));
        final Thread exitHook = factory.exitHook();
        exitHook.start();
        exitHook.join();
        final EntryRepoFactory newFactory = new EntryRepoFactory(env());
        final List<SuiteTimeEntry> frozenCollection = newFactory.createSuiteTimeRepo("name", "foo").sortedList();
        assertThat(frozenCollection.size(), is(2));
        assertThat(frozenCollection, hasItem(new SuiteTimeEntry("foo.bar.Foo", 12)));
        assertThat(frozenCollection, hasItem(new SuiteTimeEntry("foo.bar.Bar", 134)));
    }

    @Test
    public void shouldUnderstandParsingEntries() {
        SuiteTimeRepo repo = new SuiteTimeRepo();
        List<SuiteTimeEntry> entries = repo.parse("foo.Bar: 10\nbar.Baz: 20");
        assertThat(entries.get(0), is(new SuiteTimeEntry("foo.Bar", 10l)));
        assertThat(entries.get(1), is(new SuiteTimeEntry("bar.Baz", 20l)));
    }

    @Test
    public void shouldUnderstandParsingSingleEntry() {
        SuiteTimeRepo repo = new SuiteTimeRepo();
        SuiteTimeEntry entry = repo.parseLine("foo.Bar: 10");
        assertThat(entry, is(new SuiteTimeEntry("foo.Bar", 10l)));
    }
}
