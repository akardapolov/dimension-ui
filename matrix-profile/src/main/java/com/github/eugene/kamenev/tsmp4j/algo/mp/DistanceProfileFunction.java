/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.eugene.kamenev.tsmp4j.algo.mp;

import com.github.eugene.kamenev.tsmp4j.stats.RollingWindowStatistics;
import com.github.eugene.kamenev.tsmp4j.stats.WindowStatistic;
import java.util.function.Function;
import org.apache.commons.math3.complex.Complex;
import com.github.eugene.kamenev.tsmp4j.algo.mp.DistanceProfileFunction.DistanceProfile;
import com.github.eugene.kamenev.tsmp4j.algo.mp.DistanceProfileFunction.DistanceProfileQuery;

public interface DistanceProfileFunction<S extends WindowStatistic>
    extends Function<DistanceProfileQuery<S>, DistanceProfile> {

    record DistanceProfileQuery<S extends WindowStatistic>(
        RollingWindowStatistics<S> data,
        RollingWindowStatistics<S> query,
        int queryIndex,
        int windowSize,
        Complex[] dataFft,

        boolean sqrt,

        boolean norm
    ) {

        public DistanceProfileQuery(
            RollingWindowStatistics<S> ts,
            RollingWindowStatistics<S> query, int queryIndex, int windowSize, Complex[] fft) {
            this(ts, query, queryIndex, windowSize, fft, true, false);
        }

        public DistanceProfileQuery(
            RollingWindowStatistics<S> ts,
            RollingWindowStatistics<S> query, int queryIndex, int windowSize) {
            this(ts, query, queryIndex, windowSize, null, true, false);
        }

        public DistanceProfileQuery(
            RollingWindowStatistics<S> ts,
            RollingWindowStatistics<S> query, int windowSize) {
            this(ts, query, 0, windowSize, null, true, false);
        }

        public DistanceProfileQuery(
            RollingWindowStatistics<S> ts,
            RollingWindowStatistics<S> query) {
            this(ts, query, query.windowSize());
        }
    }

    record DistanceProfile(double[] profile, double[] product) {

        public DistanceProfile(double[] profile) {
            this(profile, null);
        }
    }

}
