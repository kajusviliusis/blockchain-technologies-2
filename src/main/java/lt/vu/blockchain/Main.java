package lt.vu.blockchain;

import java.util.List;
import java.util.Map;
import lt.vu.blockchain.model.Block;
import lt.vu.blockchain.model.BlockHeader;
import lt.vu.blockchain.model.MiningResult;
import lt.vu.blockchain.model.RejectedTransaction;
import lt.vu.blockchain.model.Transaction;
import lt.vu.blockchain.model.User;
import lt.vu.blockchain.service.Blockchain;
import lt.vu.blockchain.service.BlockchainSimulation;
import lt.vu.blockchain.service.DataGenerator;
import lt.vu.blockchain.service.TransactionPool;

public final class Main {

  public static void main(String[] args) {
    long startTime = System.nanoTime();

    DataGenerator generator = new DataGenerator(42L);
    List<User> users = generator.generateUsers();
    List<Transaction> transactions = generator.generateTransactions(users);
    Blockchain blockchain = new Blockchain(users);
    TransactionPool transactionPool = new TransactionPool(transactions, 42L);
    BlockchainSimulation simulation = new BlockchainSimulation(blockchain, transactionPool);
    simulation.run();

    long executionMillis = (System.nanoTime() - startTime) / 1_000_000;
    List<Block> chain = blockchain.chain();
    Map<Integer, MiningResult> miningResults = simulation.miningResultsByHeight();
    for (int height = 1; height < chain.size(); height++) {
      Block block = chain.get(height);
      BlockHeader header = block.header();
      MiningResult result = miningResults.get(height);
      if (result == null) {
        throw new IllegalStateException("Missing mining result for block " + height);
      }

      System.out.printf(
          "Block height: %d, transactions: %d%n", height, block.transactions().size());
      System.out.printf("  Previous hash: %s%n", header.prevHash());
      System.out.printf("  Transaction root: %s%n", header.txRoot());
      System.out.printf("  Block hash: %s%n", result.hash());
      System.out.printf("  Difficulty: %d, nonce: %d%n", header.difficulty(), header.nonce());
      System.out.printf(
          "  Mining attempts: %d, duration: %d ms%n", result.attempts(), result.durationMillis());
    }

    int confirmedCount = blockchain.confirmedTransactionIds().size();
    List<RejectedTransaction> rejectedTransactions = transactionPool.rejectedTransactions();
    int rejectedCount = rejectedTransactions.size();
    List<Transaction> pendingTransactions = transactionPool.pendingTransactions();
    int remainingCount = pendingTransactions.size();
    if (transactions.size() != confirmedCount + rejectedCount + remainingCount) {
      throw new IllegalStateException("Transaction counts do not match generated transactions");
    }

    long initialTotal = 0;
    for (long balance : blockchain.initialBalances().values()) {
      initialTotal += balance;
    }
    long finalTotal = blockchain.totalBalance();
    if (initialTotal != finalTotal) {
      throw new IllegalStateException("Total balance changed during the simulation");
    }
    System.out.println("Final summary:");
    System.out.printf("  Generated users: %d%n", users.size());
    System.out.printf("  Generated transactions: %d%n", transactions.size());
    System.out.printf("  Accepted non-genesis blocks: %d%n", chain.size() - 1);
    System.out.printf("  Confirmed transactions: %d%n", confirmedCount);
    System.out.printf("  Rejected transactions: %d%n", rejectedCount);
    System.out.printf("  Remaining pending transactions: %d%n", remainingCount);
    System.out.printf("  Initial total balance: %d%n", initialTotal);
    System.out.printf("  Final total balance: %d%n", finalTotal);
    System.out.printf("  Total execution time: %d ms%n", executionMillis);

    Map<String, Long> balances = blockchain.balances();
    for (Transaction pending : pendingTransactions) {
      String reason =
          balances.get(pending.sender()) < pending.amount()
              ? "insufficient balance"
              : "receiver balance overflow";
      System.out.printf("  Pending %s: %s%n", pending.transactionId(), reason);
    }
  }
}
