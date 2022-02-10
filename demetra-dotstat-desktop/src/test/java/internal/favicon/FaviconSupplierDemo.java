package internal.favicon;

import ec.nbdemetra.ui.awt.SimpleHtmlListCellRenderer;
import ec.util.various.swing.BasicSwingLauncher;
import internal.util.http.DefaultHttpClient;
import internal.util.http.HttpAuthScheme;
import internal.util.http.HttpContext;
import internal.util.http.HttpEventListener;
import internal.util.http.HttpRequest;
import internal.util.http.HttpURLConnectionFactoryLoader;
import internal.util.http.MediaType;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import sdmxdl.web.SdmxWebManager;

public class FaviconSupplierDemo {

    public static void main(String[] args) throws MalformedURLException, IOException {
        new BasicSwingLauncher()
                .content(FaviconSupplierDemo::createComponent)
                .size(400, 300)
                .launch();
    }

    private static JComponent createComponent() {
        HttpContext context = HttpContext
                .builder()
                .listener(new ConsoleHttpEventListener())
                .build();

        DefaultHttpClient client = new DefaultHttpClient(context, HttpURLConnectionFactoryLoader.get());

        FaviconSupplier[] suppliers = {
            NoOpSupplier.INSTANCE,
            new GoogleSupplier(client),
            new FaviconkitSupplier(client)
        };

        JComboBox<FaviconSupplier> supplier = new JComboBox<>(suppliers);
        supplier.setRenderer(new SimpleHtmlListCellRenderer<>(FaviconSupplier::getName));

        URL[] urls = SdmxWebManager
                .ofServiceLoader()
                .getSources()
                .values()
                .stream()
                .map(source -> source.getWebsite())
                .distinct()
                .toArray(URL[]::new);

        JComboBox<URL> url = new JComboBox<>(urls);
        url.setSelectedIndex(0);

        JPanel toolbar = new JPanel();
        toolbar.add(supplier);
        toolbar.add(url);

        JLabel favicon = new JLabel();
        favicon.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JPanel result = new JPanel(new FlowLayout());
        result.add(toolbar);
        result.add(favicon);

        ActionListener refreshIcon = listener -> {
            FaviconSupplier x = (FaviconSupplier) supplier.getSelectedItem();
            try {
                Image faviconOrNull = x.getFaviconOrNull((URL) url.getSelectedItem());
                if (faviconOrNull != null) {
                    favicon.setIcon(new ImageIcon(faviconOrNull));
                    favicon.setText(null);
                } else {
                    favicon.setIcon(null);
                    favicon.setText("No icon");
                }
            } catch (IOException ex) {
                favicon.setIcon(null);
                favicon.setText("Failed to get icon: " + ex.getMessage());
            }
        };

        url.addActionListener(refreshIcon);
        supplier.addActionListener(refreshIcon);
        supplier.setSelectedIndex(0);

        return result;
    }

    private static final class ConsoleHttpEventListener implements HttpEventListener {

        @Override
        public void onOpen(HttpRequest hr, Proxy proxy, HttpAuthScheme has) {
            System.out.println("onOpen: " + hr);
        }

        @Override
        public void onSuccess(MediaType mt) {
            System.out.println("onSuccess");
        }

        @Override
        public void onRedirection(URL url, URL url1) {
            System.out.println("onRedirection");
        }

        @Override
        public void onUnauthorized(URL url, HttpAuthScheme has, HttpAuthScheme has1) {
            System.out.println("onUnauthorized");
        }

        @Override
        public void onEvent(String string) {
            System.out.println("onEvent");
        }
    }

    private enum NoOpSupplier implements FaviconSupplier {

        INSTANCE;

        @Override
        public String getName() {
            return "NoOp";
        }

        @Override
        public Image getFaviconOrNull(URL url) throws IOException {
            return null;
        }
    }
}
