package tlb.server;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.util.ServerList;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.SubsetSizeEntry;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SubsetSizeRepo;
import tlb.utils.SystemEnvironment;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static junit.framework.Assert.fail;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static tlb.TlbConstants.Server.EntryRepoFactory.SUBSET_SIZE;
import static tlb.TlbConstants.Server.TLB_VERSION_LIFE_IN_DAYS;
import static tlb.server.repo.EntryRepoFactory.LATEST_VERSION;

public class TlbServerInitializerTest {
    private TlbServerInitializer initializer;
    private HashMap<String, String> systemEnv;
    private Context context = new Context();

    @Before
    public void setUp() {
        systemEnv = new HashMap<String, String>();
        SystemEnvironment env = new SystemEnvironment(systemEnv);
        initializer = new TlbServerInitializer(env);
    }

    @Test
    public void shouldCreateTlbApplication() {
        final Restlet app = initializer.application();
        assertThat(app, is(TlbApplication.class));
    }

    @Test
    public void shouldCreateApplicationContextWithRepoFactory() {
        ConcurrentMap<String,Object> map = initializer.application().getContext().getAttributes();
        assertThat(map.get(TlbConstants.Server.REPO_FACTORY), is(EntryRepoFactory.class));
    }

    @Test
    public void shouldInitializeTlbToRunOnConfiguredPort() {
        systemEnv.put(TlbConstants.Server.TLB_SERVER_PORT.key, "1234");
        assertThat(new TlbServerInitializer(new SystemEnvironment(systemEnv)).appPort(), is(1234));
    }

    @Test
    public void shouldInitializeTlbWithDefaultPortIfNotGiven() {
        Component component = initializer.init();
        ServerList servers = component.getServers();
        assertThat(servers.size(), is(1));
        assertThat(servers.get(0).getPort(), is(7019));
    }


    @Test
    public void shouldRegisterEntryRepoFactoryExitHook() {
        final EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        class TestMain extends TlbServerInitializer {
            TestMain(SystemEnvironment env) {
                super(env);
            }

            @Override
            EntryRepoFactory repoFactory() {
                return repoFactory;
            }
        }
        TestMain main = new TestMain(new SystemEnvironment());
        Context ctx = main.application().getContext();
        assertThat((EntryRepoFactory) ctx.getAttributes().get(TlbConstants.Server.REPO_FACTORY), sameInstance(repoFactory));
        verify(repoFactory).registerExitHook();
    }

    @Test
    public void shouldInitializeEntryRepoFactoryWithPresentWorkingDirectoryAsDiskStorageRoot() throws IOException, ClassNotFoundException {
        EntryRepoFactory factory = initializer.repoFactory();
        File dir = TestUtil.mkdirInPwd("tlb_store");
        File file = new File(dir, new EntryRepoFactory.VersionedNamespace(LATEST_VERSION, SUBSET_SIZE).getIdUnder("foo"));
        List<SubsetSizeEntry> entries = writeEntriedTo(file);
        SubsetSizeRepo repo = factory.createSubsetRepo("foo", LATEST_VERSION);
        assertThat((List<SubsetSizeEntry>) repo.list(), is(entries));
    }

    private List<SubsetSizeEntry> writeEntriedTo(File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        List<SubsetSizeEntry> entries = Arrays.asList(new SubsetSizeEntry(1), new SubsetSizeEntry(2), new SubsetSizeEntry(3));
        for (SubsetSizeEntry entry : entries) {
            writer.append(entry.dump());
        }
        writer.close();
        return entries;
    }

    @Test
    public void shouldHonorDiskStorageRootOverride() throws IOException, ClassNotFoundException {
        File tmpDir = TestUtil.createTmpDir();
        try {
            String tmpDirPath = tmpDir.getAbsolutePath();
            systemEnv.put(TlbConstants.Server.TLB_DATA_DIR.key, tmpDirPath);
            initializer = new TlbServerInitializer(new SystemEnvironment(systemEnv));
            EntryRepoFactory factory = initializer.repoFactory();
            File file = new File(tmpDirPath, new EntryRepoFactory.VersionedNamespace(LATEST_VERSION, SUBSET_SIZE).getIdUnder("quux"));
            file.deleteOnExit();
            writeEntriedTo(file);
            SubsetSizeRepo repo = factory.createSubsetRepo("quux", LATEST_VERSION);
            assertThat((List<SubsetSizeEntry>) repo.list(), is(Arrays.asList(new SubsetSizeEntry(1), new SubsetSizeEntry(2), new SubsetSizeEntry(3))));
        } finally {
            FileUtils.deleteQuietly(tmpDir);
        }
    }
    
    @Test
    public void shouldEscapeTheEscapeCharInName() {
        assertThat(new EntryRepoFactory.VersionedNamespace("bar", "baz").getIdUnder("foo"), is("foo_bar_baz"));
        assertThat(new EntryRepoFactory.VersionedNamespace("b_ar_", "baz|").getIdUnder("fo_o"), is("fo__o_b__ar___baz|"));
    }

    @Test
    public void shouldSetTimerToPurgeOldVersions() {
        final TimerTask[] tasks = new TimerTask[1];
        final EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        final Timer timer = new Timer() {
            @Override
            public void schedule(TimerTask task, long delay, long period) {
                if (task instanceof TlbServerInitializer.Purge) {
                    tasks[0] = task;
                    assertThat(delay, is(0l));
                    assertThat(period, is(TlbServerInitializer.ONCE_A_DAY));
                }
            }
        };

        new TlbServerInitializer(new SystemEnvironment(systemEnv), timer) {
            @Override
            EntryRepoFactory repoFactory() {
                return repoFactory;
            }
        }.init();

        verify(repoFactory).registerExitHook();
        verifyNoMoreInteractions(repoFactory);

        tasks[0].run();
        verify(repoFactory).purgeVersionsOlderThan(7);
        verifyNoMoreInteractions(repoFactory);

        final EntryRepoFactory anotherRepoFactory = mock(EntryRepoFactory.class);

        systemEnv.put(TLB_VERSION_LIFE_IN_DAYS.key, "3");

        new TlbServerInitializer(new SystemEnvironment(systemEnv), timer) {
            @Override
            EntryRepoFactory repoFactory() {
                return anotherRepoFactory;
            }
        }.init();

        verify(repoFactory).registerExitHook();
        verifyNoMoreInteractions(repoFactory);

        tasks[0].run();
        verify(anotherRepoFactory).purgeVersionsOlderThan(3);
        verifyNoMoreInteractions(repoFactory);
    }

    @Test
    public void shouldSetTimerToFlushFilesToDisk_EveryHour() {
        final TimerTask[] tasks = new TimerTask[1];
        final EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        final Timer timer = new Timer() {
            @Override
            public void schedule(TimerTask task, long delay, long period) {
                if (task instanceof TlbServerInitializer.SyncToDisk) {
                    tasks[0] = task;
                    assertThat(delay, is(0l));
                    assertThat(period, is((long) TlbServerInitializer.MILLS_PER_HOUR));
                }
            }
        };

        new TlbServerInitializer(new SystemEnvironment(systemEnv), timer) {
            @Override
            EntryRepoFactory repoFactory() {
                return repoFactory;
            }
        }.init();

        verify(repoFactory).registerExitHook();
        verifyNoMoreInteractions(repoFactory);

        tasks[0].run();
        verify(repoFactory).syncReposToDisk();

        verifyNoMoreInteractions(repoFactory);
    }
    
    @Test
    public void shouldSetTimerToFlushFilesToDisk_AtSpecifiedInterval() {
        final TimerTask[] tasks = new TimerTask[1];
        final EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        final Timer timer = new Timer() {
            @Override
            public void schedule(TimerTask task, long delay, long period) {
                if (task instanceof TlbServerInitializer.SyncToDisk) {
                    tasks[0] = task;
                    assertThat(delay, is(0l));
                    assertThat(period, is(2l * TlbServerInitializer.MILLS_PER_MINUTE));//every 2 mins
                }
            }
        };

        new TlbServerInitializer(new SystemEnvironment(Collections.singletonMap(TlbConstants.Server.TLB_SYNC_TO_DISK_INTERVAL_IN_MINS.key, "2")), timer) {
            @Override
            EntryRepoFactory repoFactory() {
                return repoFactory;
            }
        }.init();

        verify(repoFactory).registerExitHook();
        verifyNoMoreInteractions(repoFactory);

        assertThat(tasks[0], is(notNullValue()));
    }

    @Test
    public void shouldNotSetTimerIfNoVersionLifeIsMentioned() {
        systemEnv.put(TLB_VERSION_LIFE_IN_DAYS.key, "-1");

        final EntryRepoFactory repoFactory = mock(EntryRepoFactory.class);
        final Timer timer = new Timer() {
            @Override
            public void schedule(TimerTask task, long delay, long period) {
                if (task instanceof TlbServerInitializer.Purge) {
                    fail("Should not have scheduled anything!");
                }
            }
        };

        new TlbServerInitializer(new SystemEnvironment(systemEnv), timer) {
            @Override
            EntryRepoFactory repoFactory() {
                return repoFactory;
            }
        }.init();

        verify(repoFactory).registerExitHook();
        verifyNoMoreInteractions(repoFactory);
    }
}
