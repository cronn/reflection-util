package de.cronn.reflection.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

public final class ClassUtils {

	private static final String CGLIB_JAVASSIST_CLASS_SEPARATOR = "$$";
	private static final String BYTE_BUDDY_CLASS_SEPARATOR = "$ByteBuddy$";

	private static final Map<Class<?>, Set<MethodSignature>> methodsSignaturesCache = new ConcurrentHashMap<>();

	private ClassUtils() {
	}

	public static <T> Class<T> getRealClass(T object) {
		@SuppressWarnings("unchecked")
		Class<T> entityClass = (Class<T>) object.getClass();
		return getRealClass(entityClass);
	}

	private static <T> Class<T> getRealClass(Class<T> clazz) {
		if (isProxyClass(clazz)) {
			if (Proxy.isProxyClass(clazz)) {
				Class<?>[] interfaces = clazz.getInterfaces();
				if (interfaces.length != 1) {
					throw new IllegalArgumentException("Unexpected number of interfaces: " + interfaces.length);
				}
				@SuppressWarnings("unchecked")
				Class<T> proxiedInterface = (Class<T>) interfaces[0];
				return proxiedInterface;
			}
			@SuppressWarnings("unchecked")
			Class<T> superclass = (Class<T>) clazz.getSuperclass();
			return getRealClass(superclass);
		}
		return clazz;
	}

	public static <T> T createNewInstanceLike(T source) {
		if (source == null) {
			return null;
		}

		Class<T> sourceClass = getRealClass(source);
		T result = createNewInstance(sourceClass);
		return result;
	}

	public static boolean isFromPackage(Class<?> clazz, String packageName) {
		Package aPackage = clazz.getPackage();
		return aPackage != null && aPackage.getName().equals(packageName);
	}

	@SuppressWarnings(/* this unchecked cast is OK, since this is the contract of newInstance() */"unchecked")
	public static <T> T createNewInstance(Class<T> sourceClass) {
		try {
			Constructor<T> constructor = sourceClass.getDeclaredConstructor();
			return createInstance(constructor);
		} catch (ReflectiveOperationException e) {
			throw new ReflectionRuntimeException("Failed to construct an instance of " + sourceClass, e);
		}
	}

	private static <T> T createInstance(Constructor<T> constructor) throws ReflectiveOperationException {
		boolean accessible = constructor.isAccessible();
		try {
			if (!accessible) {
				constructor.setAccessible(true);
			}
			return constructor.newInstance();
		} finally {
			if (!accessible) {
				constructor.setAccessible(false);
			}
		}
	}

	@Nonnull
	public static <T> String getVoidMethodName(T bean, VoidMethod<T> voidMethod) {
		Class<T> beanClass = getRealClass(bean);
		return getVoidMethodName(beanClass, voidMethod);
	}

	@Nonnull
	public static <T> String getVoidMethodName(Class<T> beanClass, VoidMethod<T> voidMethod) {
		Method method = getVoidMethod(beanClass, voidMethod);
		return method.getName();
	}

	@Nonnull
	public static <T> Method getVoidMethod(Class<T> beanClass, VoidMethod<T> voidMethod) {
		PropertyDescriptorCache<T> cache = PropertyUtils.getCache(beanClass);
		return cache.getMethod(voidMethod);
	}

	public static boolean isProxy(Object object) {
		return object != null && isProxyClass(object.getClass());
	}

	public static boolean isProxyClass(Class<?> clazz) {
		if (clazz == null) {
			return false;
		}

		if (Proxy.isProxyClass(clazz)) {
			return true;
		}

		return clazz.getName().contains(BYTE_BUDDY_CLASS_SEPARATOR)
			|| clazz.getName().contains(CGLIB_JAVASSIST_CLASS_SEPARATOR);
	}

	public static boolean haveSameSignature(Method oneMethod, Method otherMethod) {
		return new MethodSignature(oneMethod).equals(new MethodSignature(otherMethod));
	}

	public static List<Method> findMethodsByArgumentTypes(Class<?> classToSearchIn, Class<?>... argumentTypes) {
		return Stream.of(classToSearchIn.getMethods())
			.filter(method -> Arrays.equals(method.getParameterTypes(), argumentTypes))
			.collect(Collectors.toList());
	}

	public static boolean hasMethodWithSameSignature(Class<?> aClass, Method method) {
		Set<MethodSignature> aClassMethods = methodsSignaturesCache.computeIfAbsent(aClass, ClassUtils::getAllDeclaredMethodSignatures);
		return aClassMethods.contains(new MethodSignature(method));
	}

	public static Set<Method> getAllDeclaredMethods(Class<?> aClass) {
		Set<Method> methods = new LinkedHashSet<>();
		methods.addAll(Arrays.asList(aClass.getDeclaredMethods()));
		if (aClass.getSuperclass() != null && !aClass.getSuperclass().equals(Object.class)) {
			methods.addAll(getAllDeclaredMethods(aClass.getSuperclass()));
		}
		for (Class<?> interfaceClass : aClass.getInterfaces()) {
			methods.addAll(getAllDeclaredMethods(interfaceClass));
		}
		return Collections.unmodifiableSet(methods);
	}

	public static Set<MethodSignature> getAllDeclaredMethodSignatures(Class<?> aClass) {
		return getAllDeclaredMethods(aClass).stream()
			.map(MethodSignature::new)
			.collect(Collectors.toCollection(TreeSet::new));
	}

}
