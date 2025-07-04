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

package com.github.eugene.kamenev.tsmp4j.algo.mp.mpx;

import com.github.eugene.kamenev.tsmp4j.stats.BaseRollingWindowStatistics;

public class MPXRollingWindowStatistics extends BaseRollingWindowStatistics<MPXStatistic> {

    public MPXRollingWindowStatistics(int windowSize, int statsBufferSize) {
        super(windowSize, statsBufferSize);
    }

    @Override
    protected MPXStatistic computeStats(double x, double mean, double stdDev, double variance,
        long id,
        boolean skip) {
        var df = 0.0d;
        var dg = 0.0d;
        var tail = this.getStatsBuffer().getR(0);
        var head = this.getStatsBuffer().getR(windowSize() - 1);
        if (head != null) {
            // df[i - w + 1] = 0.5 * (sb.x(i) - sb.x(i - w));
            df = 0.5 * (x - head.x());
            // dg[i - w + 1] = (sb.x(i) - sb.mean(i - w + 1)) + (sb.x(i - w) - sb.mean(i - w));
            dg = (x - mean) + (head.x() - tail.mean());
        }
        return new MPXStatistic(x, mean,
            this.getDataBuffer().size() > 2 ? 1 / Math.sqrt(variance * this.getDataBuffer().size())
                : 0, id, df, dg, false);
    }

    public double df(int i) {
        if (i == 0) {
            // always return zero for first element, this allows streaming to follow batch approach
            return 0.0d;
        }
        return this.getStatsBuffer().get(shiftIndex(i)).df();
    }

    public double dg(int i) {
        if (i == 0) {
            // always return zero for first element, this allows streaming to follow batch approach
            return 0.0d;
        }
        return this.getStatsBuffer().get(shiftIndex(i)).dg();
    }
}
