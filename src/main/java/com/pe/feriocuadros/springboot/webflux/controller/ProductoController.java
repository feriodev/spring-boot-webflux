package com.pe.feriocuadros.springboot.webflux.controller;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;

import com.pe.feriocuadros.springboot.webflux.documents.Categoria;
import com.pe.feriocuadros.springboot.webflux.documents.Producto;
import com.pe.feriocuadros.springboot.webflux.service.ProductService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SessionAttributes("producto")
@Controller
public class ProductoController {

	@Autowired
	private ProductService service;
	
	@Value("${config.uploads.path}")
	private String path;
	
	private static final Logger log = LoggerFactory.getLogger(ProductoController.class);
	
	@ModelAttribute("categorias")
	public Flux<Categoria> categorias(){
		return service.findAllCategoria()
				.doOnNext(c -> log.info(c.getNombre()));
	}
	
	@GetMapping({"/ver/{id}"})
	public Mono<String> ver(Model model, @PathVariable String id) {
		return service.findById(id)
				.doOnNext(prod -> {
					model.addAttribute("producto", prod);
					model.addAttribute("titulo", "Detalle producto");
				})
				.switchIfEmpty(Mono.just(new Producto()))
				.flatMap(p -> {
					if (p.getId() == null) {
						return Mono.error(new InterruptedException("No existe el producto"));
					}
					return Mono.just(p);
				})
				.then(Mono.just("ver"))
				.onErrorResume(error -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
		
	}
	
	@GetMapping({"/uploads/img/{nombrefoto:.+}"})
	public Mono<ResponseEntity<Resource>> verFoto(Model model, @PathVariable String nombrefoto) throws MalformedURLException {
		Path ruta = Paths.get(path).resolve(nombrefoto).toAbsolutePath();
		
		Resource img = new UrlResource(ruta.toUri());
		
		return Mono.just(
				ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + img.getFilename() + "\"")
				.body(img)
				);
		
	}
	
	@GetMapping({"/listar", "/"})
	public Mono<String> listar(Model model) {
		Flux<Producto> lista = service.findAllConNombreUpperCase();
		lista.subscribe(p -> log.info(p.getNombre()));		
		model.addAttribute("productos", lista);
		model.addAttribute("titulo", "Lista de productos");
		return Mono.just("listar");
		
	}
	
	@GetMapping("/form")
	public Mono<String> crear(Model model) {
		model.addAttribute("producto", new Producto());
		model.addAttribute("titulo", "Formulario de producto");
		model.addAttribute("boton", "Crear");
		return Mono.just("form");
		
	}
	
	@PostMapping("/form")
	public Mono<String> guardar(@Valid @ModelAttribute("producto") Producto producto, 
			BindingResult result, 
			Model model,
			@RequestPart FilePart file,
			SessionStatus session){
		
		if(result.hasErrors()) {
			model.addAttribute("titulo", "Errores en formulario producto");
			model.addAttribute("boton", "Guardar");
			return Mono.just("form");
			
		} else {
			session.isComplete();			
			
			return service.findCategoriaById(producto.getCategoria().getId())
				.flatMap(cat -> {					
					if(producto.getCreateAt() == null) {
						producto.setCreateAt(new Date());
					}
					log.info("Nombre foto: " + file.filename());
					if(!file.filename().isEmpty()) {
						producto.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
							.replace(" ", "")
							.replace(":", "")
							.replace("\\", "")
							.trim());
					}
					
					producto.setCategoria(cat);
					return service.save(producto);
				})
				.doOnNext(p -> {
					log.info("Producto guardado: "+ p.getNombre() + " - Id: " + p.getId());	
				})
				.flatMap(p -> {
					if(!file.filename().isEmpty()) {
						log.info("Moviendo foto:  "+ path + p.getFoto());
						return file.transferTo(new File(path + p.getFoto()));
					}
					return Mono.empty();
				})
				.thenReturn("redirect:/listar?success=Producto+guardado+con+exito");			
		}			
	}
	
	@GetMapping("/form/{id}")
	public Mono<String> editar(@PathVariable String id, Model model){
		
		Mono<Producto> producto = service.findById(id)
				.doOnNext(prod -> {
					log.info("Producto: " + prod.getNombre());
				}).defaultIfEmpty(new Producto());
		model.addAttribute("producto", producto);
		model.addAttribute("titulo", "Editar producto");
		model.addAttribute("boton", "Editar");
		return Mono.just("form");
	}
	
	@GetMapping("/form-v2/{id}")
	public Mono<String> editarV2(@PathVariable String id, Model model){
		
		return service.findById(id)
				.doOnNext(prod -> {
					log.info("Producto: " + prod.getNombre());
					model.addAttribute("producto", prod);
					model.addAttribute("titulo", "Editar producto");
					model.addAttribute("boton", "Editar");
				})
				.defaultIfEmpty(new Producto())
				.flatMap(p -> {
					if (p.getId() == null) {
						return Mono.error(new InterruptedException("No existe el producto"));
					}
					return Mono.just(p);
				})
				.then(Mono.just("form"))
				.onErrorResume(error -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
		
	}
	
	@GetMapping("/eliminar/{id}")
	public Mono<String> delete(@PathVariable String id, Model model){
		
		return service.findById(id)
				.defaultIfEmpty(new Producto())
				.flatMap(p -> {
					if (p.getId() == null) {
						return Mono.error(new InterruptedException("No existe el producto a eliminar"));
					}
					return Mono.just(p);
				})
				.flatMap(prod -> {
					log.info("Eliminando producto : " + prod.getNombre());
					return service.delete(prod);
				})
				.then(Mono.just("redirect:/listar?success=producto+eliminado+con+exito"))
				.onErrorResume(error -> Mono.just("redirect:/listar?error=no+existe+el+producto+a+eliminar"));
	}
	
	@GetMapping("/listar-datadriver")
	public String listarDataDriver(Model model) {
		Flux<Producto> lista = service.findAllConNombreUpperCase()
			.delayElements(Duration.ofSeconds(1));
		lista.subscribe(p -> log.info(p.getNombre()));		
		model.addAttribute("productos", new ReactiveDataDriverContextVariable(lista, 2));
		model.addAttribute("titulo", "Lista de productos");
		return "listar";
		
	}
	
	@GetMapping("/listarfull")
	public String listarFull(Model model) {
		Flux<Producto> lista = service.findAllConNombreUpperCaseRepeat();		
		model.addAttribute("productos", lista);
		model.addAttribute("titulo", "Lista de productos");
		return "listar";
		
	}
	
	@GetMapping("/listarchunked")
	public String listarChunked(Model model) {
		Flux<Producto> lista = service.findAllConNombreUpperCaseRepeat();		
		model.addAttribute("productos", lista);
		model.addAttribute("titulo", "Lista de productos");
		return "listar-chunked";
		
	}
}
