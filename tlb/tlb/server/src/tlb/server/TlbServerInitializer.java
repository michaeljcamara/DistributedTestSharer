package tlb.server;

import tlb.TlbConstants;
import tlb.server.repo.EntryRepoFactory;
import tlb.utils.SystemEnvironment;
import org.restlet.Context;
import org.restlet.Restlet;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @understands running the server as a standalone process
 */
public class TlbServerInitializer extends ServerInitializer {
    public static final int MILLS_PER_MINUTE = 60 * 1000;
    public static final int MILLS_PER_HOUR = 60 * MILLS_PER_MINUTE;
    public static final long ONCE_A_DAY = 24 * MILLS_PER_HOUR;
    private final SystemEnvironment env;
    private final Timer timer;


    public TlbServerInitializer(SystemEnvironment env) {
        this(env, new Timer());
    }

    public TlbServerInitializer(SystemEnvironment env, Timer timer) {
        this.env = env;
        this.timer = timer;
    }

    protected Restlet application() {
        Context applicationContext = new Context();
        initializeApplicationContext(applicationContext);
        return new TlbApplication(applicationContext);
    }

    public void initializeApplicationContext(Context applicationContext) {
        HashMap<String, Object> appMap = new HashMap<String, Object>();
        final EntryRepoFactory repoFactory = repoFactory();

        setupTimerForPurgingOlderVersions(repoFactory);
        setupTimerForFlushingToDisk(repoFactory);

        repoFactory.registerExitHook();
        appMap.put(TlbConstants.Server.REPO_FACTORY, repoFactory);
        applicationContext.setAttributes(appMap);
    }

    private void setupTimerForFlushingToDisk(EntryRepoFactory repoFactory) {
        timer.schedule(new SyncToDisk(repoFactory), 0, Integer.parseInt(env.val(TlbConstants.Server.TLB_SYNC_TO_DISK_INTERVAL_IN_MINS)) * MILLS_PER_MINUTE);
    }

    private void setupTimerForPurgingOlderVersions(final EntryRepoFactory repoFactory) {
        final int versionLifeInDays = versionLifeInDays();
        if (versionLifeInDays == -1) {
            return;
        }
        timer.schedule(new Purge(repoFactory, versionLifeInDays), 0, ONCE_A_DAY);
    }

    private int versionLifeInDays() {
        return Integer.parseInt(env.val(TlbConstants.Server.TLB_VERSION_LIFE_IN_DAYS));
    }

    @Override
    protected int appPort()  {
        return Integer.parseInt(env.val(TlbConstants.Server.TLB_SERVER_PORT));
    }

    EntryRepoFactory repoFactory() {
        return new EntryRepoFactory(env);
    }

    static class Purge extends TimerTask {
        private final EntryRepoFactory repoFactory;
        private final int versionLifeInDays;

        public Purge(EntryRepoFactory repoFactory, int versionLifeInDays) {
            this.repoFactory = repoFactory;
            this.versionLifeInDays = versionLifeInDays;
        }

        @Override
        public void run() {
            repoFactory.purgeVersionsOlderThan(versionLifeInDays);
        }
    }

    static class SyncToDisk extends TimerTask {
        private final EntryRepoFactory repoFactory;

        public SyncToDisk(EntryRepoFactory repoFactory) {
            this.repoFactory = repoFactory;
        }

        @Override
        public void run() {
            repoFactory.syncReposToDisk();
        }
    }
}
