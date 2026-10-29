package lt.vu.blockchain.model;

public record MiningResult(BlockHeader header, String hash, long attempts, long durationMillis) {}
