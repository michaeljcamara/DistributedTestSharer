package tlb.ant;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import tlb.TlbConstants;
import tlb.factory.TlbFactory;
import tlb.service.Server;
import tlb.splitter.correctness.ValidationResult;
import tlb.utils.SystemEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * @understands failing the build if one or more partitions didn't report for correctness checking, when correctness check is enabled.
 */
public class CheckMissingPartitions extends Task {
    private List<String> moduleNames;
    private final Server talkToService;

    public CheckMissingPartitions() {
        this(new SystemEnvironment());
    }

    public CheckMissingPartitions(SystemEnvironment env) {
        this(env, TlbFactory.getTalkToService(env));
    }

    public CheckMissingPartitions(SystemEnvironment env, Server talkToService) {
        this.talkToService = talkToService;
        moduleNames = new ArrayList<String>();
    }

    @Override
    public void execute() throws BuildException {
        StringBuilder validationErrors = new StringBuilder();
        if (moduleNames.isEmpty()) {
            moduleNames.add(TlbConstants.Balancer.DEFAULT_MODULE_NAME);
        }
        for (String moduleName : moduleNames) {
            ValidationResult validationResult = talkToService.verifyAllPartitionsExecutedFor(moduleName);
            if (validationResult.hasFailed()) {
                validationErrors.append(validationResult.getMessage()).append("\n\n");
            }
        }
        if (validationErrors.length() > 0) {
            throw new BuildException(validationErrors.toString());
        }
    }

    /**
     * accepts module name(s) for which all-partitions-have-run assertion is to be made.
     * @param moduleNamesString: comma separated list of module names which are expected to have been run by all partitions.
     */
    public void setModuleNames(String moduleNamesString) {
        String[] split = moduleNamesString.split(",");
        for (String s : split) {
            this.moduleNames.add(s.trim());
        }
    }

}
