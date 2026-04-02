package ai.luumo.ukcompaniesgenderdiversity.app;

import ai.luumo.ukcompaniesgenderdiversity.app.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan(basePackageClasses = AppProperties.class)
public class UkCompaniesGenderDiversityApplication {

    public static void main(String[] args) {
        SpringApplication.run(UkCompaniesGenderDiversityApplication.class, args);
    }
}
