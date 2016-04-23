package tlb.server.resources;

import tlb.domain.Entry;
import tlb.domain.SubsetSizeEntry;
import tlb.server.repo.EntryRepoFactory;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import tlb.server.repo.SubsetSizeRepo;

import java.io.IOException;
import java.util.List;

/**
 * @understands subset sizes reported by a job
 */
public class SubsetSizeResource extends SimpleCRUResource<SubsetSizeRepo> {

    public SubsetSizeResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    protected SubsetSizeRepo getRepo(EntryRepoFactory repoFactory, String namespace) throws IOException, ClassNotFoundException {
        return repoFactory.createSubsetRepo(namespace, EntryRepoFactory.LATEST_VERSION);
    }

    @Override
    protected Entry parseEntry(Representation entity) throws IOException {
        return SubsetSizeEntry.parseSingleEntry(entity.getText());
    }

    @Override
    protected List<SubsetSizeEntry> parseEntries(Representation entity) throws IOException {
        return SubsetSizeEntry.parse(entity.getText());
    }

    @Override
    public boolean allowPost() {
        return true;
    }
}
