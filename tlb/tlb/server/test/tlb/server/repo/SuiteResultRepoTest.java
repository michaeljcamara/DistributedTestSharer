package tlb.server.repo;

import org.junit.Before;
import org.junit.Test;
import tlb.domain.SuiteResultEntry;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SuiteResultRepoTest {
    private SuiteResultRepo repo;

    @Before
    public void setUp() throws Exception {
        repo = new SuiteResultRepo();
    }

    @Test
    public void shouldNotAllowVersioning() {
        try {
            repo.list("foo");
            fail("should not have allowed versioning");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("versioning not allowed"));
        }
    }

    @Test
    public void shouldUnderstandParsingEntries() {
        List<SuiteResultEntry> parsedEntries = new SuiteResultRepo().parse("foo.Bar: true\nbar.Baz: false");
        assertThat(parsedEntries.get(0), is(new SuiteResultEntry("foo.Bar", true)));
        assertThat(parsedEntries.get(1), is(new SuiteResultEntry("bar.Baz", false)));
    }

    @Test
    public void shouldUnderstandParsingSingleEntry() {
        SuiteResultEntry parsedEntries = new SuiteResultRepo().parseLine("foo.Bar: true");
        assertThat(parsedEntries, is(new SuiteResultEntry("foo.Bar", true)));
    }

}
