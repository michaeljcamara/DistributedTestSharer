package tlb.server.resources.correctness;

import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import tlb.domain.SuiteNamePartitionEntry;
import tlb.server.repo.PartitionRecordRepo;
import tlb.server.repo.SetRepo;
import tlb.server.repo.model.SubsetCorrectnessChecker;
import tlb.utils.Function;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static tlb.TlbConstants.Correctness.CURRENT_PARTITION_VIOLATES_CORRECTNESS_CHECK_FOR_SUBSET;
import static tlb.TlbConstants.Correctness.NO_UNIVERSAL_SET_FOUND;
import static tlb.TlbConstants.Server.JOB_NUMBER;
import static tlb.TlbConstants.Server.TOTAL_JOBS;

/**
 * @understands checking subsets against universal set data for correctness
 */
public class UpdateSubsetResource extends SetResource {

    public static final SuiteNamePartitionEntry.SuiteNameCountEntryComparator NAME_ENTRY_COMPARATOR = new SuiteNamePartitionEntry.SuiteNameCountEntryComparator();
    private SubsetCorrectnessChecker subsetCorrectnessChecker;

    private static final Logger logger = Logger.getLogger(UpdateSubsetResource.class.getName());

    public UpdateSubsetResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    protected void createRepos() throws IOException, ClassNotFoundException {
        super.createRepos();
        PartitionRecordRepo partitionRecordRepo = repoFactory().createPartitionRecordRepo(reqNamespace(), reqVersion(), reqModuleName());
        subsetCorrectnessChecker = new SubsetCorrectnessChecker(universalSetRepo, partitionRecordRepo);
    }

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Partition %s/%s of %s[v:%s](m:%s) reported its subset.", jobNumber(), totalJobs(), reqNamespace(), reqVersion(), reqModuleName()));
        }
        if (! universalSetRepo.isPrimed()) {
            getResponse().setStatus(new Status(Status.CLIENT_ERROR_NOT_ACCEPTABLE, NO_UNIVERSAL_SET_FOUND));
            getResponse().setEntity(new StringRepresentation("Universal set for given job-name, job-version and module-name combination doesn't exist."));
            return;
        }
        final int partitionNumber = jobNumber();
        final int totalPartitions = totalJobs();
        final String moduleName = reqModuleName();
        SetRepo.OperationResult result = reqPayload(new Function<Reader, IOException, SetRepo.OperationResult>() {
            public SetRepo.OperationResult execute(Reader reader) throws IOException {
                return subsetCorrectnessChecker.reportSubset(partitionNumber, totalPartitions, moduleName, reader);
            }
        }, entity);

        if (result.isSuccess()) {
            getResponse().setStatus(Status.SUCCESS_OK);
        } else {
            getResponse().setStatus(new Status(Status.CLIENT_ERROR_CONFLICT, CURRENT_PARTITION_VIOLATES_CORRECTNESS_CHECK_FOR_SUBSET));
            getResponse().setEntity(new StringRepresentation(result.getMessage()));
        }
    }

    private int totalJobs() {
        return Integer.parseInt(strAttr(TOTAL_JOBS));
    }

    private int jobNumber() {
        return Integer.parseInt(strAttr(JOB_NUMBER));
    }
}
