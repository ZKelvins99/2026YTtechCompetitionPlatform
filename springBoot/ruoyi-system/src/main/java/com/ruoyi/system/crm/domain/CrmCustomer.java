package com.ruoyi.system.crm.domain;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 客户对象 crm_customer
 */
public class CrmCustomer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "客户ID", cellType = ColumnType.NUMERIC)
    private Long id;

    @Excel(name = "客户编号")
    private String customerNo;

    @Excel(name = "客户名称")
    private String customerName;

    @Excel(name = "客户等级ID", cellType = ColumnType.NUMERIC)
    private Long levelId;

    @Excel(name = "客户等级")
    private String levelName;

    @Excel(name = "所属行业")
    private String industry;

    @Excel(name = "省份")
    private String province;

    @Excel(name = "城市")
    private String city;

    @Excel(name = "详细地址")
    private String address;

    @Excel(name = "联系电话")
    private String phone;

    @Excel(name = "邮箱")
    private String email;

    /** 关联联系人列表（详情查询用） */
    private List<CrmContact> contacts;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getCustomerNo()
    {
        return customerNo;
    }

    public void setCustomerNo(String customerNo)
    {
        this.customerNo = customerNo;
    }

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称不能超过100个字符")
    public String getCustomerName()
    {
        return customerName;
    }

    public void setCustomerName(String customerName)
    {
        this.customerName = customerName;
    }

    public Long getLevelId()
    {
        return levelId;
    }

    public void setLevelId(Long levelId)
    {
        this.levelId = levelId;
    }

    public String getLevelName()
    {
        return levelName;
    }

    public void setLevelName(String levelName)
    {
        this.levelName = levelName;
    }

    public String getIndustry()
    {
        return industry;
    }

    public void setIndustry(String industry)
    {
        this.industry = industry;
    }

    public String getProvince()
    {
        return province;
    }

    public void setProvince(String province)
    {
        this.province = province;
    }

    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public List<CrmContact> getContacts()
    {
        return contacts;
    }

    public void setContacts(List<CrmContact> contacts)
    {
        this.contacts = contacts;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("customerNo", getCustomerNo())
            .append("customerName", getCustomerName())
            .append("levelId", getLevelId())
            .append("industry", getIndustry())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
