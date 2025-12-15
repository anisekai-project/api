package fr.anisekai;

import fr.anisekai.core.persistence.AnisekaiRepositoryImpl;
import fr.anisekai.discord.InteractionService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableJpaRepositories(repositoryBaseClass = AnisekaiRepositoryImpl.class)
@EnableJpaAuditing
public class AnisekaiApplication {

    @SuppressWarnings({"StaticNonFinalField", "CanBeFinal"})
    public static boolean enableDetailedOutput = false;

    public AnisekaiApplication(InteractionService service) {

        service.login();
    }

    static void main(String... args) {

        SpringApplication.run(AnisekaiApplication.class, args);
    }

}
