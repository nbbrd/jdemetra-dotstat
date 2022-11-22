package internal.favicon;

import com.google.common.net.InternetDomainName;
import ec.tstoolkit.design.VisibleForTesting;
import internal.util.http.*;
import shaded.dotstat.nbbrd.io.net.MediaType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public final class FaviconkitSupplier implements FaviconSupplier {

    private final HttpClient client;

    public FaviconkitSupplier() {
        this(new DefaultHttpClient(HttpContext.builder().build()));
    }

    @VisibleForTesting
    FaviconkitSupplier(HttpClient client) {
        this.client = client;
    }

    @Override
    public String getName() {
        return "Faviconkit";
    }

    @Override
    public Image getFaviconOrNull(URL url) throws IOException {
        try (HttpResponse response = client.send(getFaviconRequest(url))) {
            if (isDefaultFavicon(response)) {
                return null;
            }
            try (InputStream stream = response.getBody()) {
                return resize(ImageIO.read(stream));
            }
        }
    }

    private static HttpRequest getFaviconRequest(URL url) throws MalformedURLException {
        InternetDomainName domainName = InternetDomainName.from(url.getHost());
        return HttpRequest
                .builder()
                .query(new URL("https://api.faviconkit.com/" + domainName + "/57")) //16
                .build();
    }

    private static boolean isDefaultFavicon(final HttpResponse response) throws IllegalArgumentException, IOException {
        return response.getContentType().equals(SVG);
    }

    private static final MediaType SVG = MediaType.parse("image/svg+xml");

    private static Image resize(BufferedImage img) {
        return img.getWidth() > 16 || img.getHeight() > 16 ? img.getScaledInstance(16, 16, Image.SCALE_SMOOTH) : img;
    }
}
