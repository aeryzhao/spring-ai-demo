package org.aeryzhao.langchain4j.assistant;

import dev.langchain4j.model.output.structured.Description;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 结构化输出示例用的 POJO。
 *
 * <p>{@link Description} 注解会写进自动生成的 JSON Schema，
 * 相当于给模型「每个字段该填什么」的提示 —— 字段描述写得越准，抽取准确率越高。
 */
@Schema(description = "从自然语言文本中抽取的人物信息（由模型按 Schema 生成并反序列化）")
public record Person(

        @Schema(description = "姓名，中文或英文原名", example = "张三")
        @Description("姓名，中文或英文原名")
        String name,

        @Schema(description = "年龄，纯数字；如果文本中没提到则填 -1", example = "35")
        @Description("年龄，纯数字；如果文本中没提到则填 -1")
        int age,

        @Schema(description = "所在城市；如果没提到则填 '未知'", example = "杭州")
        @Description("所在城市；如果没提到则填 '未知'")
        String city,

        @Schema(description = "职业；如果没提到则填 '未知'", example = "后端工程师")
        @Description("职业；如果没提到则填 '未知'")
        String occupation,

        @Schema(description = "一句话总结这个人的核心特点", example = "擅长高并发系统设计的后端工程师")
        @Description("一句话总结这个人的核心特点")
        String summary
) {
}
