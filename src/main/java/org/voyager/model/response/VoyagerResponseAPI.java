package org.voyager.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class VoyagerResponseAPI<T> {
    Integer totalResponseCount;
    List<T> responseList;
}
