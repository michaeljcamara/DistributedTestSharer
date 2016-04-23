package tlb.orderer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.Times;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.TestUtil;
import tlb.domain.SuiteResultEntry;
import tlb.service.GoServer;
import tlb.service.TalksToServer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static tlb.TestUtil.initEnvironment;
import static tlb.TestUtil.convertToPlatformSpecificPath;

public class FailedFirstOrdererTest {
    private FailedFirstOrderer orderer;
    private GoServer toCruise;

    @Before
    public void setUp() throws Exception {
        orderer = new FailedFirstOrderer(initEnvironment("job-1"));
        toCruise = mock(GoServer.class);
        orderer.talksToServer(toCruise);
    }

    @Test
    public void shouldImplementTalksToCruise() throws Exception{
        assertTrue("Failed first orderer must be talk to cruise aware", TalksToServer.class.isAssignableFrom(FailedFirstOrderer.class));
    }

    @Test
    public void shouldNotReorderTestsWhenNoneFailed() throws Exception{
        TlbSuiteFile bazClass = new TlbSuiteFileImpl("foo/bar/Baz.class");
        TlbSuiteFile quuxClass = new TlbSuiteFileImpl("foo/baz/Quux.class");
        TlbSuiteFile bangClass = new TlbSuiteFileImpl("foo/baz/Bang.class");
        List<SuiteResultEntry> failedTests = Arrays.asList(new SuiteResultEntry("baz/bang/Foo.class", true), new SuiteResultEntry("foo/bar/Bang.class", true));
        when(toCruise.getLastRunFailedTests()).thenReturn(failedTests);
        List<TlbSuiteFile> fileList = new ArrayList<TlbSuiteFile>(Arrays.asList(bazClass, quuxClass, bangClass));

        Collections.sort(fileList, orderer);
        final List<TlbSuiteFile> resources = new ArrayList<TlbSuiteFile>(Arrays.asList(bazClass, quuxClass, bangClass));
        assertThat(fileList, is(resources));
        verify(toCruise, new Times(1)).getLastRunFailedTests();
    }

    @Test
    public void shouldReorderTestsToBringFailedTestsFirst() throws Exception{
        TlbSuiteFile bazClass = new TlbSuiteFileImpl(normalize("foo/bar/Baz.class"));
        TlbSuiteFile quuxClass = new TlbSuiteFileImpl(normalize("foo/baz/Quux.class"));
        TlbSuiteFile failedFooClass = new TlbSuiteFileImpl(normalize("baz/bang/Foo.class"));
        TlbSuiteFile failedBangClass = new TlbSuiteFileImpl(normalize("foo/bar/Bang.class"));
        List<SuiteResultEntry> failedTests = Arrays.asList(new SuiteResultEntry(convertToPlatformSpecificPath("baz/bang/Foo.class"), true), new SuiteResultEntry(convertToPlatformSpecificPath("foo/bar/Bang.class"), true));
        when(toCruise.getLastRunFailedTests()).thenReturn(failedTests);
        List<TlbSuiteFile> fileList = new ArrayList<TlbSuiteFile>(Arrays.asList(bazClass, failedFooClass, quuxClass, failedBangClass));
        Collections.sort(fileList, orderer);

        assertThat(fileList.get(0), anyOf(is(failedBangClass), is(failedFooClass)));
        assertThat(fileList.get(1), anyOf(is(failedBangClass), is(failedFooClass)));

        assertThat(fileList.get(2), anyOf(is(bazClass), is(quuxClass)));
        assertThat(fileList.get(3), anyOf(is(bazClass), is(quuxClass)));
        verify(toCruise, new Times(1)).getLastRunFailedTests();
    }

    private String normalize(String actual) {
        return new File(actual).getPath();
    }

    @Test
    public void shouldNotReorderPassedSuitesInspiteOfHavingResults() throws Exception {
        TlbSuiteFile bazClass = new TlbSuiteFileImpl(convertToPlatformSpecificPath("foo/bar/Baz.class"));
        TlbSuiteFile quuxClass = new TlbSuiteFileImpl(convertToPlatformSpecificPath("foo/baz/Quux.class"));
        TlbSuiteFile reportedButPassedFooClass = new TlbSuiteFileImpl(convertToPlatformSpecificPath("baz/bang/Foo.class"));
        TlbSuiteFile reportedButPassedBangClass = new TlbSuiteFileImpl(convertToPlatformSpecificPath("foo/bar/Bang.class"));
        List<SuiteResultEntry> failedTests = Arrays.asList(new SuiteResultEntry("baz.bang.Foo", false), new SuiteResultEntry("foo.bar.Bang", false));
        when(toCruise.getLastRunFailedTests()).thenReturn(failedTests);

        List<TlbSuiteFile> fileList = new ArrayList<TlbSuiteFile>(Arrays.asList(bazClass, reportedButPassedFooClass, quuxClass, reportedButPassedBangClass));
        Collections.sort(fileList, orderer);

        final List<TlbSuiteFile> expected = new ArrayList<TlbSuiteFile>(Arrays.asList(bazClass, reportedButPassedFooClass, quuxClass, reportedButPassedBangClass));
        assertThat(fileList, is(expected));
        verify(toCruise, new Times(1)).getLastRunFailedTests();
    }

}
