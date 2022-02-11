package com.quanquan.dto.prerequisite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.annotation.Nonnull;

/**
 * Represents a leaf node of each prerequisite.
 * <p>
 * For example,
 * the terms in {@code AndPrerequisite}/{@code OrPrerequisite} can be either
 * implementation of {@code Prerequisite}.
 */
@Data
public class CoursePrerequisite implements Prerequisite {
    private final String courseID;

    @JsonCreator
    public CoursePrerequisite(@JsonProperty("courseID") @Nonnull String courseID) {
        this.courseID = courseID;
    }

    @Override
    public <R> R when(Cases<R> cases) {
        return cases.match(this);
    }
}
