package com.somosayni.asistente.infrastructure.ai;

import com.somosayni.asistente.application.port.CatalogoRecursosPort;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CatalogoRecursosAdapter implements CatalogoRecursosPort {

    private final VectorStore vectorStore;

    public CatalogoRecursosAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public int ingestar() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] archivos = resolver.getResources("classpath:docs/*.md");
            List<Document> documentos = new ArrayList<>();
            for (Resource archivo : archivos) {
                String texto = archivo.getContentAsString(StandardCharsets.UTF_8);
                String[] secciones = texto.split("(?m)^## ");
                for (int i = 1; i < secciones.length; i++) {
                    documentos.add(new Document("## " + secciones[i].trim()));
                }
            }
            vectorStore.add(documentos);
            return documentos.size();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String buscarRelevantes(String consulta, int topK) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(consulta).topK(topK).build());
        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
    }
}
