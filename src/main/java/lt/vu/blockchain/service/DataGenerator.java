package lt.vu.blockchain.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lt.vu.blockchain.hashing.HashFunctionV012;
import lt.vu.blockchain.model.Transaction;
import lt.vu.blockchain.model.User;

public final class DataGenerator {

  private static final int USER_COUNT = 1_000;
  private static final int TRANSACTION_COUNT = 10_000;
  private static final long MIN_BALANCE = 100;
  private static final long MAX_BALANCE = 1_000_000;
  private static final int MAX_AMOUNT = 10_000;

  private final Random random;
  private final HashFunctionV012 hashFunction = new HashFunctionV012();
  private long nextSequence;

  public DataGenerator() {
    this(42L);
  }

  public DataGenerator(long seed) {
    random = new Random(seed);
  }

  public List<User> generateUsers() {
    List<User> users = new ArrayList<>(USER_COUNT);
    for (int i = 0; i < USER_COUNT; i++) {
      users.add(
          new User(
              "User-" + (i + 1),
              Long.toHexString(i + 1L),
              random.nextLong(MIN_BALANCE, MAX_BALANCE + 1)));
    }
    return List.copyOf(users);
  }

  public List<Transaction> generateTransactions(List<User> users) {
    if (users.size() < 2) {
      throw new IllegalArgumentException("at least two users are required");
    }

    List<Transaction> transactions = new ArrayList<>(TRANSACTION_COUNT);
    for (int i = 0; i < TRANSACTION_COUNT; i++) {
      int senderIndex = random.nextInt(users.size());
      int receiverIndex;
      do {
        receiverIndex = random.nextInt(users.size());
      } while (receiverIndex == senderIndex);

      String sender = users.get(senderIndex).publicKey();
      String receiver = users.get(receiverIndex).publicKey();
      long amount = random.nextInt(1, MAX_AMOUNT + 1);
      long sequence = nextSequence++;
      Transaction transactionWithoutId = new Transaction(sender, receiver, amount, sequence, null);
      String transactionId =
          hashFunction.hash(transactionWithoutId.serialize().getBytes(StandardCharsets.UTF_8));

      transactions.add(new Transaction(sender, receiver, amount, sequence, transactionId));
    }
    return List.copyOf(transactions);
  }
}
