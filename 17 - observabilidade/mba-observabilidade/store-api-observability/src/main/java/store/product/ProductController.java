package store.product;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository repository;
    private final ProductModelAssembler assembler;


    @GetMapping("/products")
    public CollectionModel<EntityModel<Product>> all() {

        List<EntityModel<Product>> products = repository.findAll().stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return CollectionModel.of(products, linkTo(methodOn(ProductController.class).all()).withSelfRel());
    }

    @PostMapping("/products")
    public ResponseEntity<?> newProduct(@RequestBody Product product) {

        EntityModel<Product> entityModel = assembler.toModel(repository.save(product));

        return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(entityModel);
    }

    @GetMapping("/products/{id}")
    public EntityModel<Product> one(@PathVariable Long id) {

        Product product = repository.findById(id) //
                .orElseThrow(() -> new ProductNotFoundException(id));

        return assembler.toModel(product);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {

        repository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
