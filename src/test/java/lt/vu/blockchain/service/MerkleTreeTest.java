package lt.vu.blockchain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import lt.vu.blockchain.hashing.HashFunctionV012;
import org.junit.jupiter.api.Test;

class MerkleTreeTest {

  private final HashFunctionV012 hashFunction = new HashFunctionV012();

  @Test
  void emptyAndSingleTransactionRoots() {
    assertEquals(hashFunction.hash(new byte[0]), MerkleTree.calculateRoot(List.of()));
    assertEquals("01", MerkleTree.calculateRoot(List.of("01")));
  }

  @Test
  void pairsDecodedHexBytes() {
    assertEquals(
        hashFunction.hash(new byte[] {1, 2}), MerkleTree.calculateRoot(List.of("01", "02")));
  }

  @Test
  void duplicatesTheLastHashAtOddLevels() {
    assertEquals(
        MerkleTree.calculateRoot(List.of("01", "02", "03", "03")),
        MerkleTree.calculateRoot(List.of("01", "02", "03")));
  }
}
