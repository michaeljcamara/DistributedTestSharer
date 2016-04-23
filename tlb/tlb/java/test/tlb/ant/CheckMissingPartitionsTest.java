package tlb.ant;

import org.apache.tools.ant.BuildException;
import org.junit.Before;
import org.junit.Test;
import tlb.TlbConstants;
import tlb.service.Server;
import tlb.splitter.correctness.ValidationResult;
import tlb.utils.SystemEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class CheckMissingPartitionsTest {

    private CheckMissingPartitions task;
    private Server server;

    @Before
    public void setUp() throws Exception {
        Map<String, String> envMap = new HashMap<String, String>();
        SystemEnvironment env = new SystemEnvironment(envMap);
        server = mock(Server.class);
        task = new CheckMissingPartitions(env, server);
    }

    @Test
    public void shouldQueryTlbServerForModulesPartitionCompleteness() {
        when(server.verifyAllPartitionsExecutedFor("foo")).thenReturn(new ValidationResult(ValidationResult.Status.OK, ""));
        when(server.verifyAllPartitionsExecutedFor("bar")).thenReturn(new ValidationResult(ValidationResult.Status.OK, ""));
        task.setModuleNames("foo, bar");
        task.execute();
        verify(server).verifyAllPartitionsExecutedFor("foo");
        verify(server).verifyAllPartitionsExecutedFor("bar");
        verifyNoMoreInteractions(server);
    }
    
    @Test
    public void shouldFailTheTaskIfAllPartitionsHaveNotExecutedForAModule() {
        when(server.verifyAllPartitionsExecutedFor(TlbConstants.Balancer.DEFAULT_MODULE_NAME)).thenReturn(new ValidationResult(ValidationResult.Status.OK, ""));
        task.execute();
        verify(server).verifyAllPartitionsExecutedFor(TlbConstants.Balancer.DEFAULT_MODULE_NAME);
        verifyNoMoreInteractions(server);
    }

    @Test
    public void shouldUseDefaultModuleNameIfNoneGiven() {

    }

}
