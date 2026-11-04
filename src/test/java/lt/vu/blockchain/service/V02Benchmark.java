package lt.vu.blockchain.service;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import lt.vu.blockchain.model.Transaction;
import lt.vu.blockchain.model.User;

public final class V02Benchmark {

  private static final int WARMUP_RUNS = 3;
  private static final int MEASURED_RUNS = 5;
  private static final int CANDIDATES_PER_HEIGHT = 5;
  private static final int HEIGHT_REPEATS = 20;
  private static final int SELECTIONS_PER_RUN = CANDIDATES_PER_HEIGHT * HEIGHT_REPEATS;
  private static final int MERKLE_REPEATS = 200;
  private static volatile long checksum;
  private static volatile int selectedCount;

  private V02Benchmark() {}

  public static void main(String[] args) {
    DataGenerator generator = new DataGenerator(42L);
    List<User> users = generator.generateUsers();
    List<Transaction> transactions = generator.generateTransactions(users);
    Blockchain blockchain = new Blockchain(users);
    TransactionPoolV02Baseline baseline =
        new TransactionPoolV02Baseline(blockchain, transactions, 42L);
    TransactionPool optimized = new TransactionPool(blockchain, transactions, 42L);

    List<String> merkleIds = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      merkleIds.add(transactions.get(i).transactionId());
    }
    if (!MerkleTreeV02Baseline.calculateRoot(merkleIds)
        .equals(MerkleTree.calculateRoot(merkleIds))) {
      throw new IllegalStateException("Merkle roots differ");
    }

    for (int i = 0; i < SELECTIONS_PER_RUN; i++) {
      if (!baseline.selectTransactions().equals(optimized.selectTransactions())) {
        throw new IllegalStateException("Selected transactions differ at selection " + i);
      }
    }

    for (int i = 0; i < WARMUP_RUNS; i++) {
      timeSelection(baseline);
      timeSelection(optimized);
      timeMerkle(merkleIds, true);
      timeMerkle(merkleIds, false);
    }

    double[] baselineSelectionTimes = new double[MEASURED_RUNS];
    double[] optimizedSelectionTimes = new double[MEASURED_RUNS];
    double[] baselineMerkleTimes = new double[MEASURED_RUNS];
    double[] currentMerkleTimes = new double[MEASURED_RUNS];
    int transactionsPerRun = 0;
    for (int i = 0; i < MEASURED_RUNS; i++) {
      long baselineChecksum;
      int baselineCount;
      long optimizedChecksum;
      int optimizedCount;
      if (i % 2 == 0) {
        baselineSelectionTimes[i] = timeSelection(baseline);
        baselineChecksum = checksum;
        baselineCount = selectedCount;
        optimizedSelectionTimes[i] = timeSelection(optimized);
        optimizedChecksum = checksum;
        optimizedCount = selectedCount;
      } else {
        optimizedSelectionTimes[i] = timeSelection(optimized);
        optimizedChecksum = checksum;
        optimizedCount = selectedCount;
        baselineSelectionTimes[i] = timeSelection(baseline);
        baselineChecksum = checksum;
        baselineCount = selectedCount;
      }
      if (baselineChecksum != optimizedChecksum || baselineCount != optimizedCount) {
        throw new IllegalStateException("Selection work differs in measured run " + i);
      }
      transactionsPerRun = baselineCount;
      if (i % 2 == 0) {
        baselineMerkleTimes[i] = timeMerkle(merkleIds, true);
        currentMerkleTimes[i] = timeMerkle(merkleIds, false);
      } else {
        currentMerkleTimes[i] = timeMerkle(merkleIds, false);
        baselineMerkleTimes[i] = timeMerkle(merkleIds, true);
      }
    }

    if (baseline.pendingTransactions().size() != transactions.size()
        || optimized.pendingTransactions().size() != transactions.size()) {
      throw new IllegalStateException("Selection changed the pending pool");
    }

    System.out.println(
        "Transaction selection: "
            + transactions.size()
            + " pending, "
            + SELECTIONS_PER_RUN
            + " candidate selections, "
            + transactionsPerRun
            + " selected per run");
    double baselineSelectionAverage =
        printTimes("TransactionPool v0.2 (ms per run)", baselineSelectionTimes);
    double optimizedSelectionAverage =
        printTimes("TransactionPool v0.3 (ms per run)", optimizedSelectionTimes);
    System.out.printf(
        "TransactionPool time reduction: %.1f%%%n",
        100 * (baselineSelectionAverage - optimizedSelectionAverage) / baselineSelectionAverage);
    System.out.println("Merkle: 100 fixed IDs, " + MERKLE_REPEATS + " roots per run");
    double baselineMerkleAverage = printTimes("MerkleTree v0.2 (ms per root)", baselineMerkleTimes);
    double currentMerkleAverage =
        printTimes("MerkleTree current (ms per root)", currentMerkleTimes);
    System.out.printf(
        "MerkleTree time reduction: %.1f%%%n",
        100 * (baselineMerkleAverage - currentMerkleAverage) / baselineMerkleAverage);
    printAllocations(baseline, optimized, merkleIds);
    System.out.println("Checksum: " + checksum);
  }

  private static void printAllocations(
      TransactionPoolV02Baseline baseline, TransactionPool optimized, List<String> merkleIds) {
    if (!(ManagementFactory.getThreadMXBean() instanceof ThreadMXBean bean)
        || !bean.isThreadAllocatedMemorySupported()) {
      System.out.println("Thread allocation measurement unavailable on this JVM");
      return;
    }
    if (!bean.isThreadAllocatedMemoryEnabled()) {
      bean.setThreadAllocatedMemoryEnabled(true);
    }
    long threadId = Thread.currentThread().threadId();
    long start = bean.getThreadAllocatedBytes(threadId);
    timeSelection(baseline);
    long baselineBytes = bean.getThreadAllocatedBytes(threadId) - start;
    start = bean.getThreadAllocatedBytes(threadId);
    timeSelection(optimized);
    long optimizedBytes = bean.getThreadAllocatedBytes(threadId) - start;
    System.out.println(
        "Allocated bytes per "
            + SELECTIONS_PER_RUN
            + " selections: v0.2 "
            + baselineBytes
            + ", v0.3 "
            + optimizedBytes);

    start = bean.getThreadAllocatedBytes(threadId);
    timeMerkle(merkleIds, true);
    long baselineMerkleBytes = bean.getThreadAllocatedBytes(threadId) - start;
    start = bean.getThreadAllocatedBytes(threadId);
    timeMerkle(merkleIds, false);
    long currentMerkleBytes = bean.getThreadAllocatedBytes(threadId) - start;
    System.out.println(
        "Allocated bytes per "
            + MERKLE_REPEATS
            + " Merkle roots: v0.2 "
            + baselineMerkleBytes
            + ", v0.3 "
            + currentMerkleBytes);
  }

  private static double timeSelection(TransactionPoolV02Baseline pool) {
    long value = 0;
    int count = 0;
    long start = System.nanoTime();
    for (int i = 0; i < SELECTIONS_PER_RUN; i++) {
      List<Transaction> selected = pool.selectTransactions();
      count += selected.size();
      for (Transaction transaction : selected) {
        value = 31 * value + transaction.transactionId().hashCode();
      }
    }
    long elapsed = System.nanoTime() - start;
    checksum = value;
    selectedCount = count;
    return elapsed / 1_000_000.0;
  }

  private static double timeSelection(TransactionPool pool) {
    long value = 0;
    int count = 0;
    long start = System.nanoTime();
    for (int i = 0; i < SELECTIONS_PER_RUN; i++) {
      List<Transaction> selected = pool.selectTransactions();
      count += selected.size();
      for (Transaction transaction : selected) {
        value = 31 * value + transaction.transactionId().hashCode();
      }
    }
    long elapsed = System.nanoTime() - start;
    checksum = value;
    selectedCount = count;
    return elapsed / 1_000_000.0;
  }

  private static double timeMerkle(List<String> ids, boolean baseline) {
    long value = 0;
    long start = System.nanoTime();
    for (int i = 0; i < MERKLE_REPEATS; i++) {
      String root =
          baseline ? MerkleTreeV02Baseline.calculateRoot(ids) : MerkleTree.calculateRoot(ids);
      value = 31 * value + root.hashCode();
    }
    long elapsed = System.nanoTime() - start;
    checksum = value;
    return elapsed / 1_000_000.0 / MERKLE_REPEATS;
  }

  private static double printTimes(String label, double[] times) {
    double sum = 0;
    for (int i = 0; i < times.length; i++) {
      System.out.printf("%s run %d: %.3f ms%n", label, i + 1, times[i]);
      sum += times[i];
    }
    double average = sum / times.length;
    double squaredDifferences = 0;
    for (double time : times) {
      double difference = time - average;
      squaredDifferences += difference * difference;
    }
    System.out.printf(
        "%s average: %.3f ms, standard deviation: %.3f ms%n",
        label, average, Math.sqrt(squaredDifferences / times.length));
    return average;
  }
}
