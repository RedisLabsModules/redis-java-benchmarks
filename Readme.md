


# redis-java-benchmarks

This repo contains scripts that produce reproducible timings of various jedis features, and to visualize what your jedis client code is spending time without modifying the code in any way.

It attempts to avoid falling victim to undesirable optimizations by offering BlackHoles and a solid collection of conventions by using [Java Microbenchmark Harness (JMH)](https://github.com/openjdk/jmh).

We've specifically focused on learning as much from JMH samples themselfs at [github.com/openjdk/jmh/jmh-samples](https://github.com/openjdk/jmh/tree/master/jmh-samples/src/main/java/org/openjdk/jmh/samples).

In the future, it will also provide mechanisms to compare jedis with other client libraries.

## Setup environment
Make sure you're on a machine with  jdk installed, and access to a running Redis Server. Install in the following order:
```bash
# Install the required packages
mvn package
```

### Running the full benchmark suite

To run the benchmarks you simply use JMH (Java Microbenchmark Harness) to run your Microbenchmarks. 

By default the JMH benchmarks will measure the average time it takes for the benchmark method to execute (a single execution) -- i.e. Average Time.

The plugin will automatically do the benchmarking and generate a result table. Run with -h to see the command line options available.

```bash
$ java -jar target/redis-java-benchmarks.jar
(...)
redis-py-benchmarks % pytest redis-py/bench_*  --benchmark-min-time 1
Benchmark                      (dataSize)  (keyMaximum)  Mode  Cnt  Score   Error  Units
BenchmarkString.BenchmarkGet           32        100000  avgt    3  0,029 ± 0,034  ms/op
BenchmarkString.BenchmarkSet           32        100000  avgt    3  0,038 ± 0,088  ms/op
BenchmarkString.BenchmarkSetx          32        100000  avgt    3  0,043 ± 0,013  ms/op

```

**REMEMBER:** To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, make sure the benchmarking environment is stable on JVM/OS/HW level, 
ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

### Profiling a specific benchmark

JMH has a few very handy profilers that help to understand your benchmarks. While
these profilers are not the substitute for full-fledged external profilers, in many
cases, these are handy to quickly dig into the benchmark behavior. 

When you are
doing many cycles of tuning up the benchmark code itself, it is important to have
a quick turnaround for the results.

As an example, to run the `stack` profiler (Simple and naive Java stack profiler) for `BenchmarkSetx` you simply:

```bash
$ java -jar target/redis-java-benchmarks.jar "com.redis.benchmarks.*.BenchmarkSetx" -prof stack
```

Which will generate the top 10 stacks per Thread state looking like:

```bash
(...)
Secondary result "com.redis.benchmarks.BenchmarkString.BenchmarkSetx:·stack":
Stack profiler:

....[Thread state distributions]....................................................................
 50,0%         RUNNABLE
 50,0%         TIMED_WAITING

....[Thread state: RUNNABLE]........................................................................
 39,3%  78,5% java.net.SocketInputStream.socketRead0
  9,4%  18,7% java.net.SocketOutputStream.socketWrite0
  0,3%   0,7% redis.clients.jedis.Protocol.sendCommand
  0,3%   0,6% java.lang.String.<init>
  0,3%   0,6% redis.clients.jedis.Connection.getStatusCodeReply
  0,2%   0,4% redis.clients.jedis.Client.setex
  0,2%   0,3% redis.clients.jedis.Protocol.toByteArray
  0,1%   0,1% org.openjdk.jmh.infra.Blackhole.consume
  0,0%   0,0% java.lang.ref.ReferenceQueue.remove
  0,0%   0,0% redis.clients.jedis.Jedis.setex

....[Thread state: TIMED_WAITING]...................................................................
 50,0% 100,0% java.lang.Object.wait
```



**Note 1**: 

Use -lprof to list the profilers. There are quite a few profilers, and this sample
will expand on the stack profiler.

```bash
$ java -jar target/redis-java-benchmarks.jar -lprof
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.openjdk.jmh.util.Utils (file:/home/fco/redislabs/redis-java-benchmarks/target/redis-java-benchmarks.jar) to method java.io.Console.encoding()
WARNING: Please consider reporting this to the maintainers of org.openjdk.jmh.util.Utils
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
Supported profilers:
          cl: Classloader profiling via standard MBeans 
        comp: JIT compiler profiling via standard MBeans 
          gc: GC profiling via standard MBeans 
         jfr: Java Flight Recorder profiler 
      pauses: Pauses profiler 
     perfc2c: Linux perf c2c profiler 
  safepoints: Safepoints profiler 
       stack: Simple and naive Java stack profiler 
```


**Note 2**: 

Many profilers have their own options,
usually accessible via -prof <profiler-name>:help.

```bash
$ java -jar target/redis-java-benchmarks.jar -prof stack:help
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.openjdk.jmh.util.Utils (file:/home/fco/redislabs/redis-java-benchmarks/target/redis-java-benchmarks.jar) to method java.io.Console.encoding()
WARNING: Please consider reporting this to the maintainers of org.openjdk.jmh.util.Utils
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
Profilers failed to initialize, exiting.
Usage: -prof <profiler-name>:opt1=value1,value2;opt2=value3

Options accepted by org.openjdk.jmh.profile.StackProfiler:                                   

  lines=<int>                      Number of stack lines to save in each stack trace. 
                                   Larger values provide more insight into who is calling 
                                   the top stack method, as the expense of more stack 
                                   trace shapes to collect. (default: [1]) 

  top=<int>                        Number of top stacks to show in the profiling results. 
                                   Larger values may catch some stack traces that linger 
                                   in the distribution tail. (default: [10]) 

  period=<int>                     Sampling period, in milliseconds. Smaller values 
                                   improve accuracy, at the expense of more profiling 
                                   overhead. (default: [10]) 

  detailLine=<bool>                Record detailed source line info. This adds the line 
                                   numbers to the recorded stack traces. (default: 
                                   [false]) 

  excludePackages=<bool>           Enable package filtering. Use excludePackages 
                                   option to control what packages are filtered (default: 
                                   [false]) 

  excludePackageNames=<package+>   Filter there packages. This is expected to be a comma-separated 
                                   list of the fully qualified package names to be excluded. 
                                   Every stack line that starts with the provided patterns 
                                   will be excluded. (default: [java., javax., sun., 
                                   sunw., com.sun., org.openjdk.jmh.]) 

  help                             Display help. 

```

### Profiling a specific benchmark with detailed source line info

```bash
$ java -jar target/redis-java-benchmarks.jar "com.redis.benchmarks.*.BenchmarkSetx" -prof stack:detailLine=true,top=20
```

Which will generate the top 10 stacks per Thread state broken by LOC looking like:
```bash
(...)
Stack profiler:


....[Thread state distributions]....................................................................
 50,1%         RUNNABLE
 49,9%         TIMED_WAITING

....[Thread state: RUNNABLE]........................................................................
 39,6%  79,1% java.net.SocketInputStream.socketRead0:-2
  9,5%  18,9% java.net.SocketOutputStream.socketWrite0:-2
  0,2%   0,4% redis.clients.jedis.Client.setex:189
  0,2%   0,4% redis.clients.jedis.Protocol.toByteArray:242
  0,2%   0,4% redis.clients.jedis.Protocol.sendCommand:106
  0,2%   0,3% redis.clients.jedis.Connection.getStatusCodeReply:270
  0,1%   0,2% java.lang.String.<init>:537
  0,1%   0,1% java.lang.invoke.MethodHandleNatives$CallSiteContext.run:92
  0,1%   0,1% org.openjdk.jmh.infra.Blackhole.consume:317

....[Thread state: TIMED_WAITING]...................................................................
 49,9% 100,0% java.lang.Object.wait:-2
```