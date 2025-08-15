/*
 * Copyright (c) 2017-present, Workday, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root repository.
 */

package com.workday.insights.timeseries.arima;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.workday.insights.matrix.InsightsMatrix;
import com.workday.insights.matrix.InsightsVector;
import org.junit.jupiter.api.Test;

public class InsightsMatrixTest {

  @Test
  public void constructorTests() {
    double[][] data = {{3.0, 3.0, 3.0},
        {3.0, 3.0, 3.0},
        {3.0, 3.0, 3.0}};

    InsightsMatrix im1 = new InsightsMatrix(data, false);
    assertEquals(3, im1.getNumberOfColumns(), "Column count mismatch");
    assertEquals(3, im1.getNumberOfRows(), "Row count mismatch");

    for (int i = 0; i < im1.getNumberOfColumns(); i++) {
      for (int j = 0; j < im1.getNumberOfColumns(); j++) {
        assertEquals(3.0, im1.get(i, j), "Matrix value mismatch at (" + i + "," + j + ")");
      }
    }

    im1.set(0, 0, 0.0);
    assertEquals(0.0, im1.get(0, 0), "Value after set mismatch");
    im1.set(0, 0, 3.0);

    InsightsVector iv = new InsightsVector(3, 3.0);
    InsightsVector result = im1.timesVector(iv);
    for (int i = 0; i < result.size(); i++) {
      assertEquals(27.0, result.get(i), "Vector multiplication result mismatch at index " + i);
    }
  }

  @Test
  public void solverTestSimple() {
    double[][] A = {{2.0}};
    double[] B = {4.0};
    double[] solution = {2.0};

    InsightsMatrix im = new InsightsMatrix(A, true);
    InsightsVector iv = new InsightsVector(B, true);

    InsightsVector solved = im.solveSPDIntoVector(iv, -1);
    assertEquals(solution.length, solved.size(), "Solution vector length mismatch");
    for (int i = 0; i < solution.length; i++) {
      assertEquals(solution[i], solved.get(i), 1e-6, "Solution mismatch at index " + i);
    }
  }

  @Test
  public void solverTestOneSolution() {
    double[][] A = {{1.0, 1.0},
        {1.0, 2.0}};

    double[] B = {2.0, 16.0};
    double[] solution = {-12.0, 14.0};

    InsightsMatrix im = new InsightsMatrix(A, true);
    InsightsVector iv = new InsightsVector(B, true);

    InsightsVector solved = im.solveSPDIntoVector(iv, -1);
    assertEquals(solution.length, solved.size(), "Solution vector length mismatch");
    for (int i = 0; i < solution.length; i++) {
      assertEquals(solution[i], solved.get(i), 1e-6, "Solution mismatch at index " + i);
    }
  }

  @Test
  public void timesVectorTestSimple() {
    double[][] A = {{1.0, 1.0},
        {2.0, 2.0}};

    double[] x = {3.0, 4.0};
    double[] solution = {7.0, 14.0};

    InsightsMatrix im = new InsightsMatrix(A, true);
    InsightsVector iv = new InsightsVector(x, true);

    InsightsVector solved = im.timesVector(iv);
    assertEquals(solution.length, solved.size(), "Result vector length mismatch");
    for (int i = 0; i < solution.length; i++) {
      assertEquals(solution[i], solved.get(i), 1e-6, "Vector multiplication result mismatch at index " + i);
    }
  }

  @Test
  public void timesVectorTestIncorrectDimension() {
    double[][] A = {{1.0, 1.0, 1.0},
        {2.0, 2.0, 2.0},
        {3.0, 3.0, 3.0}};

    double[] x = {4.0, 4.0, 4.0, 4.0};

    InsightsMatrix im = new InsightsMatrix(A, true);
    InsightsVector iv = new InsightsVector(x, true);

    assertThrows(RuntimeException.class, () -> im.timesVector(iv),
                 "Expected RuntimeException for dimension mismatch");
  }
}