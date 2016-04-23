package tlb.server.repo;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import tlb.TestUtil;
import tlb.TlbConstants;
import tlb.domain.SuiteTimeEntry;
import tlb.utils.SystemEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static tlb.server.repo.EntryRepoFactory.LATEST_VERSION;

public class ServerWith_LowMem {
    public static final int NUMBER_OF_REPOS_TO_CREATE = 1000;
    private EntryRepoFactory factory;
    private File baseDir;

    private SystemEnvironment env() {
        final HashMap<String, String> env = new HashMap<String, String>();
        env.put(TlbConstants.Server.TLB_DATA_DIR.key, baseDir.getAbsolutePath());
        env.put(TlbConstants.Server.TLB_DATA_CACHE_SIZE.key, System.getenv(TlbConstants.Server.TLB_DATA_CACHE_SIZE.key));
        return new SystemEnvironment(env);
    }

    @Before
    public void setUp() throws Exception {
        File tmpDir = TestUtil.createTmpDir();
        baseDir = new File(tmpDir, "test_case_tlb_store");
        factory = new EntryRepoFactory(env());
    }

    @After
    public void tearDown() throws IOException {
        factory = null;
        //FileUtils.deleteQuietly(baseDir); this causes GC issues
    }
 
    @Test
    @Ignore
    public void shouldNotRunOutOfMemoryWhenTooManyRepositoriesAndVersionsAreLoaded() throws ClassNotFoundException, IOException, InterruptedException {
        final AtomicInteger idx = new AtomicInteger(0);
        Runnable test = new Runnable() {
            public void run() {
                while (true) {
                    int old = idx.get();
                    if (old == NUMBER_OF_REPOS_TO_CREATE) break;
                    int i = old + 1;
                    if (idx.compareAndSet(old, i)) {
                        String name = "repo-" + i;
                        System.out.println(name);
                        try {
                            SuiteTimeRepo suiteTimeRepo = factory.createSuiteTimeRepo(name, LATEST_VERSION);
                            for (int j = 0; j < 10000; j++) {
                                int val = i + 31 * j;
                                suiteTimeRepo.update(new SuiteTimeEntry(String.valueOf(val), val));
                            }
                            for (int j = 0; j < 100; j++)
                                factory.createSuiteTimeRepo(name, "abcde" + j);
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        };

        int twiceTheNumberOfCpus = Runtime.getRuntime().availableProcessors() * 2;
        Thread[] thds = new Thread[twiceTheNumberOfCpus];

        final List<Throwable> exceptionsInThd = new ArrayList<Throwable>();
        Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                exceptionsInThd.add(e);
            }
        };

        for(int i = 0; i < twiceTheNumberOfCpus; i++) {
            Thread thd = new Thread(test);
            thds[i] = thd;
            thd.setUncaughtExceptionHandler(exceptionHandler);
            thd.start();
        }
        for (Thread thd : thds) {
            thd.join();
        }

        for (Throwable throwable : exceptionsInThd) {
            throwable.printStackTrace();
        }
        assertThat(exceptionsInThd.size(), is(0));
    }
}
