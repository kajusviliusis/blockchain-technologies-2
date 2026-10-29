package lt.vu.blockchain.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lt.vu.blockchain.hashing.HashFunctionV012;
import lt.vu.blockchain.model.Block;
import lt.vu.blockchain.model.BlockHeader;
import lt.vu.blockchain.model.Transaction;
import lt.vu.blockchain.model.User;

public final class Blockchain {

  private final List<Block> chain = new ArrayList<>();
  private final Map<String, Long> balances = new HashMap<>();
  private final Set<String> confirmedTransactionIds = new HashSet<>();
  private final Map<String, Long> initialBalances;
  private final HashFunctionV012 hashFunction = new HashFunctionV012();

  public Blockchain(List<User> users) {
    for (User user : users) {
      balances.put(user.publicKey(), user.initialBalance());
    }
    initialBalances = Map.copyOf(balances);

    String emptyTxRoot = hashFunction.hash(new byte[0]);
    BlockHeader genesisHeader = new BlockHeader("0".repeat(64), 0, 1, emptyTxRoot, 0, 3);
    chain.add(new Block(genesisHeader, List.of()));
  }

  public List<Block> chain() {
    return List.copyOf(chain);
  }

  public Map<String, Long> balances() {
    return Map.copyOf(balances);
  }

  public Set<String> confirmedTransactionIds() {
    return Set.copyOf(confirmedTransactionIds);
  }

  public Map<String, Long> initialBalances() {
    return initialBalances;
  }

  public long totalBalance() {
    return balances.values().stream().mapToLong(Long::longValue).sum();
  }

  public boolean addBlock(Block block) {
    if (block == null || block.header() == null || block.transactions() == null) {
      return false;
    }

    List<Transaction> transactions = new ArrayList<>(block.transactions());
    if (transactions.isEmpty() || transactions.size() > 100) {
      return false;
    }

    BlockHeader header = block.header();
    BlockHeader previousHeader = chain.getLast().header();
    if (header.version() != 1
        || header.difficulty() != 3
        || !hashFunction.hash(previousHeader.serialize()).equals(header.prevHash())
        || !hashFunction.hash(header.serialize()).startsWith("000")) {
      return false;
    }

    Map<String, Long> temporaryBalances = new HashMap<>(balances);
    Set<String> blockTransactionIds = new HashSet<>();
    StringBuilder concatenatedIds = new StringBuilder();
    for (Transaction transaction : transactions) {
      if (!isValidTransaction(transaction, temporaryBalances, blockTransactionIds)) {
        return false;
      }

      long senderBalance = temporaryBalances.get(transaction.sender());
      long receiverBalance = temporaryBalances.get(transaction.receiver());
      if (senderBalance < transaction.amount()
          || receiverBalance > Long.MAX_VALUE - transaction.amount()) {
        return false;
      }

      temporaryBalances.put(transaction.sender(), senderBalance - transaction.amount());
      temporaryBalances.put(transaction.receiver(), receiverBalance + transaction.amount());
      blockTransactionIds.add(transaction.transactionId());
      concatenatedIds.append(transaction.transactionId());
    }

    String expectedRoot =
        hashFunction.hash(concatenatedIds.toString().getBytes(StandardCharsets.UTF_8));
    if (!expectedRoot.equals(header.txRoot())) {
      return false;
    }

    chain.add(new Block(header, List.copyOf(transactions)));
    balances.clear();
    balances.putAll(temporaryBalances);
    confirmedTransactionIds.addAll(blockTransactionIds);
    return true;
  }

  private boolean isValidTransaction(
      Transaction transaction, Map<String, Long> currentBalances, Set<String> blockTransactionIds) {
    if (transaction == null
        || transaction.sender() == null
        || transaction.sender().isBlank()
        || transaction.receiver() == null
        || transaction.receiver().isBlank()
        || transaction.sender().equals(transaction.receiver())
        || transaction.amount() <= 0
        || transaction.sequence() < 0
        || transaction.transactionId() == null
        || transaction.transactionId().isBlank()
        || confirmedTransactionIds.contains(transaction.transactionId())
        || blockTransactionIds.contains(transaction.transactionId())) {
      return false;
    }

    Long senderBalance = currentBalances.get(transaction.sender());
    Long receiverBalance = currentBalances.get(transaction.receiver());
    if (senderBalance == null
        || receiverBalance == null
        || senderBalance < 0
        || receiverBalance < 0) {
      return false;
    }

    String expectedId = hashFunction.hash(transaction.serialize().getBytes(StandardCharsets.UTF_8));
    return expectedId.equals(transaction.transactionId());
  }
}
