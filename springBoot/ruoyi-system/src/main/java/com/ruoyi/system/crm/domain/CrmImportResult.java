package com.ruoyi.system.crm.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入结果
 */
public class CrmImportResult
{
    private int successCount;

    private int failCount;

    private int skipCount;

    private List<CrmImportError> errors = new ArrayList<>();

    public int getSuccessCount()
    {
        return successCount;
    }

    public void setSuccessCount(int successCount)
    {
        this.successCount = successCount;
    }

    public int getFailCount()
    {
        return failCount;
    }

    public void setFailCount(int failCount)
    {
        this.failCount = failCount;
    }

    public int getSkipCount()
    {
        return skipCount;
    }

    public void setSkipCount(int skipCount)
    {
        this.skipCount = skipCount;
    }

    public List<CrmImportError> getErrors()
    {
        return errors;
    }

    public void setErrors(List<CrmImportError> errors)
    {
        this.errors = errors;
    }

    public void addError(CrmImportError error)
    {
        this.errors.add(error);
    }
}
