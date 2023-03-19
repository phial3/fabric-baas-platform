package com.anhui.fabricbaasweb.configuration;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// @PropertySource("classpath:application.yaml")
@Configuration
@EnableOpenApi
public class SwaggerConfiguration {
    // 自定义当前文档的标题
    @Value("${swagger.title}")
    private String title;
    // 自定义当前文档的详细描述
    @Value("${swagger.description}")
    private String description;
    // 自定义当前文档的版本
    @Value("${swagger.version}")
    private String version;
    //自定义作者的信息，包括作者名字、个人主页、邮箱等相关信息
    @Value("${swagger.name}")
    private String name;
    @Value("${swagger.url}")
    private String url;
    @Value("${swagger.email}")
    private String email;

    @Bean
    public Docket createRestApi() {
//        Docket docket=new Docket(DocumentationType.SWAGGER_2)
//                .apiInfo(apiInfo())
//                .select()
//                //RequestHandlerSelectors配置要烧苗接口的方式
//                //basePackage()指定要扫描的包
//                //any()扫描全部
//                //none()不扫描
//                //withClassAnnotation()扫描类上的注解，参数是一个注解的反射对象
//                //withMethodAnnotation()扫描方法上的注解
//                .apis(RequestHandlerSelectors.basePackage("com.example.swagger.controller"))
//                //paths()过滤什么路径
//                .paths(PathSelectors.ant("/hello/**"))
//                .build();
//

        // 为swagger设置jwt
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .enable(true)
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts())
                .select()
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(title)
                .description(description)
                .version(version)
                .contact(new Contact(name, url, email))
                .build();
    }

    private List<SecurityScheme> securitySchemes() {
        List<SecurityScheme> securitySchemes = new ArrayList<>();
        securitySchemes.add(new ApiKey("Authorization", "Authorization", "header"));
        return securitySchemes;
    }

    private List<SecurityContext> securityContexts() {
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[]{new AuthorizationScope("global", "")};
        SecurityReference securityReference = new SecurityReference("Authorization", authorizationScopes);
        return Collections.singletonList(SecurityContext.builder().securityReferences(Collections.singletonList(securityReference)).build());
    }
}
