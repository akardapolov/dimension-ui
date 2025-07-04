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

import java.util.Arrays;

public interface OnlineMatrixProfile extends MatrixProfile {

    int offset();

    public static OnlineMatrixProfile extend(OnlineMatrixProfile profile, int newPoints) {
        var newSize = profile.profile().length + newPoints;
        double[] newProfile = Arrays.copyOf(profile.profile(), newSize);
        int[] newIndexes = Arrays.copyOf(profile.indexes(), newSize);
        double[] newRightProfile = Arrays.copyOf(profile.rightProfile(), newSize);
        double[] newLeftProfile = Arrays.copyOf(profile.leftProfile(), newSize);
        int[] newRightIndexes = Arrays.copyOf(profile.rightIndexes(), newSize);
        int[] newLeftIndexes = Arrays.copyOf(profile.leftIndexes(), newSize);
        for (int i = profile.profile().length; i < newProfile.length; i++) {
            newProfile[i] = Double.POSITIVE_INFINITY;
            newIndexes[i] = -1;
            newRightProfile[i] = Double.POSITIVE_INFINITY;
            newLeftProfile[i] = Double.POSITIVE_INFINITY;
            newRightIndexes[i] = -1;
            newLeftIndexes[i] = -1;
        }
        return new BaseOnlineMatrixProfile(
            profile.offset(),
            profile.windowSize(),
            profile.exclusionZone(),
            newProfile,
            newIndexes,
            newLeftProfile,
            newRightProfile,
            newLeftIndexes,
            newRightIndexes
        );
    }

    public static OnlineMatrixProfile offset(OnlineMatrixProfile profile, int offset) {
        var size = profile.profile().length;
        double[] newProfile = Arrays.copyOfRange(profile.profile(), offset, size);
        int[] newIndexes = Arrays.copyOfRange(profile.indexes(), offset, size);
        double[] newRightProfile = Arrays.copyOfRange(profile.rightProfile(), offset, size);
        double[] newLeftProfile = Arrays.copyOfRange(profile.leftProfile(), offset, size);
        int[] newRightIndexes = Arrays.copyOfRange(profile.rightIndexes(), offset, size);
        int[] newLeftIndexes = Arrays.copyOfRange(profile.leftIndexes(), offset, size);
        for (int i = 0; i < newIndexes.length; i++) {
            newIndexes[i] -= offset;
            newRightIndexes[i] -= offset;
            newLeftIndexes[i] -= offset;
        }
        return new BaseOnlineMatrixProfile(
            profile.offset() + offset,
            profile.windowSize(),
            profile.exclusionZone(),
            newProfile,
            newIndexes,
            newLeftProfile,
            newRightProfile,
            newLeftIndexes,
            newRightIndexes
        );
    }
}
