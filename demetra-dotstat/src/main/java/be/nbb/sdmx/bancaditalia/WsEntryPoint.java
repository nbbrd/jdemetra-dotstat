/*
 * Copyright 2015 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package be.nbb.sdmx.bancaditalia;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 *
 * @author Philippe Charles
 */
@Data
public final class WsEntryPoint {

    public enum Type {

        DotStat20, Sdmx20, Sdmx21
    }

    private String name;
    private String description;
    private URL url;
    private Type type;
    private boolean needsCredentials;
    private boolean needsURLEncoding;
    private boolean supportsCompression;

    @Nonnull
    public WsEntryPoint copy() {
        WsEntryPoint result = new WsEntryPoint();
        result.setName(name);
        result.setDescription(description);
        result.setUrl(url);
        result.setType(type);
        result.setNeedsCredentials(needsCredentials);
        result.setNeedsURLEncoding(needsURLEncoding);
        result.setSupportsCompression(supportsCompression);
        return result;
    }

    @Nonnull
    public static List<WsEntryPoint> loadBuiltIn() {
        List<WsEntryPoint> result = new ArrayList<>();
        for (XmlEntrypoint o : XmlEntrypoints.load().entrypoint) {
            WsEntryPoint item = new WsEntryPoint();
            item.setName(o.name);
            item.setDescription(o.description);
            item.setUrl(o.url);
            item.setType(o.type);
            item.setNeedsCredentials(o.needsCredentials);
            item.setNeedsURLEncoding(o.needsURLEncoding);
            item.setSupportsCompression(o.supportsCompression);
            result.add(item);
        }
        return result;
    }

    @XmlRootElement(name = "entrypoints")
    static final class XmlEntrypoints {

        public XmlEntrypoint[] entrypoint;

        public static XmlEntrypoints load() {
            try {
                JAXBContext context = JAXBContext.newInstance(XmlEntrypoints.class);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                try (InputStream is = WsEntryPoint.class.getResourceAsStream("WsEntryPoints.xml")) {
                    return (XmlEntrypoints) unmarshaller.unmarshal(is);
                }
            } catch (JAXBException | IOException ex) {
                throw new RuntimeException("Unexpected exception", ex);
            }
        }
    }

    static final class XmlEntrypoint {

        @XmlAttribute
        public String name;
        @XmlAttribute
        public String description;
        @XmlAttribute
        public URL url;
        @XmlAttribute
        public Type type;
        @XmlAttribute
        public boolean needsCredentials;
        @XmlAttribute
        public boolean needsURLEncoding;
        @XmlAttribute
        public boolean supportsCompression;
    }

}
