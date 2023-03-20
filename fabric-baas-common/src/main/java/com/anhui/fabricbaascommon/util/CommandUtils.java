package com.anhui.fabricbaascommon.util;


import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CommandUtils {
    public static Map<String, String> buildEnvs(String... params) {
        Assert.isTrue(params.length % 2 == 0);
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < params.length; i += 2) {
            result.put(params[i], params[i + 1]);
        }
        return result;
    }

    public static String exec(String... cmd) throws IOException, InterruptedException {
        return exec(Collections.emptyMap(), cmd);
    }

    public static String exec(Map<String, String> envs, String... cmd) throws IOException, InterruptedException {
        log.info("执行命令：" + String.join(" ", cmd));
        ProcessBuilder builder = new ProcessBuilder(cmd);
        Map<String, String> environment = builder.environment();
        environment.putAll(envs);
        log.info("环境变量：" + environment);
        Process process = builder.start();
        process.waitFor();
        String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8) + IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
        log.info("命令输出：\n" + output);
        return output;
    }

    public static Process asyncExec(String... cmd) throws IOException {
        return asyncExec(Collections.emptyMap(), cmd);
    }

    public static Process asyncExec(Map<String, String> envs, String... cmd) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(cmd);
        Map<String, String> environment = builder.environment();
        environment.putAll(envs);
        return builder.start();
    }
}
