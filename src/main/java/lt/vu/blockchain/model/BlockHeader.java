package lt.vu.blockchain.model;

import java.nio.charset.StandardCharsets;

public record BlockHeader(
    String prevHash, long timestamp, int version, String txRoot, long nonce, int difficulty) {

  public byte[] serialize() {
    String value =
        prevHash + "|" + timestamp + "|" + version + "|" + txRoot + "|" + nonce + "|" + difficulty;
    return value.getBytes(StandardCharsets.UTF_8);
  }
}
