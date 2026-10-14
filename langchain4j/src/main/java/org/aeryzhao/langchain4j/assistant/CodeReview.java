package org.aeryzhao.langchain4j.assistant;

import dev.langchain4j.model.output.structured.Description;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 结构化输出示例用的 POJO：代码评审结果。
 *
 * <p>注意集合字段（{@code List<String>}）同样支持，框架会自动生成 {@code array} 类型的 Schema。
 */
@Schema(description = "由模型按 Schema 生成并反序列化的代码评审结果")
public record CodeReview(

        @Schema(description = "代码质量评分，0-100 的整数", example = "75")
        @Description("代码质量评分，0-100 的整数")
        int score,

        @Schema(description = "是否可以通过评审", example = "false")
        @Description("是否可以通过评审")
        boolean approved,

        @Schema(description = "发现的问题列表，每条一句话；没有问题则返回空数组",
                example = "[\"add 方法实际做的是减法，与方法名语义不符\", \"缺少参数校验\"]")
        @Description("发现的问题列表，每条一句话；没有问题则返回空数组")
        java.util.List<String> issues,

        @Schema(description = "改进建议，一段话", example = "建议将减法逻辑改名或修正实现，并补充单元测试覆盖边界情况。")
        @Description("改进建议，一段话")
        String suggestion
) {
}
