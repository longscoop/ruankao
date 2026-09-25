package com.longscoop.ruankao.ai.knowledge;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeTextTest {
    @Test
    void indexesChineseBigramsAndEnglishWords() {
        assertThat(KnowledgeText.terms("设计模式 Architecture TCP/IP"))
                .contains("设计", "计模", "模式", "architecture", "tcp", "ip");
        assertThat(KnowledgeText.tsQuery("设计模式 | ' & TCP"))
                .isEqualTo("设计 | 计模 | 模式 | tcp");
    }

    @Test
    void keepsRealPageNumbersAndBoundsEveryChunk() {
        var chunks = KnowledgeText.split(List.of(
                new KnowledgeText.Page(2, "架构设计".repeat(500)),
                new KnowledgeText.Page(4, "数据库事务")));
        assertThat(chunks).hasSizeGreaterThan(2);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.text().length()).isLessThanOrEqualTo(800);
            assertThat(chunk.searchText()).isNotBlank();
        });
        assertThat(chunks.get(0).page()).isEqualTo(2);
        assertThat(chunks.get(chunks.size() - 1).page()).isEqualTo(4);
        assertThat(chunks.get(1).text().substring(0, 120))
                .isEqualTo(chunks.get(0).text().substring(680));
    }

    @Test
    void ignoresBlankPagesAndRejectsEmptyDocuments() {
        assertThatThrownBy(() -> KnowledgeText.split(List.of(new KnowledgeText.Page(1, " \n"))))
                .isInstanceOf(IllegalArgumentException.class);
        var chunks = KnowledgeText.split(List.of(new KnowledgeText.Page(3, "ACID 数据库事务")));
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).ordinal()).isZero();
        assertThat(KnowledgeText.terms("!!! \n")).isEmpty();
    }
}
