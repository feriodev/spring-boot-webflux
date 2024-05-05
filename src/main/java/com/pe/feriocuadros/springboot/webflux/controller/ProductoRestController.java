package com.pe.feriocuadros.springboot.webflux.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pe.feriocuadros.springboot.webflux.documents.Producto;
import com.pe.feriocuadros.springboot.webflux.repository.ProductoRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/productos")
public class ProductoRestController {

	@Autowired
	private ProductoRepository repository;
	
	private static final Logger log = LoggerFactory.getLogger(ProductoRestController.class);
	
	@GetMapping
	public Flux<Producto> index(){
		return repository.findAll()
			.map(producto ->{
				producto.setNombre(producto.getNombre().toUpperCase());
				return producto;
			})
			.doOnNext(p -> log.info(p.getNombre()));

	}
	
	@GetMapping("{id}")
	public Mono<Producto> show(@PathVariable String id){
		Flux<Producto> lista = repository.findAll();
		
		return lista.filter(prod -> prod.getId().equals(id)).next()
			.doOnNext(p -> log.info(p.getNombre()));

	}
}
