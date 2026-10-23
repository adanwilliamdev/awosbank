package com.awosbank.api.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ExtratoFiltroDTO {
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String tipo; // nome do enum como String ou null
}