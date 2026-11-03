package lt.vu.blockchain.service;

import lt.vu.blockchain.hashing.HashFunctionV012;
import lt.vu.blockchain.model.BlockHeader;
import lt.vu.blockchain.model.MiningResult;

public final class Miner {

  private final HashFunctionV012 hashFunction = new HashFunctionV012();

  // Ribotas nonce intervalas neleidzia vienam kandidatui kasti be galo per viena raunda.
  public MiningResult mine(BlockHeader header, long startNonce, int maxAttempts) {
    if (header.difficulty() < 0 || header.difficulty() > 64) {
      throw new IllegalArgumentException("difficulty must be between 0 and 64");
    }
    if (startNonce < 0 || maxAttempts <= 0 || startNonce > Long.MAX_VALUE - (maxAttempts - 1L)) {
      throw new IllegalArgumentException("invalid nonce range or mining budget");
    }

    String requiredPrefix = "0".repeat(header.difficulty());
    long startTime = System.nanoTime();
    BlockHeader candidate = null;
    String hash = null;
    for (int attempt = 0; attempt < maxAttempts; attempt++) {
      long nonce = startNonce + attempt;
      candidate =
          new BlockHeader(
              header.prevHash(),
              header.timestamp(),
              header.version(),
              header.txRoot(),
              nonce,
              header.difficulty());
      hash = hashFunction.hash(candidate.serialize());

      if (hash.startsWith(requiredPrefix)) {
        long durationMillis = (System.nanoTime() - startTime) / 1_000_000;
        return new MiningResult(candidate, hash, attempt + 1L, durationMillis, true);
      }
    }
    long durationMillis = (System.nanoTime() - startTime) / 1_000_000;
    // Neradus tinkamo hash, graziname paskutini bandyma, kad veliau galetume testi.
    return new MiningResult(candidate, hash, maxAttempts, durationMillis, false);
  }
}
