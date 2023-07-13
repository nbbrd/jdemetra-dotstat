/*
 * Copyright 2016 National Bank of Belgium
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
package be.nbb.demetra.sdmx.web;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.cube.CubeId;
import ec.tss.tsproviders.cube.CubeSupport;
import ec.tss.tsproviders.utils.IConfig;
import ec.tss.tsproviders.utils.IParam;
import lombok.NonNull;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ec.tss.tsproviders.utils.Params.*;

/**
 * @author Philippe Charles
 */
interface SdmxWebParam extends IParam<DataSource, SdmxWebBean> {

    String getVersion();

    IParam<DataSet, CubeId> getCubeIdParam(CubeId root);

    final class V1 implements SdmxWebParam {

        private final Splitter dimensionSplitter = Splitter.on(',').trimResults().omitEmptyStrings();
        private final Joiner dimensionJoiner = Joiner.on(',');

        private final IParam<DataSource, String> dbName = onString("", "dbName");
        private final IParam<DataSource, String> flowRef = onString("", "tableName");
        private final IParam<DataSource, List<String>> dimensionIds = onStringList(ImmutableList.of(), "dimColumns", dimensionSplitter, dimensionJoiner);
        private final IParam<DataSource, String> labelAttribute = onString("", "l");
        private final IParam<DataSource, Long> cacheTtl = onLong(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES), "cacheTtl");
        private final IParam<DataSource, Integer> cacheDepth = onInteger(1, "cacheDepth");

        @Override
        public String getVersion() {
            return "20150203";
        }

        @Override
        public @NonNull SdmxWebBean defaultValue() {
            SdmxWebBean result = new SdmxWebBean();
            result.setSource(dbName.defaultValue());
            result.setFlow(flowRef.defaultValue());
            result.setDimensions(dimensionIds.defaultValue());
            result.setLabelAttribute(labelAttribute.defaultValue());
            result.setCacheTtl(Duration.ofMillis(cacheTtl.defaultValue()));
            result.setCacheDepth(cacheDepth.defaultValue());
            return result;
        }

        @Override
        public @NonNull SdmxWebBean get(@NonNull DataSource dataSource) {
            SdmxWebBean result = new SdmxWebBean();
            result.setSource(dbName.get(dataSource));
            result.setFlow(flowRef.get(dataSource));
            result.setDimensions(dimensionIds.get(dataSource));
            result.setLabelAttribute(labelAttribute.get(dataSource));
            result.setCacheTtl(Duration.ofMillis(cacheTtl.get(dataSource)));
            result.setCacheDepth(cacheDepth.get(dataSource));
            return result;
        }

        @Override
        public void set(IConfig.@NonNull Builder<?, DataSource> builder, SdmxWebBean value) {
            if (value != null) {
                dbName.set(builder, value.getSource());
                flowRef.set(builder, value.getFlow());
                dimensionIds.set(builder, value.getDimensions());
                labelAttribute.set(builder, value.getLabelAttribute());
                cacheTtl.set(builder, value.getCacheTtl().toMillis());
                cacheDepth.set(builder, value.getCacheDepth());
            }
        }

        @Override
        public IParam<DataSet, CubeId> getCubeIdParam(CubeId root) {
            return CubeSupport.idByName(root);
        }
    }
}
