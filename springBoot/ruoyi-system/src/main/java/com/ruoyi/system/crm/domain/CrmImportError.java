package com.ruoyi.system.crm.domain;

/**
 * Excel 导入校验错误项
 */
public class CrmImportError
{
    /** Excel 行号（从1开始，含表头） */
    private int row;

    /** Excel 列号（从1开始） */
    private int col;

    /** 字段名 */
    private String field;

    /** 错误描述 */
    private String message;

  /** 结果类型：error / skip / success */
    private String type;

    public CrmImportError()
    {
    }

    public CrmImportError(int row, int col, String field, String message)
    {
        this.row = row;
        this.col = col;
        this.field = field;
        this.message = message;
        this.type = "error";
    }

    public CrmImportError(int row, int col, String field, String message, String type)
    {
        this.row = row;
        this.col = col;
        this.field = field;
        this.message = message;
        this.type = type;
    }

    public int getRow()
    {
        return row;
    }

    public void setRow(int row)
    {
        this.row = row;
    }

    public int getCol()
    {
        return col;
    }

    public void setCol(int col)
    {
        this.col = col;
    }

    public String getField()
    {
        return field;
    }

    public void setField(String field)
    {
        this.field = field;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
}
