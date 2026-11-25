package org.dromara.common.excel.core;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.metadata.Head;
import org.apache.fesod.sheet.write.handler.SheetWriteHandler;
import org.apache.fesod.sheet.write.merge.AbstractMergeStrategy;
import org.apache.fesod.sheet.write.metadata.holder.WriteSheetHolder;
import org.apache.fesod.sheet.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.List;

/**
 * 列值重复合并策略
 *
 * @author Lion Li
 */
@Slf4j
public class CellMergeStrategy extends AbstractMergeStrategy implements SheetWriteHandler {

    /**
     * 合并单元格区域集合。
     */
    private List<CellRangeAddress> cellList;
    private final CellMergeHandler handler;
    private final List<?> rows;

    public CellMergeStrategy(List<CellRangeAddress> cellList) {
        this.cellList = cellList;
        this.handler = null;
        this.rows = List.of();
    }

    public CellMergeStrategy(List<?> list, boolean hasTitle) {
        this(list, hasTitle, 1);
    }

    public CellMergeStrategy(List<?> list, boolean hasTitle, int rowIndex) {
        this.handler = CellMergeHandler.of(hasTitle, rowIndex);
        this.rows = list;
    }

    @Override
    protected void merge(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
        if (CollUtil.isEmpty(cellList)) {
            return;
        }
        // 单元格写入了,遍历合并区域,如果该Cell在区域内,但非首行,则清空
        final int rowIndex = cell.getRowIndex();
        for (CellRangeAddress cellAddresses : cellList) {
            final int firstRow = cellAddresses.getFirstRow();
            if (cellAddresses.isInRange(cell) && rowIndex != firstRow) {
                cell.setBlank();
            }
        }
    }

    @Override
    public void afterSheetCreate(final WriteWorkbookHolder writeWorkbookHolder, final WriteSheetHolder writeSheetHolder) {
        if (handler != null) {
            var head = writeSheetHolder.excelWriteHeadProperty();
            cellList = head.getHeadClazz() == null ? handler.handle(rows) : handler.handle(rows, head);
        }
        if (CollUtil.isEmpty(cellList)) {
            return;
        }
        // 在 Sheet 创建时提前写入合并区域；后续写入只会影响首格，不会移除合并
        final Sheet sheet = writeSheetHolder.getSheet();
        for (CellRangeAddress item : cellList) {
            sheet.addMergedRegion(item);
        }
    }

}
