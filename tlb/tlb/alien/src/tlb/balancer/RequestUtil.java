package tlb.balancer;

import com.noelios.restlet.http.HttpConstants;
import org.restlet.data.Form;
import org.restlet.data.Request;
import tlb.TlbConstants;

/**
 * @understands getting information out of request
 */
public class RequestUtil {
    private static String header(final Request request, final String headerName, final String defaultValue) {
        return ((Form) request.getAttributes().get(HttpConstants.ATTRIBUTE_HEADERS)).getFirstValue(headerName, defaultValue);
    }

    public static String moduleName(final Request request) {
        return header(request, TlbConstants.Balancer.TLB_MODULE_NAME_HEADER, TlbConstants.Balancer.DEFAULT_MODULE_NAME);
    }
}
