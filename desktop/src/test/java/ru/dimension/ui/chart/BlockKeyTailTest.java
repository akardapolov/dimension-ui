package ru.dimension.ui.chart;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;
import java.util.ArrayList;
import ru.dimension.db.model.output.BlockKeyTail;
public class BlockKeyTailTest {

  private Deque<BlockKeyTail> blockKeyTailsDeque = new ArrayDeque<BlockKeyTail>();

  // Assume these operations are sequentially dependent
  public void iterateWithBatchNavigation(int batchSize) {
    List<BlockKeyTail> prevBatch = null;
    List<BlockKeyTail> nextBatch = null;

    int totalElements = blockKeyTailsDeque.size();
    for (int start = 0; start < totalElements; start += batchSize) {
      List<BlockKeyTail> currentBatch = new ArrayList<>();

      // Get current batch
      for (int i = 0; i < batchSize && !blockKeyTailsDeque.isEmpty(); i++) {
        currentBatch.add(blockKeyTailsDeque.pollFirst());
      }
      System.out.println(currentBatch.size());

      // Process the current batch
      processBatch(currentBatch, prevBatch);

      // Save the processed current batch
      prevBatch = new ArrayList<>(currentBatch);

      // Prepare for potential access to the next batch
      if (start + batchSize < totalElements) {
        System.out.println("Next batch would start from index: " + (start + batchSize));

        nextBatch = new ArrayList<>();
        int peekLimit = 0;
        for (BlockKeyTail blockKeyTail : blockKeyTailsDeque) {
          if (peekLimit < batchSize) {
            nextBatch.add(blockKeyTail);
            peekLimit++;
          } else {
            break;
          }
        }

        System.out.println(nextBatch);
      }
    }
  }

  private void processBatch(List<BlockKeyTail> currentBatch, List<BlockKeyTail> previousBatch) {
    // Processing logic for a batch
    if (previousBatch != null) {
      System.out.println("Accessing previous batch:");
      for (BlockKeyTail blockKeyTail : previousBatch) {
        System.out.println("Previous - Key: " + blockKeyTail.getKey());
      }
    }
    System.out.println("Processing current batch:");
    for (BlockKeyTail blockKeyTail : currentBatch) {
      System.out.println("Current - Key: " + blockKeyTail.getKey());
    }
  }

  public static void main(String[] args) {
    BlockKeyTailTest example = new BlockKeyTailTest();

    // Add elements to the deque
    example.getBlockKeyTailsDeque().add(new BlockKeyTail(1, 100));
    example.getBlockKeyTailsDeque().add(new BlockKeyTail(2, 200));
    example.getBlockKeyTailsDeque().add(new BlockKeyTail(3, 300));
    example.getBlockKeyTailsDeque().add(new BlockKeyTail(4, 400));
    example.getBlockKeyTailsDeque().add(new BlockKeyTail(5, 500));

    // Process elements in batches with navigation
    example.iterateWithBatchNavigation(2);
  }

  public Deque<BlockKeyTail> getBlockKeyTailsDeque() {
    return blockKeyTailsDeque;
  }
}



