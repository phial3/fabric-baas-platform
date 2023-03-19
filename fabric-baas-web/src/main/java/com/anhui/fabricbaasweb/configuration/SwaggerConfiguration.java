package com.anhui.fabricbaasweb.configuration;

import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// @PropertySource("classpath:fabricbaasweb.properties")
@Configuration
public class SwaggerConfiguration {
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

//        Docket docket=new Docket(DocumentationType.SWAGGER_2)
//                .apiInfo(apiInfo())
//                .enable(true)//表示是否使用Swagger,如果为false,则Swagger不能再浏览器中访问
//                .select()
//                .apis(RequestHandlerSelectors.basePackage("com.example.swagger.controller"))
//                .build();
//

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .enable(true)
                .select()
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                .paths(PathSelectors.any())
                .build()
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Fabric BaaS Platform")
                .description("Restful API Doc")
                .version("1.0")
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
