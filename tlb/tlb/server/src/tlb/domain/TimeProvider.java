package tlb.domain;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @understands system time
 */
public class TimeProvider {
    public GregorianCalendar cal() {
        //NOTE: Do not cache, mutating methods while trying to find out if needs to purge old versions will dirty it if cached. -janmejay
        return new GregorianCalendar();
    }

    public Date now() {
        return new Date();
    }
}
