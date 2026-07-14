package com.naturalist.vision;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

public record ToolResult(
        String toolName,
        String argumentsJson
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notBlank(toolName, "toolName")
                .notBlank(argumentsJson, "argumentsJson");
    }
}
