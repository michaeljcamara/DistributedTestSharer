package tlb.server.resources;

import tlb.domain.Entry;
import tlb.domain.SuiteResultEntry;
import tlb.server.repo.EntryRepoFactory;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import tlb.server.repo.SuiteResultRepo;

import java.io.IOException;
import java.util.List;

/**
 * @understands result of suite reported by job
 */
public class SuiteResultResource extends SimpleCRUResource<SuiteResultRepo> {

    public SuiteResultResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    protected SuiteResultRepo getRepo(EntryRepoFactory repoFactory, String namespace) throws IOException, ClassNotFoundException {
        return repoFactory.createSuiteResultRepo(namespace, EntryRepoFactory.LATEST_VERSION);
    }

    @Override
    protected Entry parseEntry(Representation entity) throws IOException {
        return SuiteResultEntry.parseSingleEntry(entity.getText());
    }

    @Override
    protected List<SuiteResultEntry> parseEntries(Representation entity) throws IOException {
        return SuiteResultEntry.parse(entity.getText());
    }

    @Override
    public boolean allowPut() {
        return true;
    }
}
