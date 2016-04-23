package tlb.server.resources;

import org.restlet.resource.Representation;
import tlb.domain.Entry;
import tlb.server.repo.EntryRepoFactory;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import tlb.server.repo.SuiteTimeRepo;

import java.io.IOException;
import java.util.List;

/**
 * @understands versioned run time of suite reported by job
 */
public class VersionedSuiteTimeResource extends SimpleCRUResource<SuiteTimeRepo> {
    public VersionedSuiteTimeResource(Context context, Request request, Response response) {
        super(context, request, response);
        setModifiable(false);
    }

    @Override
    protected SuiteTimeRepo getRepo(EntryRepoFactory repoFactory, String namespace) throws ClassNotFoundException, IOException {
        return repoFactory.createSuiteTimeRepo(namespace, reqVersion());
    }

    @Override
    protected Entry parseEntry(Representation entity) throws IOException {
        throw new UnsupportedOperationException("parsing does not make sense, as mutation of versioned data is not allowed");
    }

    @Override
    protected List<Entry> parseEntries(Representation entity) throws IOException {
        throw new UnsupportedOperationException("parsing does not make sense, as mutation of versioned data is not allowed");
    }
}