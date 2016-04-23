package tlb.splitter.correctness;

import org.junit.Before;
import org.junit.Test;
import tlb.TlbSuiteFile;
import tlb.TlbSuiteFileImpl;
import tlb.service.Server;
import tlb.splitter.TestSplitter;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class AbortOnFailureTest {

    private AbortOnFailure checker;

    private TlbSuiteFile foo;
    private TlbSuiteFile bar;
    private TlbSuiteFile baz;
    private Server server;
    private String moduleName = "foo_module";

    @Before
    public void setUp() {
        server = mock(Server.class);

        checker = new AbortOnFailure(mock(TestSplitter.class));
        checker.talksToServer(server);

        foo = new TlbSuiteFileImpl("foo");
        bar = new TlbSuiteFileImpl("bar");
        baz = new TlbSuiteFileImpl("baz");
    }

    @Test
    public void shouldPostUniversalSetToServer() {
        List<TlbSuiteFile> given = Arrays.asList(foo, bar, baz);
        when(server.validateUniversalSet(given, moduleName)).thenReturn(new ValidationResult(ValidationResult.Status.OK));
        moduleName = moduleName;
        checker.universalSet(given, moduleName);
        verify(server).validateUniversalSet(given, moduleName);
        verifyNoMoreInteractions(server);
    }

    @Test
    public void shouldAbortExecution_OnUniversalSetValidationFailure() {
        List<TlbSuiteFile> given = Arrays.asList(foo, bar, baz);
        when(server.validateUniversalSet(given, moduleName)).thenReturn(new ValidationResult(ValidationResult.Status.FAILED, "Expected set ['foo', 'baz'] but given ['foo', 'bar', 'baz']."));
        try {
            checker.universalSet(given, moduleName);
            fail("invalid check-result should have failed the call");
        } catch(IncorrectBalancingException e) {
            assertThat(e.getMessage(), is("Expected set ['foo', 'baz'] but given ['foo', 'bar', 'baz']."));
        }
        verify(server).validateUniversalSet(given, moduleName);
        verifyNoMoreInteractions(server);
    }

    @Test
    public void shouldPostSubsetToServer() {
        List<TlbSuiteFile> given = Arrays.asList(foo, bar, baz);
        when(server.validateSubSet(given, moduleName)).thenReturn(new ValidationResult(ValidationResult.Status.OK));
        checker.subSet(given, moduleName);
        verify(server).validateSubSet(given, moduleName);
        verifyNoMoreInteractions(server);
    }
    
    @Test
    public void shouldAbortExecution_OnSubSetValidationFailure() {
        List<TlbSuiteFile> given = Arrays.asList(foo, bar, baz);
        when(server.validateSubSet(given, moduleName)).thenReturn(new ValidationResult(ValidationResult.Status.FAILED, "Expected set ['foo', 'baz'] but given ['foo', 'bar', 'baz']."));
        try {
            checker.subSet(given, moduleName);
            fail("invalid check-result should have failed the call");
        } catch(IncorrectBalancingException e) {
            assertThat(e.getMessage(), is("Expected set ['foo', 'baz'] but given ['foo', 'bar', 'baz']."));
        }
        verify(server).validateSubSet(given, moduleName);
        verifyNoMoreInteractions(server);
    }
}
