package com.ruoyi.system.crm.service;

import java.util.Map;
import com.ruoyi.system.crm.domain.CrmBehaviorTaskStatus;

/**
 * 客户行为海量数据服务接口
 * <p>
 * 支持 10 万+ 级行为数据的异步生成与 Excel 导入、
 * keyset 分页滚动查询、模板下载及一键清空。</p>
 */
public interface ICrmCustomerBehaviorService
{
    /**
     * 异步生成指定数量的随机行为数据
     * @param count 生成条数（建议 100000+）
     * @return 任务 ID（用于轮询进度）
     */
    String startGenerate(int count);

    /**
     * 异步导入 Excel 文件中的行为数据
     * @param file 上传的 Excel 文件（MultipartFile）
     * @return 任务 ID（用于轮询进度）
     */
    String startImport(org.springframework.web.multipart.MultipartFile file) throws java.io.IOException;

    /**
     * 下载导入模板（含列头说明）
     * @param response HttpServletResponse
     */
    void writeImportTemplate(jakarta.servlet.http.HttpServletResponse response);

    /**
     * 下载指定数量的样例 Excel（用于测试海量数据导入）
     * @param response HttpServletResponse
     * @param count 生成条数
     */
    void writeSampleExcel(jakarta.servlet.http.HttpServletResponse response, int count) throws java.io.IOException;

    /**
     * 清空全部行为数据
     * @return 清空条数
     */
    int clearAll();

    /**
     * 查询异步任务状态
     * @param taskId 任务 ID
     * @return 任务状态（RUNNING / DONE / FAILED + 进度）
     */
    CrmBehaviorTaskStatus getTaskStatus(String taskId);

    /**
     * keyset 分页滚动查询（无 offset 性能退化）
     * @param lastId 上一页最后一个 ID（首次传 0）
     * @param pageSize 每页条数
     * @return map 包含 list / total / lastId / hasMore
     */
    Map<String, Object> scrollList(Long lastId, int pageSize);

    /**
     * 获取总记录数
     * @return 总条数
     */
    long countTotal();
}
