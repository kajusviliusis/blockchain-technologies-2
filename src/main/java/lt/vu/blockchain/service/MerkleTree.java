package lt.vu.blockchain.service;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import lt.vu.blockchain.hashing.HashFunctionV012;

public final class MerkleTree {

  private static final HashFunctionV012 HASH_FUNCTION = new HashFunctionV012();

  private MerkleTree() {}

  public static String calculateRoot(List<String> transactionIds) {
    if (transactionIds.isEmpty()) {
      return HASH_FUNCTION.hash(new byte[0]);
    }

    HexFormat hex = HexFormat.of();
    List<String> level = new ArrayList<>(transactionIds);
    while (level.size() > 1) {
      List<String> parents = new ArrayList<>((level.size() + 1) / 2);
      for (int i = 0; i < level.size(); i += 2) {
        String left = level.get(i);
        // Jei desinio kaimyno nera, dubliuojame paskutini, kad susidarytu pora.
        String right = i + 1 < level.size() ? level.get(i + 1) : left;
        // Hash paverciame baitais, kad tevus skaiciuotume is duomenu, o ne is teksto.
        byte[] leftBytes = hex.parseHex(left);
        byte[] rightBytes = hex.parseHex(right);
        byte[] combined = new byte[leftBytes.length + rightBytes.length];
        System.arraycopy(leftBytes, 0, combined, 0, leftBytes.length);
        System.arraycopy(rightBytes, 0, combined, leftBytes.length, rightBytes.length);
        parents.add(HASH_FUNCTION.hash(combined));
      }
      level = parents;
    }
    return level.get(0);
  }
}
