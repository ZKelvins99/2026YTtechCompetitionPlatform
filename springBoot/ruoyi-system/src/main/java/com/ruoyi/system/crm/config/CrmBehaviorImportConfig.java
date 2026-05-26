package com.ruoyi.system.crm.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * CRM 行为数据批量导入 Spring 配置。
 * <p>
 * 数据源：本项目主库使用 Druid（见 application-druid.yml），需在连接属性中设置
 * {@code oracle.jdbc.defaultBatchValue=2000}，落库时 {@link com.ruoyi.system.crm.support.CrmBehaviorBulkInserter}
 * 对每条批量再显式 {@code setAutoCommit(false)} 并按批提交。
 * </p>
 * <p>
 * 若改用 HikariCP，可参考 {@code application-crm-import-hikari.example.yml} 中的等价配置：
 * </p>
 * <pre>
 * spring.datasource.type=com.zaxxer.hikari.HikariDataSource
 * spring.datasource.hikari.maximum-pool-size=20
 * spring.datasource.hikari.auto-commit=false
 * spring.datasource.hikari.data-source-properties.oracle.jdbc.defaultBatchValue=2000
 * crm.behavior.import.batch-size=2000
 * </pre>
 */
@Configuration
@EnableConfigurationProperties(CrmBehaviorImportProperties.class)
public class CrmBehaviorImportConfig
{
}
