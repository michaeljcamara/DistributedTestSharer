package tlb.server.resources.correctness;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import tlb.TlbConstants;
import tlb.domain.Entry;
import tlb.server.repo.EntryRepoFactory;
import tlb.server.repo.SetRepo;
import tlb.server.resources.SimpleCRUResource;
import tlb.server.resources.TlbResource;

import java.io.IOException;

/**
 * @understands common features of a resource dealing with immutable sets of suites
 */
public abstract class SetResource extends TlbResource {
    protected SetRepo universalSetRepo;

    public SetResource(Context context, Request request, Response response) {
        super(context, request, response);
        setModifiable(false);
        setReadable(false);
    }

    @Override
    protected void createRepos() throws IOException, ClassNotFoundException {
        universalSetRepo = repoFactory().createUniversalSetRepo(reqNamespace(), reqVersion(), reqModuleName());
    }

    @Override
    public boolean allowPost() {
        return true;
    }
}
