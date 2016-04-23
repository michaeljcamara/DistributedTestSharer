package tlb.utils;

import tlb.domain.SuiteTimeEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CollectionUtils {
    public static double mean(List<Long> groupedTime) {
        double sum = 0;
        for (Long aLong : groupedTime) {
            sum += aLong;
        }
        return sum / groupedTime.size();
    }

    public static List<SuiteTimeEntry> sortedCopy(List<SuiteTimeEntry> suiteTimeEntries) {
        List<SuiteTimeEntry> copy = new ArrayList<SuiteTimeEntry>(suiteTimeEntries);
        Collections.sort(copy, new Comparator<SuiteTimeEntry>() {
            public int compare(SuiteTimeEntry me, SuiteTimeEntry her) {
                return ((Long) me.getTime()).compareTo(her.getTime());
            }
        });
        return copy;
    }
}
