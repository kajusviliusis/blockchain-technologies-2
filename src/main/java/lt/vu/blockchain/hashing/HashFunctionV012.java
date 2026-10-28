package lt.vu.blockchain.hashing;

public final class HashFunctionV012 {

  private static final int[] INITIAL_STATE = {
    0x13579BDF, 0x2468ACE0, 0xFDB97531, 0xECA86420, 0x0F1E2D3C, 0x4B5A6978, 0x89ABCDEF, 0x10203040
  };

  public String hash(byte[] input) {
    int[] state = INITIAL_STATE.clone();

    for (int i = 0; i < input.length; i++) {
      int unsignedByte = input[i] & 0xFF;
      int currentIndex = i % state.length;
      int nextIndex = (currentIndex + 1) % state.length;

      state[currentIndex] ^= unsignedByte + i;

      int rotation = (unsignedByte % 13) + 5;
      state[currentIndex] = Integer.rotateLeft(state[currentIndex], rotation);

      state[currentIndex] += state[nextIndex];
    }

    state[0] ^= input.length;
    state[state.length - 1] += Integer.rotateLeft(input.length, 16);

    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < state.length; j++) {
        int nextIndex = (j + 1) % state.length;

        state[j] ^= state[nextIndex];
        state[j] = Integer.rotateLeft(state[j], 7);
        state[nextIndex] += state[j];
      }
    }

    return toHex(state);
  }

  private static String toHex(int[] state) {
    StringBuilder result = new StringBuilder(state.length * 8);

    for (int value : state) {
      result.append(String.format("%08x", value));
    }

    return result.toString();
  }
}
