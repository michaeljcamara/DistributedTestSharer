package tlb.twist;

import tlb.factory.TlbFactory;
import tlb.service.Server;
import tlb.utils.SystemEnvironment;
import tlb.utils.XmlUtil;
import tlb.utils.FileUtil;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.commons.io.FileUtils;
import org.dom4j.Element;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @understands task to publish the time taken to execute scenarios
 */
public class PublishScenarioExecutionTime extends Task {
    private String reportsDir;
    private Server server;
    private static final String XML_REPORT_PATH = "/xml";

    public PublishScenarioExecutionTime(Server server) {
        this.server = server;
    }

    public PublishScenarioExecutionTime() {
        this(new SystemEnvironment());
    }

    public PublishScenarioExecutionTime(SystemEnvironment systemEnvironment) {
        this(TlbFactory.getTalkToService(systemEnvironment));
    }

    public void setReportsDir(String reportsDir) {
        this.reportsDir = reportsDir;
    }

    @Override
    public String getTaskName() {
        return "publishTestTime";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute() throws BuildException {
        Iterator<File> reports = FileUtils.iterateFiles(new File(reportsDir + XML_REPORT_PATH), null, false);
        List<File> reportFiles = FileUtil.toFileList(reports);
        server.publishSubsetSize(reportFiles.size());
        for (File report : reportFiles) {
            try {
                Element element = XmlUtil.domFor(FileUtils.readFileToString(report));
                Element testCase = (Element) element.selectSingleNode("//testcase");
                server.testClassTime(testCase.attribute("name").getText(), toSecond(testCase));
            } catch (IOException e) {
                throw new RuntimeException("Could not read the twist report: " + report.getName(), e);
            }
        }
    }

    private long toSecond(Element testCase) {
        double time = Double.parseDouble(testCase.attribute("time").getText());
        return (long)(time * 1000);
    }
}
