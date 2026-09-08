package com.xaamxaam.mapper;

import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.exercice.Indice;
import com.xaamxaam.domain.exercice.Reformulation;
import com.xaamxaam.dto.response.ExerciceResponse;
import com.xaamxaam.dto.response.IndiceResponse;
import com.xaamxaam.dto.response.ReformulationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExerciceMapper {

    ExerciceResponse toResponse(Exercice exercice);

    IndiceResponse toResponse(Indice indice);

    ReformulationResponse toResponse(Reformulation reformulation);
}
