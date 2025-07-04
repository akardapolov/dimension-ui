package com.github.eugene.kamenev.tsmp4j.mp.stamp;

import com.github.eugene.kamenev.tsmp4j.BaseSpecTest;
import com.github.eugene.kamenev.tsmp4j.algo.mp.BaseMatrixProfile;
import com.github.eugene.kamenev.tsmp4j.algo.mp.MatrixProfile;
import com.github.eugene.kamenev.tsmp4j.algo.mp.stamp.STAMP;
import com.github.eugene.kamenev.tsmp4j.stats.BaseRollingWindowStatistics;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class STAMPSpecTest extends BaseSpecTest {

  double delta = 1e-10;

  @Test
  public void stampSelfJoinTest() throws IOException, URISyntaxException {
    int limit = 200;
    int windowSize = 30;
    MatrixProfile check = loadCheck("stamp_self_join.csv", false);

    STAMP stamp = new STAMP(windowSize, limit);
    data.stream()
        .mapToDouble(ToyData::x)
        .limit(limit)
        .forEach(stamp::update);

    MatrixProfile mp = stamp.get();
    assertArrayEquals(check.profile(), mp.profile(), delta);
    assertArrayEquals(check.profile(), mp.profile(), delta);
    assertArrayEquals(check.rightIndexes(), mp.rightIndexes());
    assertArrayEquals(check.rightProfile(), mp.rightProfile(), delta);
    assertArrayEquals(check.leftProfile(), mp.leftProfile(), delta);
    assertArrayEquals(check.leftIndexes(), mp.leftIndexes());
  }

  @Test
  public void testStampAbJoin() throws IOException, URISyntaxException {
    int limit = 200;
    int windowSize = 30;
    MatrixProfile check = loadCheck("stamp_ab_join.csv", true);

    STAMP stamp = new STAMP(windowSize, limit);
    BaseRollingWindowStatistics query = new BaseRollingWindowStatistics(windowSize, 60);

    data.stream()
        .mapToDouble(ToyData::x)
        .limit(limit)
        .forEach(stamp::update);

    data.stream()
        .mapToDouble(ToyData::y)
        .limit(60)
        .forEach(query::apply);

    MatrixProfile mp = stamp.get(query);
    assertArrayEquals(check.profile(), mp.profile(), delta);
    assertArrayEquals(check.indexes(), mp.indexes());
  }

  @Test
  public void testStampWithNaNValuesInData() throws IOException, URISyntaxException {
    int limit = 200;
    int windowSize = 30;
    MatrixProfile check = loadCheck("stamp_self_join_nan.csv", false);

    double[] ts = data.stream()
        .mapToDouble(ToyData::x)
        .limit(limit)
        .toArray();

    ts[100] = Double.NaN;
    STAMP stamp = new STAMP(windowSize, limit);
    Arrays.stream(ts).forEach(stamp::update);
    MatrixProfile mp = stamp.get();
    assertArrayEquals(check.profile(), mp.profile(), delta);
    assertArrayEquals(check.profile(), mp.profile(), delta);
    assertArrayEquals(check.rightIndexes(), mp.rightIndexes());
    assertArrayEquals(check.rightProfile(), mp.rightProfile(), delta);
    assertArrayEquals(check.leftProfile(), mp.leftProfile(), delta);
    assertArrayEquals(check.leftIndexes(), mp.leftIndexes());
  }

  protected MatrixProfile loadCheck(String fileName,
                                  boolean partial) throws IOException, URISyntaxException {
    return loadData(fileName, dataArray -> {
      double[] mp = new double[dataArray.length];
      double[] lmp = partial ? null : new double[dataArray.length];
      double[] rmp = partial ? null : new double[dataArray.length];
      int[] pi = new int[dataArray.length];
      int[] lpi = partial ? null : new int[dataArray.length];
      int[] rpi = partial ? null : new int[dataArray.length];

      for (int i = 0; i < dataArray.length; i++) {
        String[] row = dataArray[i];
        mp[i] = parseDouble(row[0]);
        pi[i] = parseInt(row[1]);
        pi[i] = pi[i] >= 0 ? pi[i] - 1 : -1;
        if (!partial) {
          lmp[i] = parseDouble(row[2]);
          lpi[i] = parseInt(row[3]);
          lpi[i] = lpi[i] >= 0 ? lpi[i] - 1 : -1;
          rmp[i] = parseDouble(row[4]);
          rpi[i] = parseInt(row[5]);
          rpi[i] = rpi[i] >= 0 ? rpi[i] - 1 : -1;
        }
      }

      return new BaseMatrixProfile(0, 0, mp, pi, rmp, lmp, rpi, lpi);
    });
  }
}
