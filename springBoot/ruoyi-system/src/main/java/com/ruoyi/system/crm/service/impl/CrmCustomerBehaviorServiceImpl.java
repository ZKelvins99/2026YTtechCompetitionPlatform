package com.ruoyi.system.crm.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.crm.domain.CrmBehaviorTaskStatus;
import com.ruoyi.system.crm.domain.CrmCustomerBehavior;
import com.ruoyi.system.crm.domain.CrmCustomerBehaviorImportTemplate;
import com.ruoyi.system.crm.mapper.CrmCustomerBehaviorMapper;
import com.ruoyi.system.crm.service.ICrmCustomerBehaviorService;
import com.ruoyi.system.crm.support.CrmBehaviorStatsCache;
import com.ruoyi.system.crm.support.CrmBehaviorTaskManager;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class CrmCustomerBehaviorServiceImpl implements ICrmCustomerBehaviorService
{
    static final String[] BEHAVIOR_TYPES = { "电话沟通", "拜访", "邮件", "演示" };

    @Autowired
    private CrmCustomerBehaviorMapper crmCustomerBehaviorMapper;

    @Autowired
    private CrmBehaviorTaskManager taskManager;

    @Autowired
    private CrmCustomerBehaviorAsyncService asyncService;

    @Autowired
    private CrmBehaviorStatsCache statsCache;

    @Override
    public String startGenerate(int count)
    {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        taskManager.createGenerateTask(taskId, SecurityUtils.getUserId(), count);
        asyncService.generateAsync(taskId, count);
        return taskId;
    }

    @Override
    public String startImport(MultipartFile file) throws IOException
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("请选择 Excel 文件");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase().endsWith(".xlsx"))
        {
            throw new ServiceException("批量导入仅支持 xlsx 格式（请将 xls 另存为 xlsx）");
        }
        Long userId = SecurityUtils.getUserId();
        String taskId = UUID.randomUUID().toString().replace("-", "");
        taskManager.createImportTask(taskId, userId, 0, original);
        File dir = new File(RuoYiConfig.getImportPath() + "/behavior");
        if (!dir.exists() && !dir.mkdirs())
        {
            throw new ServiceException("创建导入目录失败");
        }
        File dest = new File(dir, taskId + ".xlsx");
        file.transferTo(dest);
        asyncService.importExcelAsync(taskId, dest.getAbsolutePath());
        return taskId;
    }

    @Override
    public void writeImportTemplate(HttpServletResponse response)
    {
        ExcelUtil<CrmCustomerBehaviorImportTemplate> util = new ExcelUtil<>(CrmCustomerBehaviorImportTemplate.class);
        util.importTemplateExcel(response, "客户行为导入模板");
    }

    @Override
    public void writeSampleExcel(HttpServletResponse response, int count) throws IOException
    {
        if (count <= 0 || count > 500000)
        {
            throw new ServiceException("样例数量须在 1~500000 之间");
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition",
            "attachment;filename=behavior_sample_" + count + ".xlsx");
        Random random = new Random();
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(500))
        {
            Sheet sheet = workbook.createSheet("客户行为");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("客户ID");
            header.createCell(1).setCellValue("行为类型");
            header.createCell(2).setCellValue("描述");
            header.createCell(3).setCellValue("行为时间");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            for (int i = 1; i <= count; i++)
            {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue(random.nextInt(500) + 1);
                String type = BEHAVIOR_TYPES[random.nextInt(BEHAVIOR_TYPES.length)];
                row.createCell(1).setCellValue(type);
                row.createCell(2).setCellValue(type + "记录-" + i);
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_YEAR, -random.nextInt(365));
                row.createCell(3).setCellValue(sdf.format(cal.getTime()));
            }
            workbook.write(response.getOutputStream());
            workbook.dispose();
        }
    }

    @Override
    public int clearAll()
    {
        int rows = crmCustomerBehaviorMapper.deleteAll();
        statsCache.invalidate();
        try
        {
            statsCache.setCachedTotal(crmCustomerBehaviorMapper.countTotal());
        }
        catch (Exception ignored)
        {
            statsCache.invalidate();
        }
        return rows;
    }

    @Override
    public CrmBehaviorTaskStatus getTaskStatus(String taskId)
    {
        CrmBehaviorTaskStatus task = taskManager.getTask(taskId);
        assertTaskOwner(task);
        return task;
    }

    @Override
    public CrmBehaviorTaskStatus getActiveImportTask()
    {
        return taskManager.getActiveImportTask(SecurityUtils.getUserId());
    }

    private void assertTaskOwner(CrmBehaviorTaskStatus task)
    {
        if (task == null)
        {
            return;
        }
        Long userId = SecurityUtils.getUserId();
        if (task.getUserId() != null && userId != null && !userId.equals(task.getUserId()))
        {
            throw new ServiceException("无权查看该任务");
        }
    }

    @Override
    public Map<String, Object> scrollList(Long lastId, int pageSize)
    {
        if (lastId == null)
        {
            lastId = 0L;
        }
        if (pageSize <= 0 || pageSize > 5000)
        {
            pageSize = 2000;
        }
        List<CrmCustomerBehavior> list = crmCustomerBehaviorMapper.selectScrollList(lastId, pageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        Long newLastId = lastId;
        if (!list.isEmpty())
        {
            newLastId = list.get(list.size() - 1).getId();
        }
        data.put("lastId", newLastId);
        data.put("hasMore", list.size() >= pageSize);
        if (lastId == 0L)
        {
            data.put("total", resolveDisplayTotal());
        }
        return data;
    }

    @Override
    public long countTotal()
    {
        return resolveDisplayTotal();
    }

    @Override
    public void invalidateTotalCache()
    {
        statsCache.invalidate();
    }

    /** 展示用总量：精确 COUNT，5 分钟内存缓存；不用 USER_TABLES.NUM_ROWS（易与真实行数偏差） */
    private long resolveDisplayTotal()
    {
        return statsCache.getTotal(() -> crmCustomerBehaviorMapper.countTotal());
    }
}
