package tlb.domain;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * tested from suite-time-entry test
 */
public class NameNumberEntryTest {

    @Test
    public void shouldParseItselfFromString() {
        String testTimesString = "foo-bar: 45\nbar-baz: 103\nbaz-quux: 54";
        List<NameNumberEntry> entries = NameNumberEntry.parse(testTimesString, new NameNumberEntry.EntryCreator<NameNumberEntry>() {
            public NameNumberEntry create(String name, long number) {
                return new NameNumberEntry(name, number);
            }
        });
        assertThat(entries.get(0).name, is("foo-bar"));
        assertThat(entries.get(0).number, is(45l));
        assertThat(entries.get(1).name, is("bar-baz"));
        assertThat(entries.get(1).number, is(103l));
        assertThat(entries.get(2).name, is("baz-quux"));
        assertThat(entries.get(2).number, is(54l));
    }
}
