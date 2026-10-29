package lt.vu.blockchain.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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
  private final Random random;
  private final HashFunctionV012 hashFunction = new HashFunctionV012();

  public TransactionPool(List<Transaction> transactions) {
    this(transactions, 42L);
  }

  public TransactionPool(List<Transaction> transactions, long seed) {
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
    if (!pendingIds.add(transaction.transactionId())) {
      recordRejection(transaction, "duplicate transaction ID");
      return false;
    }
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

  public List<Transaction> selectTransactions(Blockchain blockchain) {
    Map<String, Long> temporaryBalances = new HashMap<>(blockchain.balances());
    Set<String> confirmedIds = blockchain.confirmedTransactionIds();
    List<Transaction> shuffled = new ArrayList<>(pending);
    Collections.shuffle(shuffled, random);

    List<Transaction> selected = new ArrayList<>(MAX_BLOCK_TRANSACTIONS);
    for (Transaction transaction : shuffled) {
      if (selected.size() == MAX_BLOCK_TRANSACTIONS) {
        break;
      }
      String rejectionReason = rejectionReason(transaction, temporaryBalances, confirmedIds);
      if (rejectionReason != null) {
        pending.remove(transaction);
        pendingIds.remove(transaction.transactionId());
        recordRejection(transaction, rejectionReason);
        continue;
      }

      long senderBalance = temporaryBalances.get(transaction.sender());
      long receiverBalance = temporaryBalances.get(transaction.receiver());
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
