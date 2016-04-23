package tlb.ant;

import org.apache.log4j.Logger;
import tlb.factory.TlbFactory;
import tlb.service.Server;
import tlb.utils.FileUtil;
import tlb.utils.SystemEnvironment;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import java.io.OutputStream;


/**
 * @understands recording test suite time as cruise artifact
 */
public class JunitDataRecorder implements JUnitResultFormatter {
    private static final Logger logger = Logger.getLogger(JunitDataRecorder.class.getName());
    private Server server;
    private final FileUtil fileUtil;

    public JunitDataRecorder(Server server, SystemEnvironment environment) {
        this(server, new FileUtil(environment));
    }

    public JunitDataRecorder() {//default constructor
        this(new SystemEnvironment());
    }

    private JunitDataRecorder(SystemEnvironment systemEnvironment) {
        this(TlbFactory.getTalkToService(systemEnvironment), systemEnvironment);
    }

    JunitDataRecorder(Server server, FileUtil fileUtil) {
        this.server = server;
        this.fileUtil = fileUtil;
    }

    public void startTestSuite(JUnitTest jUnitTest) throws BuildException {}

    public void endTestSuite(JUnitTest jUnitTest) throws BuildException {
        String suiteFileName = fileUtil.classFileRelativePath(jUnitTest.getName());
        try {
            server.testClassFailure(suiteFileName, (jUnitTest.failureCount() + jUnitTest.errorCount()) > 0);
            server.testClassTime(suiteFileName, jUnitTest.getRunTime());
        } catch (Exception e) {
            logger.warn(String.format("recording suite time failed for %s, gobbling exception, things may not work too well for the next run", suiteFileName), e);
        }
    }

    public void setOutput(OutputStream outputStream) {}

    public void setSystemOutput(String s) {}

    public void setSystemError(String s) {}

    public void addError(Test test, Throwable throwable) {}

    public void addFailure(Test test, AssertionFailedError assertionFailedError) {}

    public void endTest(Test test) {}

    public void startTest(Test test) {}
}
