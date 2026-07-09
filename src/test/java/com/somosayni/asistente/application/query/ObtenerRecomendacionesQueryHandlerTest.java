package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.CatalogoRecursosPort;
import com.somosayni.asistente.application.port.HabilidadContextoRepository;
import com.somosayni.asistente.domain.model.HabilidadContexto;
import com.somosayni.asistente.domain.model.RecomendacionAprendizaje;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerRecomendacionesQueryHandlerTest {

    @Mock
    HabilidadContextoRepository habilidadContextoRepository;

    @Mock
    CatalogoRecursosPort catalogoRecursosPort;

    @Mock
    AsistenteIAPort asistenteIAPort;

    @Test
    void construyeLaConsultaRagConLasHabilidadesDelTalento() {
        when(habilidadContextoRepository.obtenerPorTalentoId("talento-1"))
                .thenReturn(List.of(new HabilidadContexto("React", "INTERMEDIO", 60)));
        when(catalogoRecursosPort.buscarRelevantes(anyString(), anyInt())).thenReturn("contexto");
        List<RecomendacionAprendizaje> esperado = List.of(
                new RecomendacionAprendizaje("Angular", "Cierra tu brecha frontend", "INTERMEDIO",
                        "Angular desde cero", "https://recursos.somosayni.com/angular"));
        when(asistenteIAPort.recomendarConRag("talento-1", "contexto")).thenReturn(esperado);

        ObtenerRecomendacionesQueryHandler handler = new ObtenerRecomendacionesQueryHandler(
                habilidadContextoRepository, catalogoRecursosPort, asistenteIAPort);
        List<RecomendacionAprendizaje> resultado = handler.handle(new ObtenerRecomendacionesQuery("talento-1"));

        assertThat(resultado).isEqualTo(esperado);
        ArgumentCaptor<String> consultaCaptor = ArgumentCaptor.forClass(String.class);
        verify(catalogoRecursosPort).buscarRelevantes(consultaCaptor.capture(), eq(5));
        assertThat(consultaCaptor.getValue()).contains("React");
    }

    @Test
    void usaUnaConsultaGenericaCuandoElTalentoNoTieneHabilidadesValidadas() {
        when(habilidadContextoRepository.obtenerPorTalentoId("talento-2")).thenReturn(List.of());
        when(catalogoRecursosPort.buscarRelevantes(anyString(), anyInt())).thenReturn("contexto");
        when(asistenteIAPort.recomendarConRag(eq("talento-2"), anyString())).thenReturn(List.of());

        ObtenerRecomendacionesQueryHandler handler = new ObtenerRecomendacionesQueryHandler(
                habilidadContextoRepository, catalogoRecursosPort, asistenteIAPort);
        handler.handle(new ObtenerRecomendacionesQuery("talento-2"));

        ArgumentCaptor<String> consultaCaptor = ArgumentCaptor.forClass(String.class);
        verify(catalogoRecursosPort).buscarRelevantes(consultaCaptor.capture(), eq(5));
        assertThat(consultaCaptor.getValue()).contains("junior");
    }
}
