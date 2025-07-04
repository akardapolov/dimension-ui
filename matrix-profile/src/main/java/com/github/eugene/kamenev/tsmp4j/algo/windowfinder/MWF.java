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

package com.github.eugene.kamenev.tsmp4j.algo.windowfinder;

import java.util.ArrayList;

/**
 * Multi-Window-Finder: Domain Agnostic Window Size for Time Series Data
 * Shima Imani, Alireza Abdoli, Ali Beyram, Azam Imani and Eamonn Keogh
 * https://kdd-milets.github.io/milets2021/papers/MiLeTS2021_paper_9.pdf
 * https://sites.google.com/view/multi-window-finder/
 */
public class MWF {

    public static double[] movmean(double[] ts, int w) {
        double[] moving_avg = new double[ts.length];
        double sum = 0;

        for (int i = 0; i < ts.length; i++) {
            sum += ts[i];
            if (i >= w) {
                sum -= ts[i - w];
            }
            moving_avg[i] = (i < w - 1) ? 0 : sum / w;
        }

        double[] result = new double[moving_avg.length - w + 1];
        if (ts.length - (w - 1) >= 0) {
            System.arraycopy(moving_avg, w - 1, result, 0, ts.length - (w - 1));
        }

        return result;
    }

    public static int mwf(double[] ts, int lbound, int ubound) {
        double[][] averages = new double[ubound - lbound][];
        int[] winSizes = new int[ubound - lbound];
        for (int w = lbound, i = 0; w < ubound; w++, i++) {
            averages[i] = movmean(ts, w);
            winSizes[i] = w;
        }

        double[] movingAvgResiduals = new double[winSizes.length];
        int maxLen = averages[averages.length - 1].length;
        for (int i = 0; i < winSizes.length; i++) {
            double[] moving_avg = averages[i];
            double avg_mean = 0;
            for (int j = 0; j < maxLen; j++) {
                avg_mean += moving_avg[j];
            }
            avg_mean /= maxLen;

            double residual = 0;
            for (int j = 0; j < maxLen; j++) {
                residual += Math.abs(moving_avg[j] - avg_mean);
            }
            movingAvgResiduals[i] = Math.log(residual);
        }

        ArrayList<Integer> minIndices = new ArrayList<>();
        for (int i = 1; i < movingAvgResiduals.length - 1; i++) {
            if (movingAvgResiduals[i] < movingAvgResiduals[i - 1] &&
                movingAvgResiduals[i] < movingAvgResiduals[i + 1]) {
                minIndices.add(i);
            }
        }

        if (minIndices.isEmpty()) {
            return -1;
        }
        if (minIndices.size() < 3) {
            return winSizes[minIndices.get(0)];
        }

        double[] reswin = new double[3];
        for (int i = 0; i < 3; i++) {
            reswin[i] = winSizes[minIndices.get(i)] / (double) (i + 1);
        }
        double w = (reswin[0] + reswin[1] + reswin[2]) / 3;

        return (int) w;
    }


}
