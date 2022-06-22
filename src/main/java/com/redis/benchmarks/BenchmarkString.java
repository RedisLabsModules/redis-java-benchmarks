package com.redis.benchmarks;


import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.profile.StackProfiler;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.infra.Blackhole;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations=1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations=3,time = 10, timeUnit = TimeUnit.SECONDS)
public class BenchmarkString {

    // data size
    @Param({"32"})
    private int dataSize;

    // keyspace range related
    @Param({"100000"})
    private int keyMaximum;

    String hostAndPort = "redis://127.0.0.1:6379";
    private String value;

    // operation counter
    private int op;
    private JedisPool jedisPool;
    private Jedis client;

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(BenchmarkString.class.getSimpleName())
                .addProfiler(StackProfiler.class)
                .build();
        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        jedisPool = new JedisPool(hostAndPort);
        client = jedisPool.getResource();
        client.flushAll();
        value = "";
        for (int i = 0; i < dataSize; i++) {
            value = value + "x";
        }
        client.set("key-1", value);
    }
    @TearDown
    public void teardown() {
        client.flushAll();
        client.close();
    }

    @Benchmark
    public void BenchmarkSet(BenchmarkString bs, Blackhole bh) {
        bh.consume(client.set("key-1", value));
    }

    @Benchmark
    public void BenchmarkSetx(BenchmarkString bs, Blackhole bh) {
        bh.consume(client.setex("key-1", 1000, value));
    }
    @Benchmark
    public void BenchmarkGet(BenchmarkString bs, Blackhole bh) {
        bh.consume(client.get("key-1"));

    }
}
