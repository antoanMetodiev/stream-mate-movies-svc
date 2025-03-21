package com.example.streammatemoviesvc.app.feather.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CinemaRecRequestDto {
    @NotBlank(message = "Record Name cannot be empty!")
    private String recordName;
}
