package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.RetoContextoRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarRetoQueryHandlerTest {

    @Mock
    RetoContextoRepository retoContextoRepository;

    @Mock
    AsistenteIAPort asistenteIAPort;

    @Test
    void respondeConElTextoQueDevuelveElPuertoDeIaCuandoElRetoExiste() {
        RetoContexto reto = new RetoContexto("reto-1", "Landing page en React",
                "Construir una landing responsive", "FRONTEND", "JUNIOR",
                List.of("Usar React", "Ser responsive"));
        when(retoContextoRepository.obtenerPorId("reto-1")).thenReturn(Optional.of(reto));
        when(asistenteIAPort.responder(anyString(), anyString())).thenReturn("Respuesta de la IA");

        ConsultarRetoQueryHandler handler = new ConsultarRetoQueryHandler(retoContextoRepository, asistenteIAPort);
        String respuesta = handler.handle(new ConsultarRetoQuery("reto-1", "¿Qué tecnologías necesito?"));

        assertThat(respuesta).isEqualTo("Respuesta de la IA");
        verify(asistenteIAPort).responder(anyString(), anyString());
    }

    @Test
    void lanzaIllegalArgumentExceptionCuandoElRetoNoExiste() {
        when(retoContextoRepository.obtenerPorId("no-existe")).thenReturn(Optional.empty());

        ConsultarRetoQueryHandler handler = new ConsultarRetoQueryHandler(retoContextoRepository, asistenteIAPort);

        assertThatThrownBy(() -> handler.handle(new ConsultarRetoQuery("no-existe", "¿Qué necesito?")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no-existe");
    }
}
