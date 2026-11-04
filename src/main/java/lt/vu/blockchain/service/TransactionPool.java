package lt.vu.blockchain.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import lt.vu.blockchain.hashing.HashFunctionV012;
import lt.vu.blockchain.model.RejectedTransaction;
import lt.vu.blockchain.model.Transaction;

public final class TransactionPool {

  private static final int MAX_BLOCK_TRANSACTIONS = 100;

  private final List<Transaction> pending = new ArrayList<>();
  private final Set<String> pendingIds = new HashSet<>();
  private final List<RejectedTransaction> rejectedTransactions = new ArrayList<>();
  private final Blockchain blockchain;
  private final Random random;
  private final HashFunctionV012 hashFunction = new HashFunctionV012();
  private int[] shuffledIndexes = new int[0];

  public TransactionPool(Blockchain blockchain, List<Transaction> transactions) {
    this(blockchain, transactions, 42L);
  }

  public TransactionPool(Blockchain blockchain, List<Transaction> transactions, long seed) {
    this.blockchain = blockchain;
    random = new Random(seed);
    for (Transaction transaction : transactions) {
      addTransaction(transaction);
    }
  }

  public boolean addTransaction(Transaction transaction) {
    if (transaction == null) {
      recordRejection(transaction, "null transaction");
      return false;
    }
    if (pendingIds.contains(transaction.transactionId())) {
      recordRejection(transaction, "duplicate transaction ID");
      return false;
    }
    String reason =
        rejectionReason(
            transaction, blockchain.balancesView(), blockchain.confirmedTransactionIdsView());
    if (reason != null) {
      recordRejection(transaction, reason);
      return false;
    }
    pendingIds.add(transaction.transactionId());
    pending.add(transaction);
    return true;
  }

  public List<Transaction> pendingTransactions() {
    return List.copyOf(pending);
  }

  public List<RejectedTransaction> rejectedTransactions() {
    return List.copyOf(rejectedTransactions);
  }

  public void removeConfirmedTransactions(List<Transaction> transactions) {
    Set<String> confirmedIds = new HashSet<>();
    for (Transaction transaction : transactions) {
      confirmedIds.add(transaction.transactionId());
    }
    pending.removeIf(transaction -> confirmedIds.contains(transaction.transactionId()));
    pendingIds.removeAll(confirmedIds);
  }

  public List<Transaction> selectTransactions() {
    // Laikome tik pakitusius balansus, kad atranka nekeistu patvirtintos busenos.
    Map<String, Long> confirmedBalances = blockchain.balancesView();
    Map<String, Long> temporaryBalances = new HashMap<>();
    Set<String> confirmedIds = blockchain.confirmedTransactionIdsView();
    int pendingCount = pending.size();
    if (shuffledIndexes.length < pendingCount) {
      shuffledIndexes = new int[pendingCount];
    }
    for (int i = 0; i < pendingCount; i++) {
      shuffledIndexes[i] = i;
    }
    for (int i = pendingCount; i > 1; i--) {
      int other = random.nextInt(i);
      int previous = shuffledIndexes[i - 1];
      shuffledIndexes[i - 1] = shuffledIndexes[other];
      shuffledIndexes[other] = previous;
    }

    List<Transaction> selected = new ArrayList<>(MAX_BLOCK_TRANSACTIONS);
    List<Transaction> newlyRejected = null;
    for (int i = 0; i < pendingCount; i++) {
      if (selected.size() == MAX_BLOCK_TRANSACTIONS) {
        break;
      }
      Transaction transaction = pending.get(shuffledIndexes[i]);
      // Sandorio laukai ir ID jau patikrinti ji priimant i telkini.
      if (confirmedIds.contains(transaction.transactionId())) {
        if (newlyRejected == null) {
          newlyRejected = new ArrayList<>();
        }
        newlyRejected.add(transaction);
        recordRejection(transaction, "already confirmed");
        continue;
      }

      Long senderBalance = temporaryBalances.get(transaction.sender());
      if (senderBalance == null) {
        senderBalance = confirmedBalances.get(transaction.sender());
      }
      Long receiverBalance = temporaryBalances.get(transaction.receiver());
      if (receiverBalance == null) {
        receiverBalance = confirmedBalances.get(transaction.receiver());
      }
      // Jei siuntejui neuztenka balanso, transakcija paliekame pending, nes balance
      // gali atsirasti veliau.
      if (senderBalance < transaction.amount()) {
        continue;
      }
      if (receiverBalance > Long.MAX_VALUE - transaction.amount()) {
        continue;
      }

      temporaryBalances.put(transaction.sender(), senderBalance - transaction.amount());
      temporaryBalances.put(transaction.receiver(), receiverBalance + transaction.amount());
      selected.add(transaction);
    }

    if (newlyRejected != null) {
      Set<String> rejectedIds = new HashSet<>();
      for (Transaction transaction : newlyRejected) {
        rejectedIds.add(transaction.transactionId());
      }
      pending.removeIf(transaction -> rejectedIds.contains(transaction.transactionId()));
      pendingIds.removeAll(rejectedIds);
    }

    return List.copyOf(selected);
  }

  private String rejectionReason(
      Transaction transaction, Map<String, Long> balances, Set<String> confirmedIds) {
    if (transaction.transactionId() == null || transaction.transactionId().isBlank()) {
      return "missing transaction ID";
    }
    if (confirmedIds.contains(transaction.transactionId())) {
      return "already confirmed";
    }
    if (transaction.sender() == null
        || transaction.sender().isBlank()
        || transaction.receiver() == null
        || transaction.receiver().isBlank()) {
      return "missing participant";
    }
    if (transaction.sender().equals(transaction.receiver())) {
      return "same sender and receiver";
    }
    if (transaction.amount() <= 0) {
      return "nonpositive amount";
    }
    if (transaction.sequence() < 0) {
      return "negative sequence";
    }
    if (!balances.containsKey(transaction.sender())
        || !balances.containsKey(transaction.receiver())) {
      return "unknown participant";
    }
    if (balances.get(transaction.sender()) < 0 || balances.get(transaction.receiver()) < 0) {
      return "negative balance";
    }

    String expectedId = hashFunction.hash(transaction.serialize().getBytes(StandardCharsets.UTF_8));
    return expectedId.equals(transaction.transactionId()) ? null : "invalid transaction ID";
  }

  private void recordRejection(Transaction transaction, String reason) {
    rejectedTransactions.add(new RejectedTransaction(transaction, reason));
  }
}
