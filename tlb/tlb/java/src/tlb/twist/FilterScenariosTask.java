package tlb.twist;

import org.apache.tools.ant.*;

import tlb.TlbConstants;
import tlb.factory.TlbBalancerFactory;
import tlb.splitter.AbstractTestSplitter;
import tlb.utils.SystemEnvironment;

public class FilterScenariosTask extends Task {

    public static final String DEFAULT_TWIST_LOCATION = "tlb-balanced-filtered-twist-scenarios";

    private final LoadBalancedTwistSuite suite;
    private String scenariosFolder;
    private String destinationFolder = DEFAULT_TWIST_LOCATION;
    private String moduleName = TlbConstants.Balancer.DEFAULT_MODULE_NAME;

    //Needed for ant
    public FilterScenariosTask() {
        this(new SystemEnvironment());
    }

    private FilterScenariosTask(SystemEnvironment systemEnvironment) {
        this(new LoadBalancedTwistSuite(TlbBalancerFactory.getCriteria(systemEnvironment.val(AbstractTestSplitter.TLB_SPLITTER), systemEnvironment)));
    }
    
    FilterScenariosTask(LoadBalancedTwistSuite suite) {
        this.suite = suite;
    }

    @Override
    public void execute() throws BuildException {
        suite.balance(scenariosFolder, destinationFolder, moduleName);
    }

    @Override
    public String getTaskName() {
        return "filterScenarios";
    }

    public void setScenariosFolder(String scenariosFolder) {
        this.scenariosFolder = scenariosFolder;
    }

    public void setDestinationFolder(String destinationFolder) {
        this.destinationFolder = destinationFolder;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
}
