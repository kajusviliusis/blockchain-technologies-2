package lt.vu.blockchain.service;

import java.util.HexFormat;
import java.util.List;
import lt.vu.blockchain.hashing.HashFunctionV012;

public final class MerkleTree {

  private static final int HASH_BYTES = 32;
  private static final int HASH_HEX_LENGTH = HASH_BYTES * 2;
  private static final HashFunctionV012 HASH_FUNCTION = new HashFunctionV012();

  private MerkleTree() {}

  public static String calculateRoot(List<String> transactionIds) {
    int count = transactionIds.size();
    if (count == 0) {
      return HASH_FUNCTION.hash(new byte[0]);
    }
    if (count == 1) {
      return transactionIds.get(0);
    }

    boolean fullLengthHashes = true;
    for (String id : transactionIds) {
      if (id.length() != HASH_HEX_LENGTH) {
        fullLengthHashes = false;
        break;
      }
    }
    if (fullLengthHashes) {
      return calculateFixedWidthRoot(transactionIds);
    }
    return calculateVariableWidthRoot(transactionIds);
  }

  private static String calculateFixedWidthRoot(List<String> transactionIds) {
    int count = transactionIds.size();
    byte[] nodes = new byte[count * HASH_BYTES];
    for (int i = 0; i < count; i++) {
      decodeHash(transactionIds.get(i), nodes, i * HASH_BYTES);
    }

    byte[] pair = new byte[HASH_BYTES * 2];
    while (count > 1) {
      int parentCount = (count + 1) / 2;
      for (int i = 0; i < parentCount; i++) {
        int left = i * 2;
        int right = left + 1 < count ? left + 1 : left;
        System.arraycopy(nodes, left * HASH_BYTES, pair, 0, HASH_BYTES);
        System.arraycopy(nodes, right * HASH_BYTES, pair, HASH_BYTES, HASH_BYTES);

        String parent = HASH_FUNCTION.hash(pair);
        if (parentCount == 1) {
          return parent;
        }
        // Sunaudotus vaiku baitus panaudojame kito lygio tevui.
        decodeParent(parent, nodes, i * HASH_BYTES);
      }
      count = parentCount;
    }
    throw new IllegalStateException("Merkle root was not calculated");
  }

  private static String calculateVariableWidthRoot(List<String> transactionIds) {
    int count = transactionIds.size();
    byte[][] nodes = new byte[count][];
    HexFormat hex = HexFormat.of();
    for (int i = 0; i < count; i++) {
      nodes[i] = hex.parseHex(transactionIds.get(i));
    }

    byte[] pair = null;
    while (count > 1) {
      int parentCount = (count + 1) / 2;
      for (int i = 0; i < parentCount; i++) {
        int left = i * 2;
        int right = left + 1 < count ? left + 1 : left;
        byte[] leftBytes = nodes[left];
        byte[] rightBytes = nodes[right];
        int pairLength = leftBytes.length + rightBytes.length;
        if (pair == null || pair.length != pairLength) {
          pair = new byte[pairLength];
        }
        System.arraycopy(leftBytes, 0, pair, 0, leftBytes.length);
        System.arraycopy(rightBytes, 0, pair, leftBytes.length, rightBytes.length);

        String parent = HASH_FUNCTION.hash(pair);
        if (parentCount == 1) {
          return parent;
        }
        byte[] destination = nodes[i];
        if (destination.length != HASH_BYTES) {
          destination = new byte[HASH_BYTES];
          nodes[i] = destination;
        }
        decodeParent(parent, destination, 0);
      }
      count = parentCount;
    }
    throw new IllegalStateException("Merkle root was not calculated");
  }

  private static void decodeHash(String hash, byte[] destination, int offset) {
    for (int i = 0; i < HASH_BYTES; i++) {
      int high = HexFormat.fromHexDigit(hash.charAt(i * 2));
      int low = HexFormat.fromHexDigit(hash.charAt(i * 2 + 1));
      destination[offset + i] = (byte) ((high << 4) | low);
    }
  }

  private static void decodeParent(String hash, byte[] destination, int offset) {
    for (int i = 0; i < HASH_BYTES; i++) {
      char high = hash.charAt(i * 2);
      char low = hash.charAt(i * 2 + 1);
      int highValue = high <= '9' ? high - '0' : high - 'a' + 10;
      int lowValue = low <= '9' ? low - '0' : low - 'a' + 10;
      destination[offset + i] = (byte) ((highValue << 4) | lowValue);
    }
  }
}
