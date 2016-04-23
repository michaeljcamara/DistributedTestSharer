package tlb.server.repo;

import org.junit.Before;
import org.junit.Test;
import tlb.domain.SuiteNamePartitionEntry;
import tlb.server.RepoFactoryTestUtil;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class SetRepoTest {

    private SetRepo repo;

    @Before
    public void setUp() throws Exception {
        repo = new SetRepo();
    }

    @Test
    public void shouldUnderstandParsingMultipleEntries() {
        List<SuiteNamePartitionEntry> parsedList = repo.parse("foo/bar/Baz\nbaz/bang/Quux\nhello/World");
        assertThat(parsedList, is(Arrays.asList(new SuiteNamePartitionEntry("foo/bar/Baz"), new SuiteNamePartitionEntry("baz/bang/Quux"), new SuiteNamePartitionEntry("hello/World"))));
        for (SuiteNamePartitionEntry suiteNamePartitionEntry : parsedList) {
            assertThat(suiteNamePartitionEntry.isUsedByAnyPartition(), is(false));
        }
    }

    @Test
    public void shouldUnderstandParsingSingleEntry() {
        SuiteNamePartitionEntry parsedEntry = repo.parseLine("foo/bar/Baz");
        assertThat(parsedEntry, is(new SuiteNamePartitionEntry("foo/bar/Baz")));
    }


    @Test
    public void shouldUnderstandIfRepoHasBeenPrimedWithData() throws IOException {
        assertThat(repo.isPrimed(), is(false));
        synchronized (repo) {
            StringReader stringReader = new StringReader("foo/bar/Baz\nbar/baz/Quux");
            repo.loadAndMarkDirty(stringReader);
        }
        assertThat(repo.isPrimed(), is(true));
        String s = RepoFactoryTestUtil.diskDump(repo);
        assertThat(repo.isPrimed(), is(true));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotAllowUpdate() {
        repo.update(new SuiteNamePartitionEntry("foo"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotAllowUpdateAll() {
        repo.updateAll(Arrays.asList(new SuiteNamePartitionEntry("foo")));
    }
}
