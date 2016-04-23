package tlb.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @understands storing a name to number mapping
 */
public class NameNumberEntry  implements NamedEntry {
    public static interface EntryCreator<T> {
        T create(String name, long number);
    }

    public static final Pattern NAME_NUMBER_PATTERN = Pattern.compile("(.*?):\\s*(-?\\d+)");
    protected String name;
    protected long number;

    public NameNumberEntry(String name, long number) {
        this.name = name;
        this.number = number;
    }

    public static String dump(List<? extends NameNumberEntry> entries) {
        StringBuilder buffer = new StringBuilder();
        for (Entry entry : entries) {
            buffer.append(entry.dump());
        }
        return buffer.toString();
    }

    protected static <T extends NameNumberEntry> T parseSingleEntry(String entryString, EntryCreator<T> creator) {
        Matcher matcher = NAME_NUMBER_PATTERN.matcher(entryString);
        T entry = null;
        if (matcher.matches()) {
            entry = creator.create(matcher.group(1), Long.parseLong(matcher.group(2)));
        } else {
            throw new IllegalArgumentException(String.format("failed to parse '%s' as %s", entryString, NameNumberEntry.class.getSimpleName()));
        }
        return entry;
    }

    protected static <T extends NameNumberEntry> List<T> parse(List<String> listOfStrings, final EntryCreator<T> creator) {
        List<T> parsed = new ArrayList<T>();
        for (String entryString : listOfStrings) {
            if (entryString.trim().length() > 0) parsed.add(parseSingleEntry(entryString, creator));
        }
        return parsed;
    }

    protected static <T extends NameNumberEntry> List<T> parse(String buffer, EntryCreator<T> creator) {
        return parse(Arrays.asList(buffer.split("\n")), creator);
    }

    public String getName() {
        return name;
    }

    public String dump() {
        return toString() + "\n";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NameNumberEntry that = (NameNumberEntry) o;

        if (number != that.number) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (int) (number ^ (number >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", name, number);
    }
}
