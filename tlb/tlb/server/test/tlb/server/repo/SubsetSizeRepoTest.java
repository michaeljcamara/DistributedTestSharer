package tlb.server.repo;

import tlb.domain.SubsetSizeEntry;
import org.junit.Before;
import org.junit.Test;
import tlb.server.RepoFactoryTestUtil;
import tlb.utils.FileUtil;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class SubsetSizeRepoTest {
    private SubsetSizeRepo subsetSizeRepo;

    @Before
    public void setUp() throws Exception {
        subsetSizeRepo = new SubsetSizeRepo();
    }
    
    @Test
    public void shouldNotAllowUpdate() {
        try {
            subsetSizeRepo.update(new SubsetSizeEntry(10));
            fail("update should not have been allowed");
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage(), is("update not allowed on repository"));
        }
    }

    @Test
    public void shouldNotAllowUpdateAll() {
        try {
            subsetSizeRepo.updateAll(Arrays.asList(new SubsetSizeEntry(10)));
            fail("update should not have been allowed");
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage(), is("update not allowed on repository"));
        }
    }


    @Test
    public void shouldListAddedEntries() {
        addToRepo();

        List<SubsetSizeEntry> entries = (List<SubsetSizeEntry>) subsetSizeRepo.list();

        assertListContents(entries);
    }

    private void addToRepo() {
        subsetSizeRepo.add(new SubsetSizeEntry(10));
        subsetSizeRepo.add(new SubsetSizeEntry(12));
        subsetSizeRepo.add(new SubsetSizeEntry(7));
    }

    private void assertListContents(List<SubsetSizeEntry> entries) {
        assertThat(entries.size(), is(3));
        assertThat(entries.get(0), is(new SubsetSizeEntry(10)));
        assertThat(entries.get(1), is(new SubsetSizeEntry(12)));
        assertThat(entries.get(2), is(new SubsetSizeEntry(7)));
    }

    @Test
    public void shouldDumpDataAsString() throws IOException, ClassNotFoundException {
        addToRepo();
        String dump = RepoFactoryTestUtil.diskDump(subsetSizeRepo);
        subsetSizeRepo.loadCopyFromDisk(new StringReader(dump));
        assertListContents((List<SubsetSizeEntry>) subsetSizeRepo.list());
    }

    @Test
    public void shouldLoadFromDisk() throws IOException, ClassNotFoundException {
        final StringReader reader = new StringReader("10\n12\n7\n");
        subsetSizeRepo.loadCopyFromDisk(new StringReader(FileUtil.readIntoString(new BufferedReader(reader))));
        assertListContents((List<SubsetSizeEntry>) subsetSizeRepo.list());
        assertThat(subsetSizeRepo.isDirty(), is(false));
    }

    @Test
    public void shouldLoadFromGivenSource() throws IOException, ClassNotFoundException {
        final StringReader reader = new StringReader("10\n12\n7\n");
        StringReader reader1 = new StringReader(FileUtil.readIntoString(new BufferedReader(reader)));
        subsetSizeRepo.loadAndMarkDirty(reader1);
        assertListContents((List<SubsetSizeEntry>) subsetSizeRepo.list());
        assertThat(subsetSizeRepo.isDirty(), is(true));
    }
    
    @Test
    public void shouldNotAllowVersioning() {
        try {
            subsetSizeRepo.list("crap");
            fail("should not have allowed versioning");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("versioning not allowed"));
        }
    }

    @Test
    public void shouldKnowHowToParseEntries() {
        List<SubsetSizeEntry> subsetSizeEntry = subsetSizeRepo.parse("10\n19");
        assertThat(subsetSizeEntry.get(0), is(new SubsetSizeEntry(10)));
        assertThat(subsetSizeEntry.get(1), is(new SubsetSizeEntry(19)));
        assertThat(subsetSizeEntry.size(), is(2));
    }

    @Test
    public void shouldKnowHowToParseASingleEntry() {
        SubsetSizeEntry subsetSizeEntry = subsetSizeRepo.parseLine("10");
        assertThat(subsetSizeEntry, is(new SubsetSizeEntry(10)));
    }

    @Test
    public void shouldUnderstandDirtiness() throws IOException {
        SubsetSizeRepo repo = new SubsetSizeRepo();
        assertThat(repo.isDirty(), is(false));

        repo.add(new SubsetSizeEntry(10));
        assertThat(repo.isDirty(), is(true));

        RepoFactoryTestUtil.diskDump(repo);
        assertThat(repo.isDirty(), is(false));

        repo.add(new SubsetSizeEntry(25));
        assertThat(repo.isDirty(), is(true));

        repo.loadCopyFromDisk(new StringReader("10"));
        assertThat("Its not dirty if just loaded from file.", repo.isDirty(), is(false));

        StringReader reader = new StringReader("10\n15");
        repo.loadAndMarkDirty(reader);
        assertThat("Loaded, but not from file.", repo.isDirty(), is(true));
    }

    @Test
    public void shouldUnderstandWhenHasFactorySet() {
        SubsetSizeRepo repo = new SubsetSizeRepo();
        assertThat(repo.hasFactory(), is(false));
        repo.setFactory(mock(EntryRepoFactory.class));
        assertThat(repo.hasFactory(), is(true));
        repo.setFactory(null);
        assertThat(repo.hasFactory(), is(false));
    }

    @Test
    public void shouldReturnIdentifier() {
        SubsetSizeRepo repo = new SubsetSizeRepo();
        assertThat(repo.getIdentifier(), is(nullValue()));
        repo.setIdentifier("foo");
        assertThat(repo.getIdentifier(), is("foo"));
    }
}
