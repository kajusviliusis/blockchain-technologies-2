package lt.vu.blockchain.model;

public record RejectedTransaction(Transaction transaction, String reason) {}
