package org.aeryzhao.langchain4j.assistant;

import dev.langchain4j.model.output.structured.Description;

/**
 * 结构化输出示例用的 POJO：代码评审结果。
 *
 * <p>注意集合字段（{@code List<String>}）同样支持，框架会自动生成 {@code array} 类型的 Schema。
 */
public record CodeReview(

        @Description("代码质量评分，0-100 的整数")
        int score,

        @Description("是否可以通过评审")
        boolean approved,

        @Description("发现的问题列表，每条一句话；没有问题则返回空数组")
        java.util.List<String> issues,

        @Description("改进建议，一段话")
        String suggestion
) {
}
