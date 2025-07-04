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

package com.github.eugene.kamenev.tsmp4j.stats;


import com.github.eugene.kamenev.tsmp4j.utils.Buffer.DoubleBuffer;
import com.github.eugene.kamenev.tsmp4j.utils.Buffer.ObjBuffer;

/**
 * Class computes rolling window statistics for a data stream, which is used in Matrix Profile
 * algorithms. Can be used offline/online. K-algorithm taken from: <a
 * href="https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Computing_shifted_data">Computing
 * shifted data</a>
 */
public class BaseRollingWindowStatistics<S extends WindowStatistic>
    implements RollingWindowStatistics<S> {

    private final DoubleBuffer dataBuffer;
    private final ObjBuffer<S> statsBuffer;
    private int n;
    private double K;
    private double Ex;
    private double Ex2;

    private long totalDataCount = 0;
    private int toSkip = 0;

    public BaseRollingWindowStatistics(int windowSize, S[] statsBuffer) {
        this.dataBuffer = new DoubleBuffer(windowSize);
        this.statsBuffer = new ObjBuffer<>(statsBuffer);
    }

    public BaseRollingWindowStatistics(int windowSize, S[] statsBuffer, boolean isFull) {
        this.dataBuffer = new DoubleBuffer(windowSize);
        this.statsBuffer = new ObjBuffer<>(statsBuffer, isFull);
    }

    @SuppressWarnings("unchecked")
    public BaseRollingWindowStatistics(int windowSize, int statsBufferSize) {
        this(windowSize, (S[]) new WindowStatistic[statsBufferSize]);
    }

    @SuppressWarnings("unchecked")
    public BaseRollingWindowStatistics(BaseRollingWindowStatistics<S> stats, int size) {
        this.Ex2 = stats.Ex2;
        this.Ex = stats.Ex;
        this.K = stats.K;
        this.n = stats.n;
        this.toSkip = stats.toSkip;
        this.totalDataCount = stats.totalDataCount;
        this.dataBuffer = new DoubleBuffer(stats.dataBuffer);
        this.statsBuffer = new ObjBuffer<>((S[]) new WindowStatistic[size]);
            stats.getStatsBuffer().toStream()
                .skip(stats.statsBuffer.size() - size)
                .limit(size)
                .forEach(this.statsBuffer::addToEnd);
    }

    @Override
    public S apply(double value) {
        totalDataCount++;
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            toSkip = windowSize();
            value = 0.0d;
        } else {
            toSkip--;
        }
        if (this.dataBuffer.isFull()) {
            this.removeValue(this.dataBuffer.head());
        }
        this.dataBuffer.addToEnd(value);
        this.addValue(value);
        var mean = getMean();
        var populationVariance = getPopulationVariance();
        var stat = computeStats(value, mean, Math.sqrt(Math.max(0, populationVariance)),
            populationVariance, totalDataCount, toSkip > 0);
        getStatsBuffer().addToEnd(stat);
        return stat;
    }

    private void addValue(double value) {
        if (n == 0) {
            K = value;
        }
        var diff = value - K;
        Ex += diff;
        Ex2 += diff * diff;
        n++;
    }

    private void removeValue(double value) {
        var diff = value - K;
        Ex -= diff;
        Ex2 -= diff * diff;
        n--;
    }

    @SuppressWarnings("unchecked")
    protected S computeStats(double x, double mean, double stdDev, double variance, long id,
        boolean skip) {
        return (S) new BaseWindowStatistic(x, mean, stdDev, id, skip);
    }

    @Override
    public DoubleBuffer getDataBuffer() {
        return dataBuffer;
    }

    @Override
    public ObjBuffer<S> getStatsBuffer() {
        return statsBuffer;
    }

    private double getMean() {
        return K + Ex / n;
    }

    private double getPopulationVariance() {
        return (Ex2 - Ex * Ex / n) / n;
    }
}
