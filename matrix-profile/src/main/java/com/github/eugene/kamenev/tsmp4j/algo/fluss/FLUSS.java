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

package com.github.eugene.kamenev.tsmp4j.algo.fluss;

import com.github.eugene.kamenev.tsmp4j.algo.mp.MatrixProfile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.math3.distribution.BetaDistribution;

/**
 * Gharghabi S, Ding Y, Yeh C-CM, Kamgar K, Ulanova L, Keogh E. Matrix Profile VIII:
 * Domain Agnostic Online Semantic Segmentation at Superhuman Performance Levels. In: 2017 IEEE
 * International Conference on Data Mining (ICDM). IEEE; 2017. p. 117-26.
 *
 * Website: <a href="https://sites.google.com/site/onlinesemanticsegmentation">Online Semantic Segmentation</a>
 * Website: <a href="http://www.cs.ucr.edu/~eamonn/MatrixProfile.html">MatrixProfile</a>
 */
public class FLUSS implements Function<MatrixProfile, FLUSSCP> {

    private final int windowSize;

    private final int numSegments;

    public FLUSS(int windowSize, int numSegments) {
        this.windowSize = windowSize;
        this.numSegments = numSegments;
    }

    @Override
    public FLUSSCP apply(MatrixProfile profile) {
        int exclusionZone = (int) Math.ceil(profile.exclusionZone() * 10);
        double[] cac = flussCAC(exclusionZone, windowSize, profile);
        int[] fluss = flussExtract(exclusionZone, windowSize, numSegments, cac);
        return new FLUSSCP(cac, fluss);
    }


    public static int[] flussExtract(int exclusionZone, int windowSize, int numSegments,
        double[] cac) {
        List<Integer> segmentsPositions = new ArrayList<>();
        int arcCountsSize = cac.length;
        int ez = windowSize * exclusionZone;

        Set<Integer> selectedIndices = new HashSet<>();

        for (int i = 0; i < numSegments; i++) {
            int idx = minIndexExcluding(cac, selectedIndices);
            if (cac[idx] >= 1) {
                break;
            }
            segmentsPositions.add(idx);
            selectedIndices.add(idx);
            for (int j = Math.max(0, idx - ez); j < Math.min(arcCountsSize, idx + ez); j++) {
                selectedIndices.add(j);
            }
        }

        return segmentsPositions.stream().mapToInt(i -> i).toArray();
    }

    public static double[] flussCAC(int exclusionZone, int windowSize, MatrixProfile profile) {
        int profileIndexSize = profile.indexes().length;
        int[] nnmark = new int[profileIndexSize];
        double[] arcCounts = new double[profileIndexSize];

        for (int i = 0; i < profileIndexSize; i++) {
            int j = profile.indexes()[i];
            if (j >= 0 && j < profileIndexSize) {
                nnmark[Math.min(i, j)] += 1;
                nnmark[Math.max(i, j)] -= 1;
            }
        }

        int sum = 0;
        for (int i = 0; i < profileIndexSize; i++) {
            sum += nnmark[i];
            arcCounts[i] = sum;
        }

        var betaDistribution = new BetaDistribution(2, 2);
        double[] correctedArcCounts = new double[profileIndexSize];
        int ez = windowSize * exclusionZone;
        for (int i = 0; i < profileIndexSize; i++) {
            if (i < Math.min(ez, profileIndexSize) || i >= Math.max(profileIndexSize - ez, 0)) {
                correctedArcCounts[i] = 1;
            } else {
                var x = (double) i / (profileIndexSize - 1);
                var idealArcCounts = betaDistribution.density(x) * profileIndexSize / 3;
                correctedArcCounts[i] = Math.min(arcCounts[i] / idealArcCounts, 1);
            }
        }
        return correctedArcCounts;
    }

    /**
     * Calculates the score of predicted semantic transitions compared with the ground truth.
     * @param gtruth
     * @param extracted
     * @param dataLength
     * @return score, closer to 0 is best
     */
    public static double score(int[] gtruth, int[] extracted, int dataLength) {
        int n = gtruth.length;
        double[] minv = new double[n];

        for (int j = 0; j < n; j++) {
            minv[j] = Double.POSITIVE_INFINITY;
            for (int k : extracted) {
                if (Math.abs(k - gtruth[j]) < Math.abs(minv[j])) {
                    minv[j] = Math.abs(k - gtruth[j]);
                }
            }
        }

        double score = 0.0;
        for (int j = 0; j < n; j++) {
            score += minv[j];
        }

        score /= dataLength;

        return score;
    }

    private static int minIndexExcluding(double[] array, Set<Integer> excludedIndices) {
        int minIndex = -1;
        double minValue = Double.POSITIVE_INFINITY;
        for (int i = 0; i < array.length; i++) {
            if (!excludedIndices.contains(i) && array[i] < minValue) {
                minValue = array[i];
                minIndex = i;
            }
        }
        return minIndex;
    }
}
