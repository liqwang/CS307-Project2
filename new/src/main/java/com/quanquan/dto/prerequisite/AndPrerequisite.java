package com.quanquan.dto.prerequisite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Represents the AND relationship.
 * <p>
 * This prerequisite expression is fulfilled if and only if all elements of
 * {@code terms} are fulfilled.
 */
@Data
public class AndPrerequisite implements Prerequisite {
    private final List<Prerequisite> terms;

    @JsonCreator
    public AndPrerequisite(@JsonProperty("terms") @Nonnull List<Prerequisite> terms) {
        this.terms = terms;
    }

    @Override
    public <R> R when(Cases<R> cases) {
        return cases.match(this);
    }
}
