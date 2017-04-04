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
package be.nbb.demetra.dotstat2;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.cube.CubeId;
import ec.tss.tsproviders.cube.CubeSupport;
import ec.tss.tsproviders.utils.IConfig;
import ec.tss.tsproviders.utils.IParam;
import static ec.tss.tsproviders.utils.Params.onInteger;
import static ec.tss.tsproviders.utils.Params.onLong;
import static ec.tss.tsproviders.utils.Params.onString;
import static ec.tss.tsproviders.utils.Params.onStringList;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

/**
 *
 * @author Philippe Charles
 */
interface DotStatParam extends IParam<DataSource, DotStatBean2> {

    String getVersion();

    @Nonnull
    IParam<DataSet, CubeId> getCubeIdParam(@Nonnull DataSource dataSource);

    static final class V1 implements DotStatParam {

        private final Splitter dimensionSplitter = Splitter.on(',').trimResults().omitEmptyStrings();
        private final Joiner dimensionJoiner = Joiner.on(',');

        private final IParam<DataSource, String> dbName = onString("", "dbName");
        private final IParam<DataSource, String> flowRef = onString("", "tableName");
        private final IParam<DataSource, List<String>> dimensionIds = onStringList(ImmutableList.of(), "dimColumns", dimensionSplitter, dimensionJoiner);
        private final IParam<DataSource, Long> cacheTtl = onLong(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES), "cacheTtl");
        private final IParam<DataSource, Integer> cacheDepth = onInteger(1, "cacheDepth");

        @Override
        public String getVersion() {
            return "20150203";
        }

        @Override
        public DotStatBean2 defaultValue() {
            DotStatBean2 result = new DotStatBean2();
            result.setDbName(dbName.defaultValue());
            result.setFlowRef(flowRef.defaultValue());
            result.setDimensionIds(dimensionIds.defaultValue());
            result.setCacheTtl(Duration.ofMillis(cacheTtl.defaultValue()));
            result.setCacheDepth(cacheDepth.defaultValue());
            return result;
        }

        @Override
        public DotStatBean2 get(DataSource dataSource) {
            DotStatBean2 result = new DotStatBean2();
            result.setDbName(dbName.get(dataSource));
            result.setFlowRef(flowRef.get(dataSource));
            result.setDimensionIds(dimensionIds.get(dataSource));
            result.setCacheTtl(Duration.ofMillis(cacheTtl.get(dataSource)));
            result.setCacheDepth(cacheDepth.get(dataSource));
            return result;
        }

        @Override
        public void set(IConfig.Builder<?, DataSource> builder, DotStatBean2 value) {
            dbName.set(builder, value.getDbName());
            flowRef.set(builder, value.getFlowRef());
            dimensionIds.set(builder, value.getDimensionIds());
            cacheTtl.set(builder, value.getCacheTtl().toMillis());
            cacheDepth.set(builder, value.getCacheDepth());
        }

        @Override
        public IParam<DataSet, CubeId> getCubeIdParam(DataSource dataSource) {
            return CubeSupport.idByName(CubeId.root(dimensionIds.get(dataSource)));
        }
    }
}
