package lt.vu.blockchain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lt.vu.blockchain.hashing.HashFunctionV012;
import org.junit.jupiter.api.Test;

class VersionComparisonTest {

  @Test
  void merkleBaselineMatchesProductionAcrossSizes() {
    List<String> ids = transactionIds();
    int[] sizes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 15, 16, 31, 32, 33, 99, 100};
    for (int size : sizes) {
      List<String> input = ids.subList(0, size);
      assertEquals(MerkleTree.calculateRoot(input), MerkleTreeV02Baseline.calculateRoot(input));
    }

    List<String> shortIds = List.of("01", "0203", "04", "050607", "08", "09");
    for (int size = 0; size <= shortIds.size(); size++) {
      List<String> input = shortIds.subList(0, size);
      assertEquals(MerkleTreeV02Baseline.calculateRoot(input), MerkleTree.calculateRoot(input));
    }
    List<String> uppercase = new ArrayList<>(ids.subList(0, 3));
    uppercase.set(0, uppercase.get(0).toUpperCase(Locale.ROOT));
    assertEquals(
        MerkleTreeV02Baseline.calculateRoot(uppercase), MerkleTree.calculateRoot(uppercase));
    List<String> mixedWidths = List.of(ids.get(0), "01", ids.get(1));
    assertEquals(
        MerkleTreeV02Baseline.calculateRoot(mixedWidths), MerkleTree.calculateRoot(mixedWidths));
    assertEquals("AB", MerkleTree.calculateRoot(List.of("AB")));
  }

  @Test
  void changedIdAndOrderMatchBaselineAndChangeTheRoot() {
    List<String> ids = transactionIds();
    String originalRoot = MerkleTree.calculateRoot(ids);
    assertEquals(MerkleTreeV02Baseline.calculateRoot(ids), originalRoot);

    List<String> changed = new ArrayList<>(ids);
    changed.set(50, "0".repeat(64));
    assertEquals(MerkleTreeV02Baseline.calculateRoot(changed), MerkleTree.calculateRoot(changed));
    assertNotEquals(originalRoot, MerkleTree.calculateRoot(changed));

    List<String> reordered = new ArrayList<>(ids);
    Collections.swap(reordered, 0, 1);
    assertEquals(
        MerkleTreeV02Baseline.calculateRoot(reordered), MerkleTree.calculateRoot(reordered));
    assertNotEquals(originalRoot, MerkleTree.calculateRoot(reordered));
  }

  private List<String> transactionIds() {
    HashFunctionV012 hashFunction = new HashFunctionV012();
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      ids.add(hashFunction.hash(("transaction-" + i).getBytes(StandardCharsets.UTF_8)));
    }
    return ids;
  }
}
