package town.lost;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
public class HashCodeBenchmarkMain {

    public static final int BYTE_ARRAY_OFFSET = UnsafeMemory.INSTANCE.UNSAFE.arrayBaseOffset(byte[].class);
    private static final int K0 = 0x6d0f27bd;
    static Field HASH = Jvm.getField(String.class, "hash");
    static Field VALUE = Jvm.getField(String.class, "value");
    int counter = 0;
    String[] strings = {
            "",
            "1",
            "22",
            "4444",
            string(8),
            string(16),
            string(32),
            string(64),
            string(128),
    };

    public static void main(String... args) throws RunnerException, InvocationTargetException, IllegalAccessException {
        if (Jvm.isDebug()) {
            HashCodeBenchmarkMain main = new HashCodeBenchmarkMain();
            for (Method m : HashCodeBenchmarkMain.class.getMethods()) {
                if (m.getAnnotation(Benchmark.class) != null) {
                    m.invoke(main);
                }
            }
        } else {
            int time = Boolean.getBoolean("longTest") ? 30 : 2;
            System.out.println("measurementTime: " + time + " secs");
            Options opt = new OptionsBuilder()
                    .include(HashCodeBenchmarkMain.class.getSimpleName())
//                    .warmupIterations(5)
                    .measurementIterations(5)
                    .forks(1)
//                    .mode(Mode.SampleTime)
                    .measurementTime(TimeValue.seconds(time))
                    .timeUnit(TimeUnit.MICROSECONDS)
                    .build();

            new Runner(opt).run();
        }
    }

    static String string(int length) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length)
            sb.append(length);
        sb.setLength(length);
        return sb.toString();
    }

    private static int hashCodeFor(String s) throws IllegalAccessException {
        byte[] value = (byte[]) VALUE.get(s);
        return hashCodeFor(value);
    }

    private static int hashCodeFor(byte[] value) {
        int h = UnsafeMemory.INSTANCE.UNSAFE.getInt(value, BYTE_ARRAY_OFFSET);
        if (value.length <= 4) {
            return h;
        }
        long h2 = h;
        for (int i = 4; i < value.length; i += 4)
            h2 = h2 * K0 + UnsafeMemory.INSTANCE.UNSAFE.getInt(value, BYTE_ARRAY_OFFSET + i);
        return (int) (h2 ^ (h2 >>> 32));
    }

    @Benchmark
    public int String_hashCode() throws IllegalAccessException {
        String s = strings[counter++ & (strings.length - 1)];
        HASH.setInt(s, 0);
        return s.hashCode();
    }

    @Benchmark
    public int String_native_hashCode() throws IllegalAccessException {
        String s = strings[counter++ & (strings.length - 1)];
        return hashCodeFor(s);
    }
}
