package tlb.domain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PartitionIdentifierTest {
    @Test
    public void shouldParsePartitionIdentifiersString() {
        List<PartitionIdentifier> parsed = PartitionIdentifier.parse("2/4\n1/4\n3/4\n4/4");
        assertThat(parsed.get(0), is(new PartitionIdentifier(2, 4)));
        assertThat(parsed.get(1), is(new PartitionIdentifier(1, 4)));
        assertThat(parsed.get(2), is(new PartitionIdentifier(3, 4)));
        assertThat(parsed.get(3), is(new PartitionIdentifier(4, 4)));
    }

    @Test
    public void shouldParsePartitionIdentifierListOfStrings() {
        List<PartitionIdentifier> parsed = PartitionIdentifier.parse(Arrays.asList("2/4", "1/4", "3/4", "4/4"));
        assertThat(parsed.get(0), is(new PartitionIdentifier(2, 4)));
        assertThat(parsed.get(1), is(new PartitionIdentifier(1, 4)));
        assertThat(parsed.get(2), is(new PartitionIdentifier(3, 4)));
        assertThat(parsed.get(3), is(new PartitionIdentifier(4, 4)));
    }

    @Test
    public void shouldParseSingleEntriesOfPartitionIdentifier() {
        assertThat(PartitionIdentifier.parseSingleEntry("2/4"), is(new PartitionIdentifier(2, 4)));
        assertThat(PartitionIdentifier.parseSingleEntry("1/3"), is(new PartitionIdentifier(1, 3)));
        assertThat(PartitionIdentifier.parseSingleEntry("2  /   3"), is(new PartitionIdentifier(2, 3)));
    }

    @Test
    public void shouldFailIfPartitionIdentifierTurnsOutNotValidForParsing() {
        PartitionIdentifier parsed = null;
        try {
            parsed = PartitionIdentifier.parseSingleEntry("2/A");
            fail("should have failed to parse single entry pattern which has number instead of alphabet");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("failed to parse '2/A' as " + PartitionIdentifier.class.getSimpleName()));
        }
        assertThat(parsed, is(nullValue()));
    }

    @Test
    public void shouldDumpStringFromEntry() {
        assertThat(new PartitionIdentifier(2, 4).dump(), is("2/4\n"));
        assertThat(new PartitionIdentifier(1, 3).dump(), is("1/3\n"));
    }

    @Test
    public void shouldReturnDumpAsToString() {
        PartitionIdentifier entry = new PartitionIdentifier(2, 3);
        assertThat(entry.toString(), is("2/3"));
    }
}
