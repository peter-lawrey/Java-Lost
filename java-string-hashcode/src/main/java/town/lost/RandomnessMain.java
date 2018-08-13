package town.lost;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToIntFunction;

public class RandomnessMain {

    public static void main(String[] args) {
        System.out.printf("%-7s%16s%16s%16s%16s%16s%16s%n",
                "length", "String.hashCode", "Arrays.hashCode", "hashCode 109", "hashCode 251", "nativeHashCode", "Random.nextInt");
        for (int length = 1; length <= 16; length++) {
            System.out.printf("%-7d%16.2f%16.2f%16.2f%16.2f%16.2f%16.2f%n",
                    length,
                    randomnessScore(v -> new String(v, 0).hashCode(), length),
                    randomnessScore(Arrays::hashCode, length),
                    randomnessScore(HashCodeBenchmarkMain::hashCode109, length),
                    randomnessScore(HashCodeBenchmarkMain::hashCode251, length),
                    randomnessScore(HashCodeBenchmarkMain::nativeHashCode, length),
                    randomnessScore(value -> ThreadLocalRandom.current().nextInt(), length));
        }
    }

    static double randomnessScore(ToIntFunction<byte[]> hashCode, int length) {
        Random random = new Random(length);
        byte[] bytes = new byte[length];
        int score = 0;
        int tests = 0;
        for (int t = 0; t < 1000000; t += length) {
            for (int i = 0; i < length; i++)
                bytes[i] = (byte) (' ' + random.nextInt(96));
            int base = hashCode.applyAsInt(bytes);

            for (int i = 0; i < length; i++) {
                for (int j = 0; j < 8; j++) {
                    bytes[i] ^= 1 << j;
                    int val = hashCode.applyAsInt(bytes);
                    int score2 = Integer.bitCount(base ^ val);
                    score += score2;
                    tests++;
//                    base = val;
                    bytes[i] ^= 1 << j;
                }
            }
        }
        return Math.round(100.0 * score / tests) / 100.0;
    }
}
