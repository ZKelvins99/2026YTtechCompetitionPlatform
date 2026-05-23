package com.ruoyi.system.crm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

/**
 * 联系人对象 crm_contact
 */
public class CrmContact extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "联系人ID", cellType = ColumnType.NUMERIC)
    private Long id;

    @Excel(name = "客户ID", cellType = ColumnType.NUMERIC)
    private Long customerId;

    @Excel(name = "客户名称")
    private String customerName;

    @Excel(name = "联系人姓名")
    private String contactName;

    @Excel(name = "联系方式类型ID", cellType = ColumnType.NUMERIC)
    private Long contactTypeId;

    @Excel(name = "联系方式类型")
    private String contactTypeName;

    @Excel(name = "联系方式")
    private String contactValue;

    @Excel(name = "职位")
    private String position;

    @Excel(name = "是否首要", readConverterExp = "0=否,1=是")
    private String isPrimary;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @NotNull(message = "所属客户不能为空")
    public Long getCustomerId()
    {
        return customerId;
    }

    public void setCustomerId(Long customerId)
    {
        this.customerId = customerId;
    }

    public String getCustomerName()
    {
        return customerName;
    }

    public void setCustomerName(String customerName)
    {
        this.customerName = customerName;
    }

    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 50, message = "联系人姓名不能超过50个字符")
    public String getContactName()
    {
        return contactName;
    }

    public void setContactName(String contactName)
    {
        this.contactName = contactName;
    }

    public Long getContactTypeId()
    {
        return contactTypeId;
    }

    public void setContactTypeId(Long contactTypeId)
    {
        this.contactTypeId = contactTypeId;
    }

    public String getContactTypeName()
    {
        return contactTypeName;
    }

    public void setContactTypeName(String contactTypeName)
    {
        this.contactTypeName = contactTypeName;
    }

    public String getContactValue()
    {
        return contactValue;
    }

    public void setContactValue(String contactValue)
    {
        this.contactValue = contactValue;
    }

    public String getPosition()
    {
        return position;
    }

    public void setPosition(String position)
    {
        this.position = position;
    }

    public String getIsPrimary()
    {
        return isPrimary;
    }

    public void setIsPrimary(String isPrimary)
    {
        this.isPrimary = isPrimary;
    }

    @Override
    public Date getCreateTime()
    {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("customerId", getCustomerId())
            .append("contactName", getContactName())
            .toString();
    }
}
