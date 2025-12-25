package fr.anisekai.core.persistence;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateConfiguration {

    private final EntityManagerFactory entityManagerFactory;
    private final EntityEventProcessor postUpdateListener;

    public HibernateConfiguration(EntityManagerFactory entityManagerFactory, EntityEventProcessor postUpdateListener) {

        this.entityManagerFactory = entityManagerFactory;
        this.postUpdateListener   = postUpdateListener;
    }

    @PostConstruct
    public void registerListeners() {

        SessionFactoryImpl sessionFactory = this.entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry()
                                                       .getService(EventListenerRegistry.class);

        if (registry == null) {
            throw new IllegalStateException("Could not obtain EventListenerRegistry");
        }

        registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(this.postUpdateListener);
        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(this.postUpdateListener);
        registry.getEventListenerGroup(EventType.POST_DELETE).appendListener(this.postUpdateListener);
    }

}
