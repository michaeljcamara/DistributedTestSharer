package tlb.splitter.timebased;

import org.junit.Test;
import tlb.TlbSuiteFileImpl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TestFileTest {

    @Test
    public void testShouldCompareBasedOnTime() {
        assertThat(new TestFile(new TlbSuiteFileImpl("foo"), 2.33).compareTo(new TestFile(new TlbSuiteFileImpl("foo"), 2.34)), is(1));
        assertThat(new TestFile(new TlbSuiteFileImpl("foo"), 2.34).compareTo(new TestFile(new TlbSuiteFileImpl("foo"), 2.33)), is(-1));
    }

    @Test
    public void testShouldUseFileNameWhenTimeIsSameToCompare() {
        assertThat(new TestFile(new TlbSuiteFileImpl("foo"), 2.33).compareTo(new TestFile(new TlbSuiteFileImpl("fop"), 2.33)), is(1));
        assertThat(new TestFile(new TlbSuiteFileImpl("fop"), 2.33).compareTo(new TestFile(new TlbSuiteFileImpl("foo"), 2.33)), is(-1));
    }

    @Test
    public void testReturn0WhenTheNameAndTimeAreSame() {
        assertThat(new TestFile(new TlbSuiteFileImpl("fop"), 2.33).compareTo(new TestFile(new TlbSuiteFileImpl("fop"), 2.33)), is(0));
    }
}
