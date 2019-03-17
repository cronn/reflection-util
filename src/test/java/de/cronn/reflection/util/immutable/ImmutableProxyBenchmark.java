package de.cronn.reflection.util.immutable;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Note: You need to enable annotation processing in your IDE to run this class.
 * See https://github.com/artyushov/idea-jmh-plugin/issues/13
 *
 * In IntelliJ: <tt>Preferences -> Build, Execution, Deployment -> Compiler -> Annotation Processors</tt>
 */
@State(Scope.Benchmark)
public class ImmutableProxyBenchmark {

	public static class Bean {

		private Long value = 0L;

		public void setValue(Long value) {
			this.value = value;
		}

		public Long getValue() {
			return value;
		}
	}

	private final Bean bean = new Bean();

	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder()
			.include(ImmutableProxyBenchmark.class.getSimpleName())
			.warmupIterations(3)
			.warmupTime(TimeValue.seconds(10))
			.measurementIterations(3)
			.measurementTime(TimeValue.seconds(5))
			.forks(1)
			.build();

		new Runner(opt).run();
	}

	@Benchmark
	public void unproxiedSimpleFieldAccess() {
		for (long i = 0; i < 10_000; i++) {
			Long value = bean.getValue();
			bean.setValue(value);
		}
	}

	@Benchmark
	public void proxiedSimpleFieldAccess() {
		Bean proxy = ImmutableProxy.create(bean);
		for (long i = 0; i < 10_000; i++) {
			Long value = proxy.getValue();
			bean.setValue(value);
		}
	}

}
