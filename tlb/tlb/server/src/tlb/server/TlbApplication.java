package tlb.server;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.Router;
import tlb.server.resources.SubsetSizeResource;
import tlb.server.resources.SuiteResultResource;
import tlb.server.resources.SuiteTimeResource;
import tlb.server.resources.VersionedSuiteTimeResource;
import tlb.server.resources.correctness.HomeResource;
import tlb.server.resources.correctness.UpdateSubsetResource;
import tlb.server.resources.correctness.UpdateUniversalSetResource;
import tlb.server.resources.correctness.VerifyPartitionCompletenessResource;

import static tlb.TlbConstants.Server.EntryRepoFactory.*;
import static tlb.TlbConstants.Server.*;

/**
 * @understands restlet tlb application for tlb server
 */
public class TlbApplication extends Application {

    public TlbApplication(Context context) {
        super(context);
    }

    @Override
    public Restlet createRoot() {
        Router router = new Router(getContext());

        router.attachDefault(HomeResource.class);

        router.attach(String.format("/{%s}/%s", REQUEST_NAMESPACE, SUBSET_SIZE), SubsetSizeResource.class);

        router.attach(String.format("/{%s}/%s", REQUEST_NAMESPACE, SUITE_RESULT), SuiteResultResource.class);

        router.attach(String.format("/{%s}/%s", REQUEST_NAMESPACE, SUITE_TIME), SuiteTimeResource.class);
        router.attach(String.format("/{%s}/%s/{%s}", REQUEST_NAMESPACE, SUITE_TIME, LISTING_VERSION), VersionedSuiteTimeResource.class);

        router.attach(String.format("/{%s}/%s/{%s}/%s/{%s}", REQUEST_NAMESPACE, CORRECTNESS_CHECK, LISTING_VERSION, UNIVERSAL_SET, MODULE_NAME), UpdateUniversalSetResource.class);
        router.attach(String.format("/{%s}/%s/{%s}/{%s}/{%s}/%s/{%s}", REQUEST_NAMESPACE, CORRECTNESS_CHECK, LISTING_VERSION, TOTAL_JOBS, JOB_NUMBER, SUB_SET, MODULE_NAME), UpdateSubsetResource.class);

        router.attach(String.format("/{%s}/%s/{%s}/%s/{%s}", REQUEST_NAMESPACE, CORRECTNESS_CHECK, LISTING_VERSION, VERIFY_PARTITION_COMPLETENESS, MODULE_NAME), VerifyPartitionCompletenessResource.class);

        return router;
    }
}
