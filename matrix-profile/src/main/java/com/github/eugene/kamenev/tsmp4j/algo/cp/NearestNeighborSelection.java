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

public class NearestNeighborSelection {

    public static NearestNeighbors getNearestNeighbors(double[] distanceProfile, int subLength,
        int K) {
        int exclusionLength = subLength;
        int KMax = 2 * (distanceProfile.length / subLength);
        if (K == -1) {
            exclusionLength = (int) Math.ceil(subLength / 2.0);
        }
        K = KMax;

        double[][] A = new double[distanceProfile.length][2];
        for (int i = 0; i < distanceProfile.length; i++) {
            A[i][0] = i;
            A[i][1] = -distanceProfile[i];
        }

        Arrays.sort(A, (a, b) -> Double.compare(b[1], a[1]));

        int[] exclusionZone = new int[distanceProfile.length];
        int[] sortedLowestIndices = new int[K];
        double[] correspondingDistances = new double[K];

        Arrays.fill(sortedLowestIndices, -1);
        Arrays.fill(correspondingDistances, Double.NaN);

        int index = 0;
        int iterKum = 0;
        while (index < K && iterKum < A.length - 1) {
            int trialIndex = (int) A[iterKum][0];
            if (exclusionZone[trialIndex] == 0 && !Double.isNaN(distanceProfile[trialIndex])) {
                sortedLowestIndices[index] = trialIndex;
                correspondingDistances[index] = distanceProfile[trialIndex];
                index++;

                for (int j = Math.max(0, trialIndex - exclusionLength);
                    j < Math.min(distanceProfile.length, trialIndex + exclusionLength + 1); j++) {
                    exclusionZone[j] = 1;
                }
            }
            iterKum++;
        }

        int validCount = 0;
        for (int i = 0; i < K; i++) {
            if (sortedLowestIndices[i] != -1) {
                validCount++;
            }
        }

        int[] finalIndices = new int[validCount];
        double[] finalDistances = new double[validCount];
        System.arraycopy(sortedLowestIndices, 0, finalIndices, 0, validCount);
        System.arraycopy(correspondingDistances, 0, finalDistances, 0, validCount);

        return new NearestNeighbors(finalIndices, finalDistances);
    }

    public record NearestNeighbors(int[] indexes, double[] distances) {

    }
}