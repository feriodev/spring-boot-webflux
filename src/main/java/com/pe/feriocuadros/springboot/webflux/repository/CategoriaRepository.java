package com.pe.feriocuadros.springboot.webflux.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.pe.feriocuadros.springboot.webflux.documents.Categoria;

public interface CategoriaRepository extends ReactiveMongoRepository<Categoria, String>{

}
