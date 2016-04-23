package tlb.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* @understands uniquely identifies a partition among all partitions of a build
*/
public final class PartitionIdentifier implements NamedEntry {
    public final int partitionNumber;
    public final int totalPartitions;

    public static final Pattern PARSE_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    public PartitionIdentifier(int partitionNumber, int totalPartitions) {
        this.partitionNumber = partitionNumber;
        this.totalPartitions = totalPartitions;
    }

    @Override
    public String toString() {
        return String.format("%s/%s", partitionNumber, totalPartitions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PartitionIdentifier that = (PartitionIdentifier) o;

        if (partitionNumber != that.partitionNumber) return false;
        if (totalPartitions != that.totalPartitions) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = partitionNumber;
        result = 31 * result + totalPartitions;
        return result;
    }

    public String getName() {
        return toString();
    }

    public String dump() {
        return toString() + "\n";
    }

    public static List<PartitionIdentifier> parse(String partitionIdsString) {
        return parse(Arrays.asList(partitionIdsString.split("\n")));
    }

    public static List<PartitionIdentifier> parse(List<String> partitionIdStrings) {
        List<PartitionIdentifier> parsed = new ArrayList<PartitionIdentifier>();
        for (String entryString : partitionIdStrings) {
            if (entryString.trim().length() > 0) parsed.add(parseSingleEntry(entryString));
        }
        return parsed;
    }

    public static PartitionIdentifier parseSingleEntry(String partitionIdStr) {
        Matcher matcher = PARSE_PATTERN.matcher(partitionIdStr);
        if (matcher.matches()) {
            return new PartitionIdentifier(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        } else {
            throw new IllegalArgumentException(String.format("failed to parse '%s' as %s", partitionIdStr, PartitionIdentifier.class.getSimpleName()));
        }
    }
}
