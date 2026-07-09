package com.somosayni.asistente.application.port;

public interface CatalogoRecursosPort {
    int ingestar();
    String buscarRelevantes(String consulta, int topK);
}
