# Dragonboat-Backend 数据来源分析

Dragonboat-Backend 项目从图片或表格文件提取数据结构，并生成数据库表设计建议。

## 触发词

- "数据来源分析"
- "data source analysis"
- "分析数据结构"
- "从图片生成表"
- "Excel转数据库"
- "表格分析"
- "analyze data"
- "图片数据提取"

## 功能概述

本技能帮助你从各种数据源（图片、Excel、CSV）中提取数据结构，并自动生成符合 RuoYi-Cloud-Plus 规范的数据库设计建议。

**支持的数据源**:
- 📊 Excel 文件 (.xlsx, .xls)
- 📄 CSV 文件
- 🖼️ 图片文件（包含表格）
- 📋 PDF 文件（包含表格）
- 📝 JSON/XML 文件（结构化数据）

## 工作流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     数据来源分析工作流                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. 数据收集                                                                   │
│     ↓                                                                       │
│  2. 数据提取 (ExcelUtil / OCR)                                               │
│     ↓                                                                       │
│  3. 结构分析 (字段识别、类型推断、关系分析)                                    │
│     ↓                                                                       │
│  4. 数据库设计生成 (符合 RuoYi-Cloud-Plus 规范)                                │
│     ↓                                                                       │
│  5. DDL 生成                                                                  │
│     ↓                                                                       │
│  6. 业务代码生成 (ry-code-generator)                                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 数据源处理

### 数据源 1: Excel/CSV 文件

#### 步骤 1: 上传或指定文件

```bash
# 假设文件位于项目目录
/data/source/products.xlsx
/data/source/orders.csv
```

#### 步骤 2: 使用 EasyExcel 读取数据

**创建临时的分析对象**:
```java
package org.dromara.common.excel.analysis;

import lombok.Data;
import com.alibaba.excel.annotation.ExcelProperty;

/**
 * 数据分析临时对象 - 直接映射 Excel 列
 */
@Data
public class DataAnalysisRow {

    @ExcelProperty(value = "*")  // 捕获所有列
    private Map<String, Object> rawData;
}
```

**读取并分析数据**:
```java
public class DataSourceAnalyzer {

    /**
     * 分析 Excel 文件结构
     */
    public TableStructure analyzeExcel(MultipartFile file) throws IOException {
        // 读取 Excel 第一行（表头）
        List<Map<Integer, String>> headers = ExcelUtil.readExcel(
            file.getInputStream(),
            DataAnalysisRow.class
        );

        // 读取前 10 行数据用于类型推断
        List<Map<Integer, String>> sampleData = ExcelUtil.importExcel(
            file.getInputStream(),
            DataAnalysisRow.class
        );

        // 分析字段类型
        return analyzeStructure(headers, sampleData);
    }

    /**
     * 分析数据结构
     */
    private TableStructure analyzeStructure(
        List<Map<Integer, String>> headers,
        List<Map<Integer, String>> sampleData
    ) {
        TableStructure structure = new TableStructure();

        // 1. 提取表名建议
        structure.setTableName(generateTableName(file.getOriginalFilename()));

        // 2. 分析每个字段
        for (Map.Entry<Integer, String> header : headers) {
            FieldInfo field = analyzeField(header.getValue(), sampleData);
            structure.addField(field);
        }

        // 3. 推断关系
        structure.inferRelationships();

        return structure;
    }
}
```

### 数据源 2: 图片文件 (OCR)

#### 步骤 1: 使用 OCR 提取表格

**工具选择**:
- Tesseract OCR (开源)
- 百度 OCR API
- 腾讯云 OCR API
- ABBYY FineReader

#### 步骤 2: 调用 OCR API

```java
/**
 * 图片数据提取器
 */
public class ImageDataExtractor {

    /**
     * 从图片中提取表格数据
     */
    public List<TableRow> extractTableFromImage(String imageUrl) {
        // 使用 OCR API
        // 1. 调用 OCR 服务识别图片
        // 2. 解析识别结果，提取表格结构
        // 3. 返回结构化数据

        // 示例：使用百度 OCR
        OcrClient ocrClient = new OcrClient();
        String result = ocrClient.tableRecognition(imageUrl);

        // 解析 JSON 结果
        return parseOcrResult(result);
    }
}
```

**OCR 结果解析**:
```json
{
  "words_result": [
    {"location": {"top": 10, "left": 20, "width": 100, "height": 30}, "words": "用户ID"},
    {"location": {"top": 10, "left": 130, "width": 80, "height": 30}, "words": "用户名"}
  ]
}
```

### 数据源 3: CSV 文件

#### 自动分析 CSV 结构

```java
/**
 * CSV 分析器
 */
public class CsvAnalyzer {

    /**
     * 分析 CSV 文件结构
     */
    public TableStructure analyzeCsv(String filePath) throws IOException {
        TableStructure structure = new TableStructure();

        // 读取 CSV
        CsvReader reader = new CsvReader(filePath);

        // 解析表头
        String[] headers = reader.readHeaders();
        structure.setTableName(generateTableName(filePath));

        // 解析每列类型
        for (int i = 0; i < headers.length; i++) {
            String columnName = headers[i];

            // 读取前几行推断类型
            String[] sampleValues = readSampleValues(reader, i, 10);
           FieldType fieldType = inferFieldType(sampleValues);

            FieldInfo field = new FieldInfo();
            field.setName(columnName);
            field.setType(fieldType);
            field.setNullable(isNullable(sampleValues));

            structure.addField(field);
        }

        return structure;
    }
}
```

## 结构分析

### 字段类型推断规则

| Excel 数据 | Java 类型 | 数据库类型 | 示例 |
|-----------|----------|------------|------|
| 纯数字 | Long / Integer | bigint / int | 123, 1 |
| 带小数数字 | BigDecimal | decimal(10,2) | 123.45 |
| 日期字符串 | LocalDateTime | datetime | 2024-01-17 |
| 时间字符串 | LocalTime | time | 14:30:00 |
| 短文本 | String | varchar(50) | 状态 |
| 长文本 | String | varchar(500) | 描述 |
| 布尔值 | Integer | tinyint(1) | 0/1 |

### 字段名规范化

**规则**:
1. 转换为驼峰命名
2. 移除特殊字符
3. 中文转拼音（可选）
4. 添加统一前缀（可选）

**示例**:
```java
"用户ID"      → "userId"
"用户名"      → "userName"
"创建时间"    → "createTime"
"订单编号"    → "orderNo"
"是否删除"    → "delFlag"
```

## 数据库设计生成

### 自动生成表结构

基于 RuoYi-Cloud-Plus 规范自动生成:

```sql
CREATE TABLE `{table_name}` (
  `id` bigint NOT NULL COMMENT '{primary_comment}',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `{field1}` {type1} COMMENT '{comment1}',
  `{field2}` {type2} COMMENT '{comment2}',
  ...
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='{table_comment}';
```

### 字段映射规则

| 数据源特征 | 数据库类型 | Java 类型 | 是否必填 | 示例 |
|-----------|----------|----------|---------|------|
| 主键/编号 | bigint | Long | 是 | id |
| 名称/标题 | varchar(100) | String | 是 | name |
| 描述/备注 | varchar(500) | String | 否 | remark |
| 状态/类型 | varchar(20) | String | 否 | status |
| 金额/价格 | decimal(10,2) | BigDecimal | 是 | amount |
| 数量/库存 | int | Integer | 是 | quantity |
| 日期时间 | datetime | LocalDateTime | 是 | createTime |
| 是否/布尔 | tinyint(1) | Integer | 否 | isValid |

## 完整示例

### 示例 1: 从 Excel 生成数据库表

**输入**: `products.xlsx`

```
| 产品ID | 产品名称 | 产品价格 | 库存数量 | 创建时间 |
|--------|----------|----------|----------|----------|
| P001   | 手机A    | 2999.00  | 100      | 2024-01-17 |
| P002   | 电脑B    | 5999.00  | 50       | 2024-01-17 |
```

**输出**: 数据库设计建议

```sql
-- 表名: business_product
CREATE TABLE `business_product` (
  `id` bigint NOT NULL COMMENT '产品ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `product_code` varchar(50) NOT NULL COMMENT '产品编号',
  `product_name` varchar(100) NOT NULL COMMENT '产品名称',
  `product_price` decimal(10,2) NOT NULL COMMENT '产品价格',
  `stock_quantity` int NOT NULL DEFAULT 0 COMMENT '库存数量',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_product_code` (`product_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务产品表';
```

**Entity 类**:
```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("business_product")
public class BusinessProduct extends TenantEntity {

    /**
     * 产品ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 产品编号
     */
    private String productCode;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 产品价格
     */
    private BigDecimal productPrice;

    /**
     * 库存数量
     */
    private Integer stockQuantity;
}
```

### 示例 2: 从图片生成数据库表

**输入**: `table_image.png`

**OCR 提取**:
```
| 订单编号 | 客户名称 | 订单金额 | 订单状态 | 下单时间 |
|----------|----------|----------|----------|----------|
| SO001    | 张三     | 1000.00  | 待支付   | 2024-01-17 |
| SO002    | 李四     | 2000.00  | 已支付   | 2024-01-17 |
```

**输出**: 数据库设计建议

```sql
-- 表名: business_order
CREATE TABLE `business_order` (
  `id` bigint NOT NULL COMMENT '订单ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `order_no` varchar(50) NOT NULL COMMENT '订单编号',
  `customer_name` varchar(100) NOT NULL COMMENT '客户名称',
  `order_amount` decimal(10,2) NOT NULL COMMENT '订单金额',
  `order_status` varchar(20) DEFAULT 'pending' COMMENT '订单状态',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_order_status` (`order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务订单表';
```

### 示例 3: 从 CSV 生成数据库表

**输入**: `users.csv`

```csv
user_id,user_name,email,phone,register_date
1001,张三,zhangsan@example.com,13800138000,2024-01-17
1002,李四,lisi@example.com,13900139000,2024-01-17
```

**输出**: 数据库设计建议

```sql
-- 表名: business_user
CREATE TABLE `business_user` (
  `id` bigint NOT NULL COMMENT '用户ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) COMMENT '邮箱',
  `phone` varchar(20) COMMENT '手机号',
  `register_date` datetime NOT NULL COMMENT '注册日期',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务用户表';
```

## 智能建议

### 1. 表名生成规则

```java
/**
 * 表名生成器
 */
public class TableNameGenerator {

    /**
     * 从文件名生成表名
     */
    public String generateTableName(String fileName) {
        // 移除扩展名
        String name = fileName.replaceAll("\\.(xlsx|xls|csv)$", "");

        // 转换为下划线命名
        name = camelToSnake(name);

        // 添加业务前缀（如果需要）
        if (!name.startsWith("business_")) {
            name = "business_" + name;
        }

        return name;
    }

    /**
     * 驼峰转下划线
     */
    private String camelToSnake(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
```

### 2. 字段类型智能推断

```java
/**
 * 字段类型推断器
 */
public class FieldTypeInferer {

    /**
     * 推断字段类型
     */
    public FieldType inferType(String columnName, List<String> sampleValues) {
        if (isPrimaryKey(columnName)) {
            return FieldType.PRIMARY_KEY;
        }

        if (allNumeric(sampleValues)) {
            if (hasDecimal(sampleValues)) {
                return FieldType.DECIMAL;
            }
            return FieldType.INTEGER;
        }

        if (isDate(sampleValues)) {
            return FieldType.DATETIME;
        }

        if (isBoolean(sampleValues)) {
            return FieldType.BOOLEAN;
        }

        // 默认为字符串
        returnFieldType.VARCHAR;
    }
}
```

### 3. 关系自动识别

```java
/**
 * 关系推断器
 */
public class RelationshipInferer {

    /**
     * 推断字段关系
     */
    public void inferRelationships(TableStructure structure) {
        Map<String, List<FieldInfo>> groupedFields = structure.getGroupedFields();

        // 识别外键关系
        for (Map.Entry<String, List<FieldInfo>> entry : groupedFields.entrySet()) {
            String suffix = entry.getKey(); // 如 "id", "name", "code"

            // 如果其他表有相同的字段，可能存在外键关系
            if (isForeignKey(suffix)) {
                structure.addRelationship(suffix);
            }
        }

        // 识别一对多关系
        for (FieldInfo field : structure.getFields()) {
            if (field.isPlural()) {
                structure.addOneToManyRelationship(field);
            }
        }
    }
}
```

## 业务代码生成

### 生成流程

```
数据来源分析
    ↓
生成 DDL SQL
    ↓
生成 Entity 类
    ↓
生成 Bo/Vo 类
    ↓
调用 /skill ry-code-generator
```

### 集成代码生成器

分析完成后，自动调用代码生成技能：

```bash
# 1. 生成 DDL
/skill ry-data-source-analysis --input products.xlsx --output-ddl

# 2. 生成代码
/skill ry-code-generator --input products.xlsx

# 3. 完整流程
/skill ry-workflow "从 products.xlsx 创建产品管理模块"
```

## 实用工具类

### Excel 数据分析工具

```java
/**
 * Excel 数据分析工具类
 */
public class ExcelAnalysisUtils {

    /**
     * 从 Excel 文件提取表结构
     *
     * @param filePath Excel 文件路径
     * @return 表结构分析结果
     */
    public static TableAnalysisResult analyzeExcel(String filePath) {
        try {
            // 读取 Excel
            List<Map<String, Object>> data = readExcel(filePath);

            // 分析表头
            List<ColumnInfo> columns = extractColumns(data);

            // 分析类型
            Map<String, String> types = inferTypes(data);

            // 生成表名
            String tableName = generateTableName(filePath);

            // 生成 DDL
            String ddl = generateDDL(tableName, columns, types);

            return TableAnalysisResult.builder()
                .tableName(tableName)
                .columns(columns)
                .types(types)
                .ddl(ddl)
                .build();

        } catch (Exception e) {
            throw new ServiceException("Excel 分析失败: " + e.getMessage());
        }
    }

    /**
     * 生成符合 RuoYi-Cloud-Plus 规范的表结构
     */
    public static TableStructure analyzeAndDesign(String filePath) {
        TableAnalysisResult analysis = analyzeExcel(filePath);

        // 转换为 RuoYi 格式
        return convertToRuoYiFormat(analysis);
    }
}
```

### 图片数据提取工具

```java
/**
 * 图片数据提取工具类
 */
public class ImageAnalysisUtils {

    /**
     * 从图片中提取表格结构
     *
     * @param imagePath 图片路径
     * @return 表结构分析结果
     */
    public static TableAnalysisResult analyzeImage(String imagePath) {
        try {
            // 1. 调用 OCR API
            OcrResult ocrResult = OcrClient.recognizeTable(imagePath);

            // 2. 解析表格数据
            List<List<String>> tableData = parseOcrTable(ocrResult);

            // 3. 分析表结构
            TableStructure structure = analyzeTableData(tableData);

            // 4. 生成 DDL
            String ddl = generateDDL(structure);

            return TableAnalysisResult.builder()
                .sourceType("IMAGE")
                .tableName(structure.getTableName())
                .columns(structure.getColumns())
                .ddl(ddl)
                .build();

        } catch (Exception e) {
            throw new ServiceException("图片分析失败: " + e.getMessage());
        }
    }
}
```

## 与其他技能的集成

### 1. 与代码生成器集成

```java
/**
 * 数据来源分析后自动生成代码
 */
public class DataAnalysisCodeGenerator {

    /**
     * 分析数据源并生成代码
     */
    public void generateFromDataSource(String dataSourcePath) {
        // 1. 分析数据源
        TableAnalysisResult analysis = ExcelAnalysisUtils.analyzeAndDesign(dataSourcePath);

        // 2. 生成 DDL
        executeDDL(analysis.getDdl());

        // 3. 创建临时文件供代码生成器使用
        createTempImportFile(analysis);

        // 4. 调用代码生成器
        System.out.println("请使用 /skill ry-code-generator 生成代码");
    }
}
```

### 2. 与工作流集成

**在 `ry-workflow.md` 中添加场景**:

```markdown
### 场景 6: 从数据源创建模块

```
1. 准备数据源文件 (Excel/图片/CSV)
2. /skill ry-data-source-analysis products.xlsx
3. 审查分析结果
4. 执行生成的 DDL
5. /skill ry-code-generator 使用导入模板
6. 实现业务逻辑
7. /skill commit-commands:commit
8. /skill create-pr
```

**相关 Skills**:
- `/skill ry-data-source-analysis` - 数据来源分析
- `/skill ry-code-generator` - 代码生成器
- `/skill ry-multi-tenant` - 多租户配置

## 使用示例

### 示例 1: 从 Excel 创建产品管理表

```bash
# 1. 分析 Excel 文件
/skill ry-data-source-analysis /data/products.xlsx

# 输出:
# 表名: business_product
# 字段: productCode, productName, productPrice, stockQuantity
# 类型: varchar(50), varchar(100), decimal(10,2), int
# DDL: [生成的 CREATE TABLE 语句]

# 2. 执行 DDL
mysql -u root -p ry-vue3 < products_ddl.sql

# 3. 使用代码生成器
/skill ry-code-generator

# 4. 选择 business_product 表
# 5. 配置字段信息
# 6. 生成代码
```

### 示例 2: 从图片创建订单表

```bash
# 1. 上传图片到服务器
scp /data/table_image.png server:/tmp/

# 2. 分析图片
/skill ry-data-source-analysis /tmp/table_image.png

# 输出:
# 表名: business_order
# 字段: orderNo, customerName, orderAmount, orderStatus, orderTime
# ...

# 3. 执行 DDL
# 4. 生成代码
```

### 示例 3: 从 CSV 创建用户表

```bash
# 1. 分析 CSV 文件
/skill ry-data-source-analysis /data/users.csv

# 输出:
# 表名: business_user
# 字段: userId, userName, email, phone, registerDate
# ...

# 2. 执行 DDL
# 3. 生成代码
```

## 数据质量检查

### 数据验证规则

```java
/**
 * 数据质量检查器
 */
public class DataQualityChecker {

    /**
     * 检查数据质量问题
     */
    public List<QualityIssue> checkQuality(List<Map<String, Object>> data) {
        List<QualityIssue> issues = new ArrayList<>();

        // 1. 检查空值
        long nullCount = countNullValues(data);
        if (nullCount > data.size() * 0.1) {
            issues.add(new QualityIssue(
                "空值过多",
                "空值占比: " + (nullCount * 100 / data.size()) + "%",
                QualityLevel.MAJOR
            ));
        }

        // 2. 检查数据类型一致性
        Map<String, Set<String>> typeInconsistencies = checkTypeConsistency(data);
        if (!typeInconsistencies.isEmpty()) {
            issues.add(new QualityIssue(
                "数据类型不一致",
                "字段: " + typeInconsistencies,
                QualityLevel.MAJOR
            ));
        }

        // 3. 检查重复数据
        long duplicates = countDuplicates(data);
        if (duplicates > 0) {
            issues.add(new QualityIssue(
                "存在重复数据",
                "重复行数: " + duplicates,
                QualityLevel.MINOR
            ));
        }

        return issues;
    }
}
```

## 常见问题

### Q1: Excel 表头是中文怎么办？

**A**:
1. 系统自动识别中文表头
2. 自动转换为英文字段名（驼峰命名）
3. 在注释中保留原中文名称

### Q2: 如何处理多张表格？

**A**:
- 每张表格生成一个独立的表
- 根据字段关联自动推断关系
- 建议分多次处理

### Q3: 如何处理复杂表格（合并单元格等）？

**A**:
1. 检测合并单元格
2. 展平嵌套结构
3. 创建对应的 Bo/Vo

### Q4: 图片识别不准确怎么办？

**A**:
1. 使用更高质量的 OCR 服务
2. 调整图片分辨率和对比度
3. 手动校正识别结果

## 配置 OCR 服务

### 百度 OCR 配置示例

```yaml
# application-ocr.yml
ocr:
  baidu:
    app-id: your-app-id
    api-key: your-api-key
    secret-key: your-secret-key
    general:
      basic-url: https://aip.baidubce.com/rest/2.0/ocr
```

### 使用 OCR 服务

```java
/**
 * OCR 客户
 */
@Component
public class OcrClient {

    @Value("${ocr.baidu.app-id}")
    private String appId;

    @Value("${ocr.baidu.api-key}")
    private String apiKey;

    /**
     * 表格识别
     */
    public OcrResult tableRecognition(String imageUrl) {
        // 调用百度 OCR API
        // 返回识别结果
    }
}
```

## 版本历史

- v1.0.0 (2026-01-17) - 初始版本，支持 Excel、CSV、图片数据源分析
