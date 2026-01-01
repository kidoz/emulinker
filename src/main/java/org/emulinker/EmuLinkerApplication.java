package org.emulinker;

import java.util.Date;
import org.emulinker.release.ReleaseInfo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EmuLinkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmuLinkerApplication.class, args);
    }

    @Bean
    public CommandLineRunner startupRunner(ReleaseInfo releaseInfo) {
        return args -> {
            System.out.println("EmuLinker server Starting...");
            System.out.println(releaseInfo.getWelcome());
            System.out.println("EmuLinker server is running @ " + new Date());
        };
    }
}
