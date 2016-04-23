package tlb.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @understands a single element of a Set of test-suites
 */
public class SuiteNamePartitionEntry implements NamedEntry {
    public static final Pattern SUITE_SET_ENTRY_STRING = Pattern.compile("(.*?)(:\\s*(\\d+)/(\\d+))?");

    private final String name;
    private PartitionIdentifier partitionIdentifier;

    public SuiteNamePartitionEntry(String name) {
        this(name, null);
    }

    public SuiteNamePartitionEntry(String name, PartitionIdentifier partitionIdentifier) {
        this.name = name;
        this.partitionIdentifier = partitionIdentifier;
    }

    public String getName() {
        return name;
    }

    public String dump() {
        return toString() + "\n";
    }

    public static List<SuiteNamePartitionEntry> parse(String suiteNamesString) {
        return parse(Arrays.asList(suiteNamesString.split("\n")));
    }

    public static List<SuiteNamePartitionEntry> parse(List<String> listOfStrings) {
        List<SuiteNamePartitionEntry> parsed = new ArrayList<SuiteNamePartitionEntry>();
        for (String entryString : listOfStrings) {
            if (entryString.trim().length() > 0) parsed.add(parseSingleEntry(entryString));
        }
        return parsed;
    }

    public static String dump(List<SuiteNamePartitionEntry> partitionEntries) {
        StringBuilder buffer = new StringBuilder();
        for (Entry entry : partitionEntries) {
            buffer.append(entry.dump());
        }
        return buffer.toString();
    }



    public static SuiteNamePartitionEntry parseSingleEntry(String singleEntryString) {
        Matcher matcher = SUITE_SET_ENTRY_STRING.matcher(singleEntryString);
        if (matcher.matches()) {
            PartitionIdentifier partitionIdentifier = null;
            if (matcher.group(2) != null) {
                partitionIdentifier = new PartitionIdentifier(Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4)));
            }
            return new SuiteNamePartitionEntry(matcher.group(1), partitionIdentifier);
        } else {
            throw new IllegalArgumentException(String.format("failed to parse '%s' as %s", singleEntryString, SuiteNamePartitionEntry.class.getSimpleName()));
        }
    }

    @Override
    public String toString() {
        return isUsedByAnyPartition() ? String.format("%s: %s", name, partitionIdentifier) : name;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SuiteNamePartitionEntry that = (SuiteNamePartitionEntry) o;

        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public boolean isUsedByPartitionOtherThan(PartitionIdentifier partitionIdentifier) {
        return isUsedByAnyPartition() && !this.partitionIdentifier.equals(partitionIdentifier);
    }

    public boolean isUsedByAnyPartition() {
        return this.partitionIdentifier != null;
    }

    public void markUsedBy(final PartitionIdentifier partitionIdentifier) {
        this.partitionIdentifier = partitionIdentifier;
    }

    public PartitionIdentifier getPartitionIdentifier() {
        return partitionIdentifier;
    }

    public static class SuiteNameCountEntryComparator implements Comparator<SuiteNamePartitionEntry> {
        public int compare(SuiteNamePartitionEntry o1, SuiteNamePartitionEntry o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

}
