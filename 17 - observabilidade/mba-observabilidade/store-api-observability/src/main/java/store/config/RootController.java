package store.config;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import store.customer.CustomerController;
import store.order.OrderController;
import store.product.ProductController;

@RestController
public class RootController {

	@GetMapping
	RepresentationModel<?> index() {

		RepresentationModel<?> rootModel = new RepresentationModel<>();
		rootModel.add(linkTo(methodOn(CustomerController.class).all()).withRel("employees"));
		rootModel.add(linkTo(methodOn(OrderController.class).all(0, 100)).withRel("orders"));
		rootModel.add(linkTo(methodOn(ProductController.class).all()).withRel("products"));
		return rootModel;
	}

}
