[![CI](https://github.com/cronn/reflection-util/workflows/CI/badge.svg)](https://github.com/cronn/reflection-util/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.cronn/reflection-util/badge.svg)](http://maven-badges.herokuapp.com/maven-central/de.cronn/reflection-util)
[![Apache 2.0](https://img.shields.io/github/license/cronn/reflection-util.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![codecov](https://codecov.io/gh/cronn/reflection-util/branch/master/graph/badge.svg?token=KD1WJK5ZFK)](https://codecov.io/gh/cronn/reflection-util)

# cronn reflection-util #

Utility classes that simplify common use cases of Java Reflection.

We ship the utility classes `PropertyUtils`, `ClassUtils`, and `ImmutableProxy` that are described in the following sections.

## PropertyUtils ##

Replacement for `org.apache.commons.beanutils.PropertyUtils` with deterministic behaviour
and support to retrieve [PropertyDescriptors][property-descriptor] via Java 8 method references.

Example:

```java
class MyPojo
{
    private Long number;
    // getters and setters
}
```

```java
PropertyDescriptor numberProperty = PropertyUtils.getPropertyDescriptor(MyPojo.class, MyPojo::getNumber);
MyPojo pojo = new MyPojo();
PropertyUtils.write(pojo, numberProperty, 12345L);
Long number = PropertyUtils.read(pojo, numberProperty);
assertEquals(12345L, number);
```

## ClassUtils ##

```java
interface MyInterface
{
    void doSomething();

    int getSomething();
}
```

```java
String methodName = ClassUtils.getMethodName(MyInterface.class, MyInterface::doSomething);
assertEquals("doSomething", methodName);
```

```java
String methodName = ClassUtils.getMethodName(MyInterface.class, MyInterface::getSomething);
assertEquals("getSomething", methodName);
```

```java
Class<?>[] interfaces = { MyInterface.class };
Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), interfaces, (p, method, args) -> null);
assertEquals(MyInterface.class, ClassUtils.getRealClass(proxy));
```

## ImmutableProxy ##

It’s sometimes desirable to make objects immutable to prevent programming mistakes,
such as the accidental modification of an object that is passed to another method.

In some cases the class itself cannot be made immutable. For example, because
mutability is required by the JPA provider, the JSON serialization library,
or the class is not owned by you.

Creating deep clones might come to the rescue, however, the negative
performance impact might not be acceptable and you cannot detect that the clone
is accidentally modified.

In such cases, `ImmutableProxy` can be used to create a deep but lightweight
read-only view of a [POJO](pojo) that follows the [JavaBean
conventions](java-bean-conventions).
Invocation of getters and read-only methods is allowed but other methods such
as setters are rejected by default.

Example:

```java
class MyPojo
{
    private String name;
    private List<MyPojo> children = new ArrayList<>();
    // getters and setters
}
```
```java
MyPojo original = new MyPojo();
original.setName("original");
MyPojo immutableProxy = ImmutableProxy.create(original);
immutableProxy.getName()    // ✔ returns "original"
immutableProxy.setName("…") // ✖ throws UnsupportedOperationException
```

By default, `ImmutableProxy` wraps the return value of a getter in a immutable proxy:

```java
original.getChildren().add(new MyPojo("child"));
immutableProxy.getChildren().size()  // ✔ returns 1

MyPojo firstChild = immutableProxy.getChildren().get(0);
firstChild.getName()    // ✔ returns "child"
firstChild.setName("…") // ✖ throws UnsupportedOperationException

immutableProxy.getChildren().clear() // ✖ throws UnsupportedOperationException
```

Some methods need to be annotated with `@ReadOnly`
if `ImmutableProxy` incorrectly considers the method as mutating:

```java
class MyPojo
{
    List<String> elements;

    @ReadOnly
    int size() {
        return elements.size();
    }
}
```

As a final word of warning, please note that `ImmutableProxy` follows a best-effort approach but cannot _guarantee_ to detect all possible modifications.
For example, it cannot detect that a getter actually modifies the state as a side-effect.

## SoftImmutableProxy ##

Sometimes you cannot control types of all nested objects, but you still want to make your objects immutable as deep as possible.

In such cases `SoftImmutableProxy` can help you. It behaves almost the same as `ImmutableProxy` except it doesn't throw an exception if encounters a final class that it cannot wrap into a proxy.

```java
MyFinalClass original = new MyFinalClass();
original.setProperty(new MyPojo());
List<MyFinalClass> proxyList = SoftImmutableProxy.create(List.of(original));
proxyList.get(0)               // ✔ returns original, because it cannot create a proxy for final class
proxyList.get(0).getProperty() // ✔ returns original, because parent class is not a proxy
```

### JMH Benchmark

To get a rough idea about the performance impact of the `ImmutableProxy` method interception,
we include a JMH benchmark: [ImmutableProxyBenchmark.java](src/test/java/de/cronn/reflection/util/immutable/ImmutableProxyBenchmark.java).

The benchmark compares direct method invocation of simple fields vs. method invocations through the immutable proxy.
Below you can find one of the benchmark results as conducted on a Thinkpad T480s with an Intel i7-8650U CPU running on Linux `5.10.16`.

```
# JMH version: 1.28
# VM version: JDK 15.0.2, OpenJDK 64-Bit Server VM, 15.0.2+7

[…]

# Run complete. Total time: 00:41:57

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. […]

Benchmark                                                  Mode  Cnt       Score       Error  Units
ImmutableProxyBenchmark.unproxiedEquals                   thrpt   25   77108,438 ±  2576,836  ops/s
ImmutableProxyBenchmark.proxiedEquals                     thrpt   25   10867,996 ±   175,106  ops/s
```

It shows that the invocation of the `equals()` method is about 8 times slower when routed through the `ImmutableProxy`.
However, please note that the benchmark itself runs an inner loop with 10000 cycles.
This actually gives us 10867,996 * 10000 ops/s ≈ 10 million invocations per second!

## Usage ##
Add the following Maven dependency to your project:

```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>reflection-util</artifactId>
    <version>2.11.0</version>
</dependency>
```

## Requirements ##

- Java 8+

## Related Work ##

- [Apache Commons BeanUtils](apache-commons-beanutils)
- [Jodd Methref](jodd-methref)
- `ImmutableProxy`: [Immutator](https://github.com/verhas/immutator)

[apache-commons-beanutils]: http://commons.apache.org/proper/commons-beanutils/
[property-descriptor]: https://docs.oracle.com/javase/10/docs/api/java/beans/PropertyDescriptor.html
[jodd-methref]: https://jodd.org/ref/methref.html
[pojo]: https://en.wikipedia.org/wiki/Plain_old_Java_object
[java-bean-conventions]: https://en.wikipedia.org/wiki/JavaBeans#JavaBean_conventions
[verhas/immutator]: https://github.com/verhas/immutator
