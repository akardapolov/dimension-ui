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

import com.github.eugene.kamenev.tsmp4j.algo.mp.DistanceProfileFunction.DistanceProfileQuery;
import com.github.eugene.kamenev.tsmp4j.algo.mp.mass.MASS2;
import com.github.eugene.kamenev.tsmp4j.stats.BaseRollingWindowStatistics;
import com.github.eugene.kamenev.tsmp4j.utils.Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiFunction;

public class RelativeFrequencyMatrixProfileAlgorithm implements
    BiFunction<double[], double[], RelativeFrequencyMatrixProfile> {

    private final int maxFreq;

    private final int windowSize;

    private final int selfFlag;

    private final boolean hasNan;

    public RelativeFrequencyMatrixProfileAlgorithm(int maxFreq, int windowSize, boolean hasNan,
        boolean selfFlag) {
        this.maxFreq = maxFreq;
        this.windowSize = windowSize;
        this.selfFlag = selfFlag ? 1 : 0;
        this.hasNan = hasNan;
    }

    /**
     * @param positiveTS a timeseries containing at least 2 instances of desired behaviour
     * @param negativeTS a timeseries containing zero instances of desired behaviour
     * @return RelativeFrequencyMatrixProfile
     */
    @Override
    public RelativeFrequencyMatrixProfile apply(double[] positiveTS, double[] negativeTS) {
        var isNanPositive = new boolean[positiveTS.length];
        var isNanNegative = new boolean[negativeTS.length];
        var nonNanNegativeTS = negativeTS;
        var RFMP = new double[maxFreq][positiveTS.length - windowSize + 1];
        var RFMPIndexes = new int[maxFreq][positiveTS.length - windowSize + 1];

        if (hasNan) {
            var replacedPositive = replaceNanWithMean(positiveTS);
            var replacedNegative = replaceNanWithMean(negativeTS);
            isNanPositive = replacedPositive.isNan();
            nonNanNegativeTS = replacedNegative.ts();
            isNanNegative = replacedNegative.isNan();
        }
        var statsB = new BaseRollingWindowStatistics<>(windowSize, negativeTS.length);
        for (var i = 0; i < negativeTS.length; i++) {
            statsB.apply(nonNanNegativeTS[i]);
        }
        var fft = Util.forwardFft(statsB, false, 0, Util.padSize(negativeTS.length));

        for (int i = 0; i < positiveTS.length - windowSize + 1; i++) {
            var statsA = new BaseRollingWindowStatistics<>(windowSize, windowSize);
            boolean shouldSkip = false;
            for (int j = i; j < i + windowSize; j++) {
                if (isNanPositive[j]) {
                    shouldSkip = true;
                    break;
                }
                statsA.apply(positiveTS[j]);
            }
            if (shouldSkip) {
                continue;
            }
            var dp = new MASS2<>().apply(
                    new DistanceProfileQuery<>(statsB, statsA, 0, windowSize, fft, true, true))
                .profile();
            for (int j = 0; j < negativeTS.length - windowSize + 1; j++) {
                if (isNanNegative[j]) {
                    for (int idx = Math.max(0, j - windowSize); idx < j + windowSize; idx++) {
                        dp[idx] = Double.NaN;
                    }
                }
            }

            var neighbors = NearestNeighborSelection.getNearestNeighbors(dp, windowSize,
                maxFreq + selfFlag);
            var kMin = Math.min(maxFreq + selfFlag, neighbors.indexes().length);
            for (int j = 0; j < kMin - selfFlag; j++) {
                RFMP[j][i] = neighbors.distances()[j + selfFlag];
                RFMPIndexes[j][i] = neighbors.indexes()[j + selfFlag];
            }
        }
        return new RelativeFrequencyMatrixProfile(RFMP, RFMPIndexes);
    }

    private static ReplacedNonNan replaceNanWithMean(double[] tsA) {
        double[] tsANonNan = Arrays.copyOf(tsA, tsA.length);
        boolean[] isNan = new boolean[tsA.length];
        var nanIndexes = new ArrayList<Integer>();
        var sum = 0.0;
        var count = 0;
        for (int i = 0; i < tsA.length; i++) {
            if (Double.isNaN(tsA[i])) {
                isNan[i] = true;
                nanIndexes.add(i);
            } else {
                sum += tsA[i];
                count++;
            }
        }
        double mean = count == 0 ? 0 : sum / count;
        for (var i : nanIndexes) {
            tsANonNan[i] = mean;
        }
        return new ReplacedNonNan(tsANonNan, isNan);
    }

    private record ReplacedNonNan(double[] ts, boolean[] isNan) {

    }
}
