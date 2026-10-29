package lt.vu.blockchain.model;

import java.util.List;

public record Block(BlockHeader header, List<Transaction> transactions) {}
