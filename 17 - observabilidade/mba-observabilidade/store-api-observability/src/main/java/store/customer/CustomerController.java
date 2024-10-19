package store.customer;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerRepository repository;
	private final CustomerModelAssembler assembler;

	@GetMapping("/customers")
	public CollectionModel<EntityModel<Customer>> all() {
		List<EntityModel<Customer>> customers = repository.findAll().stream()
				.map(assembler::toModel)
				.collect(Collectors.toList());
		return CollectionModel.of(customers, linkTo(methodOn(CustomerController.class).all()).withSelfRel());
	}

	@PostMapping("/customers")
	public ResponseEntity<?> newCustomer(@RequestBody Customer customer) {
		EntityModel<Customer> entityModel = assembler.toModel(repository.save(customer));
		return ResponseEntity.created(entityModel.getRequiredLink(IanaLinkRelations.SELF).toUri())
				.body(entityModel);
	}

	// Single item

	@GetMapping("/customers/{id}")
	public EntityModel<Customer> one(@PathVariable Long id) {

		Customer customer = repository.findById(id)
				.orElseThrow(() -> new CustomerNotFoundException(id));

		return assembler.toModel(customer);
	}

	@DeleteMapping("/customers/{id}")
	public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {

		repository.deleteById(id);

		return ResponseEntity.noContent().build();
	}
}
