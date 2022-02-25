package internal.sdmx;

import org.openide.awt.StatusDisplayer;
import sdmxdl.SdmxSource;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class BuddyEventListener  {

    public static void onSourceEvent(SdmxSource source, String message) {
        StatusDisplayer.getDefault().setStatusText(message);
    }
}
