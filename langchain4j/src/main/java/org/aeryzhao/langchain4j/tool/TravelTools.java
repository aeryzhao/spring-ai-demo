package org.aeryzhao.langchain4j.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * 工具（Function Calling）示例。
 *
 * <p>LangChain4j 声明工具的方式极其简单：在任意 Spring Bean 的公开方法上打 {@link Tool} 注解即可，
 * 框架会读取方法签名自动生成 JSON Schema 交给模型。
 *
 * <p>与 Spring AI 的差异：
 * <ul>
 *   <li>Spring AI 用 {@code @Tool} + {@code @ToolParam}（{@code org.springframework.ai.tool.annotation}）</li>
 *   <li>LangChain4j 用 {@link Tool} + {@link P}（{@code dev.langchain4j.agent.tool}）</li>
 * </ul>
 * 注解本身语义几乎一致，但 LangChain4j 的工具是通过 {@code AiServices.tools(...)} 显式注册的，
 * 不存在「自动扫描全容器工具」这种事 —— 好处是可控，坏处是别忘了注册。
 *
 * <p>下面这些方法故意写成「真能算」的实现（而不是返回假数据），方便验证模型是否真的调用了工具。
 */
@Slf4j
@Component
public class TravelTools {

    /** 各城市纬度，用于估算日照时长 */
    private static final Map<String, Double> CITY_LATITUDE = Map.of(
            "北京", 39.9,
            "上海", 31.2,
            "广州", 23.1,
            "三亚", 18.3
    );

    /**
     * 查询指定城市的当前天气。
     *
     * <p>{@link P} 的 value 是参数描述，会写进 Schema 帮助模型正确填参。
     */
    @Tool("查询指定城市的实时天气，返回温度、天气状况和湿度")
    public String getWeather(@P("城市名称，例如：北京、上海") String city) {
        log.info("[工具调用] getWeather(city={})", city);

        // 演示用：真实项目里这里应该调用天气 API
        double base = switch (city) {
            case "北京" -> 18.0;
            case "上海" -> 22.0;
            case "广州" -> 27.0;
            case "三亚" -> 31.0;
            default -> 20.0;
        };
        String condition = base > 26 ? "晴" : "多云";
        return String.format("%s 当前天气：%s，气温 %.1f℃，湿度 %d%%", city, condition, base, 55 + city.length() * 3);
    }

    /**
     * 计算两个日期之间相差的天数。
     *
     * <p>这类「精确计算」是工具调用的经典场景 —— 大模型算日期很容易出错，交给代码算就万无一失。
     */
    @Tool("计算从出发日期到返回日期之间的旅行天数（含首尾两天）")
    public long countTravelDays(
            @P("出发日期，格式 yyyy-MM-dd") String startDate,
            @P("返回日期，格式 yyyy-MM-dd") String endDate) {
        log.info("[工具调用] countTravelDays({}, {})", startDate, endDate);

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    /**
     * 根据城市和天数估算旅行预算。
     */
    @Tool("根据城市和旅行天数估算旅行预算（人民币元），包含交通、住宿、餐饮")
    public String estimateBudget(
            @P("城市名称") String city,
            @P("旅行天数，正整数") int days) {
        log.info("[工具调用] estimateBudget({}, {})", city, days);

        int dailyCost = switch (city) {
            case "北京", "上海" -> 800;
            case "广州" -> 650;
            case "三亚" -> 1000;
            default -> 600;
        };
        int total = dailyCost * days;
        return String.format("%s %d 天行程，按日均 %d 元估算，总预算约 %d 元（不含往返大交通）",
                city, days, dailyCost, total);
    }

    /**
     * 根据纬度估算夏季日照时长，演示「多参数 + 默认值」的玩法。
     */
    @Tool("查询城市的夏季平均日照时长（小时）")
    public double getSunshineHours(@P("城市名称") String city) {
        log.info("[工具调用] getSunshineHours({})", city);
        Double latitude = CITY_LATITUDE.get(city);
        if (latitude == null) {
            return 8.0;   // 未知城市给个经验值
        }
        // 纬度越低日照越稳定，这里只是个粗糙的演示公式
        return Math.round((14.0 - latitude / 15.0) * 10) / 10.0;
    }

}
