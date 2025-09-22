package database;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;


public class HibernateUtil {

    private static SessionFactory sessionFactory;
    private static StandardServiceRegistry registry;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                // Create registry
                registry = new StandardServiceRegistryBuilder()
                        .configure("hibernate.cfg.xml")
                        .build();

                // Create MetadataSources
                MetadataSources sources = new MetadataSources(registry);

                // Add entity classes
                sources.addAnnotatedClass(com.networksimulation.database.entity.UserProfileEntity.class);
                sources.addAnnotatedClass(com.networksimulation.database.entity.ScoreRecordEntity.class);
                sources.addAnnotatedClass(com.networksimulation.database.entity.LevelRecordEntity.class);
                sources.addAnnotatedClass(com.networksimulation.database.entity.PlayerRecordEntity.class);
                sources.addAnnotatedClass(com.networksimulation.database.entity.MultiplayerSessionEntity.class);
                sources.addAnnotatedClass(com.networksimulation.database.entity.GameSessionEntity.class);
                sources.addAnnotatedClass(com.networksimulation.database.entity.SystemEntity.class);
                sources.addAnnotatedClass(com.networksimulation.database.entity.WireConnectionEntity.class);
                sources.addAnnotatedClass(com.networksimulation.database.entity.PortEntity.class);

                // Create Metadata
                Metadata metadata = sources.getMetadataBuilder().build();

                // Create SessionFactory
                sessionFactory = metadata.getSessionFactoryBuilder().build();

                System.out.println("Hibernate SessionFactory created successfully");

            } catch (Exception e) {
                System.err.println("Error creating Hibernate SessionFactory: " + e.getMessage());
                e.printStackTrace();

                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
                throw new RuntimeException("Failed to create Hibernate SessionFactory", e);
            }
        }
        return sessionFactory;
    }

    public static SessionFactory createSessionFactory() {
        try {
            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml");

            // Add entity classes
            configuration.addAnnotatedClass(com.networksimulation.database.entity.UserProfileEntity.class);
            configuration.addAnnotatedClass(com.networksimulation.database.entity.ScoreRecordEntity.class);
            configuration.addAnnotatedClass(com.networksimulation.database.entity.LevelRecordEntity.class);
            configuration.addAnnotatedClass(com.networksimulation.database.entity.PlayerRecordEntity.class);
            configuration.addAnnotatedClass(com.networksimulation.database.entity.MultiplayerSessionEntity.class);
            configuration.addAnnotatedClass(com.networksimulation.database.entity.GameSessionEntity.class);
            configuration.addAnnotatedClass(com.networksimulation.database.entity.SystemEntity.class);
            configuration.addAnnotatedClass(com.networksimulation.database.entity.WireConnectionEntity.class);
            configuration.addAnnotatedClass(com.networksimulation.database.entity.PortEntity.class);

            return configuration.buildSessionFactory();

        } catch (Exception e) {
            System.err.println("Error creating SessionFactory with Configuration: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create SessionFactory", e);
        }
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
            System.out.println("Hibernate SessionFactory closed");
        }

        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
            registry = null;
            System.out.println("Hibernate registry destroyed");
        }
    }

    public static boolean testConnection() {
        try {
            SessionFactory sf = getSessionFactory();
            sf.openSession().close();
            System.out.println("Database connection test successful");
            return true;
        } catch (Exception e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static String getDatabaseInfo() {
        try {
            SessionFactory sf = getSessionFactory();
            return "Hibernate Version: " + org.hibernate.Version.getVersionString() +
                    "\nSessionFactory: " + (sf != null ? "Active" : "Not initialized");
        } catch (Exception e) {
            return "Error getting database info: " + e.getMessage();
        }
    }
}


