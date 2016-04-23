package tlb.server;

import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;

/**
 * @understands initializing a restlet application
 */
public abstract class ServerInitializer {
    protected Component component;

    public final Component init() {
        if (component == null) {
            component = new Component();
            component.getServers().add(Protocol.HTTP, appPort());
            component.getDefaultHost().attach(application());
        }
        return component;
    }

    protected abstract Restlet application();

    protected abstract int appPort();
}
