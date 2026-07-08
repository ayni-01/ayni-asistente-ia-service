package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.HabilidadContextoRepository;
import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.HabilidadContexto;
import com.somosayni.asistente.domain.model.RecomendacionAprendizaje;
import com.somosayni.asistente.domain.model.RetoContexto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerRecomendacionesQueryHandlerTest {

    @Mock
    HabilidadContextoRepository habilidadContextoRepository;

    @Mock
    RetoContextoRepository retoContextoRepository;

    @Mock
    AsistenteIAPort asistenteIAPort;

    @Test
    void incluyeLasHabilidadesDelTalentoEnElPromptCuandoTiene() {
        when(habilidadContextoRepository.obtenerPorTalentoId("talento-1"))
                .thenReturn(List.of(new HabilidadContexto("React", "INTERMEDIO", 60)));
        when(retoContextoRepository.obtenerActivos())
                .thenReturn(List.of(new RetoContexto("reto-1", "t", "d", "BACKEND", "JUNIOR", List.of())));
        List<RecomendacionAprendizaje> esperado = List.of(
                new RecomendacionAprendizaje("Spring Boot", "Hay retos activos de BACKEND", "JUNIOR"));
        when(asistenteIAPort.responderRecomendaciones(anyString(), anyString())).thenReturn(esperado);

        ObtenerRecomendacionesQueryHandler handler = new ObtenerRecomendacionesQueryHandler(
                habilidadContextoRepository, retoContextoRepository, asistenteIAPort);
        List<RecomendacionAprendizaje> resultado = handler.handle(new ObtenerRecomendacionesQuery("talento-1"));

        assertThat(resultado).isEqualTo(esperado);
        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(asistenteIAPort).responderRecomendaciones(anyString(), userPromptCaptor.capture());
        assertThat(userPromptCaptor.getValue()).contains("React").contains("BACKEND");
    }

    @Test
    void usaUnPromptGenericoCuandoElTalentoNoTieneHabilidadesValidadas() {
        when(habilidadContextoRepository.obtenerPorTalentoId("talento-2")).thenReturn(List.of());
        when(retoContextoRepository.obtenerActivos()).thenReturn(List.of());
        when(asistenteIAPort.responderRecomendaciones(anyString(), anyString())).thenReturn(List.of());

        ObtenerRecomendacionesQueryHandler handler = new ObtenerRecomendacionesQueryHandler(
                habilidadContextoRepository, retoContextoRepository, asistenteIAPort);
        handler.handle(new ObtenerRecomendacionesQuery("talento-2"));

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(asistenteIAPort).responderRecomendaciones(anyString(), userPromptCaptor.capture());
        assertThat(userPromptCaptor.getValue()).contains("aún no tiene habilidades validadas");
    }
}
