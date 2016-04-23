package tlb;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.Route;
import org.restlet.Router;
import org.restlet.util.RouteList;

import java.util.HashMap;

public class RestletTestUtil {
    public static HashMap<String, Restlet> getRoutePatternsAndResources(final Application app) {
        Router router = (Router) app.createRoot();
        RouteList routeList = router.getRoutes();
        HashMap<String, Restlet> map = new HashMap<String, Restlet>();
        for (Route route : routeList) {
            map.put(route.getTemplate().getPattern(), route.getNext());
        }
        return map;
    }
}
