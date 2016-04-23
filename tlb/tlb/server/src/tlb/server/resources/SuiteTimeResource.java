package tlb.server.resources;

import tlb.domain.Entry;
import tlb.domain.SuiteTimeEntry;
import tlb.server.repo.EntryRepoFactory;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import tlb.server.repo.SuiteTimeRepo;

import java.io.IOException;
import java.util.List;

/**
 * @understands run time of suite reported by job
 */
public class SuiteTimeResource extends SimpleCRUResource<SuiteTimeRepo> {
    public SuiteTimeResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    protected SuiteTimeRepo getRepo(EntryRepoFactory repoFactory, String namespace) throws ClassNotFoundException, IOException {
        return repoFactory.createSuiteTimeRepo(namespace, EntryRepoFactory.LATEST_VERSION);
    }

    @Override
    protected Entry parseEntry(Representation entity) throws IOException {
        return SuiteTimeEntry.parseSingleEntry(entity.getText());
    }

    @Override
    protected List<SuiteTimeEntry> parseEntries(Representation entity) throws IOException {
        return SuiteTimeEntry.parse(entity.getText());
    }

    @Override
    public boolean allowPut() {
        return true;
    }
}
