package lt.vu.blockchain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class V02BaselineTest {

  @Test
  void merkleBaselineMatchesProduction() {
    List<String> ids = List.of("01", "02", "03", "04", "05");
    for (int size = 0; size <= ids.size(); size++) {
      List<String> input = ids.subList(0, size);
      assertEquals(MerkleTree.calculateRoot(input), MerkleTreeV02Baseline.calculateRoot(input));
    }
  }
}
