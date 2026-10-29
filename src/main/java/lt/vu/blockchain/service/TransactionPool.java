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
import lt.vu.blockchain.model.Transaction;

public final class TransactionPool {

  private static final int MAX_BLOCK_TRANSACTIONS = 100;

  private final List<Transaction> pending = new ArrayList<>();
  private final Set<String> pendingIds = new HashSet<>();
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
    if (!pendingIds.add(transaction.transactionId())) {
      return false;
    }
    pending.add(transaction);
    return true;
  }

  public List<Transaction> pendingTransactions() {
    return List.copyOf(pending);
  }

  public List<Transaction> selectTransactions(Blockchain blockchain) {
    Map<String, Long> temporaryBalances = new HashMap<>(blockchain.balances());
    Set<String> confirmedIds = blockchain.confirmedTransactionIds();
    List<Transaction> shuffled = new ArrayList<>(pending);
    Collections.shuffle(shuffled, random);

    List<Transaction> selected = new ArrayList<>(MAX_BLOCK_TRANSACTIONS);
    Set<String> selectedIds = new HashSet<>();
    for (Transaction transaction : shuffled) {
      if (selected.size() == MAX_BLOCK_TRANSACTIONS) {
        break;
      }
      if (!isValidTransaction(transaction, temporaryBalances, confirmedIds, selectedIds)) {
        continue;
      }

      long senderBalance = temporaryBalances.get(transaction.sender());
      long receiverBalance = temporaryBalances.get(transaction.receiver());
      if (senderBalance < transaction.amount()
          || receiverBalance > Long.MAX_VALUE - transaction.amount()) {
        continue;
      }

      temporaryBalances.put(transaction.sender(), senderBalance - transaction.amount());
      temporaryBalances.put(transaction.receiver(), receiverBalance + transaction.amount());
      selected.add(transaction);
      selectedIds.add(transaction.transactionId());
    }

    return List.copyOf(selected);
  }

  private boolean isValidTransaction(
      Transaction transaction,
      Map<String, Long> balances,
      Set<String> confirmedIds,
      Set<String> selectedIds) {
    // confirmed - jau blockchaine, selected - dabar atrenkame
    if (confirmedIds.contains(transaction.transactionId())
        || selectedIds.contains(transaction.transactionId())
        || transaction.sender() == null
        || transaction.receiver() == null
        || transaction.sender().equals(transaction.receiver())
        || transaction.amount() <= 0
        || !balances.containsKey(transaction.sender())
        || !balances.containsKey(transaction.receiver())) {
      return false;
    }

    String expectedId = hashFunction.hash(transaction.serialize().getBytes(StandardCharsets.UTF_8));
    return expectedId.equals(transaction.transactionId());
  }
}
