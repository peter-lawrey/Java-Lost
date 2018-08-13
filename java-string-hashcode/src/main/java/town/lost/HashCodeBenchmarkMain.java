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
    static final String[] strings = {
            "1",
            "22",
            "4444",
            string(8),
            string(12),
            string(22),
            string(32),
            string(62),
            string(128),
    };
    private static final int K0 = 0x6d0f27bd;
    private static Field HASH = Jvm.getField(String.class, "hash");
    private static Field VALUE = Jvm.getField(String.class, "value");

    int counter = 0;

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

    private static int nativeHashCode(String s) throws IllegalAccessException {
        byte[] value = (byte[]) VALUE.get(s);
        return nativeHashCode(value);
    }

    private static int nativeHashCode(byte[] value) {
        int h = UnsafeMemory.INSTANCE.UNSAFE.getInt(value, BYTE_ARRAY_OFFSET);
        if (value.length <= 4) {
            return h;
        }
        long h2 = h;
        for (int i = 4; i < value.length; i += 4)
            h2 = h2 * K0 + UnsafeMemory.INSTANCE.UNSAFE.getInt(value, BYTE_ARRAY_OFFSET + i);
        return (int) (h2 ^ (h2 >>> 32));
    }

    private static int hashCode31(String s) throws IllegalAccessException {
        byte[] value = (byte[]) VALUE.get(s);
        return hashCode109(value);
    }

    private static int hashCode31(byte[] value) {
        int h = 0;
        for (byte b : value)
            h = h * 31 + (b & 0xFF);
        return h;
    }

    private static int hashCode109(String s) throws IllegalAccessException {
        byte[] value = (byte[]) VALUE.get(s);
        return hashCode109(value);
    }

    private static int hashCode109(byte[] value) {
        int h = 0;
        for (byte b : value)
            h = h * 109 + (b & 0xFF);
        return h;
    }

    @Benchmark
    public int String_hashCode() throws IllegalAccessException {
        String s = strings[counter++ & (strings.length - 1)];
        HASH.setInt(s, 0);
        return s.hashCode();
    }

    @Benchmark
    public int String_hashCode31() throws IllegalAccessException {
        String s = strings[counter++ & (strings.length - 1)];
        return hashCode31(s);
    }

    @Benchmark
    public int String_hashCode109() throws IllegalAccessException {
        String s = strings[counter++ & (strings.length - 1)];
        return hashCode109(s);
    }

    @Benchmark
    public int String_native_hashCode() throws IllegalAccessException {
        String s = strings[counter++ & (strings.length - 1)];
        return nativeHashCode(s);
    }
}
