package lt.vu.blockchain.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lt.vu.blockchain.hashing.HashFunctionV012;
import lt.vu.blockchain.model.Block;
import lt.vu.blockchain.model.BlockHeader;
import lt.vu.blockchain.model.User;

public final class Blockchain {

  private final List<Block> chain = new ArrayList<>();
  private final Map<String, Long> balances = new HashMap<>();
  private final Set<String> confirmedTransactionIds = new HashSet<>();
  private final Map<String, Long> initialBalances;

  public Blockchain(List<User> users) {
    for (User user : users) {
      balances.put(user.publicKey(), user.initialBalance());
    }
    initialBalances = Map.copyOf(balances);

    String emptyTxRoot = new HashFunctionV012().hash(new byte[0]);
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
}
