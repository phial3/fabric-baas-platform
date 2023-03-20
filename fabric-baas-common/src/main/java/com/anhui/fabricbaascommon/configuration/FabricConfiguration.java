package com.anhui.fabricbaascommon.configuration;

import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.entity.CertfileEntity;
import com.anhui.fabricbaascommon.repository.CertfileRepo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;


// @PropertySource("classpath:fabricbaascommon.properties")
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "fabric")
public class FabricConfiguration {
    private String systemChannelName;
    private String caRootUsername;
    private String caRootPassword;
    private int caServerPort;

    @Autowired
    private CertfileRepo certfileRepo;

    @Bean
    public CommandLineRunner adminCertfileInitializer() {
        return args -> {
            log.info("正在检查CA管理员信息...");
            Optional<CertfileEntity> adminCertfileOptional = certfileRepo.findById(caRootUsername);
            if (!adminCertfileOptional.isPresent()) {
                log.info("正在初始化CA管理员信息...");
                CertfileEntity certfile = new CertfileEntity(caRootUsername, caRootPassword, CertfileType.ADMIN);
                certfileRepo.save(certfile);
            }
            log.info("检查CA管理员信息完成！");
        };
    }
}

