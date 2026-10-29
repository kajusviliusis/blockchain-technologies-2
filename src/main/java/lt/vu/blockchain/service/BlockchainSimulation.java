package lt.vu.blockchain.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lt.vu.blockchain.hashing.HashFunctionV012;
import lt.vu.blockchain.model.Block;
import lt.vu.blockchain.model.BlockHeader;
import lt.vu.blockchain.model.MiningResult;
import lt.vu.blockchain.model.Transaction;

public final class BlockchainSimulation {

  private final Blockchain blockchain;
  private final TransactionPool transactionPool;
  private final Miner miner = new Miner();
  private final HashFunctionV012 hashFunction = new HashFunctionV012();
  private final Map<Integer, MiningResult> miningResultsByHeight = new LinkedHashMap<>();

  public BlockchainSimulation(Blockchain blockchain, TransactionPool transactionPool) {
    this.blockchain = blockchain;
    this.transactionPool = transactionPool;
  }

  public void run() {
    while (true) {
      List<Transaction> selected = transactionPool.selectTransactions(blockchain);
      if (selected.isEmpty()) {
        return;
      }

      StringBuilder transactionIds = new StringBuilder();
      for (Transaction transaction : selected) {
        transactionIds.append(transaction.transactionId());
      }
      String txRoot = hashFunction.hash(transactionIds.toString().getBytes(StandardCharsets.UTF_8));
      String prevHash = hashFunction.hash(blockchain.chain().getLast().header().serialize());
      BlockHeader header =
          new BlockHeader(prevHash, Instant.now().getEpochSecond(), 1, txRoot, 0, 3);

      MiningResult miningResult = miner.mine(header);
      Block block = new Block(miningResult.header(), selected);
      if (!blockchain.addBlock(block)) {
        throw new IllegalStateException("Mined block failed validation");
      }

      int height = blockchain.chain().size() - 1;
      miningResultsByHeight.put(height, miningResult);
      transactionPool.removeConfirmedTransactions(selected);
    }
  }

  public Map<Integer, MiningResult> miningResultsByHeight() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(miningResultsByHeight));
  }
}
