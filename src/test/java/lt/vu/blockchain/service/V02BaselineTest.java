package lt.vu.blockchain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import lt.vu.blockchain.model.BlockHeader;
import lt.vu.blockchain.model.MiningResult;
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

  @Test
  void miningBaselineMatchesProductionForSuccessAndExhaustedRange() {
    MinerV02Baseline baseline = new MinerV02Baseline();
    Miner current = new Miner();
    BlockHeader header = new BlockHeader("0".repeat(64), 0, 1, "0".repeat(64), 0, 3);
    MiningResult success = current.mine(header, 0, 10_000);
    assertTrue(success.found());
    assertSameResult(success, baseline.mine(header, 0, 10_000));

    BlockHeader difficult = new BlockHeader("0".repeat(64), 0, 1, "0".repeat(64), 0, 64);
    MiningResult exhausted = current.mine(difficult, 5, 10);
    assertFalse(exhausted.found());
    assertSameResult(exhausted, baseline.mine(difficult, 5, 10));
  }

  private void assertSameResult(MiningResult expected, MiningResult actual) {
    assertEquals(expected.header(), actual.header());
    assertEquals(expected.hash(), actual.hash());
    assertEquals(expected.attempts(), actual.attempts());
    assertEquals(expected.found(), actual.found());
  }
}
