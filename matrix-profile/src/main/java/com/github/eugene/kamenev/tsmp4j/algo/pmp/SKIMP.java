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

package com.github.eugene.kamenev.tsmp4j.algo.pmp;

import com.github.eugene.kamenev.tsmp4j.algo.mp.MatrixProfile;
import com.github.eugene.kamenev.tsmp4j.algo.mp.MatrixProfileAlgorithm;
import com.github.eugene.kamenev.tsmp4j.algo.mp.mpx.MPX;
import com.github.eugene.kamenev.tsmp4j.stats.WindowStatistic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Scalable KInetoscopic Matrix Profile (SKIMP) algorithm. SKIMP is a scalable algorithm for
 * computing the matrix profile for a set of window sizes. Reference: <a
 * href="https://sites.google.com/view/pan-matrix-profile/home">SKIMP</a>
 */
public class SKIMP<S extends WindowStatistic> implements PanMatrixProfileAlgorithm<S> {

    private final int[] windows;

    private final MatrixProfileAlgorithm<? extends WindowStatistic, ? extends MatrixProfile>[] algos;

    private final int[] splitIDX;

    @SuppressWarnings("unchecked")
    public SKIMP(int numInstances, boolean crossCorrelation, int... windows) {
        this.splitIDX = binarySplit(windows.length);
        this.windows = windows;
        this.algos = new MatrixProfileAlgorithm[windows.length];
        for (int i = 0; i < windows.length; i++) {
            algos[i] = new MPX(windows[i], numInstances, crossCorrelation, 0.5d);
        }
    }

    public <M extends MatrixProfile> SKIMP(int[] windows,
        MatrixProfileAlgorithm<S, M>[] algos) {
        this.splitIDX = binarySplit(windows.length);
        this.windows = windows;
        this.algos = algos;
    }

    @Override
    public void update(double value) {
        for (var algo : algos) {
            algo.update(value);
        }
    }

    @Override
    public PanMatrixProfile get() {
        if (!algos[0].isReady()) {
            return null;
        }
        double[][] pmp = new double[windows.length][];
        int[][] pmpi = new int[windows.length][];
        int[] idx = new int[windows.length];
        for (int splitIDXVal : splitIDX) {
            var mp = algos[splitIDXVal].get();
            var dist = mp.profile();
            var idxs = mp.indexes();

            for (int j = 0; j < dist.length; j++) {
                if (pmp[splitIDXVal] == null) {
                    pmp[splitIDXVal] = new double[dist.length];
                    pmpi[splitIDXVal] = new int[idxs.length];
                }
                pmp[splitIDXVal][j] = dist[j];
                pmpi[splitIDXVal][j] = idxs[j];
            }
            int j = splitIDXVal;
            while (j < splitIDX.length && idx[j] != j) {
                idx[j] = splitIDXVal;
                j++;
            }
        }
        return new PanMatrixProfile(pmp, pmpi);
    }

    public static PanMatrixProfile of(double[] ts, int[] windows, boolean crossCorrelation) {
        var skimp = new SKIMP<>(ts.length, crossCorrelation, windows);
        Arrays.stream(ts)
            .forEach(skimp::update);
        return skimp.get();
    }

    public static <S extends WindowStatistic, M extends MatrixProfile> PanMatrixProfile of(
        double[] ts, int[] windows, MatrixProfileAlgorithm<S, M>[] algos) {

        var skimp = new SKIMP<>(windows, algos);
        Arrays.stream(ts)
            .forEach(skimp::update);
        return skimp.get();
    }

    private static int[] binarySplit(int n) {
        List<Integer> indexList = new ArrayList<>();
        List<int[]> intervals = new ArrayList<>();
        // Always begin by exploring the first integer
        indexList.add(0);

        if (n < 2) {
            return convertListToArray(indexList);
        }

        // After exploring the first integer, split interval 2:n
        intervals.add(new int[]{1, n - 1});

        while (!intervals.isEmpty()) {
            int[] interval = intervals.remove(0);
            int lowerBound = interval[0];
            int upperBound = interval[1];
            int middle = (lowerBound + upperBound) / 2;
            indexList.add(middle);

            if (lowerBound != upperBound) {
                int[][] splitResults = split(lowerBound, upperBound, middle);
                if (splitResults[0] != null) {
                    intervals.add(splitResults[0]);
                }
                if (splitResults[1] != null) {
                    intervals.add(splitResults[1]);
                }
            }
        }

        return convertListToArray(indexList);
    }

    private static int[][] split(int lowerBound, int upperBound, int middle) {
        int[][] result = new int[2][];
        if (lowerBound == middle) {
            result[0] = null;
            result[1] = new int[]{middle + 1, upperBound};
        } else if (upperBound == middle) {
            result[0] = new int[]{lowerBound, middle - 1};
            result[1] = null;
        } else {
            result[0] = new int[]{lowerBound, middle - 1};
            result[1] = new int[]{middle + 1, upperBound};
        }
        return result;
    }

    private static int[] convertListToArray(List<Integer> list) {
        return list.stream().mapToInt(i -> i).toArray();
    }
}
