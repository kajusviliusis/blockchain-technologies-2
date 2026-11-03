package lt.vu.blockchain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lt.vu.blockchain.hashing.HashFunctionV012;
import lt.vu.blockchain.model.Block;
import lt.vu.blockchain.model.BlockHeader;
import lt.vu.blockchain.model.Transaction;
import lt.vu.blockchain.model.User;
import org.junit.jupiter.api.Test;

class ValidationTest {

  private final HashFunctionV012 hashFunction = new HashFunctionV012();

  @Test
  void rejectsInvalidTransactionsBeforeAdmissionButKeepsInsufficientFundsPending() {
    Blockchain blockchain = blockchain();
    TransactionPool pool = new TransactionPool(blockchain, List.of());

    assertFalse(pool.addTransaction(transaction("missing", "b", 10, 0)));
    assertFalse(pool.addTransaction(transaction("a", "b", 0, 1)));
    assertFalse(pool.addTransaction(new Transaction("a", "b", 10, 2, "invalid")));
    assertEquals("unknown participant", pool.rejectedTransactions().get(0).reason());
    assertEquals("nonpositive amount", pool.rejectedTransactions().get(1).reason());
    assertEquals("invalid transaction ID", pool.rejectedTransactions().get(2).reason());

    Transaction insufficientFunds = transaction("a", "b", 200, 3);
    assertTrue(pool.addTransaction(insufficientFunds));
    assertTrue(pool.selectTransactions(blockchain).isEmpty());
    assertEquals(List.of(insufficientFunds), pool.pendingTransactions());
    assertEquals(3, pool.rejectedTransactions().size());

    Transaction valid = transaction("a", "b", 10, 4);
    Transaction invalidWithSameId = new Transaction("missing", "b", 10, 4, valid.transactionId());
    assertFalse(pool.addTransaction(invalidWithSameId));
    assertTrue(pool.addTransaction(valid));
    assertEquals(List.of(insufficientFunds, valid), pool.pendingTransactions());
  }

  @Test
  void rejectsDuplicateAndAlreadyConfirmedIds() {
    Blockchain blockchain = blockchain();
    Transaction transaction = transaction("a", "b", 10, 0);
    TransactionPool pool = new TransactionPool(blockchain, List.of(transaction));

    assertFalse(pool.addTransaction(transaction));
    assertEquals("duplicate transaction ID", pool.rejectedTransactions().getFirst().reason());
    assertEquals(List.of(transaction), pool.pendingTransactions());

    assertTrue(blockchain.addBlock(minedBlock(blockchain, List.of(transaction))));
    TransactionPool laterPool = new TransactionPool(blockchain, List.of());
    assertFalse(laterPool.addTransaction(transaction));
    assertEquals("already confirmed", laterPool.rejectedTransactions().getFirst().reason());
    assertTrue(laterPool.pendingTransactions().isEmpty());
  }

  @Test
  void invalidBlockDoesNotCommitEarlierTransfers() {
    Blockchain blockchain = blockchain();
    Transaction valid = transaction("a", "b", 10, 0);
    Transaction overdraw = transaction("a", "b", 100, 1);
    Map<String, Long> balancesBefore = blockchain.balances();
    Set<String> confirmedBefore = blockchain.confirmedTransactionIds();
    List<Block> chainBefore = blockchain.chain();

    assertFalse(blockchain.addBlock(minedBlock(blockchain, List.of(valid, overdraw))));
    assertEquals(balancesBefore, blockchain.balances());
    assertEquals(confirmedBefore, blockchain.confirmedTransactionIds());
    assertEquals(chainBefore, blockchain.chain());
  }

  private Blockchain blockchain() {
    return new Blockchain(List.of(new User("A", "a", 100), new User("B", "b", 100)));
  }

  private Transaction transaction(String sender, String receiver, long amount, long sequence) {
    Transaction withoutId = new Transaction(sender, receiver, amount, sequence, null);
    String id = hashFunction.hash(withoutId.serialize().getBytes(StandardCharsets.UTF_8));
    return new Transaction(sender, receiver, amount, sequence, id);
  }

  private Block minedBlock(Blockchain blockchain, List<Transaction> transactions) {
    List<String> ids = new ArrayList<>();
    for (Transaction transaction : transactions) {
      ids.add(transaction.transactionId());
    }
    String prevHash = hashFunction.hash(blockchain.chain().getLast().header().serialize());
    BlockHeader header = new BlockHeader(prevHash, 1, 1, MerkleTree.calculateRoot(ids), 0, 3);
    return new Block(new Miner().mine(header).header(), transactions);
  }
}
