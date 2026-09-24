package com.longscoop.ruankao.ai.knowledge;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static com.longscoop.ruankao.ai.knowledge.KnowledgeModels.*;
import static org.assertj.core.api.Assertions.assertThat;

class GroundedAnswerTest {
    private final List<Citation> sources = List.of(
            new Citation(1, UUID.randomUUID(), 11, "架构.pdf", 7, "分层架构按职责划分层次。"),
            new Citation(2, UUID.randomUUID(), 12, "架构.pdf", 8, "层间依赖需要遵守约束。"));

    @Test void returnsOnlyCitationsActuallyUsed() {
        var answer = GroundedAnswer.compose("分层架构按职责划分层次。[1]", sources);
        assertThat(answer.status()).isEqualTo(AnswerStatus.GROUNDED);
        assertThat(answer.citations()).containsExactly(sources.get(0));
    }

    @Test void missingUnknownOverflowAndOverlongCitationsFallBackToOriginals() {
        for (String raw : List.of("凭空编造的答案", "凭空编造[9]", "凭空编造[999999999999999999999]",
                "凭空编造[1][9]", "凭空编造[1]".repeat(3000))) {
            var answer = GroundedAnswer.compose(raw, sources);
            assertThat(answer.status()).isEqualTo(AnswerStatus.EXTRACT_ONLY);
            assertThat(answer.content()).contains("分层架构").doesNotContain("凭空编造");
            assertThat(answer.citations()).hasSize(2);
        }
    }

    @Test void emptyEvidenceCannotProduceAnApparentlyGroundedAnswer() {
        var answer = GroundedAnswer.compose("编造[1]", List.of());
        assertThat(answer.status()).isEqualTo(AnswerStatus.NO_EVIDENCE);
        assertThat(answer.citations()).isEmpty();
    }
}
