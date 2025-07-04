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

package com.github.eugene.kamenev.tsmp4j.utils;

import com.github.eugene.kamenev.tsmp4j.stats.RollingWindowStatistics;
import java.util.Arrays;
import java.util.Random;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class Util {

    public static final double EPS = Math.sqrt(Math.ulp(1.0));

    public static final double KMODE = 0.6311142d;

    public static void shuffleArray(int[] array) {
        int index, temp;
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    public static double calMpDist(double[] mp, double thr, int data_size) {
        int k = ((int) Math.ceil(thr * data_size));
        Arrays.sort(mp);
        if (mp.length > k) {
            return mp[k - 1];
        } else {
            return mp[mp.length - 1];
        }
    }

    public static double maxValue(double[] arr) {
        double maxVal = Double.MIN_VALUE;
        for (double val : arr) {
            if (val > maxVal) {
                maxVal = val;
            }
        }
        return maxVal;
    }

    public static double sanitizeValue(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return value;
    }

    public static int padSize(int n) {
        return (int) Math.pow(2, Math.ceil(Math.log(n) / Math.log(2)));
    }

    public static int[] createRange(int start, int end, int step) {
        // Create the range of integers in the specified interval and step
        int length = (end - start) / step + 1;
        int[] range = new int[length];
        for (int i = 0; i < length; i++) {
            range[i] = start + i * step;
        }
        return range;
    }

    public static Complex[] forwardFft(
        RollingWindowStatistics<?> data,
        boolean isQuery, int skip, int padSize) {
        double[] padded = new double[padSize];
        int[] k = new int[1];
        if (isQuery) {
            data.getStatsBuffer()
                .toStreamReversed()
                .skip(skip)
                .limit(data.windowSize())
                .forEach(s -> padded[k[0]++] = s.x());
        } else {
            data.getStatsBuffer()
                .toStream()
                .forEach(s -> padded[k[0]++] = s.x());
        }
        var transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        return transformer.transform(padded, TransformType.FORWARD);
    }

    public static Complex[] inverseFft(Complex[] data) {
        var transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        return transformer.transform(data, TransformType.INVERSE);
    }
}
