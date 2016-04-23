package tlb;

import org.apache.commons.io.FileUtils;
import tlb.utils.FileUtil;
import tlb.utils.SystemEnvironment;

import java.io.File;

public class TestBalancerApp {
    public static void main(String[] args) {
        try {
            BalancerApp.main(args);
        } finally {
            FileUtils.deleteQuietly(new File(new FileUtil(new SystemEnvironment()).tmpDir()));
        }
    }
}
