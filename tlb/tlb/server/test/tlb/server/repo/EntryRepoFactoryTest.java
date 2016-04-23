package tlb.server.repo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.*;
import tlb.utils.SystemEnvironment;

import java.io.*;
import java.util.*;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.*;
import static tlb.TestUtil.deref;
import static tlb.TlbConstants.Server.EntryRepoFactory.*;
import static tlb.server.repo.EntryRepoFactory.LATEST_VERSION;

@RunWith(Theories.class)
public class EntryRepoFactoryTest {
    private EntryRepoFactory factory;
    private File baseDir;
    private TestUtil.LogFixture logFixture;
    private TimeProvider timeProvider;
    private File tmpDir;

    private SystemEnvironment env() {
        final HashMap<String, String> env = new HashMap<String, String>();
        env.put(TlbConstants.Server.TLB_DATA_DIR.key, baseDir.getAbsolutePath());
        return new SystemEnvironment(env);
    }

    @Before
    public void setUp() throws Exception {
        tmpDir = TestUtil.createTmpDir();
        System.err.println("tmpDir = " + tmpDir);
        baseDir = new File(tmpDir, "test_case_tlb_store");
        timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(new Date());
        factory = new EntryRepoFactory(baseDir, timeProvider, 100);
        logFixture = new TestUtil.LogFixture();
    }


    @After
    public void tearDown() throws IOException {
        factory.syncReposToDisk();
        FileUtils.deleteQuietly(tmpDir);
    }

    @Test
    public void shouldSyncReposToDisk() throws ClassNotFoundException, IOException {
        SubsetSizeRepo subsetRepo = factory.createSubsetRepo("dev", LATEST_VERSION);
        SuiteTimeRepo suiteTimeRepo = factory.createSuiteTimeRepo("dev", LATEST_VERSION);
        SuiteResultRepo suiteResultRepo = factory.createSuiteResultRepo("dev", LATEST_VERSION);
        String version = "foo-bar";
        String submoduleName = "module-name";
        SetRepo setRepo = factory.createUniversalSetRepo("dev", version, submoduleName);
        PartitionRecordRepo partitionRepo = factory.createPartitionRecordRepo("dev", version, submoduleName);

        subsetRepo.add(new SubsetSizeEntry(10));
        suiteTimeRepo.update(new SuiteTimeEntry("foo.bar.Quux", 25));
        suiteResultRepo.update(new SuiteResultEntry("foo.bar.Baz", true));
        synchronized (setRepo) {
            StringReader stringReader = new StringReader("foo/bar/Baz.quux");
            setRepo.loadAndMarkDirty(stringReader);
        }
        partitionRepo.subsetReceivedFromPartition(new PartitionIdentifier(2, 3));
        partitionRepo.subsetReceivedFromPartition(new PartitionIdentifier(1, 3));

        assertThat("No files should exist as sync on this factory has never been called.", baseDir.list().length, is(0));

        factory.syncReposToDisk();

        assertThat("Files should exist as sync on this factory has been called.", baseDir.list().length, is(6));//repo ledger is there as well

        assertContentIs(new EntryRepoFactory.VersionedNamespace(LATEST_VERSION, SUBSET_SIZE).getIdUnder("dev"), "10");
        assertContentIs(new EntryRepoFactory.VersionedNamespace(LATEST_VERSION, SUITE_TIME).getIdUnder("dev"), "foo.bar.Quux: 25");
        assertContentIs(new EntryRepoFactory.VersionedNamespace(LATEST_VERSION, SUITE_RESULT).getIdUnder("dev"), "foo.bar.Baz: true");
        assertContentIs(new EntryRepoFactory.SubmoduledUnderVersionedNamespace(version, UNIVERSAL_SET, submoduleName).getIdUnder("dev"), "foo/bar/Baz.quux");
        assertContentIs(new EntryRepoFactory.SubmoduledUnderVersionedNamespace(version, PARTITION_RECORD, submoduleName).getIdUnder("dev"), "2/3", "1/3");
    }

    @Test
    public void should_NOT_SyncReposToDisk_unlessDirty() throws ClassNotFoundException, IOException {
        SubsetSizeRepo subsetRepo = factory.createSubsetRepo("dev", LATEST_VERSION);
        SuiteTimeRepo suiteTimeRepo = factory.createSuiteTimeRepo("dev", LATEST_VERSION);
        SuiteResultRepo suiteResultRepo = factory.createSuiteResultRepo("dev", LATEST_VERSION);
        String version = "foo-bar";
        String submoduleName = "submodule";
        SetRepo setRepo = factory.createUniversalSetRepo("dev", version, submoduleName);
        PartitionRecordRepo partitionRepo = factory.createPartitionRecordRepo("dev", version, submoduleName);

        subsetRepo.add(new SubsetSizeEntry(10));
        suiteTimeRepo.update(new SuiteTimeEntry("foo.bar.Quux", 25));
        suiteResultRepo.update(new SuiteResultEntry("foo.bar.Baz", true));
        synchronized (setRepo) {
            StringReader stringReader1 = new StringReader("bar.baz.Quux");
            setRepo.loadAndMarkDirty(stringReader1);
        }
        partitionRepo.subsetReceivedFromPartition(new PartitionIdentifier(2, 3));

        factory.syncReposToDisk();

        FileUtils.cleanDirectory(baseDir);

        assertThat(baseDir.list().length, is(0));

        factory.syncReposToDisk();

        assertThat("No files should exist as no repos were dirty when sync-to-disk was called.", baseDir.list().length, is(0));

        subsetRepo.add(new SubsetSizeEntry(21));
        suiteTimeRepo.update(new SuiteTimeEntry("foo.bar.Bang", 35));
        suiteResultRepo.update(new SuiteResultEntry("foo.bar.Baz", true));
        synchronized (setRepo) {
            StringReader stringReader = new StringReader("quux.bar.Baz");
            setRepo.loadAndMarkDirty(stringReader);
        }
        partitionRepo.subsetReceivedFromPartition(new PartitionIdentifier(1, 3));

        factory.syncReposToDisk();

        assertThat("Files should exist as sync on this factory has been called.", baseDir.list().length, is(5));

        assertContentIs(new EntryRepoFactory.VersionedNamespace(LATEST_VERSION, SUBSET_SIZE).getIdUnder("dev"), "10", "21");
        assertContentIs(new EntryRepoFactory.VersionedNamespace(LATEST_VERSION, SUITE_TIME).getIdUnder("dev"), "foo.bar.Quux: 25", "foo.bar.Bang: 35");
        assertContentIs(new EntryRepoFactory.VersionedNamespace(LATEST_VERSION, SUITE_RESULT).getIdUnder("dev"), "foo.bar.Baz: true");
        assertContentIs(new EntryRepoFactory.SubmoduledUnderVersionedNamespace(version, UNIVERSAL_SET, submoduleName).getIdUnder("dev"), "quux.bar.Baz");
        assertContentIs(new EntryRepoFactory.SubmoduledUnderVersionedNamespace(version, PARTITION_RECORD, submoduleName).getIdUnder("dev"), "2/3", "1/3");
    }

    private void assertContentIs(final String fileName, String... str) throws IOException {
        List list = IOUtils.readLines(new FileInputStream(new File(baseDir, fileName)));
        assertThat("Expected content lines and number-of-lines-in-file do not match", list.size(), is(str.length));
        for (int i = 0; i < str.length; i++) {
            assertThat((String) list.get(i), is(str[i]));
        }
    }

    @Test
    public void shouldPassFactoryAndNamespaceToEachRepo() throws ClassNotFoundException, IOException {
        final EntryRepo createdEntryRepo = mock(EntryRepo.class);
        final EntryRepo repo = factory.findOrCreate("namespace", new EntryRepoFactory.VersionedNamespace("old_version", "suite_time"), new EntryRepoFactory.Creator<EntryRepo>() {
            public EntryRepo create() {
                return createdEntryRepo;
            }
        }, null);
        assertThat(repo, sameInstance(createdEntryRepo));
        verify(createdEntryRepo).setFactory(factory);
        verify(createdEntryRepo).setNamespace("namespace");
        verify(createdEntryRepo).setIdentifier("namespace_old__version_suite__time");
    }

    @Test
    public void shouldPrimeRepoFromGivenIdentifierWhenNotPrimed() throws ClassNotFoundException, IOException, InterruptedException {
        SuiteTimeRepo suiteTimeRepo = factory.createSuiteTimeRepo("dev", LATEST_VERSION);
        suiteTimeRepo.update(new SuiteTimeEntry("foo/Bar", 10l));
        suiteTimeRepo.update(new SuiteTimeEntry("bar/Baz", 20l));

        SuiteTimeRepo repoSnapshot = factory.createSuiteTimeRepo("dev", "snapshot");
        assertThat(repoSnapshot.list(), hasItem(new SuiteTimeEntry("foo/Bar", 10l)));
        assertThat(repoSnapshot.list(), hasItem(new SuiteTimeEntry("bar/Baz", 20l)));
        assertThat(repoSnapshot.list().size(), is(2));

        suiteTimeRepo.update(new SuiteTimeEntry("baz/Quux", 30l));
        suiteTimeRepo.update(new SuiteTimeEntry("foo/Bar", 15l));
        assertThat(suiteTimeRepo.list().size(), is(3));

        assertThat(repoSnapshot.list(), hasItem(new SuiteTimeEntry("foo/Bar", 10l)));
        assertThat(repoSnapshot.list(), hasItem(new SuiteTimeEntry("bar/Baz", 20l)));
        assertThat(repoSnapshot.list().size(), is(2));

        repoSnapshot = factory.createSuiteTimeRepo("dev", "snapshot");//again, should not re-prime this time
        assertThat(repoSnapshot.list(), hasItem(new SuiteTimeEntry("foo/Bar", 10l)));
        assertThat(repoSnapshot.list(), hasItem(new SuiteTimeEntry("bar/Baz", 20l)));
        assertThat(repoSnapshot.list().size(), is(2));

        Thread exitHook = factory.exitHook();
        exitHook.start();
        exitHook.join();

        factory.getRepos().clear();//dropped the cache, load from disk

        repoSnapshot = factory.createSuiteTimeRepo("dev", "snapshot");//once again, should not re-prime this time, should load from file
        assertThat(repoSnapshot.list(), hasItem(new SuiteTimeEntry("foo/Bar", 10l)));
        assertThat(repoSnapshot.list(), hasItem(new SuiteTimeEntry("bar/Baz", 20l)));
        assertThat(repoSnapshot.list().size(), is(2));

        suiteTimeRepo = factory.createSuiteTimeRepo("dev", LATEST_VERSION);
        suiteTimeRepo.update(new SuiteTimeEntry("foo/Bar", 15l));
        suiteTimeRepo.update(new SuiteTimeEntry("bar/Baz", 20l));
        suiteTimeRepo.update(new SuiteTimeEntry("baz/Quux", 30l));
        assertThat(suiteTimeRepo.list().size(), is(3));
    }

    @Test
    public void shouldNotOverrideSubsetRepoWithSuiteTimeRepo() throws ClassNotFoundException, IOException {
        SubsetSizeRepo subsetRepo = factory.createSubsetRepo("dev", LATEST_VERSION);
        SuiteTimeRepo suiteTimeRepo = factory.createSuiteTimeRepo("dev", LATEST_VERSION);
        SuiteResultRepo suiteResultRepo = factory.createSuiteResultRepo("dev", LATEST_VERSION);
        assertThat(factory.createSubsetRepo("dev", LATEST_VERSION), sameInstance(subsetRepo));
        assertThat(subsetRepo, is(SubsetSizeRepo.class));
        assertThat(factory.createSuiteTimeRepo("dev", LATEST_VERSION), sameInstance(suiteTimeRepo));
        assertThat(suiteTimeRepo, is(SuiteTimeRepo.class));
        assertThat(factory.createSuiteResultRepo("dev", LATEST_VERSION), sameInstance(suiteResultRepo));
        assertThat(suiteResultRepo, is(SuiteResultRepo.class));
    }

    @Test
    public void shouldKeepRecordOfAllReposCreated() throws IllegalAccessException, IOException, ClassNotFoundException {
        RepoLedger reposLedger = repoLedger(factory);
        assertThat(reposLedger.list().size(), is(0));
        when(timeProvider.now()).thenReturn(new Date(111, 10, 19, 6, 42));
        factory.createSuiteTimeRepo("name-time-1", "version-time-1");
        when(timeProvider.now()).thenReturn(new Date(111, 10, 19, 7, 0));
        factory.createSuiteTimeRepo("name-time-2", "version-time-2");
        factory.createSuiteResultRepo("name-result", "version-result");
        factory.createSuiteResultRepo("name-result", LATEST_VERSION);
        factory.createSubsetRepo("name-size", "version-size");
        factory.createSubsetRepo("name-size", LATEST_VERSION);
        when(timeProvider.now()).thenReturn(new Date(111, 10, 19, 8, 0));
        factory.createPartitionRecordRepo("name-partitions", "version-partitions", "module-name");
        factory.createUniversalSetRepo("name-partitions", "version-partitions", "module-name");

        assertThat(reposLedger.list().size(), is(10));

        assertThat(reposLedger.list(), hasItem(new RepoCreatedTimeEntry(getIdStr(EntryRepoFactory.LATEST_VERSION, SUITE_TIME, "name-time-1"), new Date(111, 10, 19, 6, 42).getTime(), false)));
        assertThat(reposLedger.list(), hasItem(new RepoCreatedTimeEntry(getIdStr("version-time-1", SUITE_TIME, "name-time-1"), new Date(111, 10, 19, 6, 42).getTime(), true)));
        assertThat(reposLedger.list(), hasItem(new RepoCreatedTimeEntry(getIdStr(EntryRepoFactory.LATEST_VERSION, SUITE_TIME, "name-time-2"), new Date(111, 10, 19, 7, 0).getTime(), false)));
        assertThat(reposLedger.list(), hasItem(new RepoCreatedTimeEntry(getIdStr("version-time-2", SUITE_TIME, "name-time-2"), new Date(111, 10, 19, 7, 0).getTime(), true)));

        assertThat(reposLedger.list(), hasItem(new RepoCreatedTimeEntry(getIdStr("version-result", SUITE_RESULT, "name-result"), new Date(111, 10, 19, 7, 0).getTime(), true)));
        assertThat(reposLedger.list(), hasItem(new RepoCreatedTimeEntry(getIdStr(EntryRepoFactory.LATEST_VERSION, SUITE_RESULT, "name-result"), new Date(111, 10, 19, 7, 0).getTime(), false)));

        assertThat(reposLedger.list(), hasItem(new RepoCreatedTimeEntry(getIdStr("version-size", SUBSET_SIZE, "name-size"), new Date(111, 10, 19, 7, 0).getTime(), true)));
        assertThat(reposLedger.list(), hasItem(new RepoCreatedTimeEntry(getIdStr(EntryRepoFactory.LATEST_VERSION, SUBSET_SIZE, "name-size"), new Date(111, 10, 19, 7, 0).getTime(), false)));

        assertThat(reposLedger.list(), hasItem(new RepoCreatedTimeEntry(new EntryRepoFactory.SubmoduledUnderVersionedNamespace("version-partitions", UNIVERSAL_SET, "module-name").getIdUnder("name-partitions"), new Date(111, 10, 19, 8, 0).getTime(), true)));
        assertThat(reposLedger.list(), hasItem(new RepoCreatedTimeEntry(new EntryRepoFactory.SubmoduledUnderVersionedNamespace("version-partitions", PARTITION_RECORD, "module-name").getIdUnder("name-partitions"), new Date(111, 10, 19, 8, 0).getTime(), true)));
    }

    private RepoLedger repoLedger(EntryRepoFactory factory) throws IllegalAccessException {
        return (RepoLedger) TestUtil.deref("repoLedger", factory);
    }

    private String getIdStr(final String version, final String type, final String namespace) {
        return new EntryRepoFactory.VersionedNamespace(version, type).getIdUnder(namespace);
    }

    @Test
    public void shouldReturnOneRepositoryForOneFamilyName() throws ClassNotFoundException, IOException {
        assertThat(factory.createSubsetRepo("dev", LATEST_VERSION), sameInstance(factory.createSubsetRepo("dev", LATEST_VERSION)));
    }

    @Test
    public void shouldCallDiskDumpForEachRepoAtExit() throws InterruptedException, IOException {
        EntryRepo repoFoo = mock(EntryRepo.class);
        EntryRepo repoBar = mock(EntryRepo.class);
        EntryRepo repoBaz = mock(EntryRepo.class);

        when(repoFoo.isDirty()).thenReturn(true);
        stubDiskDump(repoFoo, "foo-data");
        when(repoBar.isDirty()).thenReturn(true);
        stubDiskDump(repoBar, "bar-data");
        when(repoBaz.isDirty()).thenReturn(true);
        stubDiskDump(repoBaz, "baz-data");

        factory.getRepos().put("foo", repoFoo);
        factory.getRepos().put("bar", repoBar);
        factory.getRepos().put("baz", repoBaz);
        
        Thread exitHook = factory.exitHook();
        exitHook.start();
        exitHook.join();
        verify(repoFoo).diskDumpTo(any(Writer.class));
        verify(repoBar).diskDumpTo(any(Writer.class));
        verify(repoBaz).diskDumpTo(any(Writer.class));
    }

    @Test
    public void shouldBeAbleToLoadFromDumpedFile() throws ClassNotFoundException, IOException, InterruptedException {
        SubsetSizeRepo subsetSizeRepo = factory.createSubsetRepo("foo", LATEST_VERSION);
        subsetSizeRepo.add(new SubsetSizeEntry(50));
        subsetSizeRepo.add(new SubsetSizeEntry(100));
        subsetSizeRepo.add(new SubsetSizeEntry(200));

        SuiteTimeRepo subsetTimeRepo = factory.createSuiteTimeRepo("bar", LATEST_VERSION);
        subsetTimeRepo.update(new SuiteTimeEntry("foo.bar.Baz", 10));
        subsetTimeRepo.update(new SuiteTimeEntry("bar.baz.Quux", 20));

        SuiteResultRepo subsetResultRepo = factory.createSuiteResultRepo("baz", LATEST_VERSION);
        subsetResultRepo.update(new SuiteResultEntry("foo.bar.Baz", true));
        subsetResultRepo.update(new SuiteResultEntry("bar.baz.Quux", false));

        String version = "foo-bar";
        String submoduleName = "module-name";
        SetRepo setRepo = factory.createUniversalSetRepo("quux", version, submoduleName);
        synchronized (setRepo) {
            StringReader stringReader = new StringReader("foo/bar/Baz\nquux/bar/Baz\nfoo/baz/Quux");
            setRepo.loadAndMarkDirty(stringReader);
        }

        PartitionRecordRepo partitionRepo = factory.createPartitionRecordRepo("quux", version, submoduleName);
        partitionRepo.subsetReceivedFromPartition(new PartitionIdentifier(1, 2));
        partitionRepo.subsetReceivedFromPartition(new PartitionIdentifier(2, 2));

        Thread exitHook = factory.exitHook();
        exitHook.start();
        exitHook.join();
        EntryRepoFactory otherFactoryInstance = new EntryRepoFactory(env());
        assertThat(otherFactoryInstance.createSubsetRepo("foo", LATEST_VERSION).list(), is((Collection<SubsetSizeEntry>) Arrays.asList(new SubsetSizeEntry(50), new SubsetSizeEntry(100), new SubsetSizeEntry(200))));
        assertThat(otherFactoryInstance.createSuiteTimeRepo("bar", LATEST_VERSION).list().size(), is(2));
        assertThat(otherFactoryInstance.createSuiteTimeRepo("bar", LATEST_VERSION).list(), hasItems(new SuiteTimeEntry("foo.bar.Baz", 10), new SuiteTimeEntry("bar.baz.Quux", 20)));
        assertThat(otherFactoryInstance.createSuiteResultRepo("baz", LATEST_VERSION).list().size(), is(2));
        assertThat(otherFactoryInstance.createSuiteResultRepo("baz", LATEST_VERSION).list(), hasItems(new SuiteResultEntry("foo.bar.Baz", true), new SuiteResultEntry("bar.baz.Quux", false)));
        assertThat(otherFactoryInstance.createUniversalSetRepo("quux", version, submoduleName).list().size(), is(3));
        assertThat(otherFactoryInstance.createUniversalSetRepo("quux", version, submoduleName).list(), hasItems(new SuiteNamePartitionEntry("foo/bar/Baz"), new SuiteNamePartitionEntry("quux/bar/Baz"), new SuiteNamePartitionEntry("foo/baz/Quux")));
        assertThat(otherFactoryInstance.createPartitionRecordRepo("quux", version, submoduleName).list().size(), is(2));
        assertThat(otherFactoryInstance.createPartitionRecordRepo("quux", version, submoduleName).list(), hasItems(new PartitionIdentifier(1, 2), new PartitionIdentifier(2, 2)));
    }

    @Test
    public void shouldLogExceptionsButContinueDumpingRepositories() throws InterruptedException, IOException {
        EntryRepo repoFoo = mock(EntryRepo.class);
        EntryRepo repoBar = mock(EntryRepo.class);
        factory.getRepos().put("foo_subset__size", repoFoo);
        factory.getRepos().put("bar_subset__size", repoBar);
        when(repoFoo.isDirty()).thenReturn(true);
        doThrow(new RuntimeException("test exception")).when(repoFoo).diskDumpTo(any(Writer.class));

        when(repoBar.isDirty()).thenReturn(true);
        stubDiskDump(repoBar, "bar-data");

        logFixture.startListening();
        factory.run();
        logFixture.stopListening();

        verify(repoFoo).diskDumpTo(any(Writer.class));
        verify(repoBar).diskDumpTo(any(Writer.class));

        logFixture.assertHeard("disk dump of foo_subset__size failed");
    }

    private void stubDiskDump(EntryRepo repoBar, final String dumpString) throws IOException {
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                Writer writer = (Writer) arguments[0];
                writer.write(dumpString);
                return null;
            }
        }).when(repoBar).diskDumpTo(any(Writer.class));
    }

    @Test
    public void shouldWireUpAtExitHook() {
        factory.registerExitHook();
        try {
            Runtime.getRuntime().addShutdownHook(factory.exitHook());
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Hook previously registered"));
        }
    }

    @Test
    public void shouldUseWorkingDirAsDiskStorageRootWhenNotGiven() throws IOException, ClassNotFoundException {
        final File workingDirStorage = new File(TlbConstants.Server.DEFAULT_TLB_DATA_DIR);
        workingDirStorage.mkdirs();
        File file = new File(workingDirStorage, new EntryRepoFactory.VersionedNamespace(LATEST_VERSION, SUBSET_SIZE).getIdUnder("foo"));
        FileUtils.writeStringToFile(file, "1\n2\n3\n");
        EntryRepoFactory factory = new EntryRepoFactory(new SystemEnvironment(new HashMap<String, String>()));
        SubsetSizeRepo repo = factory.createSubsetRepo("foo", LATEST_VERSION);
        assertThat(repo.list(), is((Collection<SubsetSizeEntry>) Arrays.asList(new SubsetSizeEntry(1), new SubsetSizeEntry(2), new SubsetSizeEntry(3))));
    }

    @Test
    public void shouldLoadDiskDumpFromStorageRoot() throws IOException, ClassNotFoundException {
        baseDir.mkdirs();
        File file = new File(baseDir, new EntryRepoFactory.VersionedNamespace(LATEST_VERSION, SUBSET_SIZE).getIdUnder("foo"));
        FileUtils.writeStringToFile(file, "1\n2\n3\n");
        SubsetSizeRepo repo = factory.createSubsetRepo("foo", LATEST_VERSION);
        assertThat(repo.list(), is((Collection<SubsetSizeEntry>) Arrays.asList(new SubsetSizeEntry(1), new SubsetSizeEntry(2), new SubsetSizeEntry(3))));
    }

    @Test
    public void shouldNotLoadDiskDumpWhenUsingARepoThatIsAlreadyCreated() throws ClassNotFoundException, IOException {
        SubsetSizeRepo fooRepo = factory.createSubsetRepo("foo", LATEST_VERSION);
        File file = new File(baseDir, new EntryRepoFactory.VersionedNamespace(LATEST_VERSION, SUBSET_SIZE).getIdUnder("foo"));
        ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(file));
        outStream.writeObject(new ArrayList<SubsetSizeEntry>(Arrays.asList(new SubsetSizeEntry(1), new SubsetSizeEntry(2), new SubsetSizeEntry(3))));
        outStream.close();
        assertThat(fooRepo.list().size(), is(0));
        assertThat(factory.createSubsetRepo("foo", LATEST_VERSION).list().size(), is(0));
    }

    @Test
    public void shouldPurgeDiskDumpAndRepositoryWhenAsked() throws IOException, ClassNotFoundException, InterruptedException {
        SuiteTimeRepo fooRepo = factory.createSuiteTimeRepo("foo", LATEST_VERSION);
        fooRepo.update(new SuiteTimeEntry("foo.bar.Baz", 15));
        fooRepo.update(new SuiteTimeEntry("foo.bar.Quux", 80));
        final Thread exitHook = factory.exitHook();
        exitHook.start();
        exitHook.join();
        factory.purge(fooRepo.identifier);
        fooRepo = factory.createSuiteTimeRepo("foo", LATEST_VERSION);
        assertThat(fooRepo.list().size(), is(0));
        fooRepo = new EntryRepoFactory(env()).createSuiteTimeRepo("foo", LATEST_VERSION);
        assertThat(fooRepo.list().size(), is(0));
    }

    @Test
    public void shouldPurgeDiskDumpAndRepositoryOlderThanGivenTime() throws IOException, ClassNotFoundException, InterruptedException, IllegalAccessException {
        final GregorianCalendar[] cal = new GregorianCalendar[1];
        final TimeProvider timeProvider = new TimeProvider() {
            @Override
            public GregorianCalendar cal() {
                GregorianCalendar gregorianCalendar = cal[0];
                return gregorianCalendar == null ? null : (GregorianCalendar) gregorianCalendar.clone();
            }

            @Override
            public Date now() {
                return cal().getTime();
            }
        };
        final EntryRepoFactory factory = new EntryRepoFactory(baseDir, timeProvider, 100);

        cal[0] = new GregorianCalendar(2010, 6, 7, 0, 37, 12);

        SuiteTimeRepo repo = factory.createSuiteTimeRepo("foo", LATEST_VERSION);
        repo.update(new SuiteTimeEntry("foo.bar.Baz", 15));
        repo.update(new SuiteTimeEntry("foo.bar.Quux", 80));

        Collection<SuiteTimeEntry> oldList = factory.createSuiteTimeRepo("foo", "old").sortedList();
        assertThat(oldList.size(), is(2));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Baz", 15)));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Quux", 80)));

        repo.update(new SuiteTimeEntry("foo.bar.Bang", 130));
        repo.update(new SuiteTimeEntry("foo.bar.Baz", 20));

        oldList = factory.createSuiteTimeRepo("foo", "old").sortedList();
        assertThat(oldList.size(), is(2));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Baz", 15)));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Quux", 80)));


        cal[0] = new GregorianCalendar(2010, 6, 9, 0, 37, 12);
        Collection<SuiteTimeEntry> notSoOld = factory.createSuiteTimeRepo("foo", "not_so_old").sortedList();
        assertThat(notSoOld.size(), is(3));
        assertThat(notSoOld, hasItem(new SuiteTimeEntry("foo.bar.Baz", 20)));
        assertThat(notSoOld, hasItem(new SuiteTimeEntry("foo.bar.Quux", 80)));
        assertThat(notSoOld, hasItem(new SuiteTimeEntry("foo.bar.Bang", 130)));

        repo.update(new SuiteTimeEntry("foo.bar.Foo", 12));

        final Thread exitHook = factory.exitHook();
        exitHook.start();
        exitHook.join();

        assertThat(baseDir.list().length, is(4));
        assertThat(Arrays.asList(baseDir.list()), hasItem("foo_old_suite__time"));
        assertThat(repoLedger(factory).list().size(), is(3));
        assertThat(reposInLedger(factory), hasItem("foo_old_suite__time"));

        cal[0] = new GregorianCalendar(2010, 6, 10, 0, 37, 12);
        factory.purgeVersionsOlderThan(2);

        assertThat(Arrays.asList(baseDir.list()), not(hasItem("foo_old_suite__time")));
        assertThat(baseDir.list().length, is(3));
        assertThat(reposInLedger(factory), not(hasItem("foo_old_suite__time")));
        assertThat(repoLedger(factory).list().size(), is(2));

        oldList = factory.createSuiteTimeRepo("foo", "old").sortedList();
        assertThat(oldList.size(), is(4));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Baz", 20)));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Quux", 80)));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Bang", 130)));
        assertThat(oldList, hasItem(new SuiteTimeEntry("foo.bar.Foo", 12)));

        notSoOld = factory.createSuiteTimeRepo("foo", "not_so_old").sortedList();
        assertThat(notSoOld.size(), is(3));
        assertThat(notSoOld, hasItem(new SuiteTimeEntry("foo.bar.Baz", 20)));
        assertThat(notSoOld, hasItem(new SuiteTimeEntry("foo.bar.Quux", 80)));
        assertThat(notSoOld, hasItem(new SuiteTimeEntry("foo.bar.Bang", 130)));

        assertThat(reposInLedger(factory), hasItem("foo_old_suite__time"));
        assertThat(repoLedger(factory).list().size(), is(3));
    }

    private List<String> reposInLedger(EntryRepoFactory factory) throws IllegalAccessException {
        List<String> repoIds = new ArrayList<String>();
        for (RepoCreatedTimeEntry repoCreatedTimeEntry : repoLedger(factory).list()) {
            repoIds.add(repoCreatedTimeEntry.getRepoIdentifier());
        }
        return repoIds;
    }

    @Test
    public void shouldHaveATimerThatPurgesOldVersions_evenWhenExceptionsAreThrownForSomeRepos() throws ClassNotFoundException, IOException {
        final GregorianCalendar[] cal = new GregorianCalendar[1];
        cal[0] = new GregorianCalendar(2011, 10, 1, 0, 0, 0);
        final TimeProvider timeProvider = new TimeProvider() {
            @Override
            public GregorianCalendar cal() {
                GregorianCalendar gregorianCalendar = cal[0];
                return gregorianCalendar == null ? null : (GregorianCalendar) gregorianCalendar.clone();
            }

            @Override
            public Date now() {
                return cal().getTime();
            }
        };

        factory = new EntryRepoFactory(baseDir, timeProvider, 100) {
            @Override
            public void purge(String identifier) throws IOException {
                if (identifier.equals("bar_some-version_foo__bar")) {
                    throw new IOException("test exception");
                } else {
                    super.purge(identifier);
                }
            }
        };

        final NamedEntryRepo repo1 = mock(NamedEntryRepo.class);
        final NamedEntryRepo repo2 = mock(NamedEntryRepo.class);
        final NamedEntryRepo repo3 = mock(NamedEntryRepo.class);
        findOrCreateRepo(repo1, "foo");
        findOrCreateRepo(repo2, "bar");
        findOrCreateRepo(repo3, "baz");
        logFixture.startListening();

        cal[0] = new GregorianCalendar(2011, 10, 20, 6, 9, 35);
        factory.purgeVersionsOlderThan(12);
        logFixture.stopListening();
        logFixture.assertHeard("purged repo identified by 'foo_some-version_foo__bar' at 'Sun Nov 20 06:09:35 IST 2011'.");
        logFixture.assertHeard("purged repo identified by 'baz_some-version_foo__bar' at 'Sun Nov 20 06:09:35 IST 2011'.");
        logFixture.assertHeard("failed to delete older versions for repo identified by 'bar_some-version_foo__bar'");
        logFixture.assertHeardException(new IOException("test exception"));
    }

    private EntryRepo findOrCreateRepo(final NamedEntryRepo repo, String name) throws IOException, ClassNotFoundException {
        return factory.findOrCreate(name, new EntryRepoFactory.VersionedNamespace("some-version", "foo_bar"), new EntryRepoFactory.Creator<EntryRepo>() {
            public EntryRepo create() {
                return repo;
            }
        }, null);
    }

    @Test
    public void shouldCheckRepoExistenceBeforeTryingPurge() throws IOException, IllegalAccessException {
        factory.createSuiteTimeRepo("foo", LATEST_VERSION);
        factory.syncReposToDisk(); //Otherwise finalize call will re-create a tmp directory to write this empty file under it, and that will be outside the before/after cycle so we'll fail to perform any cleanup. Sigh, GC bites us yet another time. --janmejay
        Cache<EntryRepo> repos = (Cache<EntryRepo>) deref("cache", factory);
        List<String> keys = repos.keys();
        assertThat(keys.size(), is(1 + 1));//+ 1 for repoLedger
        String fooKey = keys.get(0);
        repos.clear();
        try {
            factory.purge(fooKey);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Should not fail when trying to purge already purged entry");
        }
    }

    public static final class CacheSizeEffectAssertion {
        final int loopLength;
        final Map<String, String> env;

        public CacheSizeEffectAssertion(Map<String, String> env, int loopLength) {
            this.env = env;
            this.loopLength = loopLength;
        }

        public EntryRepoFactory factory() {
            return new EntryRepoFactory(new SystemEnvironment(env));
        }

        @Override
        public String toString() {
            return "CacheSizeEffectAssertion{" +
                    "loopLength=" + loopLength +
                    ", env=" + env +
                    '}';
        }
    }

    @DataPoint public static final CacheSizeEffectAssertion CACHE_SIZE_4 = new CacheSizeEffectAssertion(Collections.singletonMap(TlbConstants.Server.TLB_DATA_CACHE_SIZE.key, "4"), 5);
    @DataPoint public static final CacheSizeEffectAssertion CACHE_SIZE_DEFAULT = new CacheSizeEffectAssertion(new HashMap<String, String>(), 101);

    @Theory
    @SuppressWarnings({"ConstantConditions"})
    public void shouldHonorCacheSize(CacheSizeEffectAssertion assertion) throws IOException, InterruptedException {
        EntryRepoFactory factory = assertion.factory();
        SuiteTimeRepo firstRepo = null;
        SuiteTimeRepo secondRepo = null;
        for (int i = 0; i < assertion.loopLength; i++) {
            SuiteTimeRepo repo = factory.createSuiteTimeRepo("foo-" + i, EntryRepoFactory.LATEST_VERSION);
            Thread.sleep(1);
            if (i == 0) {
                firstRepo = repo;
            } else if(i == 1) {
                secondRepo = repo;
            }
        }
        Cache<EntryRepo> repos = factory.getRepos();
        assertThat(repos.get(firstRepo.getIdentifier()), is(nullValue()));
        assertThat(repos.get(secondRepo.getIdentifier()), not(nullValue()));
    }
}
