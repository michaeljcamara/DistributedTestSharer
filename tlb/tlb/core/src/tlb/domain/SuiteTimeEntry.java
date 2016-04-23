package tlb.domain;

import java.util.List;

/**
 * @understands time taken to execute a test suite
 */
public class SuiteTimeEntry extends NameNumberEntry {
    public static final EntryCreator<SuiteTimeEntry> SUITE_TIME_ENTRY_CREATOR = new EntryCreator<SuiteTimeEntry>() {
        public SuiteTimeEntry create(String name, long number) {
            return new SuiteTimeEntry(name, number);
        }
    };

    public SuiteTimeEntry(String name, long time) {
        super(name, time);
    }

    public long getTime() {
        return number;
    }

    public static List<SuiteTimeEntry> parse(String buffer) {
        return parse(buffer, SUITE_TIME_ENTRY_CREATOR);
    }

    public static List<SuiteTimeEntry> parse(List<String> listOfStrings) {
        return parse(listOfStrings, SUITE_TIME_ENTRY_CREATOR);
    }

    public static SuiteTimeEntry parseSingleEntry(String entryString) {
        return parseSingleEntry(entryString, SUITE_TIME_ENTRY_CREATOR);
    }


    public SuiteTimeEntry smoothedWrt(SuiteTimeEntry newDataPoint, double alpha) {
        if ( ! name.equals(newDataPoint.name)) throw new IllegalArgumentException(String.format("suite %s can not be smoothed with data point from %s", name, newDataPoint.name));
        final double smoothedTime = alpha * newDataPoint.number + (1 - alpha) * number;
        return new SuiteTimeEntry(name, Math.round(smoothedTime));
    }
}
