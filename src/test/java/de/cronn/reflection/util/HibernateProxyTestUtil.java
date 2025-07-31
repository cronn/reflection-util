package de.cronn.reflection.util;

import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

class HibernateProxyTestUtil {

	@Entity
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	static void runWithHibernateProxy(Consumer<Person> hibernateProxyConsumer) {
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
			.applySetting("hibernate.connection.driver_class", "org.h2.Driver")
			.applySetting("hibernate.connection.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
			.applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
			.applySetting("hibernate.hbm2ddl.auto", "create-drop")
			.build();

		SessionFactory sessionFactory = new MetadataSources(registry)
			.addAnnotatedClass(Person.class)
			.buildMetadata()
			.buildSessionFactory();

		try (Session session = sessionFactory.openSession()) {
			Person personProxy = session.getReference(Person.class, 123L);
			hibernateProxyConsumer.accept(personProxy);
		} finally {
			sessionFactory.close();
		}
	}
}
