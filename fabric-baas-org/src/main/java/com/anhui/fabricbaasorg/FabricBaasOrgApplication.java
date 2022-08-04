package com.anhui.fabricbaasorg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.anhui"})
@EnableMongoRepositories(basePackages = {"com.anhui.fabricbaasorg", "com.anhui.fabricbaascommon"})
@EnableScheduling
@EnableTransactionManagement
public class FabricBaasOrgApplication {

    public static void main(String[] args) {
        SpringApplication.run(FabricBaasOrgApplication.class, args);
    }

}
