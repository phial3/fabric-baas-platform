package com.anhui.fabricbaasttp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.anhui"})
@EnableMongoRepositories(basePackages = {
        "com.anhui.fabricbaasttp",
        "com.anhui.fabricbaascommon"
})
@EnableTransactionManagement
public class FabricBaasTtpApplication {

    public static void main(String[] args) {
        SpringApplication.run(FabricBaasTtpApplication.class, args);
    }

}
