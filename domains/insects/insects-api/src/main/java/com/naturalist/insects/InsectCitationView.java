package com.naturalist.insects;

import com.naturalist.authority.CitationName;
import com.naturalist.ddd.ReadModel;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public record InsectCitationView(
        InsectRankName subject,
        List<RankedCitation> citations
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .identifier(subject, "subject")
                .notNull(citations, "citations");
    }

    public record RankedCitation(
            CitationName citationName,
            InsectRankName attachedAt,
            @Nullable String note
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .entityName(citationName, "citationName")
                    .identifier(attachedAt, "attachedAt");
        }
    }
}
