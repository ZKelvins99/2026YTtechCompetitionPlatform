package com.ruoyi.system.crm.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.crm.domain.CrmCustomer;
import com.ruoyi.system.crm.domain.CrmDictItem;
import com.ruoyi.system.crm.domain.CrmImportError;
import com.ruoyi.system.crm.domain.CrmImportResult;
import com.ruoyi.system.crm.domain.CrmOpportunity;
import com.ruoyi.system.crm.mapper.CrmCustomerMapper;
import com.ruoyi.system.crm.mapper.CrmDictMapper;
import com.ruoyi.system.crm.mapper.CrmOpportunityMapper;
import com.ruoyi.system.crm.service.ICrmOpportunityService;
import com.ruoyi.system.crm.support.CrmProfileRefreshHelper;

/**
 * 商机Service业务层处理
 */
@Service
public class CrmOpportunityServiceImpl implements ICrmOpportunityService
{
    private static final int COL_OPPORTUNITY_NAME = 1;
    private static final int COL_CUSTOMER_NAME = 2;
    private static final int COL_STAGE_CODE = 3;
    private static final int COL_ESTIMATED_AMOUNT = 4;
    private static final int COL_EXPECTED_CLOSE_DATE = 5;

    @Autowired
    private CrmOpportunityMapper crmOpportunityMapper;

    @Autowired
    private CrmCustomerMapper crmCustomerMapper;

    @Autowired
    private CrmDictMapper crmDictMapper;

    @Autowired
    private CrmProfileRefreshHelper profileRefreshHelper;

    @Override
    public CrmOpportunity selectCrmOpportunityById(Long id)
    {
        return crmOpportunityMapper.selectCrmOpportunityById(id);
    }

    @Override
    public List<CrmOpportunity> selectCrmOpportunityList(CrmOpportunity opportunity)
    {
        return crmOpportunityMapper.selectCrmOpportunityList(opportunity);
    }

    @Override
    public int insertCrmOpportunity(CrmOpportunity opportunity)
    {
        int rows = crmOpportunityMapper.insertCrmOpportunity(opportunity);
        profileRefreshHelper.refreshCurrentUser();
        return rows;
    }

    @Override
    public int updateCrmOpportunity(CrmOpportunity opportunity)
    {
        int rows = crmOpportunityMapper.updateCrmOpportunity(opportunity);
        profileRefreshHelper.refreshCurrentUser();
        return rows;
    }

    @Override
    public int deleteCrmOpportunityByIds(Long[] ids)
    {
        int rows = crmOpportunityMapper.deleteCrmOpportunityByIds(ids);
        profileRefreshHelper.refreshCurrentUser();
        return rows;
    }

    @Override
    public CrmImportResult importOpportunity(InputStream inputStream, String operName)
    {
        CrmImportResult result = new CrmImportResult();
        Map<String, Long> stageCodeMap = buildStageCodeMap();
        Map<String, Integer> excelFirstRowMap = new HashMap<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(inputStream))
        {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null)
            {
                result.addError(new CrmImportError(1, 1, "file", "Excel 文件无有效工作表"));
                result.setFailCount(1);
                return result;
            }

            int lastRowNum = sheet.getLastRowNum();
            for (int i = 1; i <= lastRowNum; i++)
            {
                Row row = sheet.getRow(i);
                int excelRow = i + 1;
                if (row == null || isEmptyRow(row, formatter))
                {
                    continue;
                }

                String opportunityName = getCellString(row, COL_OPPORTUNITY_NAME - 1, formatter);
                String customerName = getCellString(row, COL_CUSTOMER_NAME - 1, formatter);
                String stageCode = getCellString(row, COL_STAGE_CODE - 1, formatter);
                String amountStr = getCellString(row, COL_ESTIMATED_AMOUNT - 1, formatter);
                String dateStr = getCellDateString(row, COL_EXPECTED_CLOSE_DATE - 1, formatter);

                List<CrmImportError> rowErrors = validateRow(excelRow, opportunityName, customerName,
                    stageCode, amountStr, dateStr, stageCodeMap);

                if (!rowErrors.isEmpty())
                {
                    result.getErrors().addAll(rowErrors);
                    result.setFailCount(result.getFailCount() + 1);
                    continue;
                }

                String dupKey = opportunityName + "|" + customerName;
                if (excelFirstRowMap.containsKey(dupKey))
                {
                    result.addError(new CrmImportError(excelRow, COL_OPPORTUNITY_NAME, "opportunityName",
                        "与第 " + excelFirstRowMap.get(dupKey) + " 行重复"));
                    result.setFailCount(result.getFailCount() + 1);
                    continue;
                }
                excelFirstRowMap.put(dupKey, excelRow);

                CrmCustomer customer = crmCustomerMapper.selectCrmCustomerByName(customerName);
                if (customer == null)
                {
                    result.addError(new CrmImportError(excelRow, COL_CUSTOMER_NAME, "customerName",
                        "客户名称不存在：" + customerName));
                    result.setFailCount(result.getFailCount() + 1);
                    continue;
                }

                if (crmOpportunityMapper.countByNameAndCustomer(opportunityName, customer.getId()) > 0)
                {
                    result.addError(new CrmImportError(excelRow, COL_OPPORTUNITY_NAME, "opportunityName",
                        "数据库已存在相同记录", "skip"));
                    result.setSkipCount(result.getSkipCount() + 1);
                    continue;
                }

                CrmOpportunity opportunity = new CrmOpportunity();
                opportunity.setOpportunityName(opportunityName);
                opportunity.setCustomerId(customer.getId());
                if (StringUtils.isNotEmpty(stageCode))
                {
                    opportunity.setStageId(stageCodeMap.get(stageCode.trim().toUpperCase()));
                }
                if (StringUtils.isNotEmpty(amountStr))
                {
                    opportunity.setEstimatedAmount(new BigDecimal(amountStr.trim()));
                }
                if (StringUtils.isNotEmpty(dateStr))
                {
                    opportunity.setExpectedCloseDate(parseDate(dateStr));
                }
                opportunity.setCreateBy(operName);
                crmOpportunityMapper.insertCrmOpportunity(opportunity);
                result.setSuccessCount(result.getSuccessCount() + 1);
                result.addError(new CrmImportError(excelRow, 0, "row", "导入成功", "success"));
            }
        }
        catch (Exception e)
        {
            result.addError(new CrmImportError(1, 1, "file", "Excel 解析失败：" + e.getMessage()));
            result.setFailCount(result.getFailCount() + 1);
        }
        if (result.getSuccessCount() > 0)
        {
            profileRefreshHelper.refreshCurrentUser();
        }
        return result;
    }

    private Map<String, Long> buildStageCodeMap()
    {
        Map<String, Long> map = new HashMap<>();
        List<CrmDictItem> stages = crmDictMapper.selectOpportunityStageList();
        for (CrmDictItem item : stages)
        {
            map.put(item.getCode().toUpperCase(), item.getId());
        }
        return map;
    }

    private List<CrmImportError> validateRow(int excelRow, String opportunityName, String customerName,
        String stageCode, String amountStr, String dateStr, Map<String, Long> stageCodeMap)
    {
        List<CrmImportError> errors = new ArrayList<>();

        if (StringUtils.isEmpty(opportunityName))
        {
            errors.add(new CrmImportError(excelRow, COL_OPPORTUNITY_NAME, "opportunityName", "商机名称不能为空"));
        }
        if (StringUtils.isEmpty(customerName))
        {
            errors.add(new CrmImportError(excelRow, COL_CUSTOMER_NAME, "customerName", "客户名称不能为空"));
        }
        if (StringUtils.isNotEmpty(amountStr))
        {
            try
            {
                new BigDecimal(amountStr.trim());
            }
            catch (NumberFormatException e)
            {
                errors.add(new CrmImportError(excelRow, COL_ESTIMATED_AMOUNT, "estimatedAmount", "预计金额须为数字"));
            }
        }
        if (StringUtils.isNotEmpty(stageCode) && !stageCodeMap.containsKey(stageCode.trim().toUpperCase()))
        {
            errors.add(new CrmImportError(excelRow, COL_STAGE_CODE, "stageCode",
                "阶段编码不存在：" + stageCode));
        }
        if (StringUtils.isNotEmpty(dateStr))
        {
            try
            {
                parseDate(dateStr);
            }
            catch (Exception e)
            {
                errors.add(new CrmImportError(excelRow, COL_EXPECTED_CLOSE_DATE, "expectedCloseDate",
                    "预计成交日期格式错误，应为 yyyy-MM-dd"));
            }
        }
        return errors;
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter)
    {
        for (int c = 0; c < 5; c++)
        {
            if (StringUtils.isNotEmpty(getCellString(row, c, formatter)))
            {
                return false;
            }
        }
        return true;
    }

    private String getCellString(Row row, int colIndex, DataFormatter formatter)
    {
        Cell cell = row.getCell(colIndex);
        if (cell == null)
        {
            return "";
        }
        return StringUtils.trim(formatter.formatCellValue(cell));
    }

    private String getCellDateString(Row row, int colIndex, DataFormatter formatter)
    {
        Cell cell = row.getCell(colIndex);
        if (cell == null)
        {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
        {
            Date date = cell.getDateCellValue();
            return new SimpleDateFormat("yyyy-MM-dd").format(date);
        }
        return StringUtils.trim(formatter.formatCellValue(cell));
    }

    private Date parseDate(String dateStr) throws Exception
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        return sdf.parse(dateStr.trim());
    }
}
