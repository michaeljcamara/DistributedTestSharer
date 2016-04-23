package tlb.twist;

import tlb.TestUtil;
import tlb.service.Server;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class PublishScenarioExecutionTimeTest {

    @Test
    public void shouldPublishScenarioExecutionTimeToCruise() throws Exception {
        Server service = mock(Server.class);
        PublishScenarioExecutionTime publishTime = new PublishScenarioExecutionTime(service);
        String reportsDir = "reports";
        publishTime.setReportsDir(reportsDir);

        File reportsFolder = new File(reportsDir + "/xml");
        reportsFolder.mkdirs();
        populateReports(reportsFolder);

        publishTime.execute();

        verify(service).testClassTime("Agent UI Auto Refresh.scn", 85822);
        verify(service).testClassTime("AgentsApi.scn", 77871);
    }

    private void populateReports(File reportsFolder) throws Exception {
        writeToFile(reportsFolder, "TWIST_TEST--scenarios.01.xml");
        writeToFile(reportsFolder, "TWIST_TEST--scenarios.02.xml");
    }

    private void writeToFile(File reportsFolder, String name) throws IOException, URISyntaxException {
        File file = new File(reportsFolder.getAbsolutePath(), name);
        file.createNewFile();
        file.deleteOnExit();
        FileUtils.writeStringToFile(file, TestUtil.fileContents("resources/" + name));
    }
}
