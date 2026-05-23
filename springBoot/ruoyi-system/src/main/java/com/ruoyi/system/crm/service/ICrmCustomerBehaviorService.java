package com.ruoyi.system.crm.service;

import java.util.Map;
import com.ruoyi.system.crm.domain.CrmBehaviorTaskStatus;

public interface ICrmCustomerBehaviorService
{
    String startGenerate(int count);

    String startImport(org.springframework.web.multipart.MultipartFile file) throws java.io.IOException;

    void writeImportTemplate(jakarta.servlet.http.HttpServletResponse response);

    void writeSampleExcel(jakarta.servlet.http.HttpServletResponse response, int count) throws java.io.IOException;

    int clearAll();

    CrmBehaviorTaskStatus getTaskStatus(String taskId);

    Map<String, Object> scrollList(Long lastId, int pageSize);

    long countTotal();
}
