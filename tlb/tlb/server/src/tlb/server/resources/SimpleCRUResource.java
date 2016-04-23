package tlb.server.resources;

import org.apache.log4j.Logger;
import tlb.domain.Entry;
import tlb.server.repo.EntryRepo;
import tlb.server.repo.EntryRepoFactory;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @understands listing and modification of tlb resource
 */
public abstract class SimpleCRUResource<T extends EntryRepo> extends TlbResource {
    private static final Logger logger = Logger.getLogger(SimpleCRUResource.class.getName());
    protected T repo;

    public SimpleCRUResource(Context context, Request request, Response response) {
        super(context, request, response);

    }

    protected void createRepos() throws IOException, ClassNotFoundException {
        repo = getRepo(repoFactory(), reqNamespace());
    }

    protected Collection<Entry> getListing() throws IOException, ClassNotFoundException {
        return repo.list();
    }

    protected abstract T getRepo(EntryRepoFactory repoFactory, String namespace) throws IOException, ClassNotFoundException;

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        StringBuilder builder = new StringBuilder();
        final Collection<Entry> listing;
        try {
            listing = getListing();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (Entry entry : listing) {
            builder.append(entry.dump());
        }
        return new StringRepresentation(builder.toString(), MediaType.TEXT_PLAIN);
    }

    @Override
    public void storeRepresentation(Representation entity) throws ResourceException {
        try {
            repo.updateAll(parseEntries(entity));
        } catch (Exception e) {
            logger.warn(String.format("update of representation failed for %s", entity), e);
            throw new RuntimeException(e);
        }
    }

    protected abstract List<? extends Entry> parseEntries(Representation entity) throws IOException;

    protected abstract Entry parseEntry(Representation entity) throws IOException;

    @Override
    public void acceptRepresentation(Representation entity) throws ResourceException {
        try {
            repo.add(parseEntry(entity));
        } catch (Exception e) {
            logger.warn(String.format("addition of representation failed for %s", entity), e);
            throw new RuntimeException(e);
        }
    }
}
