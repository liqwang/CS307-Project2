package com.quanquan.dto.prerequisite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Represents the OR relationship.
 * <p>
 * This prerequisite expression is fulfilled if and only if any elements of
 * {@code terms} is fulfilled.
 */
@Data
public class OrPrerequisite implements Prerequisite {
    private final List<Prerequisite> terms;

    @JsonCreator
    public OrPrerequisite(@JsonProperty("terms") @Nonnull List<Prerequisite> terms) {
        this.terms = terms;
    }

    @Override
    public <R> R when(Cases<R> cases) {
        return cases.match(this);
    }
}
