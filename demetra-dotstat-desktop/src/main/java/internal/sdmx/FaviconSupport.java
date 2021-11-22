package internal.sdmx;

import ec.util.various.swing.OnAnyThread;
import ec.util.various.swing.OnEDT;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.openide.awt.StatusDisplayer;

@lombok.Builder
public class FaviconSupport {

    private final FaviconSupplier supplier;
    private final ExecutorService executor;
    private final Icon fallback;
    // do not put URL as key because of very-slow first lookup
    private final Map<String, Icon> cache;

    @OnEDT
    public Icon get(URL url, Runnable onUpdate) {
        return cache.computeIfAbsent(url.getHost(), host -> request(url, onUpdate));
    }

    @OnEDT
    public Icon getOrNull(URL url) {
        return cache.get(url.getHost());
    }

    @OnEDT
    private Icon request(URL url, Runnable onUpdate) {
        executor.execute(() -> loadIntoCache(url, onUpdate));
        return fallback;
    }

    @OnAnyThread
    private void loadIntoCache(URL url, Runnable onUpdate) {
        Icon favicon = load(url);
        if (favicon != null) {
            SwingUtilities.invokeLater(() -> {
                cache.put(url.getHost(), favicon);
                onUpdate.run();
            });
        }
    }

    @OnAnyThread
    private Icon load(URL url) {
        report("Loading favicon for " + url.getHost());
        try {
            Image result = supplier.getFaviconOrNull(url);
            return result != null ? new ImageIcon(result) : null;
        } catch (IOException ex) {
            report("Cannot retrieve favicon for " + url.getHost());
            return null;
        }
    }

    @OnAnyThread
    private void report(String message) {
        StatusDisplayer.getDefault().setStatusText(message);
    }
}
