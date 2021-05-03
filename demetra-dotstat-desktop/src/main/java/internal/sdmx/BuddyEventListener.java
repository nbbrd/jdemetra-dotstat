package internal.sdmx;

import org.openide.awt.StatusDisplayer;
import sdmxdl.file.SdmxFileListener;
import sdmxdl.file.SdmxFileSource;
import sdmxdl.web.SdmxWebListener;
import sdmxdl.web.SdmxWebSource;

/**
 *
 * @author Philippe Charles
 */
public enum BuddyEventListener implements SdmxWebListener, SdmxFileListener {

    INSTANCE;

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void onWebSourceEvent(SdmxWebSource source, String message) {
        StatusDisplayer.getDefault().setStatusText(message);
    }

    @Override
    public void onFileSourceEvent(SdmxFileSource source, String message) {
        StatusDisplayer.getDefault().setStatusText(message);
    }
}
