package de.cronn.reflection.util.immutable;

import java.util.Objects;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
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

		public Object getValueAsObject() {
			return value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Bean)) {
				return false;
			}
			Bean bean = (Bean) o;
			return Objects.equals(getValue(), bean.getValue());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getValue());
		}
	}

	private final Bean bean = new Bean();
	private final Bean otherBean = new Bean();

	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder()
			.include(ImmutableProxyBenchmark.class.getSimpleName())
			.warmupIterations(5)
			.warmupTime(TimeValue.seconds(10))
			.measurementIterations(3)
			.measurementTime(TimeValue.seconds(5))
			.forks(1)
			.build();

		new Runner(opt).run();
	}

	@Benchmark
	public void unproxiedSimpleFieldAccess(Blackhole blackhole) {
		for (long i = 0; i < 10_000; i++) {
			blackhole.consume(bean.getValue());
		}
	}

	@Benchmark
	public void proxiedSimpleFieldAccess(Blackhole blackhole) {
		Bean proxy = ImmutableProxy.create(bean);
		for (long i = 0; i < 10_000; i++) {
			blackhole.consume(proxy.getValue());
		}
	}

	@Benchmark
	public void softProxiedSimpleFieldAccess(Blackhole blackhole) {
		Bean proxy = SoftImmutableProxy.create(bean);
		for (long i = 0; i < 10_000; i++) {
			blackhole.consume(proxy.getValue());
		}
	}

	@Benchmark
	public void proxiedSimpleFieldAccessAsObject(Blackhole blackhole) {
		Bean proxy = ImmutableProxy.create(bean);
		for (long i = 0; i < 10_000; i++) {
			blackhole.consume(proxy.getValueAsObject());
		}
	}

	@Benchmark
	public void softProxiedSimpleFieldAccessAsObject(Blackhole blackhole) {
		Bean proxy = SoftImmutableProxy.create(bean);
		for (long i = 0; i < 10_000; i++) {
			blackhole.consume(proxy.getValueAsObject());
		}
	}

	@Benchmark
	public void unproxiedEquals(Blackhole blackhole) {
		for (long i = 0; i < 10_000; i++) {
			blackhole.consume(bean.equals(otherBean));
		}
	}

	@Benchmark
	public void proxiedEquals(Blackhole blackhole) {
		Bean proxy = ImmutableProxy.create(this.bean);
		for (long i = 0; i < 10_000; i++) {
			blackhole.consume(proxy.equals(otherBean));
		}
	}

	@Benchmark
	public void softProxiedEquals(Blackhole blackhole) {
		Bean proxy = SoftImmutableProxy.create(this.bean);
		for (long i = 0; i < 10_000; i++) {
			blackhole.consume(proxy.equals(otherBean));
		}
	}

}
