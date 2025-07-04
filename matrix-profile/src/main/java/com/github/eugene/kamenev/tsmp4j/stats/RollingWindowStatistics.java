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

import com.github.eugene.kamenev.tsmp4j.utils.Buffer;
import java.util.function.DoubleFunction;

public interface RollingWindowStatistics<S extends WindowStatistic> extends DoubleFunction<S> {

    Buffer.ObjBuffer<S> getStatsBuffer();

    Buffer.DoubleBuffer getDataBuffer();

    default double x(int i) {
        return this.getStatsBuffer().get(i).x();
    }

    default double mean(int i) {
        return this.getStatsBuffer().get(shiftIndex(i)).mean();
    }

    default double stdDev(int i) {
        return this.getStatsBuffer().get(shiftIndex(i)).stdDev();
    }

    default boolean skip(int i) {
        return this.getStatsBuffer().get(shiftIndex(i)).skip();
    }

    default boolean isReady() {
        return this.getDataBuffer().isFull();
    }

    default int shiftIndex(int i) {
        return i + windowSize() - 1;
    }

    default int windowSize() {
        return this.getDataBuffer().getLength();
    }

    default int dataSize() {
        return this.getStatsBuffer().getLength();
    }
}
