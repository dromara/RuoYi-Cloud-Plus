package com.ruoyi.common.doc.config;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.doc.config.properties.SwaggerProperties;
import com.ruoyi.common.doc.handler.OpenApiHandler;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.*;
import org.springdoc.core.customizers.OpenApiBuilderCustomizer;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springdoc.core.customizers.ServerBaseUrlCustomizer;
import org.springdoc.core.providers.JavadocProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Swagger 文档配置
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@AutoConfiguration(before = SpringDocConfiguration.class)
@EnableConfigurationProperties(SwaggerProperties.class)
@ConditionalOnProperty(name = "swagger.enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerAutoConfiguration {

    private final SwaggerProperties swaggerProperties;

    private final ServerProperties serverProperties;

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI openApi() {
        OpenAPI openApi = new OpenAPI();
        // 文档基本信息
        SwaggerProperties.InfoProperties infoProperties = swaggerProperties.getInfo();
        Info info = convertInfo(infoProperties);
        openApi.info(info);
        // 扩展文档信息
        openApi.externalDocs(swaggerProperties.getExternalDocs());
        openApi.tags(swaggerProperties.getTags());
        openApi.paths(swaggerProperties.getPaths());
        openApi.components(swaggerProperties.getComponents());
        List<SecurityRequirement> list = new ArrayList<>();
        list.add(new SecurityRequirement().addList("apikey"));
        openApi.security(list);

        return openApi;
    }

    private Info convertInfo(SwaggerProperties.InfoProperties infoProperties) {
        Info info = new Info();
        info.setTitle(infoProperties.getTitle());
        info.setDescription(infoProperties.getDescription());
        info.setContact(infoProperties.getContact());
        info.setLicense(infoProperties.getLicense());
        info.setVersion(infoProperties.getVersion());
        return info;
    }

    /**
     * 自定义 openapi 处理器
     */
    @Bean
    public OpenAPIService openApiBuilder(Optional<OpenAPI> openAPI,
                                         SecurityService securityParser,
                                         SpringDocConfigProperties springDocConfigProperties, PropertyResolverUtils propertyResolverUtils,
                                         Optional<List<OpenApiBuilderCustomizer>> openApiBuilderCustomisers,
                                         Optional<List<ServerBaseUrlCustomizer>> serverBaseUrlCustomisers, Optional<JavadocProvider> javadocProvider) {
        return new OpenApiHandler(openAPI, securityParser, springDocConfigProperties, propertyResolverUtils, openApiBuilderCustomisers, serverBaseUrlCustomisers, javadocProvider);
    }

    /**
     * 对已经生成好的 OpenApi 进行自定义操作
     */
    @Bean
    public OpenApiCustomiser openApiCustomiser() {
        // 如果服务的自定义 Path 不存在 则采用默认去除前缀当 Path
        Map<String, String> serviceMapping = swaggerProperties.getServiceMapping();
        String appPath;
        if (serviceMapping.containsKey(appName)) {
            appPath = serviceMapping.get(appName);
        } else {
            appPath = "/" + StringUtils.substring(appName, appName.indexOf("-") + 1);
        }

        String contextPath = serverProperties.getServlet().getContextPath();
        String finalContextPath;
        if (StringUtils.isBlank(contextPath) || "/".equals(contextPath)) {
            finalContextPath = appPath;
        } else {
            finalContextPath = appPath + contextPath;
        }
        // 对所有路径增加前置上下文路径
        return openApi -> {
            Paths oldPaths = openApi.getPaths();
            Paths newPaths = new Paths();
            oldPaths.forEach((k,v) -> newPaths.addPathItem(finalContextPath + k, v));
            openApi.setPaths(newPaths);
        };
    }

}
