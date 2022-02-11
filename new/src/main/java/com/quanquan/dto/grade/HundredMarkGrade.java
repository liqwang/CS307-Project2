package com.quanquan.dto.grade;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public final class HundredMarkGrade implements Grade {
    private final Short mark;

    @JsonCreator
    public HundredMarkGrade(@JsonProperty("mark") short mark) {
        this.mark = mark;
    }

    @Override
    public <R> R when(Cases<R> cases) {
        return cases.match(this);
    }
}
