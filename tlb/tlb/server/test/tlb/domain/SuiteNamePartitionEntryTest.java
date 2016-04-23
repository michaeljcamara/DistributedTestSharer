package tlb.domain;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SuiteNamePartitionEntryTest {
    public static void assertNotInUse(SuiteNamePartitionEntry entry, final String name) {
        assertThat(entry.getName(), CoreMatchers.is(name));
        assertThat(entry.isUsedByAnyPartition(), CoreMatchers.is(false));
    }

    public static void assertInUse(SuiteNamePartitionEntry entry, final String name, int partitionNumber, int otherPartitionNumber, final int totalPartitions) {
        assertThat(entry.getName(), CoreMatchers.is(name));
        assertThat(entry.isUsedByAnyPartition(), CoreMatchers.is(true));
        assertThat(entry.isUsedByPartitionOtherThan(new PartitionIdentifier(partitionNumber, totalPartitions)), CoreMatchers.is(false));
        assertThat(entry.isUsedByPartitionOtherThan(new PartitionIdentifier(otherPartitionNumber, totalPartitions)), CoreMatchers.is(true));
    }

    //no point repeating count tests, randomly choose any entry, and assume others are the same

    @Test
    public void shouldParseItselfFromString() {
        String testTimesString = "com.thoughtworks.foo.FooBarTest\ncom.thoughtworks.hello.HelloWorldTest:1/3\ncom.thoughtworks.quux.QuuxTest:2/3";
        List<SuiteNamePartitionEntry> partitionEntries = SuiteNamePartitionEntry.parse(testTimesString);
        assertNotInUse(partitionEntries.get(0), "com.thoughtworks.foo.FooBarTest");
        assertInUse(partitionEntries.get(1), "com.thoughtworks.hello.HelloWorldTest", 1, 2, 3);
        assertInUse(partitionEntries.get(2), "com.thoughtworks.quux.QuuxTest", 2, 3, 3);
    }

    @Test
    public void shouldParseItselfFromListOfStrings() {
        List<String> listOfStrings = Arrays.asList("com.thoughtworks.foo.FooBarTest", "com.thoughtworks.hello.HelloWorldTest: 1/5", "com.thoughtworks.quux.QuuxTest");
        List<SuiteNamePartitionEntry> partitionEntries = SuiteNamePartitionEntry.parse(listOfStrings);
        assertNotInUse(partitionEntries.get(0), "com.thoughtworks.foo.FooBarTest");
        assertInUse(partitionEntries.get(1), "com.thoughtworks.hello.HelloWorldTest", 1, 4, 5);
        assertNotInUse(partitionEntries.get(2), "com.thoughtworks.quux.QuuxTest");
    }

    @Test
    public void shouldParseItselfFromListOfStringsInspiteOfEmptyStringsInBetween() {
        List<String> listOfStrings = Arrays.asList("com.thoughtworks.foo.FooBarTest: 2/3", "", "com.thoughtworks.hello.HelloWorldTest: 3/3", "", "com.thoughtworks.quux.QuuxTest");
        List<SuiteNamePartitionEntry> partitionEntries = SuiteNamePartitionEntry.parse(listOfStrings);

        assertThat(partitionEntries.size(), is(3));
        assertInUse(partitionEntries.get(0), "com.thoughtworks.foo.FooBarTest", 2, 1, 3);
        assertInUse(partitionEntries.get(1), "com.thoughtworks.hello.HelloWorldTest", 3, 1, 3);
        assertNotInUse(partitionEntries.get(2), "com.thoughtworks.quux.QuuxTest");
    }

    @Test
    public void shouldDumpStringFromListOfEntries() {
        List<SuiteNamePartitionEntry> list = new ArrayList<SuiteNamePartitionEntry>();
        list.add(new SuiteNamePartitionEntry("com.thoughtworks.foo.FooBarTest"));
        SuiteNamePartitionEntry usedEntry = new SuiteNamePartitionEntry("com.thoughtworks.hello.HelloWorldTest");
        usedEntry.markUsedBy(new PartitionIdentifier(2, 4));
        list.add(usedEntry);
        list.add(new SuiteNamePartitionEntry("com.thoughtworks.quux.QuuxTest"));
        assertThat(SuiteNamePartitionEntry.dump(list), is("com.thoughtworks.foo.FooBarTest\ncom.thoughtworks.hello.HelloWorldTest: 2/4\ncom.thoughtworks.quux.QuuxTest\n"));
    }

    @Test
    public void shouldParseItselfFromSingleString() {
        SuiteNamePartitionEntry partitionEntry = SuiteNamePartitionEntry.parseSingleEntry("com.thoughtworks.foo.FooBarTest");
        assertNotInUse(partitionEntry, "com.thoughtworks.foo.FooBarTest");

        partitionEntry = SuiteNamePartitionEntry.parseSingleEntry("com.thoughtworks.foo.FooBazTest: 2/3");
        assertInUse(partitionEntry, "com.thoughtworks.foo.FooBazTest", 2, 1, 3);

        partitionEntry = SuiteNamePartitionEntry.parseSingleEntry("com.thoughtworks.foo.FooQuuxTest:1/5");
        assertInUse(partitionEntry, "com.thoughtworks.foo.FooQuuxTest", 1, 2, 5);
    }

    @Test
    public void shouldReturnDumpAsToString() {
        SuiteNamePartitionEntry partitionEntry = new SuiteNamePartitionEntry("foo.bar.Baz");
        assertThat(partitionEntry.toString(), is("foo.bar.Baz"));

        partitionEntry = new SuiteNamePartitionEntry("foo.bar.Quux");
        partitionEntry.markUsedBy(new PartitionIdentifier(1, 2));
        assertThat(partitionEntry.toString(), is("foo.bar.Quux: 1/2"));
    }
}
