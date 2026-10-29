package lt.vu.blockchain.service;

import lt.vu.blockchain.hashing.HashFunctionV012;
import lt.vu.blockchain.model.BlockHeader;
import lt.vu.blockchain.model.MiningResult;

public final class Miner {

  private final HashFunctionV012 hashFunction = new HashFunctionV012();

  public MiningResult mine(BlockHeader header) {
    if (header.difficulty() < 0 || header.difficulty() > 64) {
      throw new IllegalArgumentException("difficulty must be between 0 and 64");
    }

    String requiredPrefix = "0".repeat(header.difficulty());
    long startTime = System.nanoTime();
    long attempts = 0;
    long nonce = 0;

    while (true) {
      BlockHeader candidate =
          new BlockHeader(
              header.prevHash(),
              header.timestamp(),
              header.version(),
              header.txRoot(),
              nonce,
              header.difficulty());
      String hash = hashFunction.hash(candidate.serialize());
      attempts++;

      if (hash.startsWith(requiredPrefix)) {
        long durationMillis = (System.nanoTime() - startTime) / 1_000_000;
        return new MiningResult(candidate, hash, attempts, durationMillis);
      }
      if (nonce == Long.MAX_VALUE) {
        throw new IllegalStateException("nonce range exceeded");
      }
      nonce++;
    }
  }
}
