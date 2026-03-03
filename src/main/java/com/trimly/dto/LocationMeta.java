package com.trimly.dto;
import lombok.*; import java.util.*; 
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LocationMeta {
    List<String> cities; Map<String, List<String>> areasByCity;
}
