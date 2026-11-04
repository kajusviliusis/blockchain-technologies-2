package lt.vu.blockchain.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lt.vu.blockchain.hashing.HashFunctionV012;
import lt.vu.blockchain.model.BlockHeader;
import lt.vu.blockchain.model.MiningResult;

public final class V02Benchmark {

  private static final int WARMUP_RUNS = 3;
  private static final int MEASURED_RUNS = 5;
  private static final long MINING_START_NONCE = 5_000;
  private static final int MINING_ATTEMPTS = 5_000;
  private static final int MERKLE_REPEATS = 50;
  private static volatile int checksum;

  private V02Benchmark() {}

  public static void main(String[] args) {
    HashFunctionV012 hashFunction = new HashFunctionV012();
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      ids.add(hashFunction.hash(("transaction-" + i).getBytes(StandardCharsets.UTF_8)));
    }

    String root = MerkleTreeV02Baseline.calculateRoot(ids);
    if (!root.equals(MerkleTree.calculateRoot(ids))) {
      throw new IllegalStateException("Merkle baseline differs from production");
    }

    BlockHeader header = new BlockHeader("0".repeat(64), 0, 1, root, 0, 3);
    MinerV02Baseline miner = new MinerV02Baseline();
    MiningResult baselineResult = miner.mine(header, MINING_START_NONCE, MINING_ATTEMPTS);
    MiningResult currentResult = new Miner().mine(header, MINING_START_NONCE, MINING_ATTEMPTS);
    if (!baselineResult.header().equals(currentResult.header())
        || !baselineResult.hash().equals(currentResult.hash())
        || baselineResult.attempts() != currentResult.attempts()
        || baselineResult.found() != currentResult.found()) {
      throw new IllegalStateException("Mining baseline differs from production");
    }

    for (int i = 0; i < WARMUP_RUNS; i++) {
      timeMining(miner, header);
      timeMerkle(ids);
    }

    double[] miningTimes = new double[MEASURED_RUNS];
    double[] merkleTimes = new double[MEASURED_RUNS];
    for (int i = 0; i < MEASURED_RUNS; i++) {
      miningTimes[i] = timeMining(miner, header);
      merkleTimes[i] = timeMerkle(ids);
    }

    System.out.println(
        "Mining: difficulty 3, nonce "
            + MINING_START_NONCE
            + "-"
            + (MINING_START_NONCE + MINING_ATTEMPTS - 1)
            + ", "
            + baselineResult.attempts()
            + " attempts per run");
    printTimes("Mining (ms per run)", miningTimes);
    System.out.println("Merkle: 100 fixed IDs, " + MERKLE_REPEATS + " roots per run");
    printTimes("Merkle (ms per root)", merkleTimes);
    System.out.println("Checksum: " + checksum);
  }

  private static double timeMining(MinerV02Baseline miner, BlockHeader header) {
    long start = System.nanoTime();
    MiningResult result = miner.mine(header, MINING_START_NONCE, MINING_ATTEMPTS);
    long elapsed = System.nanoTime() - start;
    checksum = result.hash().hashCode();
    return elapsed / 1_000_000.0;
  }

  private static double timeMerkle(List<String> ids) {
    int value = 0;
    long start = System.nanoTime();
    for (int i = 0; i < MERKLE_REPEATS; i++) {
      value += MerkleTreeV02Baseline.calculateRoot(ids).hashCode();
    }
    long elapsed = System.nanoTime() - start;
    checksum = value;
    return elapsed / 1_000_000.0 / MERKLE_REPEATS;
  }

  private static void printTimes(String label, double[] times) {
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
    double standardDeviation = Math.sqrt(squaredDifferences / times.length);
    System.out.printf(
        "%s average: %.3f ms, standard deviation: %.3f ms%n", label, average, standardDeviation);
  }
}
