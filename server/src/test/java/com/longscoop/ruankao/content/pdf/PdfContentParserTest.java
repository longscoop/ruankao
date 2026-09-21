package com.longscoop.ruankao.content.pdf;

import com.longscoop.ruankao.content.model.ImportDocumentType;
import com.longscoop.ruankao.content.model.ImportIssueSeverity;
import com.longscoop.ruankao.question.model.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfContentParserTest {

    private final PdfContentParser parser = new PdfContentParser();

    @Test
    void parsesQuestionBankWithoutRewritingSourceAnswerOrAnalysis() {
        ParsedDocument document = parser.parse(List.of(
                new ParsedPage(1, """
                        2025 年 5 月份系统架构设计师模拟题（一）
                        上午试题 综合知识
                        1、关于哈佛结构的特性，以下哪个描述是不正确的?( )。
                        A、它将指令和数据存储在分离的存储空间中
                        B、允许指令和数据的并行访问
                        C、能够提高某些场景的数据吞吐率
                        D、通常使用统一地址空间管理指令和数据
                        参考答案：D
                        试题分析
                        哈佛结构将程序指令存储和数据存储分开。
                        """, null)));

        assertEquals(ImportDocumentType.QUESTION_BANK, document.documentType());
        assertEquals("2025 年 5 月份系统架构设计师模拟题（一）", document.title());
        assertEquals(1, document.questions().size());

        ParsedQuestion question = document.questions().get(0);
        assertEquals("1", question.questionNo());
        assertEquals(QuestionType.SINGLE_CHOICE, question.questionType());
        assertEquals("D", question.answer());
        assertEquals("哈佛结构将程序指令存储和数据存储分开。", question.explanation());
        assertEquals("它将指令和数据存储在分离的存储空间中", question.options().get("A"));
        assertFalse(question.requiresReview());
    }

    @Test
    void parsesLectureSectionsAndEmbeddedTypicalQuestionAsMixedContent() {
        ParsedDocument document = parser.parse(List.of(
                new ParsedPage(1, "系统架构设计师\n第19章 大数据架构设计理论与实践\n授课：王建平", "pages/1.png"),
                new ParsedPage(2, "目录\n1 大数据处理系统概述\n2 Lambda架构与Kappa架构\n3 大数据架构设计案例分析", "pages/2.png"),
                new ParsedPage(4, """
                        传统数据处理系统存在的问题
                        传统数据库的数据过载问题。
                        大数据的利用过程分为：采集、清洗、统计和挖掘几个过程。
                        """, "pages/4.png"),
                new ParsedPage(5, """
                        大数据处理系统架构分析
                        大数据处理系统架构特征：
                        1.鲁棒性和容错性
                        2.低延迟读取和更新能力
                        3.横向扩容
                        """, "pages/5.png"),
                new ParsedPage(6, """
                        典型真题
                        以下不属于大数据处理系统架构特征的是（）。
                        A.鲁棒性
                        B.容错性
                        C.纵向扩容
                        D.即席查询能力
                        参考答案：C
                        """, "pages/6.png")));

        assertEquals(ImportDocumentType.MIXED, document.documentType());
        assertTrue(document.lessons().stream().anyMatch(x ->
                x.title().contains("大数据处理系统")));
        assertTrue(document.knowledgeCandidates().stream().anyMatch(x ->
                x.name().contains("大数据处理系统架构")));
        assertEquals(1, document.questions().size());
        assertEquals("典型真题", document.questions().get(0).sourceLabel());
        assertEquals("C", document.questions().get(0).answer());
        assertTrue(document.lessons().stream()
                .flatMap(x -> x.blocks().stream())
                .anyMatch(x -> x.imageObjectKey() != null && x.imageObjectKey().equals("pages/5.png")));
    }

    @Test
    void flagsCompositeQuestionForManualReviewInsteadOfGuessingStructure() {
        ParsedDocument document = parser.parse(List.of(
                new ParsedPage(5, """
                        16-18、在面向对象系统中，请依次选择正确答案。
                        A、依赖关系 B、泛化关系 C、聚合关系 D、组合关系
                        A、泛化 B、继承 C、重载 D、多态
                        A、依赖关系<继承关系<组合关系<聚合关系
                        B、继承关系<组合关系<聚合关系<依赖关系
                        C、继承关系<依赖关系<组合关系<聚合关系
                        D、依赖关系<聚合关系<组合关系<继承关系
                        【答案】CAD
                        """, null)));

        assertEquals(1, document.questions().size());
        assertTrue(document.questions().get(0).requiresReview());
        assertTrue(document.issues().stream().anyMatch(issue ->
                issue.severity() == ImportIssueSeverity.WARNING
                        && issue.code().equals("COMPOSITE_QUESTION")));
        assertEquals("CAD", document.questions().get(0).answer());
    }

    @Test
    void missingAnswerIsErrorAndNeverInvented() {
        ParsedDocument document = parser.parse(List.of(
                new ParsedPage(1, """
                        1、以下说法正确的是（）。
                        A、甲
                        B、乙
                        C、丙
                        D、丁
                        """, null)));

        assertNull(document.questions().get(0).answer());
        assertTrue(document.questions().get(0).requiresReview());
        assertTrue(document.issues().stream().anyMatch(issue ->
                issue.severity() == ImportIssueSeverity.ERROR
                        && issue.code().equals("MISSING_ANSWER")));
    }

    @Test
    void coverAndTableOfContentsDoNotBecomeLearnerLessons() {
        ParsedDocument document = parser.parse(List.of(
                new ParsedPage(1, "系统架构设计师\n第19章 大数据架构设计理论与实践\n授课：王建平", "pages/1.png"),
                new ParsedPage(2, "目录\n1 大数据处理系统概述\n2 Lambda架构与Kappa架构\n3 大数据架构设计案例分析", "pages/2.png"),
                new ParsedPage(4, "传统数据处理系统存在的问题\n传统数据库的数据过载问题。", "pages/4.png")
        ));

        assertEquals(1, document.lessons().size());
        assertEquals("传统数据处理系统存在的问题", document.lessons().get(0).title());
        assertTrue(document.knowledgeCandidates().stream()
                .anyMatch(x -> x.name().equals("大数据处理系统概述")));
    }

}
