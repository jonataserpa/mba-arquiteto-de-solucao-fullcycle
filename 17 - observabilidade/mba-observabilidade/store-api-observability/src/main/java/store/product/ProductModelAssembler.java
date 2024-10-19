package store.product;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class ProductModelAssembler implements RepresentationModelAssembler<Product, EntityModel<Product>> {

	@Override
	public EntityModel<Product> toModel(Product product) {

		// Unconditional links to single-item resource and aggregate root

		EntityModel<Product> productModel = EntityModel.of(product,
				linkTo(methodOn(ProductController.class).one(product.getId())).withSelfRel(),
				linkTo(methodOn(ProductController.class).all()).withRel("products"));

		return productModel;
	}
}
