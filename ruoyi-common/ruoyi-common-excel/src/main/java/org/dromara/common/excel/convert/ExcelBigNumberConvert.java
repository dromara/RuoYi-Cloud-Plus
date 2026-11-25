package org.dromara.common.excel.convert;

import cn.hutool.core.convert.Convert;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.converters.Converter;
import org.apache.fesod.sheet.enums.CellDataTypeEnum;
import org.apache.fesod.sheet.metadata.GlobalConfiguration;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.metadata.data.WriteCellData;
import org.apache.fesod.sheet.metadata.property.ExcelContentProperty;

import java.math.BigDecimal;

/**
 * 大数值转换
 * Excel 数值长度位15位 大于15位的数值转换位字符串
 *
 * @author Lion Li
 */
@Slf4j
public class ExcelBigNumberConvert implements Converter<Long> {

    @Override
    public Class<Long> supportJavaTypeKey() {
        return Long.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return null;
    }

    @Override
    public Long convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        BigDecimal number = cellData.getType() == CellDataTypeEnum.NUMBER
            ? cellData.getNumberValue() : Convert.toBigDecimal(cellData.getStringValue());
        return number == null ? null : number.longValueExact();
    }

    @Override
    public WriteCellData<Object> convertToExcelData(Long object, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        if (object == null) {
            return new WriteCellData<>("");
        }
        BigDecimal number = BigDecimal.valueOf(object);
        if (number.precision() > 15) {
            return new WriteCellData<>(object.toString());
        }
        return new WriteCellData<>(number);
    }

}
