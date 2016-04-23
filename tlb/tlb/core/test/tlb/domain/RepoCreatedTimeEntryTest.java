package tlb.domain;

import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RepoCreatedTimeEntryTest {
    @Test
    public void shouldParseList() {
        List<RepoCreatedTimeEntry> parsed = RepoCreatedTimeEntry.parse("foo: 100\nbar: 20\nbaz:10\n");
        assertThat(parsed.size(), is(3));
        assertThat(parsed.get(0), is(new RepoCreatedTimeEntry("foo", 100l)));
        assertThat(parsed.get(1), is(new RepoCreatedTimeEntry("bar", 20l)));
        assertThat(parsed.get(2), is(new RepoCreatedTimeEntry("baz", 10l)));
    }

    @Test
    public void shouldUnderstandPurgability() {
        RepoCreatedTimeEntry foo = new RepoCreatedTimeEntry("foo", new Date().getTime());
        assertThat(foo.isPurgable(), is(true));
        RepoCreatedTimeEntry bar = new RepoCreatedTimeEntry("bar", -1l);
        assertThat(bar.isPurgable(), is(false));
        RepoCreatedTimeEntry baz = new RepoCreatedTimeEntry("baz", new Date().getTime(), false);
        assertThat(baz.isPurgable(), is(false));
        RepoCreatedTimeEntry quux = new RepoCreatedTimeEntry("quux", new Date().getTime(), true);
        assertThat(quux.isPurgable(), is(true));
    }

    @Test
    public void shouldDumpAndParseNonPurgableEntries() {
        RepoCreatedTimeEntry bar = new RepoCreatedTimeEntry("bar", -1l);
        assertThat(bar.dump(), is("bar: -1\n"));
        assertThat(RepoCreatedTimeEntry.parseSingleEntry("bar: -1", RepoCreatedTimeEntry.REPO_INSTANCE_CREATOR), is(new RepoCreatedTimeEntry("bar", -1)));
    }

}
