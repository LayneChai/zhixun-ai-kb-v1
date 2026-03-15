package com.zhixun.kb.common.utils;

import cn.idev.excel.FastExcel;
import cn.idev.excel.read.listener.PageReadListener;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * FastExcel 工具类
 * 支持字典翻译、自动合并等功能 (通过传入适当的 Listener 或 Handler)
 */
public class ExcelUtil {

    /**
     * 导出 Excel
     * 
     * @param response HttpServletResponse
     * @param data     导出的数据列表
     * @param clazz    导出模型类
     * @param filename 下载的文件名
     */
    public static <T> void exportExcel(HttpServletResponse response, List<T> data, Class<T> clazz, String filename)
            throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");

        // 此处可添加自动合并、样式等自定义 WriteHandler
        FastExcel.write(response.getOutputStream(), clazz)
                .sheet("Sheet1")
                .doWrite(data);
    }

    /**
     * 导入 Excel
     * 
     * @param inputStream 输入流
     * @param clazz       模型类
     * @param consumer    处理器消费者，批量处理数据
     */
    public static <T> void importExcel(java.io.InputStream inputStream, Class<T> clazz, Consumer<List<T>> consumer) {
        FastExcel.read(inputStream, clazz, new PageReadListener<>(consumer)).sheet().doRead();
    }
}
