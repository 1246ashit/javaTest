package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonCreator; 
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor(onConstructor_ = @JsonCreator)
public class CreateRoomRequest {

    private final Long bossId;
    private final String name;
}