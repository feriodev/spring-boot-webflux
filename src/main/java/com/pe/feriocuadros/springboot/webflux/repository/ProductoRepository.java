package com.pe.feriocuadros.springboot.webflux.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.pe.feriocuadros.springboot.webflux.documents.Producto;

public interface ProductoRepository extends ReactiveMongoRepository<Producto, String> {

}
