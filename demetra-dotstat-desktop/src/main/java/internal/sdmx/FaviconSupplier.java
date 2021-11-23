package internal.sdmx;

import com.google.common.io.ByteStreams;
import com.google.common.net.InternetDomainName;
import internal.util.http.HttpClient;
import internal.util.http.HttpRequest;
import internal.util.http.HttpResponse;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import javax.imageio.ImageIO;
import nbbrd.io.function.IOFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@lombok.RequiredArgsConstructor
public class FaviconSupplier {

    public static final IOFunction<String, URL> GOOGLE_QUERY = host -> new URL("https://www.google.com/s2/favicons?domain=" + host);

    private final HttpClient client;
    private final IOFunction<String, URL> query;
    private byte[] defaultImage = null;

    @Nullable
    public Image getFaviconOrNull(@NonNull URL url) throws IOException {
        byte[] result = getFaviconByDomainName(InternetDomainName.from(url.getHost()));
        return result != null ? ImageIO.read(new ByteArrayInputStream(result)) : null;
    }

    private byte[] getFaviconByDomainName(InternetDomainName domainName) throws IOException {
        byte[] result = requestFavicon(HttpRequest.builder().query(query.applyWithIO(domainName.toString())).build());
        return !isDefaultImage(result) ? result : (domainName.hasParent() ? getFaviconByDomainName(domainName.parent()) : null);
    }

    private byte[] requestFavicon(HttpRequest request) throws IOException {
        try (HttpResponse response = client.requestGET(request)) {
            try (InputStream stream = response.getBody()) {
                return ByteStreams.toByteArray(stream);
            }
        }
    }

    private synchronized boolean isDefaultImage(byte[] found) throws IOException {
        if (defaultImage == null) {
            defaultImage = requestFavicon(HttpRequest.builder().query(query.applyWithIO(".")).build());
        }
        return Arrays.equals(defaultImage, found);
    }
}
