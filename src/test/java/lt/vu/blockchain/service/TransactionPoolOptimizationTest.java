package lt.vu.blockchain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lt.vu.blockchain.hashing.HashFunctionV012;
import lt.vu.blockchain.model.Block;
import lt.vu.blockchain.model.BlockHeader;
import lt.vu.blockchain.model.MiningResult;
import lt.vu.blockchain.model.Transaction;
import lt.vu.blockchain.model.User;
import org.junit.jupiter.api.Test;

class TransactionPoolOptimizationTest {

  private final HashFunctionV012 hashFunction = new HashFunctionV012();

  @Test
  void selectionsMatchBaselineAndKeepBalancesAndPendingUnchanged() {
    Blockchain blockchain =
        new Blockchain(
            List.of(new User("A", "a", 100), new User("B", "b", 100), new User("C", "c", 100)));
    Transaction first = transaction("a", "b", 80, 0);
    Transaction second = transaction("a", "c", 80, 1);
    List<Transaction> transactions =
        List.of(first, second, transaction("b", "c", 120, 2), transaction("c", "a", 30, 3));
    TransactionPoolV02Baseline baseline =
        new TransactionPoolV02Baseline(blockchain, transactions, 42L);
    TransactionPool optimized = new TransactionPool(blockchain, transactions, 42L);

    for (int i = 0; i < 5; i++) {
      List<Transaction> expected = baseline.selectTransactions();
      List<Transaction> actual = optimized.selectTransactions();
      assertEquals(expected, actual);
      assertFalse(actual.contains(first) && actual.contains(second));
      assertEquals(Map.of("a", 100L, "b", 100L, "c", 100L), blockchain.balances());
      assertEquals(transactions, optimized.pendingTransactions());
    }
    assertEquals(baseline.rejectedTransactions(), optimized.rejectedTransactions());

    baseline.removeConfirmedTransactions(List.of(first));
    optimized.removeConfirmedTransactions(List.of(first));
    assertEquals(baseline.pendingTransactions(), optimized.pendingTransactions());
    for (int i = 0; i < 5; i++) {
      assertEquals(baseline.selectTransactions(), optimized.selectTransactions());
    }
  }

  @Test
  void admissionAndStaleConfirmationMatchBaseline() {
    Blockchain blockchain =
        new Blockchain(List.of(new User("A", "a", 100), new User("B", "b", 100)));
    Transaction confirmed = transaction("a", "b", 10, 0);
    TransactionPoolV02Baseline baseline =
        new TransactionPoolV02Baseline(blockchain, List.of(confirmed));
    TransactionPool optimized = new TransactionPool(blockchain, List.of(confirmed));

    assertFalse(baseline.addTransaction(confirmed));
    assertFalse(optimized.addTransaction(confirmed));
    assertEquals(baseline.rejectedTransactions(), optimized.rejectedTransactions());

    String prevHash = hashFunction.hash(blockchain.chain().getLast().header().serialize());
    BlockHeader header =
        new BlockHeader(
            prevHash, 1, 1, MerkleTree.calculateRoot(List.of(confirmed.transactionId())), 0, 3);
    MiningResult mined = new Miner().mine(header, 0, 1_000_000);
    assertTrue(mined.found());
    assertTrue(blockchain.addBlock(new Block(mined.header(), List.of(confirmed))));

    assertEquals(baseline.selectTransactions(), optimized.selectTransactions());
    assertEquals(baseline.pendingTransactions(), optimized.pendingTransactions());
    assertEquals(baseline.rejectedTransactions(), optimized.rejectedTransactions());
    assertEquals("already confirmed", optimized.rejectedTransactions().getLast().reason());
  }

  private Transaction transaction(String sender, String receiver, long amount, long sequence) {
    Transaction withoutId = new Transaction(sender, receiver, amount, sequence, null);
    String id = hashFunction.hash(withoutId.serialize().getBytes(StandardCharsets.UTF_8));
    return new Transaction(sender, receiver, amount, sequence, id);
  }
}
