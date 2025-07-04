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
import com.github.eugene.kamenev.tsmp4j.utils.Util;

public abstract class BaseMatrixProfileAlgorithm<S extends WindowStatistic,
    M extends MatrixProfile> implements MatrixProfileAlgorithm<S, M> {

    private final RollingWindowStatistics<S> rollingStatistics;

    protected final double exclusionZone;

    protected final int exclusionZoneSize;

    public BaseMatrixProfileAlgorithm(RollingWindowStatistics<S> rollingWindowStatistics,
        double exclusionZone) {
        this.rollingStatistics = rollingWindowStatistics;
        this.exclusionZone = exclusionZone;
        this.exclusionZoneSize = rollingWindowStatistics != null ?
            (int) Math.floor(rollingWindowStatistics.windowSize() * exclusionZone + Util.EPS) : 0;
    }

    public RollingWindowStatistics<S> rollingStatistics() {
        return this.rollingStatistics;
    }
}
