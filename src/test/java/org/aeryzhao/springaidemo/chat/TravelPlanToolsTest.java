package org.aeryzhao.springaidemo.chat;

import org.aeryzhao.springaidemo.chat.tool.TravelPlanTools;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TravelPlanToolsTest {

    @Test
    void planTripShouldReturnStructuredSuggestion() {
        TravelPlanTools tools = new TravelPlanTools();

        TravelPlanTools.TravelPlanResult result = tools.planTrip(
                new TravelPlanTools.TravelPlanRequest("杭州", 3, 3000)
        );

        assertThat(result.city()).isEqualTo("杭州");
        assertThat(result.days()).isEqualTo(3);
        assertThat(result.totalBudget()).isEqualTo(3000);
        assertThat(result.dailyBudget()).isEqualTo(1000);
        assertThat(result.itinerary()).hasSize(3);
        assertThat(result.tips()).isNotEmpty();
    }
}
