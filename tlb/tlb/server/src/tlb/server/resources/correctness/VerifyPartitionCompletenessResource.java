package tlb.server.resources.correctness;

import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import tlb.server.repo.PartitionRecordRepo;
import tlb.server.repo.SetRepo;
import tlb.server.resources.TlbResource;

import java.io.IOException;

import static tlb.TlbConstants.Correctness.SOME_PARTITIONS_DID_NOT_EXECUTE;

/**
 * @understands verifying all partitions have run for a job-name + job-version + module-name combination
 */
public class VerifyPartitionCompletenessResource extends TlbResource {
    private PartitionRecordRepo partitionRecordRepo;
    private static final Logger logger = Logger.getLogger(VerifyPartitionCompletenessResource.class.getName());

    public VerifyPartitionCompletenessResource(Context context, Request request, Response response) {
        super(context, request, response);
        setModifiable(false);
        setReadable(false);
    }

    @Override
    public boolean allowGet() {
        return true;
    }

    @Override
    protected void createRepos() throws IOException, ClassNotFoundException {
        partitionRecordRepo = repoFactory().createPartitionRecordRepo(reqNamespace(), reqVersion(), reqModuleName());
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Partitions for %s[v:%s](m:%s) verified partition completeness.", reqNamespace(), reqVersion(), reqModuleName()));
        }
        SetRepo.OperationResult operationResult = new SetRepo.OperationResult(true);
        if (partitionRecordRepo.checkAllPartitionsExecuted(reqModuleName(), operationResult)) {
            getResponse().setStatus(Status.SUCCESS_OK);
        } else {
            getResponse().setStatus(new Status(Status.CLIENT_ERROR_EXPECTATION_FAILED, SOME_PARTITIONS_DID_NOT_EXECUTE));
        }
        return new StringRepresentation(operationResult.getMessage());
    }
}
