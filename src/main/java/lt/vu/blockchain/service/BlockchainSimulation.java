package lt.vu.blockchain.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lt.vu.blockchain.hashing.HashFunctionV012;
import lt.vu.blockchain.model.Block;
import lt.vu.blockchain.model.BlockHeader;
import lt.vu.blockchain.model.MiningResult;
import lt.vu.blockchain.model.Transaction;

public final class BlockchainSimulation {

  private static final int CANDIDATE_COUNT = 5;
  private static final int INITIAL_BUDGET = 5_000;
  private static final int MAX_ATTEMPTS_PER_CANDIDATE = 1_000_000;

  private final Blockchain blockchain;
  private final TransactionPool transactionPool;
  private final Miner miner = new Miner();
  private final HashFunctionV012 hashFunction = new HashFunctionV012();
  private final Map<Integer, List<List<MiningResult>>> candidateRoundsByHeight = new HashMap<>();
  private final Map<Integer, Integer> winningCandidateByHeight = new HashMap<>();

  public BlockchainSimulation(Blockchain blockchain, TransactionPool transactionPool) {
    this.blockchain = blockchain;
    this.transactionPool = transactionPool;
  }

  public void run() {
    while (mineNextBlock()) {
      // Tesiam simuliacija, kol nebera tinkamu transakciju naujam blokui.
    }
  }

  public boolean mineNextBlock() {
    String prevHash = hashFunction.hash(blockchain.chain().getLast().header().serialize());
    long timestamp = Instant.now().getEpochSecond();
    List<Block> candidates = new ArrayList<>(CANDIDATE_COUNT);
    for (int i = 0; i < CANDIDATE_COUNT; i++) {
      // Kiekvienas kandidatas atskirai renkasi transakcijas.
      List<Transaction> selected = transactionPool.selectTransactions();
      if (selected.isEmpty()) {
        if (i == 0) {
          return false;
        }
        throw new IllegalStateException("Candidate has no transactions");
      }

      List<String> transactionIds = new ArrayList<>(selected.size());
      for (Transaction transaction : selected) {
        transactionIds.add(transaction.transactionId());
      }
      String txRoot = MerkleTree.calculateRoot(transactionIds);
      BlockHeader header = new BlockHeader(prevHash, timestamp, 1, txRoot, 0, 3);
      candidates.add(new Block(header, selected));
    }

    // Visi kandidatai gauna ta pati nonce intervala.
    long nextNonce = 0;
    List<List<MiningResult>> rounds = new ArrayList<>();
    int budget = INITIAL_BUDGET;
    while (true) {
      List<MiningResult> roundResults = new ArrayList<>(CANDIDATE_COUNT);
      int winnerIndex = -1;

      for (int i = 0; i < CANDIDATE_COUNT; i++) {
        MiningResult result = miner.mine(candidates.get(i).header(), nextNonce, budget);
        roundResults.add(result);

        // Laimi maziausiai bandymu atlikusis, lygiu atveju pirmas.
        if (result.found()
            && (winnerIndex < 0 || result.attempts() < roundResults.get(winnerIndex).attempts())) {
          winnerIndex = i;
        }
      }
      rounds.add(List.copyOf(roundResults));

      if (winnerIndex >= 0) {
        MiningResult winningRound = roundResults.get(winnerIndex);
        Block block = new Block(winningRound.header(), candidates.get(winnerIndex).transactions());
        // Tik laimetoja pridedame, kad pralaimeje nekeistu grandines balansu.
        if (!blockchain.addBlock(block)) {
          throw new IllegalStateException("Winning block failed validation");
        }

        int height = blockchain.chain().size() - 1;
        candidateRoundsByHeight.put(height, List.copyOf(rounds));
        winningCandidateByHeight.put(height, winnerIndex + 1);
        transactionPool.removeConfirmedTransactions(candidates.get(winnerIndex).transactions());
        return true;
      }

      // Jei niekas nelaimejo, duodame daugiau bandymu ir tesime nuo kito nonce be
      // kartojimo.
      nextNonce += budget;
      if (nextNonce >= MAX_ATTEMPTS_PER_CANDIDATE) {
        throw new IllegalStateException(
            "No candidate found Proof-of-Work within "
                + MAX_ATTEMPTS_PER_CANDIDATE
                + " attempts per candidate at height "
                + blockchain.chain().size());
      }
      budget = Math.min(budget * 2, MAX_ATTEMPTS_PER_CANDIDATE - (int) nextNonce);
    }
  }

  public Map<Integer, List<List<MiningResult>>> candidateRoundsByHeight() {
    return Map.copyOf(candidateRoundsByHeight);
  }

  public Map<Integer, Integer> winningCandidateByHeight() {
    return Map.copyOf(winningCandidateByHeight);
  }
}
