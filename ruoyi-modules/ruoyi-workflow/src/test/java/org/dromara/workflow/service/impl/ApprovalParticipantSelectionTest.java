package org.dromara.workflow.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.system.api.RemoteDeptService;
import org.dromara.system.api.RemoteUserService;
import org.dromara.workflow.rule.SpelRuleComponent;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApprovalParticipantSelectionTest {
    @Test
    void resolvesEachKindAndDeduplicatesOverlappingMembers() {
        RemoteUserService users = mock(RemoteUserService.class);
        var service = new FlwTaskAssigneeServiceImpl(null);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "remoteUserService", users);

        when(users.selectUserIdsByRoleIds(List.of(2L))).thenReturn(List.of(1L, 10L));
        when(users.selectUserIdsByDeptIds(List.of(2L))).thenReturn(List.of(10L, 20L));
        when(users.selectUserIdsByPostIds(List.of(2L))).thenReturn(List.of(30L));
        var result = service.convertPermissions(List.of("1", "role:2", "dept:2", "post:2", "unknown:2"));
        assertEquals(4, result.size());
        assertEquals(Set.of("1", "10", "20", "30"), Set.copyOf(result));
    }

    @Test
    void feedbackKeepsInputOrderAndResolvesOverlappingIdsByType() {
        RemoteUserService users = mock(RemoteUserService.class);
        RemoteDeptService departments = mock(RemoteDeptService.class);
        var roles = mock(org.dromara.system.api.RemoteRoleService.class);
        var posts = mock(org.dromara.system.api.RemotePostService.class);
        var expressions = mock(org.dromara.workflow.service.IFlwSpelService.class);
        var service = new FlwTaskAssigneeServiceImpl(expressions);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "remoteUserService", users);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "remoteDeptService", departments);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "remoteRoleService", roles);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "remotePostService", posts);

        assertTrue(service.handlerFeedback(List.of()).isEmpty());
        String expression = "#leader(42)";
        var ids = List.of(42L);
        when(users.selectUserNicksByIds(ids)).thenReturn(java.util.Map.of(42L, "员工"));
        when(roles.selectRoleNamesByIds(ids)).thenReturn(java.util.Map.of(42L, "经理"));
        when(departments.selectDeptNamesByIds(ids)).thenReturn(java.util.Map.of(42L, "财务"));
        when(posts.selectPostNamesByIds(ids)).thenReturn(java.util.Map.of(42L, "会计"));
        when(expressions.selectRemarksBySpels(List.of(expression))).thenReturn(java.util.Map.of(expression, "负责人"));
        var input = List.of("post:42", expression, "dept:42", "42", "role:42", "unknown:42");
        var result = service.handlerFeedback(input);
        assertEquals(input, result.stream().map(row -> row.getStorageId()).toList());
        assertEquals(java.util.Arrays.asList("会计", "负责人", "财务", "员工", "经理", null),
            result.stream().map(row -> row.getHandlerName()).toList());
    }

    @Test
    void departmentLeaderMustBeConfigured() {
        RemoteDeptService departments = mock(RemoteDeptService.class);
        var rule = new SpelRuleComponent(departments);
        when(departments.selectDeptLeaderById(4L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> rule.selectDeptLeaderById(4L));
        when(departments.selectDeptLeaderById(4L)).thenReturn(23L);
        assertEquals(23L, rule.selectDeptLeaderById(4L));
    }
}
