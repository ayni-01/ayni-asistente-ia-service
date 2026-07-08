package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.PostulacionContextoRepository;
import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.PostulacionContexto;
import com.somosayni.asistente.domain.model.RetoContexto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerFeedbackSolucionQueryHandlerTest {

    @Mock
    PostulacionContextoRepository postulacionContextoRepository;

    @Mock
    RetoContextoRepository retoContextoRepository;

    @Mock
    AsistenteIAPort asistenteIAPort;

    private ObtenerFeedbackSolucionQueryHandler handler() {
        return new ObtenerFeedbackSolucionQueryHandler(
                postulacionContextoRepository, retoContextoRepository, asistenteIAPort);
    }

    @Test
    void devuelveFeedbackCuandoLaPostulacionPerteneceAlTalento() {
        when(postulacionContextoRepository.obtenerPorId("post-1"))
                .thenReturn(Optional.of(new PostulacionContexto("post-1", "talento-1", "reto-1")));
        when(retoContextoRepository.obtenerPorId("reto-1"))
                .thenReturn(Optional.of(new RetoContexto("reto-1", "t", "d", "BACKEND", "JUNIOR", List.of())));
        when(asistenteIAPort.responder(anyString(), anyString())).thenReturn("Buen enfoque, pero...");

        String feedback = handler().handle(
                new ObtenerFeedbackSolucionQuery("post-1", "talento-1", "Voy a usar arquitectura MVC"));

        assertThat(feedback).isEqualTo("Buen enfoque, pero...");
    }

    @Test
    void lanzaExcepcionSiLaPostulacionNoExiste() {
        when(postulacionContextoRepository.obtenerPorId("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler().handle(
                new ObtenerFeedbackSolucionQuery("no-existe", "talento-1", "enfoque")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lanzaExcepcionSiLaPostulacionNoPerteneceAlTalentoAutenticado() {
        when(postulacionContextoRepository.obtenerPorId("post-1"))
                .thenReturn(Optional.of(new PostulacionContexto("post-1", "otro-talento", "reto-1")));

        assertThatThrownBy(() -> handler().handle(
                new ObtenerFeedbackSolucionQuery("post-1", "talento-1", "enfoque")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
