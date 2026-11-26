package org.dromara.workflow.controller;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.workflow.domain.bo.FlowSpelBo;
import org.dromara.workflow.domain.vo.FlowSpelVo;
import org.dromara.workflow.service.IFlwSpelService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlwSpelControllerTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void exposesExpressionQueriesAndSaveOutcomes(boolean saved) {
        var expressions = mock(IFlwSpelService.class);
        var controller = new FlwSpelController(expressions);
        var criteria = new FlowSpelBo();
        var page = new PageQuery();
        var row = new FlowSpelVo();
        when(expressions.queryPageList(criteria, page)).thenReturn(PageResult.build(List.of(row), 7L));
        when(expressions.queryById(42L)).thenReturn(row);
        when(expressions.insertByBo(criteria)).thenReturn(saved);
        when(expressions.updateByBo(criteria)).thenReturn(saved);
        when(expressions.deleteWithValidByIds(List.of(42L), true)).thenReturn(saved);

        assertEquals(7L, controller.list(criteria, page).getData().getTotal());
        assertSame(row, controller.getInfo(42L).getData());
        assertEquals(saved, R.isSuccess(controller.add(criteria)));
        assertEquals(saved, R.isSuccess(controller.edit(criteria)));
        assertEquals(saved, R.isSuccess(controller.remove(new Long[]{42L})));
    }
}
