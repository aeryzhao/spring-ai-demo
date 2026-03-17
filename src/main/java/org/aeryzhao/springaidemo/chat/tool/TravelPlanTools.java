package org.aeryzhao.springaidemo.chat.tool;

import org.springframework.ai.tool.annotation.Tool;

import java.util.List;

/**
 * 旅游规划工具示例。
 *
 * @author zhaoxg
 * @date 2026/3/17 21:37
 */
public class TravelPlanTools {

    @Tool(description = "根据城市、出行天数和预算生成旅游建议")
    public TravelPlanResult planTrip(TravelPlanRequest request) {
        int dailyBudget = Math.max(request.budget() / Math.max(request.days(), 1), 1);
        List<String> itinerary = List.of(
                "第1天：抵达" + request.city() + "，安排城市地标和周边美食体验",
                "第2天：选择一条经典景点路线，控制单日预算在" + dailyBudget + "元左右",
                "第" + request.days() + "天：预留自由活动时间，并提前返回准备返程"
        );
        List<String> tips = List.of(
                "优先预订热门景点门票，避免排队",
                "住宿尽量选择交通便利区域",
                "预算有限时优先公共交通和本地小吃"
        );
        return new TravelPlanResult(
                request.city(),
                request.days(),
                request.budget(),
                dailyBudget,
                itinerary,
                tips
        );
    }

    public record TravelPlanRequest(String city, int days, int budget) {
    }

    public record TravelPlanResult(String city,
                                   int days,
                                   int totalBudget,
                                   int dailyBudget,
                                   List<String> itinerary,
                                   List<String> tips) {
    }
}
