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

import java.util.Arrays;
import java.util.function.BiFunction;

public class RelativeFrequencyContrastProfileAlgorithm implements
    BiFunction<double[], double[], RelativeFrequencyContrastProfile> {

    private final int windowSize;

    private final int maxFreq;

    private final boolean hasNan;

    public RelativeFrequencyContrastProfileAlgorithm(int windowSize, int maxFreq, boolean hasNan) {
        this.windowSize = windowSize;
        this.maxFreq = maxFreq;
        this.hasNan = hasNan;
    }

    /**
     * @param positiveTS a timeseries containing at least 2 instances of desired behaviour
     * @param negativeTS a timeseries containing zero instances of desired behaviour
     * @return RelativeFrequencyContrastProfile
     */
    @Override
    public RelativeFrequencyContrastProfile apply(double[] positiveTS, double[] negativeTS) {
        // matrix profile self-join using positiveTS
        var RF_AA = new RelativeFrequencyMatrixProfileAlgorithm(maxFreq, windowSize, hasNan, true)
            .apply(positiveTS, positiveTS);
        clipMatrixProfileAmplitude(RF_AA.profile(), windowSize);
        var RF_AB = new RelativeFrequencyMatrixProfileAlgorithm(maxFreq, windowSize, hasNan, false)
            .apply(positiveTS, negativeTS);
        clipMatrixProfileAmplitude(RF_AB.profile(), windowSize);
        var efficientMaxFreq = Math.min(RF_AA.profile().length, RF_AB.profile().length);
        var RFMP_AA = RF_AA.profile();
        var RFMP_AB = RF_AB.profile();
        if (maxFreq > efficientMaxFreq) {
            System.err.println(
                "Warning: maxFreq = " + maxFreq + " is too large, using " + efficientMaxFreq
                    + " instead");
            RFMP_AA = new double[efficientMaxFreq][];
            RFMP_AB = new double[efficientMaxFreq][];
            for (var i = 0; i < efficientMaxFreq; i++) {
                RFMP_AA[i] = RF_AA.profile()[i];
                RFMP_AB[i] = RF_AB.profile()[i];
            }
        } else {
            efficientMaxFreq = maxFreq;
        }
        var RF_CP = new double[RFMP_AA.length][];
        for (int i = 0; i < RFMP_AA.length; i++) {
            RF_CP[i] = new double[RFMP_AA[i].length];
            for (int j = 0; j < RFMP_AA[i].length; j++) {
                RF_CP[i][j] = RFMP_AB[i][j] - RFMP_AA[i][j];
            }
        }
        normalizeContrastProfileAmplitude(RF_CP, windowSize);
        double[] CPmean = calculateCPmean(RF_CP, positiveTS, windowSize, efficientMaxFreq);

        // Find the maxContrast and its index
        double maxContrast = Double.NEGATIVE_INFINITY;
        int maxCPIndex = -1;
        for (int i = 0; i < CPmean.length; i++) {
            if (CPmean[i] > maxContrast) {
                maxContrast = CPmean[i];
                maxCPIndex = i;
            }
        }
        double[] plato = Arrays.copyOfRange(positiveTS, maxCPIndex, maxCPIndex + windowSize);
        return new RelativeFrequencyContrastProfile(plato, RF_CP, RF_AA.indexes());
    }

    public static double[] calculateCPmean(double[][] RFCP, double[] positiveTS, int m,
        int maxFreq) {
        int rows = RFCP.length;
        int maxCols = getMaxColumns(RFCP);
        double[] CPmean = new double[maxCols];
        Arrays.fill(CPmean, 0.0);
        var sqFreq = Math.sqrt(maxFreq);
        for (int ti = 0; ti <= maxCols - m; ti++) {
            boolean containsNaN = false;
            for (int j = ti; j < ti + m; j++) {
                if (j >= positiveTS.length || Double.isNaN(positiveTS[j - ti])) {
                    containsNaN = true;
                    break;
                }
            }

            if (!containsNaN) {
                double norm = 0.0;
                for (double[] doubles : RFCP) {
                    if (ti < doubles.length) {
                        norm += doubles[ti] * doubles[ti];
                    }
                }
                norm = Math.sqrt(norm);
                CPmean[ti] = norm / sqFreq;
            }
        }
        return CPmean;
    }

    public static int getMaxColumns(double[][] array) {
        int max = 0;
        for (double[] doubles : array) {
            max = Math.max(max, doubles.length);
        }
        return max;
    }

    private static void clipMatrixProfileAmplitude(double[][] mp, double m) {
        var factor = Math.sqrt(2 * m);
        for (var i = 0; i < mp.length; i++) {
            for (var j = 0; j < mp[i].length; j++) {
                var minValue = Math.min(factor, mp[i][j]);
                mp[i][j] = Double.isNaN(minValue) ? mp[i][j] : minValue;
                mp[i][j] = Math.max(0, mp[i][j]); // negative values are replaced with zero
            }
        }
    }

    private static void normalizeContrastProfileAmplitude(double[][] cp, double m) {
        var normalizationFactor = Math.sqrt(2 * m);
        for (var i = 0; i < cp.length; i++) {
            for (var j = 0; j < cp[i].length; j++) {
                cp[i][j] /= normalizationFactor;
                cp[i][j] = Math.max(0, cp[i][j]); // negative values are replaced with zero
            }
        }
    }
}
