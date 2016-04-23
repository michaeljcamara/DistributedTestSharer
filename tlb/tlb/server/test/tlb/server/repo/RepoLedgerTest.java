package tlb.server.repo;

import org.junit.Test;
import tlb.domain.RepoCreatedTimeEntry;

import java.util.Date;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class RepoLedgerTest {
    @Test
    public void shouldParseMultipleLedgerEntries() {
        List<RepoCreatedTimeEntry> parsedList = new RepoLedger().parse("foo_bar_baz: 10\nbar_baz_quux: 20");
        assertThat(parsedList.get(0), is(new RepoCreatedTimeEntry("foo_bar_baz", 10l)));
        assertThat(parsedList.get(1), is(new RepoCreatedTimeEntry("bar_baz_quux", 20l)));
        assertThat(parsedList.size(), is(2));
    }

    @Test
    public void shouldParseSingleLedgerEntry() {
        RepoCreatedTimeEntry parsedList = new RepoLedger().parseLine("foo_bar_baz: 10");
        assertThat(parsedList, is(new RepoCreatedTimeEntry("foo_bar_baz", 10l)));
    }

    @Test
    public void shouldUnderstandRemovingRepoIdentifier() {
        RepoLedger ledger = new RepoLedger();
        RepoCreatedTimeEntry entry = new RepoCreatedTimeEntry("foo", new Date().getTime());
        ledger.update(entry);
        ledger.update(new RepoCreatedTimeEntry("bar", new Date().getTime()));
        assertThat(ledger.list().size(), is(2));
        assertThat(ledger.list(), hasItem(entry));
        ledger.deleteRepoEntryFor("foo");
        assertThat(ledger.list().size(), is(1));
        assertThat(ledger.list(), not(hasItem(entry)));
    }
}
