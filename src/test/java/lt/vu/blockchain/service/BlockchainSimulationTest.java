package lt.vu.blockchain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lt.vu.blockchain.hashing.HashFunctionV012;
import lt.vu.blockchain.model.Block;
import lt.vu.blockchain.model.BlockHeader;
import lt.vu.blockchain.model.MiningResult;
import lt.vu.blockchain.model.Transaction;
import lt.vu.blockchain.model.User;
import org.junit.jupiter.api.Test;

class BlockchainSimulationTest {

  private final HashFunctionV012 hashFunction = new HashFunctionV012();

  @Test
  void onlyWinningCandidateChangesConfirmedState() {
    Blockchain blockchain =
        new Blockchain(List.of(new User("A", "a", 1_000), new User("B", "b", 1_000)));
    List<Transaction> transactions = new ArrayList<>();
    for (int i = 0; i < 150; i++) {
      transactions.add(transaction(i));
    }

    TransactionPool pool = new TransactionPool(blockchain, transactions, 42L);
    BlockchainSimulation simulation = new BlockchainSimulation(blockchain, pool);
    assertTrue(simulation.mineNextBlock());

    assertEquals(2, blockchain.chain().size());
    Block winningBlock = blockchain.chain().get(1);
    assertEquals(100, winningBlock.transactions().size());
    assertEquals(Map.of("a", 900L, "b", 1_100L), blockchain.balances());

    Set<String> winningIds = new HashSet<>();
    for (Transaction transaction : winningBlock.transactions()) {
      winningIds.add(transaction.transactionId());
    }
    assertEquals(winningIds, blockchain.confirmedTransactionIds());
    assertEquals(50, pool.pendingTransactions().size());
    for (Transaction transaction : pool.pendingTransactions()) {
      assertFalse(winningIds.contains(transaction.transactionId()));
    }

    List<List<MiningResult>> rounds = simulation.candidateRoundsByHeight().get(1);
    int winner = simulation.winningCandidateByHeight().get(1);
    assertEquals(5, rounds.getFirst().size());
    List<MiningResult> finalRound = rounds.getLast();
    long winnerAttempts = finalRound.get(winner - 1).attempts();
    for (int i = 0; i < finalRound.size(); i++) {
      if (finalRound.get(i).found()) {
        assertTrue(winnerAttempts <= finalRound.get(i).attempts());
        if (winnerAttempts == finalRound.get(i).attempts()) {
          assertTrue(winner - 1 <= i);
        }
      }
    }
  }

  @Test
  void boundedMiningContinuesAtTheNextUntestedNonce() {
    Miner miner = new Miner();
    BlockHeader header = new BlockHeader("0".repeat(64), 0, 1, "0".repeat(64), 0, 64);
    MiningResult first = miner.mine(header, 0, 5);
    MiningResult resumed = miner.mine(header, 5, 1);

    assertFalse(first.found());
    assertEquals(5, first.attempts());
    assertEquals(4, first.header().nonce());
    assertEquals(5, resumed.header().nonce());
    BlockHeader nonceFive = new BlockHeader(header.prevHash(), 0, 1, header.txRoot(), 5, 64);
    assertEquals(hashFunction.hash(nonceFive.serialize()), resumed.hash());
  }

  @Test
  void equalAttemptsChooseTheFirstCandidate() {
    Blockchain blockchain =
        new Blockchain(List.of(new User("A", "a", 100), new User("B", "b", 100)));
    TransactionPool pool = new TransactionPool(blockchain, List.of(transaction(0)), 42L);
    BlockchainSimulation simulation = new BlockchainSimulation(blockchain, pool);

    assertTrue(simulation.mineNextBlock());
    List<MiningResult> round = simulation.candidateRoundsByHeight().get(1).getFirst();
    assertEquals(5, round.size());
    for (MiningResult result : round) {
      assertEquals(round.getFirst().attempts(), result.attempts());
    }
    assertEquals(1, simulation.winningCandidateByHeight().get(1));
  }

  private Transaction transaction(long sequence) {
    Transaction withoutId = new Transaction("a", "b", 1, sequence, null);
    String id = hashFunction.hash(withoutId.serialize().getBytes(StandardCharsets.UTF_8));
    return new Transaction("a", "b", 1, sequence, id);
  }
}
