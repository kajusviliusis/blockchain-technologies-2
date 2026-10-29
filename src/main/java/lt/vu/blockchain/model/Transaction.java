package lt.vu.blockchain.model;

public record Transaction(
    String sender, String receiver, long amount, long sequence, String transactionId) {

  public String serialize() {
    return sender + "|" + receiver + "|" + amount + "|" + sequence;
  }
}
