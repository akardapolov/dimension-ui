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

package com.github.eugene.kamenev.tsmp4j.algo.mp.stomp;

import com.github.eugene.kamenev.tsmp4j.algo.mp.BaseMatrixProfile;
import com.github.eugene.kamenev.tsmp4j.algo.mp.BaseMatrixProfileAlgorithm;
import com.github.eugene.kamenev.tsmp4j.algo.mp.DistanceProfileFunction;
import com.github.eugene.kamenev.tsmp4j.algo.mp.DistanceProfileFunction.DistanceProfileQuery;
import com.github.eugene.kamenev.tsmp4j.algo.mp.MatrixProfile;
import com.github.eugene.kamenev.tsmp4j.algo.mp.mass.MASS2;
import com.github.eugene.kamenev.tsmp4j.stats.BaseRollingWindowStatistics;
import com.github.eugene.kamenev.tsmp4j.stats.BaseWindowStatistic;
import com.github.eugene.kamenev.tsmp4j.stats.RollingWindowStatistics;
import com.github.eugene.kamenev.tsmp4j.stats.WindowStatistic;
import com.github.eugene.kamenev.tsmp4j.utils.Util;
import java.util.Arrays;

/**
 * STOMP: Scalable Time	series Ordered Matrix Profile Reference: Zhu Y, Zimmerman Z, Senobari NS,
 * Yeh CM, Funning G. Matrix Profile II : Exploiting a Novel Algorithm and GPUs to Break the One
 * Hundred Million Barrier for Time Series Motifs and Joins. Icdm. 2016 Jan 22;54(1):739-48.
 * Reference Website: <a
 * href="http://www.cs.ucr.edu/~eamonn/MatrixProfile.html">MatrixProfile.html</a>
 */
public class STOMP extends BaseMatrixProfileAlgorithm<BaseWindowStatistic, MatrixProfile> {

    public STOMP(RollingWindowStatistics<BaseWindowStatistic> rollingWindowStatistics,
        double exclusionZone) {
        super(rollingWindowStatistics, exclusionZone);
    }

    public STOMP(RollingWindowStatistics<BaseWindowStatistic> rollingWindowStatistics) {
        super(rollingWindowStatistics, 0.5d);
    }

    public STOMP(int windowSize, int bufferSize) {
        this(new BaseRollingWindowStatistics<>(windowSize, bufferSize));
    }

    public STOMP(int windowSize, int bufferSize, double exclusionZone) {
        this(new BaseRollingWindowStatistics<>(windowSize, bufferSize), exclusionZone);
    }

    @Override
    public MatrixProfile get(RollingWindowStatistics<BaseWindowStatistic> query) {
        if (this.isReady()) {
            return stomp(this.rollingStatistics(), query, this.exclusionZone,
                this.exclusionZoneSize);
        }
        return null;
    }

    @Override
    public MatrixProfile get() {
        if (this.isReady()) {
            return stomp(this.rollingStatistics(), null, this.exclusionZone,
                this.exclusionZoneSize);
        }
        return null;
    }

    public static <S extends WindowStatistic> MatrixProfile stomp(RollingWindowStatistics<S> ts,
        RollingWindowStatistics<S> query, double exclusionZone, int exclusionZoneSize,
        DistanceProfileFunction<S> distFunc) {
        int windowSize = ts.windowSize();
        boolean isJoin = query != null;
        if (!isJoin) {
            query = ts;
        } else {
            exclusionZone = 0;
            exclusionZoneSize = 0;
        }
        int exZone = exclusionZoneSize;
        int dataSize = ts.dataSize();
        int querySize = query.dataSize();
        int mpSize = dataSize - windowSize + 1;
        int numQueries = querySize - windowSize + 1;
        if (querySize > dataSize) {
            throw new IllegalArgumentException(
                "Query must be smaller or the same size as reference data.");
        }
        if (windowSize < 4) {
            throw new IllegalArgumentException("Window size must be at least 4.");
        }

        double[] leftMatrixProfile, rightMatrixProfile;
        int[] leftProfileIndex, rightProfileIndex;

        var matrixProfile = new double[mpSize];
        var profileIndex = new int[mpSize];

        if (isJoin) {
            leftMatrixProfile = rightMatrixProfile = null;
            leftProfileIndex = rightProfileIndex = null;
        } else {
            leftMatrixProfile = new double[matrixProfile.length];
            rightMatrixProfile = new double[matrixProfile.length];
            leftProfileIndex = new int[matrixProfile.length];
            rightProfileIndex = new int[matrixProfile.length];
        }

        for (int i = 0; i < mpSize; i++) {
            matrixProfile[i] = Double.POSITIVE_INFINITY;
            profileIndex[i] = -1;
            if (!isJoin) {
                rightMatrixProfile[i] = Double.POSITIVE_INFINITY;
                leftMatrixProfile[i] = Double.POSITIVE_INFINITY;
                rightProfileIndex[i] = -1;
                leftProfileIndex[i] = -1;
            }
        }

        var fftTs = Util.forwardFft(ts, false, 0, Util.padSize(dataSize));
        var nn = distFunc.apply(new DistanceProfileQuery<>(ts, query, 0, windowSize, fftTs));
        var rnn = nn;
        var fftQuery = fftTs;
        if (isJoin) {
            fftQuery = Util.forwardFft(query, false, 0, Util.padSize(querySize));
            rnn = distFunc.apply(
                new DistanceProfileQuery<>(query, ts, 0, windowSize, fftQuery));
        }

        double[] firstProduct = Arrays.stream(rnn.product()).skip(windowSize - 1).limit(numQueries)
            .toArray();
        double[] accumulatedProducts = Arrays.stream(nn.product()).skip(windowSize - 1)
            .limit(mpSize).toArray();
        double[] distanceProfile = nn.profile();
        double dropValue = query.x(0);

        for (int i = 0; i < numQueries; i++) {
            if (i > 0) {
                var prevProd = accumulatedProducts[0];
                var prod = firstProduct[i];
                for (int j = 1; j <= mpSize; j++) {
                    if (j == 1) {
                        accumulatedProducts[0] = prod;
                    }

                    var a = (prod - windowSize * ts.mean(j - 1) * query.mean(i));
                    var b = (ts.stdDev(j - 1) * query.stdDev(i));
                    var dist = 2 * (windowSize - a / b);
                    distanceProfile[j - 1] = Math.sqrt(dist);
                    if (j == mpSize) {
                        break;
                    }
                    var currProd = accumulatedProducts[j];
                    var newProd = prevProd -
                        ts.x(j - 1) * dropValue +
                        ts.x(j + windowSize - 1) * query.x(i + windowSize - 1);

                    accumulatedProducts[j] = newProd;
                    prevProd = currProd;
                    prod = newProd;
                }
            }

            dropValue = query.x(i);

            for (int k = 0; k < mpSize; k++) {
                if ((exZone > 0 && Math.abs(k - i) <= exZone) || ts.stdDev(k) < Util.EPS || ts.skip(k) || ts.skip(i)) {
                    distanceProfile[k] = Double.POSITIVE_INFINITY;
                }
                // normal matrixProfile
                if (distanceProfile[k] < matrixProfile[k]) {
                    matrixProfile[k] = distanceProfile[k];
                    profileIndex[k] = i;
                }

                if (!isJoin) {
                    // left matrixProfile
                    if (k >= i && distanceProfile[k] < leftMatrixProfile[k]) {
                        leftMatrixProfile[k] = distanceProfile[k];
                        leftProfileIndex[k] = i;
                    }

                    // right matrixProfile
                    if (k <= i && distanceProfile[k] < rightMatrixProfile[k]) {
                        rightMatrixProfile[k] = distanceProfile[k];
                        rightProfileIndex[k] = i;
                    }
                }
            }
        }

        return new BaseMatrixProfile(windowSize, exclusionZone, matrixProfile, profileIndex,
            rightMatrixProfile, leftMatrixProfile, rightProfileIndex, leftProfileIndex);
    }

    public static <S extends WindowStatistic> MatrixProfile stomp(RollingWindowStatistics<S> ts,
        RollingWindowStatistics<S> query, double exclusionZone, int exclusionZoneSize) {
        return stomp(ts, query, exclusionZone, exclusionZoneSize, new MASS2<>());
    }

    public static MatrixProfile of(double[] ts, double[] query, int windowSize) {
        var dataS = new BaseRollingWindowStatistics<BaseWindowStatistic>(windowSize, ts.length);
        var queryS = new BaseRollingWindowStatistics<BaseWindowStatistic>(windowSize, query.length);
        Arrays.stream(ts).forEach(dataS::apply);
        Arrays.stream(query).forEach(queryS::apply);
        return stomp(dataS, queryS, 0.5d, (int) Math.floor(windowSize * 0.5d + Util.EPS));
    }

    public static MatrixProfile of(double[] ts, int windowSize) {
        var dataS = new BaseRollingWindowStatistics<BaseWindowStatistic>(windowSize, ts.length);
        Arrays.stream(ts).forEach(dataS::apply);
        return stomp(dataS, null, 0.5d, (int) Math.floor(windowSize * 0.5d + Util.EPS));
    }
}
