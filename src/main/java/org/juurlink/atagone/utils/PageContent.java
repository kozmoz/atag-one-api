package org.juurlink.atagone.utils;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class PageContent {
    Map<String, List<String>> headers;
    String content;
}
