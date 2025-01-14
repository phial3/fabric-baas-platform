package com.anhui.fabricbaasorg.schedule;

import cn.hutool.core.lang.Assert;
import com.anhui.fabricbaasorg.entity.RemoteUserEntity;
import com.anhui.fabricbaasorg.remote.TTPOrganizationApi;
import com.anhui.fabricbaasorg.repository.RemoteUserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class RemoteHttpClientTask {
    @Autowired
    private TTPOrganizationApi ttpOrganizationApi;
    @Autowired
    private RemoteUserRepo remoteUserRepo;

    @Scheduled(fixedDelay = 3600000)
    public void refreshToken() throws Exception {
        List<RemoteUserEntity> ttpEntities = remoteUserRepo.findAll();
        Assert.isTrue(ttpEntities.size() <= 1);
        if (!ttpEntities.isEmpty()) {
            RemoteUserEntity ttpAccount = ttpEntities.get(0);
            String token = ttpOrganizationApi.login(ttpAccount.getOrganizationName(), ttpAccount.getPassword());
            log.info("已刷新HTTP客户端Token：" + token);
        }
    }
}
