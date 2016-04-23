package tlb.domain;

import java.util.Date;
import java.util.List;

/**
 * @understands association a repo identifier to time it was created
 */
public class RepoCreatedTimeEntry extends NameNumberEntry {
    public static final EntryCreator<RepoCreatedTimeEntry> REPO_INSTANCE_CREATOR = new EntryCreator<RepoCreatedTimeEntry>() {
        public RepoCreatedTimeEntry create(String name, long number) {
            return new RepoCreatedTimeEntry(name, number);
        }
    };

    public RepoCreatedTimeEntry(String repoIdentifier, long createAtTimestamp) {
        this(repoIdentifier, createAtTimestamp, true);
    }

    public RepoCreatedTimeEntry(String repoIdentifier, long createdAtTimestamp, boolean purgable) {
        super(repoIdentifier, purgable ? createdAtTimestamp : -1);
    }

    public static List<RepoCreatedTimeEntry> parse(String entries) {
        return parse(entries, REPO_INSTANCE_CREATOR);
    }

    public static RepoCreatedTimeEntry parseSingleEntry(String line) {
        return parseSingleEntry(line, REPO_INSTANCE_CREATOR);
    }

    public boolean isPurgable() {
        return number != -1;
    }

    public String getRepoIdentifier() {
        return name;
    }

    public Date getCreationTime() {
        return new Date(number);
    }
}
