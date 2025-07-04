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

package com.github.eugene.kamenev.tsmp4j.algo.cp;

import com.github.eugene.kamenev.tsmp4j.algo.mp.mpx.MPX;
import com.github.eugene.kamenev.tsmp4j.algo.mp.mpx.MPXRollingWindowStatistics;
import java.util.function.BiFunction;

/**
 * Matrix Profile XXIII: Contrast Profile: A Novel Time Series Primitive that Allows Real World
 * Classification DOI: 10.1109/ICDM51629.2021.00151 Time series data remains a perennially important
 * datatype considered in data mining. In the last decade there has been an increasing realization
 * that time series data can best understood by reasoning about time series subsequences on the
 * basis of their similarity to other subsequences: the two most familiar time series concepts being
 * motifs and discords. Time series motifs refer to two particularly close subsequences, whereas
 * time series discords indicate subsequences that are far from their nearest neighbors. However, we
 * argue that it can sometimes be useful to simultaneously reason about a subsequence’s closeness to
 * certain data and its distance to other data. In this work we introduce a novel primitive called
 * the Contrast Profile that allows us to efficiently compute such a definition in a principled way.
 * As we will show, the Contrast Profile has many downstream uses, including anomaly detection, data
 * exploration, and preprocessing unstructured data for classification. Reference to original code &
 * explanation:
 * <a href="https://sites.google.com/view/contrastprofile">ContrastProfile</a>
 */
public class ContrastProfileAlgorithm implements
    BiFunction<MPXRollingWindowStatistics, MPXRollingWindowStatistics, ContrastProfile> {

    /**
     * @param positiveTS a timeseries containing at least 2 instances of desired behaviour
     * @param negativeTS a timeseries containing zero instances of desired behaviour
     * @return ContrastProfile
     */
    @Override
    public ContrastProfile apply(MPXRollingWindowStatistics positiveTS,
        MPXRollingWindowStatistics negativeTS) {
        var m = positiveTS.windowSize();
        var pN = positiveTS.dataSize() + 1;
        var mpAA = new MPX(positiveTS).get();
        var paddedClippedMAA = clipMatrixProfileAmplitude(mpAA.profile(), m, pN);
        var mpAB = new MPX(positiveTS).get(negativeTS);
        var paddedClippedMAB = clipMatrixProfileAmplitude(mpAB.profile(), m, pN);
        var contrastProfile = new double[pN];
        var maxIdx = 0;
        var maxContrastValue = Double.NEGATIVE_INFINITY;
        for (var i = 0; i < pN; i++) {
            contrastProfile[i] = paddedClippedMAB[i] - paddedClippedMAA[i];
            if (contrastProfile[i] > maxContrastValue) {
                maxContrastValue = contrastProfile[i];
                maxIdx = i;
            }
        }
        normalizeContrastProfileAmplitude(contrastProfile, m);
        var plato = new double[m];
        var platoTwin = new double[m];
        for (int idx = 0, i = maxIdx, n = mpAA.indexes()[maxIdx]; idx < m; i++, n++, idx++) {
            plato[idx] = positiveTS.getStatsBuffer().get(i).x();
            platoTwin[idx] = positiveTS.getStatsBuffer().get(n).x();
        }

        return new ContrastProfile(contrastProfile, plato, platoTwin, positiveTS.windowSize());
    }

    public static double[] clipMatrixProfileAmplitude(double[] mp, double m, int padding) {
        var clippedMp = new double[padding];
        var factor = Math.sqrt(2 * m);

        for (var i = 0; i < clippedMp.length; i++) {
            if (i < mp.length) {
                var minValue = Math.min(factor, mp[i]);
                clippedMp[i] = Double.isNaN(minValue) ? mp[i] : minValue;
                clippedMp[i] = Math.max(0, clippedMp[i]); // negative values are replaced with zero
            } else {
                clippedMp[i] = Double.NaN;
            }
        }

        return clippedMp;
    }

    public static void normalizeContrastProfileAmplitude(double[] cp, double m) {
        var normalizationFactor = Math.sqrt(2 * m);
        for (var i = 0; i < cp.length; i++) {
            cp[i] = cp[i] / normalizationFactor;
            cp[i] = Math.max(0, cp[i]); // negative values are replaced with zero
        }
    }
}
