[![Build Status](https://travis-ci.org/cronn-de/reflection-util.png?branch=master)](https://travis-ci.org/cronn-de/reflection-util)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.cronn/reflection-util/badge.svg)](http://maven-badges.herokuapp.com/maven-central/de.cronn/reflection-util)
[![Apache 2.0](https://img.shields.io/github/license/cronn-de/reflection-util.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Coverage Status](https://coveralls.io/repos/github/cronn-de/reflection-util/badge.svg?branch=master)](https://coveralls.io/github/cronn-de/reflection-util?branch=master)

# cronn reflection-util #

Utility classes that simplify common use cases of Java Reflection.

We ship two utility classes `PropertyUtils` and `ClassUtils` that are described in the following sections.

## PropertyUtils ##

Replacement for `org.apache.commons.beanutils.PropertyUtils` with deterministic behaviour
and support to retrieve [PropertyDescriptors][property-descriptor] via Java 8 method references.

Example:

```java
class MyPojo {

    private Long number;

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

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
interface MyInterface {

    void doSomething();

}
```

```java
String methodName = ClassUtils.getVoidMethodName(MyInterface.class, MyInterface::doSomething);
assertEquals("doSomething", methodName);
```

```java
Class<?>[] interfaces = { MyInterface.class };
Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), interfaces, (p, method, args) -> null);
assertEquals(MyInterface.class, ClassUtils.getRealClass(proxy));
```

## Usage ##
Add the following Maven dependency to your project:

```xml
<dependency>
    <groupId>de.cronn</groupId>
    <artifactId>reflection-util</artifactId>
    <version>2.0.2</version>
</dependency>
```

## Dependencies ##

- Java 8+

## Related Work ##

- [Apache Commons BeanUtils][apache-commons-beanutils]
- [Jodd Methref][jodd-methref]

[apache-commons-beanutils]: http://commons.apache.org/proper/commons-beanutils/
[property-descriptor]: https://docs.oracle.com/javase/10/docs/api/java/beans/PropertyDescriptor.html
[jodd-methref]: https://jodd.org/ref/methref.html
