package tlb.server.repo;

import org.apache.log4j.Logger;
import tlb.TlbConstants;
import tlb.domain.RepoCreatedTimeEntry;
import tlb.domain.TimeProvider;
import tlb.utils.FileUtil;
import tlb.utils.SystemEnvironment;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.Date;
import java.util.GregorianCalendar;

import static tlb.TlbConstants.Server.EntryRepoFactory.*;

/**
 * @understands creation of EntryRepo
 */
public class EntryRepoFactory implements Runnable {
    public static final String DELIMITER = "_";
    public static final String LATEST_VERSION = "LATEST";
    private static final Logger logger = Logger.getLogger(EntryRepoFactory.class.getName());
    public static final String ERF_NAMESPACE = "tlb-erf";

    //private final Map<String, EntryRepo> repos;
    private final String tlbStoreDir;
    private final TimeProvider timeProvider;
    private Cache<EntryRepo> cache;
    private final RepoLedger repoLedger;

    static interface Creator<T> {
        T create();
    }

    public EntryRepoFactory(SystemEnvironment env) {
        this(new File(env.val(TlbConstants.Server.TLB_DATA_DIR)), new TimeProvider(), Integer.parseInt(env.val(TlbConstants.Server.TLB_DATA_CACHE_SIZE)));
    }

    EntryRepoFactory(File tlbStoreDir, TimeProvider timeProvider, int cacheSize) {
        this.tlbStoreDir = tlbStoreDir.getAbsolutePath();
        this.cache = new Cache<EntryRepo>(cacheSize);
        this.timeProvider = timeProvider;
        try {
            this.repoLedger = findOrCreate(ERF_NAMESPACE, new VersionedNamespace(LATEST_VERSION, "REPO_LEDGER"), new Creator<RepoLedger>() {
                public RepoLedger create() {
                    return new RepoLedger();
                }
            }, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void purge(String identifier) throws IOException {
        synchronized (mutex(identifier)) {
            cache.remove(identifier);
            File file = dumpFile(identifier);
            if (file.exists()) FileUtils.forceDelete(file);
            repoLedger.deleteRepoEntryFor(identifier);
        }
    }

    public static String mutex(String identifier) {
        return identifier.intern();
    }

    public void purgeVersionsOlderThan(int versionLifeInDays) {
        GregorianCalendar cal = timeProvider.cal();
        cal.add(GregorianCalendar.DAY_OF_WEEK, -versionLifeInDays);//this should be parametrized
        final Date tooOldThreshold = cal.getTime();
        for (RepoCreatedTimeEntry repoTimeEntry : repoLedger.list()) {
            if (repoTimeEntry.isPurgable() && repoTimeEntry.getCreationTime().before(tooOldThreshold)) {
                String repoIdentifier = repoTimeEntry.getRepoIdentifier();
                try {
                    this.purge(repoIdentifier);
                    logger.warn(String.format("purged repo identified by '%s' at '%s'.", repoIdentifier, timeProvider.now()));
                } catch (Exception e) {
                    logger.warn(String.format("failed to delete older versions for repo identified by '%s'", repoIdentifier), e);
                }
            }
        }
    }

    public static abstract class IdentificationScheme {
        private final String type;

        protected IdentificationScheme(String type) {
            this.type = type;
        }

        public String getIdUnder(String namespace) {
            return escape(namespace) + DELIMITER + getIdWithoutNamespace() + DELIMITER + escape(type);
        }

        public abstract String getIdWithoutNamespace();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IdentificationScheme that = (IdentificationScheme) o;

            if (type != null ? !type.equals(that.type) : that.type != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return type != null ? type.hashCode() : 0;
        }

        public abstract boolean isPurgable();
    }

    public static class VersionedNamespace extends IdentificationScheme {
        private final String version;

        public VersionedNamespace(String version, String type) {
            super(type);
            this.version = version;
        }

        @Override
        public String getIdWithoutNamespace() {
            return escape(version);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VersionedNamespace that = (VersionedNamespace) o;

            if (version != null ? !version.equals(that.version) : that.version != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return version != null ? version.hashCode() : 0;
        }

        @Override
        public boolean isPurgable() {
            return ! LATEST_VERSION.equals(version);
        }
    }

    public static class SubmoduledUnderVersionedNamespace extends VersionedNamespace {
        private final String submoduleName;

        public SubmoduledUnderVersionedNamespace(String version, String type, String submoduleName) {
            super(version, type);
            this.submoduleName = submoduleName;
        }

        @Override
        public String getIdWithoutNamespace() {
            return super.getIdWithoutNamespace() + DELIMITER + escape(submoduleName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            SubmoduledUnderVersionedNamespace that = (SubmoduledUnderVersionedNamespace) o;

            if (submoduleName != null ? !submoduleName.equals(that.submoduleName) : that.submoduleName != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (submoduleName != null ? submoduleName.hashCode() : 0);
            return result;
        }
    }


    public SuiteResultRepo createSuiteResultRepo(final String namespace, final String version) throws ClassNotFoundException, IOException {
        return findOrCreate(namespace, new VersionedNamespace(version, SUITE_RESULT), new Creator<SuiteResultRepo>() {
            public SuiteResultRepo create() {
                return new SuiteResultRepo();
            }
        }, null);
    }

    public SuiteTimeRepo createSuiteTimeRepo(final String namespace, final String version) throws IOException {
        return findOrCreate(namespace, new VersionedNamespace(version, SUITE_TIME), new Creator<SuiteTimeRepo>() {
            public SuiteTimeRepo create() {
                return new SuiteTimeRepo();
            }
        }, new VersionedNamespace(LATEST_VERSION, SUITE_TIME));
    }

    public SubsetSizeRepo createSubsetRepo(final String namespace, final String version) throws IOException {
        return findOrCreate(namespace, new VersionedNamespace(version, SUBSET_SIZE), new Creator<SubsetSizeRepo>() {
            public SubsetSizeRepo create() {
                return new SubsetSizeRepo();
            }
        }, null);
    }

    public SetRepo createUniversalSetRepo(String namespace, String version, final String submoduleName) throws IOException {
        return findOrCreate(namespace, new SubmoduledUnderVersionedNamespace(version, UNIVERSAL_SET, submoduleName), new Creator<SetRepo>() {
            public SetRepo create() {
                return new SetRepo();
            }
        }, null);
    }

    public PartitionRecordRepo createPartitionRecordRepo(String namespace, String version, String submoduleName) throws IOException {
        return findOrCreate(namespace, new SubmoduledUnderVersionedNamespace(version, PARTITION_RECORD, submoduleName), new Creator<PartitionRecordRepo>() {
            public PartitionRecordRepo create() {
                return new PartitionRecordRepo();
            }
        }, null);
    }

    <T extends EntryRepo> T findOrCreate(String namespace, IdentificationScheme idScheme, Creator<T> creator, IdentificationScheme primeFrom) throws IOException {
        String identifier = idScheme.getIdUnder(namespace);
        T repo = (T) cache.get(identifier);
        if (repo == null) {
            synchronized (mutex(identifier)) {
                repo = (T) cache.get(identifier);
                if (repo == null) {
                    repo = creator.create();
                    repo.setNamespace(namespace);
                    repo.setIdentifier(identifier);
                    cache.put(identifier, repo);

                    File diskDump = dumpFile(identifier);
                    if (diskDump.exists()) {
                        FileReader reader = null;
                        try {
                            reader = new FileReader(diskDump);
                            repo.loadCopyFromDisk(new BufferedReader(reader));
                        } finally {
                            if (reader != null) {
                                reader.close();
                            }
                        }
                    } else if (primeFrom != null) {
                        T primingVersion = findOrCreate(namespace, primeFrom, creator, null);
                        repo.copyFrom(primingVersion);
                    }
                    if (! (repo instanceof RepoLedger)) {
                        repoLedger.update(new RepoCreatedTimeEntry(identifier, timeProvider.now().getTime(), idScheme.isPurgable()));
                    }
                }
            }
        }
        if (! repo.hasFactory()) {
            synchronized (mutex(identifier)) {
                if (! repo.hasFactory()) {
                    repo.setFactory(this);
                }
            }
        }
        return repo;
    }

    private File dumpFile(String identifier) {
        new File(tlbStoreDir).mkdirs();
        return new File(tlbStoreDir, identifier);
    }

    private static String escape(String str) {
        return str.replace(DELIMITER, DELIMITER + DELIMITER);
    }

    @Deprecated //for tests only
    Cache<EntryRepo> getRepos() {
        return cache;
    }

    public void run() {
        syncReposToDisk();
    }

    public void syncReposToDisk() {
        for (String identifier : cache.keys()) {
            syncRepoToDisk(identifier, cache.get(identifier));
        }
    }

    public void syncRepoToDisk(final String identifier, final EntryRepo entryRepo) {
        try {
            OutputStreamWriter writer = null;
            FileOutputStream fos = null;
            //don't care about a couple entries not being persisted(at teardown), as client is capable of balancing on averages(treat like new suites)
            synchronized (mutex(identifier)) {
                if (entryRepo != null && entryRepo.isDirty()) {
                    try {
                        File file = dumpFile(identifier);
                        fos = new FileOutputStream(file);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        writer = new OutputStreamWriter(bos);
                        entryRepo.diskDumpTo(writer);
                    } finally {
                        try {
                            if (writer != null) {
                                writer.close();
                            }
                        } catch (IOException e) {
                            logger.warn(String.format("closing of disk dump file of %s failed, tlb server may not be able to perform data dependent operations well on next reboot.", identifier), e);
                            throw e;
                        }

                        try {
                            if (fos != null) {
                                fos.close();
                            }
                        } catch (IOException e) {
                            logger.warn(String.format("closing of disk dump file of %s failed, tlb server may not be able to perform data dependent operations well on next reboot.", identifier), e);
                            throw e;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("disk dump of %s failed, tlb server may not be able to perform data dependent operations well on next reboot.", identifier), e);
        }
    }

    public void registerExitHook() {
        Runtime.getRuntime().addShutdownHook(exitHook());
    }

    public Thread exitHook() {
        return new Thread(this);
    }
}
