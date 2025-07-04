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

import com.github.eugene.kamenev.tsmp4j.algo.mp.mpx.MPXRollingWindowStatistics;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class PanContrastProfileAlgorithm implements
    BiFunction<double[], double[], PanContrastProfile> {

    private final int[] subLenSeries;

    public PanContrastProfileAlgorithm(int minLen, int maxLen, int numSteps) {
        this.subLenSeries = numSteps > 0 ? getExpDistributedSeries(minLen, maxLen, numSteps)
            : IntStream.rangeClosed(minLen, maxLen).toArray();
    }

    /**
     * @param positiveTS a timeseries containing at least 2 instances of desired behaviour
     * @param negativeTS a timeseries containing zero instances of desired behaviour
     * @return PanContrastProfile
     */
    @Override
    public PanContrastProfile apply(double[] positiveTS, double[] negativeTS) {
        var contrastProfile = new ContrastProfile[subLenSeries.length];

        for (var i = 0; i < subLenSeries.length; i++) {
            var winSize = subLenSeries[i];
            var positiveStats = new MPXRollingWindowStatistics(winSize,
                positiveTS.length);
            var negativeStats = new MPXRollingWindowStatistics(winSize,
                negativeTS.length);
            Arrays.stream(positiveTS).forEach(positiveStats::apply);
            Arrays.stream(negativeTS).forEach(negativeStats::apply);
            contrastProfile[i] = new ContrastProfileAlgorithm().apply(positiveStats, negativeStats);
        }

        return new PanContrastProfile(contrastProfile);
    }

    private static int[] getExpDistributedSeries(int startLen, int endLen, int numSteps) {
        var powerMin = Math.log10(startLen);
        var powerMax = Math.log10(endLen);
        var powerStep = (powerMax - powerMin) / numSteps;

        var subLenSet = new HashSet<Integer>();

        for (var power = powerMin; power <= powerMax; power += powerStep) {
            subLenSet.add((int) Math.ceil(Math.pow(10, power)));
        }
        subLenSet.add(endLen);

        return subLenSet.stream().mapToInt(x -> x).sorted().toArray();
    }
}
