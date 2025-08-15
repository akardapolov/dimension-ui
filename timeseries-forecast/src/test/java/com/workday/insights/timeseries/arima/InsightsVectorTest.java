/*
 * Copyright (c) 2017-present, Workday, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root repository.
 */

package com.workday.insights.timeseries.arima;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.workday.insights.matrix.InsightsVector;
import org.junit.jupiter.api.Test;

public class InsightsVectorTest {

  @Test
  public void constructorTests() {
    InsightsVector iv = new InsightsVector(10, 3.0);

    assertEquals(10, iv.size());
    for (int i = 0; i < iv.size(); i++) {
      assertEquals(3.0, iv.get(i));
    }

    double[] data = {3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0};
    InsightsVector iv3 = new InsightsVector(data, false);
    for (int i = 0; i < iv3.size(); i++) {
      assertEquals(3.0, iv3.get(i));
    }
  }

  @Test
  public void dotOperationTest() {
    InsightsVector rowVec = new InsightsVector(3, 1.1);
    InsightsVector colVec = new InsightsVector(3, 2.2);

    double expect = 1.1 * 2.2 * 3;
    double actual = rowVec.dot(colVec);
    assertEquals(expect, actual, 0);
  }

  @Test
  public void dotOperationExceptionTest() {
    InsightsVector rowVec = new InsightsVector(4, 1);
    InsightsVector colVec = new InsightsVector(3, 2);

    assertThrows(RuntimeException.class, () -> {
      rowVec.dot(colVec);
    });
  }
}