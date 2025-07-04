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

import static com.github.eugene.kamenev.tsmp4j.utils.Util.calMpDist;

import com.github.eugene.kamenev.tsmp4j.algo.mp.BaseMatrixProfile;
import com.github.eugene.kamenev.tsmp4j.algo.mp.BaseMatrixProfileAlgorithm;
import com.github.eugene.kamenev.tsmp4j.algo.mp.DistanceProfileFunction;
import com.github.eugene.kamenev.tsmp4j.stats.RollingWindowStatistics;
import com.github.eugene.kamenev.tsmp4j.stats.WindowStatistic;
import java.util.Arrays;

/**
 * Fast implementation of MatrixProfile and MatrixProfileIndex for internal purposes, without FFT
 * Reference: <a
 * href="https://github.com/matrix-profile-foundation/tsmp/blob/master/R/mpx.R">mpx.R</a>
 */
public class MPX extends BaseMatrixProfileAlgorithm<MPXStatistic, BaseMatrixProfile> implements
    DistanceProfileFunction<MPXStatistic> {

    private final boolean crossCorrelation;

    private final double threshold = 0.05;

    public MPX(int windowSize, int bufferSize, boolean crossCorrelation, double exclusionZone) {
        this(new MPXRollingWindowStatistics(windowSize, bufferSize), exclusionZone,
            crossCorrelation);
    }

    public MPX(int windowSize, int bufferSize, boolean crossCorrelation) {
        this(new MPXRollingWindowStatistics(windowSize, bufferSize), 0.5, crossCorrelation);
    }

    public MPX(RollingWindowStatistics<MPXStatistic> rollingWindowStatistics) {
        this(rollingWindowStatistics, 0.5d, false);
    }

    public MPX() {
        this(null, 0, false);
    }

    public MPX(RollingWindowStatistics<MPXStatistic> rollingWindowStatistics,
        double exclusionZone, boolean crossCorrelation) {
        super(rollingWindowStatistics, exclusionZone);
        this.crossCorrelation = crossCorrelation;
    }

    @Override
    public BaseMatrixProfile get(double[] query) {
        if (this.isReady()) {
            var qs = new MPXRollingWindowStatistics(this.rollingStatistics().windowSize(),
                query.length);
            for (double v : query) {
                qs.apply(v);
            }
            return compute(this.rollingStatistics(), qs, crossCorrelation, this.exclusionZone);
        }
        return null;
    }

    @Override
    public BaseMatrixProfile get(RollingWindowStatistics<MPXStatistic> query) {
        if (isReady()) {
            return compute(this.rollingStatistics(), query, crossCorrelation, this.exclusionZone);
        }
        return null;
    }

    @Override
    public BaseMatrixProfile get() {
        if (this.isReady()) {
            var sb = ((MPXRollingWindowStatistics) this.rollingStatistics());

            int n = sb.dataSize();
            int w = sb.windowSize();
            int profile_len = n - w + 1;

            double[] mp = new double[profile_len];
            int[] mpi = new int[profile_len];
            var mean_0 = sb.mean(0);
            for (int diag = exclusionZoneSize; diag < profile_len; diag++) {
                var c = 0.0;
                var mean_diag = sb.mean(diag);
                for (var k = 0; k < w; k++) {
                    c += (sb.x(diag + k) - mean_diag) * (sb.x(k) - mean_0);
                }

                for (var offset = 0; offset < n - w - diag + 1; offset++) {
                    var col = offset + diag;
                    c = c + sb.df(offset) * sb.dg(col) + sb.df(col) * sb.dg(offset);
                    var c_cmp = c * sb.stdDev(offset) * sb.stdDev(col);

                    if (c_cmp > mp[offset]) {
                        mp[offset] = c_cmp;
                        mpi[offset] = col;
                    }

                    if (c_cmp > mp[col]) {
                        mp[col] = Math.min(c_cmp, 1.0);
                        mpi[col] = offset;
                    }
                }
            }

            if (!this.crossCorrelation) {
                var win = 2.0d * w;
                for (var i = 0; i < profile_len; i++) {
                    mp[i] = Math.sqrt(win * (1.0 - mp[i]));
                }
            }
            return new BaseMatrixProfile(sb.windowSize(), exclusionZone, mp, mpi,
                null, null, null, null);
        }
        return null;
    }

    @Override
    public DistanceProfile apply(DistanceProfileQuery<MPXStatistic> dsq) {
        var d = dsq.data().dataSize() < dsq.query().dataSize() ? dsq.query() : dsq.data();
        var q = dsq.data() == d ? dsq.query() : dsq.data();
        var mpx = new MPX(d);
        var mp = mpx.get(q);
        var merged = new double[mp.profile().length + mp.leftProfile().length];
        System.arraycopy(mp.leftProfile(), 0, merged, 0, mp.leftProfile().length);
        System.arraycopy(mp.profile(), 0, merged, mp.leftProfile().length, mp.profile().length);
        return new DistanceProfile(new double[]{
            calMpDist(merged, this.threshold, d.dataSize() + q.dataSize())
        });
    }

    public static BaseMatrixProfile of(double[] ts, int windowSize, boolean crossCorrelation) {
        var mpx = new MPX(windowSize, ts.length, crossCorrelation, 0.5d);
        Arrays.stream(ts)
            .forEach(mpx::update);
        return mpx.get();
    }

    public static BaseMatrixProfile of(double[] ts, int windowSize) {
        var mpx = new MPX(windowSize, ts.length, false, 0.5d);
        Arrays.stream(ts)
            .forEach(mpx::update);
        return mpx.get();
    }

    private static BaseMatrixProfile compute(RollingWindowStatistics<MPXStatistic> ts,
        RollingWindowStatistics<MPXStatistic> qs, boolean crossCorrelation, double exclusionZone) {

        int n = ts.dataSize();
        int qn = qs.dataSize();
        int w = ts.windowSize();

        int profile_len = n - w + 1;
        int profile_lenb = qn - w + 1;

        double[] mp = new double[profile_len];
        int[] mpi = new int[profile_len];
        double[] mpb = new double[profile_lenb];
        int[] mpib = new int[profile_lenb];
        for (int i = 0; i < profile_len; i++) {
            mp[i] = -1.0;
            if (i < profile_lenb) {
                mpb[i] = -1;
            }
        }

        // AB Join
        computeJoin(ts, qs, mp, mpi, mpb, mpib, w);
        // BA Join
        computeJoin(qs, ts, mpb, mpib, mp, mpi, w);

        postProcess(mp, w, crossCorrelation);
        postProcess(mpb, w, crossCorrelation);

        return new BaseMatrixProfile(w, exclusionZone, mp, mpi, null, mpb, null, mpib);
    }

    private static <S extends WindowStatistic> void computeJoin(RollingWindowStatistics<S> a,
        RollingWindowStatistics<S> b,
        double[] mp, int[] mpi, double[] mpb, int[] mpib, int w) {
        int amx = a.getStatsBuffer().getLength() - w + 1;
        int bmx = b.getStatsBuffer().getLength() - w + 1;

        var sa = (MPXRollingWindowStatistics) a;
        var sb = (MPXRollingWindowStatistics) b;
        double b_mean_0 = b.mean(0);
        for (int ia = 0; ia < amx; ia++) {
            int mx = Math.min(amx - ia, bmx);
            double c = 0;
            double mean_ia = a.mean(ia);

            for (int i = 0; i < w; i++) {
                c += (a.x(ia + i) - mean_ia) * (b.x(i) - b_mean_0);
            }

            for (int ib = 0; ib < mx; ib++) {
                c += sa.df(ib + ia) * sb.dg(ib) + sa.dg(ib + ia) * sb.df(ib);
                double c_cmp = c * a.stdDev(ib + ia) * b.stdDev(ib);

                if (c_cmp > mp[ib + ia]) {
                    mp[ib + ia] = c_cmp;
                    mpi[ib + ia] = ib;
                }

                if (c_cmp > mpb[ib]) {
                    mpb[ib] = c_cmp;
                    mpib[ib] = ia + ib;
                }
            }
        }
    }

    private static void postProcess(double[] mp, int w, boolean crossCorrelation) {
        int len = mp.length;
        for (int i = 0; i < len; i++) {
            if (!crossCorrelation) {
                if (mp[i] == -1.0) {
                    mp[i] = Double.POSITIVE_INFINITY;
                } else {
                    var dist = 2.0 * w * (1.0 - mp[i]);
                    if (dist <= 0) {
                        mp[i] = 0;
                    } else {
                        mp[i] = Math.sqrt(dist);
                    }
                }
            } else {
                if (mp[i] > 1.0) {
                    mp[i] = 1.0;
                }
            }
        }
    }
}
