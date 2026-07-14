package com.naturalist.vision;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

public record ToolSchema(
        String name,
        String description,
        String parametersJson
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notBlank(name, "name")
                .notBlank(description, "description")
                .notBlank(parametersJson, "parametersJson");
    }
}
