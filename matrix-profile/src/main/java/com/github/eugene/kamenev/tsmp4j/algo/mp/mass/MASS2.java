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

package com.github.eugene.kamenev.tsmp4j.algo.mp.mass;


import com.github.eugene.kamenev.tsmp4j.algo.mp.DistanceProfileFunction;
import com.github.eugene.kamenev.tsmp4j.stats.WindowStatistic;
import com.github.eugene.kamenev.tsmp4j.utils.Util;
import org.apache.commons.math3.complex.Complex;

/**
 * MASS_V2 Mueen's Algorithm for Similarity Search is The Fastest Similarity Search Algorithm for
 * Time Series Subsequences under Euclidean Distance and Correlation Coefficient. Reference: <a
 * href="https://www.cs.unm.edu/~mueen/FastestSimilaritySearch.html">FastestSimilaritySearch.html</a>
 */
public class MASS2<S extends WindowStatistic> implements DistanceProfileFunction<S> {

    @Override
    public DistanceProfile apply(DistanceProfileQuery<S> dsq) {
        var n = dsq.data().dataSize();
        var m = dsq.windowSize();
        var qIndex = dsq.queryIndex();
        var dataFft = dsq.dataFft();
        if (dataFft == null) {
            int padSize = Util.padSize(n);
            dataFft = Util.forwardFft(dsq.data(), false, 0, padSize);
        }
        var skip = dsq.query().dataSize() - (m + qIndex);
        var queryFft = Util.forwardFft(dsq.query(), true, skip, dataFft.length);
        var prod = new Complex[queryFft.length];
        for (int i = 0; i < queryFft.length; i++) {
            prod[i] = queryFft[i].multiply(dataFft[i]);
        }
        var inv = Util.inverseFft(prod);
        double[] z = new double[inv.length];
        for (int i = 0; i < inv.length; i++) {
            z[i] = inv[i].getReal();
        }
        var meanB = dsq.query().mean(qIndex);
        var stdDevB = dsq.query().stdDev(qIndex);
        var dist = new double[n - m + 1];
        var shift = m - 1;
        var sqN = Math.sqrt(n);
        for (var i = shift; i < n; i++) {
            var meanA = dsq.data().mean(i - shift);
            var stdDevA = dsq.data().stdDev(i - shift);
            var d = 2 * (m - (z[i] - m * meanA * meanB) / (stdDevA * stdDevB));
            if (dsq.sqrt()) {
                if (dsq.norm() && d < 0) {
                    d = 0.0d;
                } else {
                    d = Math.sqrt(d);
                    if (dsq.norm()) {
                        d /= sqN;
                    }
                }
            }
            dist[i - m + 1] = d;
        }

        return new DistanceProfile(dist, z);
    }

}
