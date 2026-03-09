package com.neo.nexora.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostMediaUpdateDto {
    List<String> MediaToRemove;
}
