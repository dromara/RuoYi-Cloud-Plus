package org.dromara.resource.runner;

import org.dromara.resource.service.ISysOssConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.*;

class ResourceApplicationRunnerTest {
    @Test
    void initializesOssConfigurationOnStartup() throws Exception {
        var configurations = mock(ISysOssConfigService.class);
        new ResourceApplicationRunner(configurations).run(new DefaultApplicationArguments());
        verify(configurations).init();
    }
}
