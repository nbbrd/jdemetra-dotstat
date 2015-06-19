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

import be.nbb.sdmx.SdmxConnection;
import be.nbb.sdmx.SdmxConnectionSupplier;
import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import it.bancaditalia.oss.sdmx.api.GenericSDMXClient;
import it.bancaditalia.oss.sdmx.client.custom.ABS;
import it.bancaditalia.oss.sdmx.client.custom.DotStat;
import it.bancaditalia.oss.sdmx.client.custom.ILO;
import it.bancaditalia.oss.sdmx.client.custom.IMF;
import it.bancaditalia.oss.sdmx.client.custom.INEGI;
import it.bancaditalia.oss.sdmx.client.custom.NBB;
import it.bancaditalia.oss.sdmx.client.custom.OECD;
import it.bancaditalia.oss.sdmx.client.custom.RestSdmx20Client;
import it.bancaditalia.oss.sdmx.client.custom.UIS;
import it.bancaditalia.oss.sdmx.client.custom.WB;
import it.bancaditalia.oss.sdmx.util.SdmxException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * @author Philippe Charles
 */
public final class SdmxConnectionSupplierImpl extends SdmxConnectionSupplier {

    public static final SdmxConnectionSupplierImpl INSTANCE = new SdmxConnectionSupplierImpl();

    private final Cache<String, Object> cache;
    private final Map<String, GenericSDMXClient> builtInClientByHost;
    private final Map<String, WsEntryPoint> entryPointByName;

    private SdmxConnectionSupplierImpl() {
        this.cache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
        this.builtInClientByHost = createClients();
        this.entryPointByName = new HashMap<>();
        for (WsEntryPoint o : WsEntryPoint.loadBuiltIn()) {
            entryPointByName.put(o.getName(), o);
        }
    }

    @Override
    public SdmxConnection getConnection(String name) {
        WsEntryPoint wsEntryPoint = entryPointByName.get(name);
        if (wsEntryPoint != null) {
            GenericSDMXClient client = builtInClientByHost.get(wsEntryPoint.getUrl().getHost());
            if (client == null) {
                client = createClient(wsEntryPoint);
            }
            if (client != null) {
                return new CachedSdmxConnection(client, wsEntryPoint.getUrl().getHost(), cache);
            }
        }
        return SdmxConnection.failing();
    }

    @Nonnull
    public List<WsEntryPoint> getEntryPoints() {
        List<WsEntryPoint> result = new ArrayList<>();
        for (WsEntryPoint o : entryPointByName.values()) {
            result.add(o.copy());
        }
        return result;
    }

    public void setEntryPoints(@Nonnull List<WsEntryPoint> list) {
        entryPointByName.clear();
        for (WsEntryPoint o : list) {
            entryPointByName.put(o.getName(), o);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static Map<String, GenericSDMXClient> createClients() {
        try {
            List<GenericSDMXClient> result = Arrays.<GenericSDMXClient>asList(
                    new ABS(), new ILO(), new IMF(), new INEGI(), new OECD(), new WB(), new NBB(), new UIS()
            );
            return Maps.uniqueIndex(result, new Function<GenericSDMXClient, String>() {
                @Override
                public String apply(GenericSDMXClient input) {
                    try {
                        return input.getEndpoint().getHost();
                    } catch (SdmxException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Nullable
    private static GenericSDMXClient createClient(WsEntryPoint o) {
        try {
            switch (o.getType()) {
                case DotStat20:
                    return new DotStat(o.getName(), o.getUrl(), o.isNeedsCredentials(), "compact_v2") {
                    };
                case Sdmx20:
                    return new RestSdmx20Client(o.getName(), o.getUrl(), o.isNeedsCredentials(), null, "compact_v2") {
                    };
                case Sdmx21:
                    return new ExtRestSdmxClient(o.getName(), o.getUrl(), o.isNeedsCredentials(), o.isNeedsURLEncoding(), o.isSupportsCompression());
                default:
                    return null;
            }
        } catch (MalformedURLException ex) {
            return null;
        }
    }
    //</editor-fold>

}
