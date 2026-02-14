package fr.anisekai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableJpaRepositories
@EnableJpaAuditing
public class AnisekaiApplication {

    @SuppressWarnings({"StaticNonFinalField", "CanBeFinal"})
    public static boolean enableDetailedOutput = false;

    static void main(String... args) {

        SpringApplication.run(AnisekaiApplication.class, args);
    }

}
