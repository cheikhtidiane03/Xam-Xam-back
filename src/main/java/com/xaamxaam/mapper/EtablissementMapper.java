package com.xaamxaam.mapper;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.dto.response.EtablissementResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EtablissementMapper {

    EtablissementResponse toResponse(Etablissement etablissement);
}
