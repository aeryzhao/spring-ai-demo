package org.aeryzhao.langchain4j.assistant;

import dev.langchain4j.model.output.structured.Description;

/**
 * 结构化输出示例用的 POJO。
 *
 * <p>{@link Description} 注解会写进自动生成的 JSON Schema，
 * 相当于给模型「每个字段该填什么」的提示 —— 字段描述写得越准，抽取准确率越高。
 */
public record Person(

        @Description("姓名，中文或英文原名")
        String name,

        @Description("年龄，纯数字；如果文本中没提到则填 -1")
        int age,

        @Description("所在城市；如果没提到则填 '未知'")
        String city,

        @Description("职业；如果没提到则填 '未知'")
        String occupation,

        @Description("一句话总结这个人的核心特点")
        String summary
) {
}
