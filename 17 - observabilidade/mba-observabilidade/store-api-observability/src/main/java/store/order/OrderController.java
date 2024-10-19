package store.order;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import store.product.Product;
import store.product.ProductRepository;

@Slf4j
@RestController
public class OrderController {

	private final OrderModelAssembler assembler;
	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final OrderProductsCrossCut orderProductsCrossCut;
	private final MeterRegistry meterRegistry;

	final Counter counterQuantidadeNovasSolicitacoes;
	final AtomicInteger gaugeValueProdutosAdquiridos = new AtomicInteger();

    public OrderController(
			OrderModelAssembler assembler,
			OrderRepository orderRepository,
			ProductRepository productRepository,
			OrderProductsCrossCut orderProductsCrossCut,
			MeterRegistry meterRegistry) {

        this.assembler = assembler;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
		this.orderProductsCrossCut = orderProductsCrossCut;
        this.meterRegistry = meterRegistry;

		this.counterQuantidadeNovasSolicitacoes = Counter.builder("app_custom_quantidade_novas_solicitacoes")
				.description("quantidade de novas solicitações efetuadas")
				.register(meterRegistry);

		Gauge.builder("app_custom_metric_quantidade_de_produtos", gaugeValueProdutosAdquiridos::get)
				.description("Quantidade de produtos adquiridos")
				.register(meterRegistry);
    }

	@GetMapping("/orders")
	public CollectionModel<EntityModel<Order>> all(
			@RequestParam(defaultValue = "0") final Integer page,
			@RequestParam(defaultValue = "100") final Integer size) {

		final Pageable pageable = PageRequest.of(page, size);

		final List<Order> orders = orderRepository.findAll(pageable).stream().toList();
		orders.stream()
				.map(Order::getItems)
				.flatMap(Collection::stream)
				.parallel()
				.forEach(item -> {
					final Product p = orderProductsCrossCut.findById(item.getProduct().getId());
					item.setProduct(p);
				});

		//final List<EntityModel<Order>> orders = orderRepository.findAll(pageable).stream()
		//		.map(assembler::toModel)
		//		.collect(Collectors.toList());

		final List<EntityModel<Order>> ordersModel = orders.stream()
				.map(assembler::toModel)
				.toList();

		log.info("{} orders retrieved", orders.size());
		return CollectionModel.of(ordersModel,
				linkTo(methodOn(OrderController.class).all(0, 100)).withSelfRel());
	}

	@GetMapping("/orders/{id}")
	public EntityModel<Order> one(@PathVariable Long id) {
		log.info("Request for order by ID: {}", id);

		final Order order = orderRepository.findById(id)
				.orElseThrow(() -> new OrderNotFoundException(id));

		order.getItems().forEach(item -> {
			final Product p = orderProductsCrossCut.findById(item.getProduct().getId());
			item.setProduct(p);
		});

		log.debug("Order: {}", order);

		return assembler.toModel(order);
	}

	@PostMapping("/orders")
	public ResponseEntity<EntityModel<Order>> newOrder(final @RequestBody Order order) {
		log.info("requested new order");

		if (CollectionUtils.isEmpty(order.getItems())) {
			log.info("order request with no items");
			log.debug("detailed request: {}", order);
			throw new OrderWithInvalidItemsException("Order items not informed correctly", order);
		}

		order.setStatus(OrderStatus.IN_PROGRESS);

		log.debug("calculating total price for order: {}", order.getId());
		BigDecimal totalPrice = BigDecimal.ZERO;
		for (OrderItem item : order.getItems()) {
			final Long productId = item.getProduct().getId();

			// final Product product = productRepository.findById(productId)
			//	.orElseThrow(() -> new OrderItemProductNotFoundException(productId));
			final Product product = orderProductsCrossCut.findById(productId);

			final BigDecimal productPrice = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
			item.setProduct(product);
			totalPrice = totalPrice.add(productPrice);
		}
		order.setTotalPrice(totalPrice);

		log.debug("order that is going to be persisted: {}", order);
		final Order newOrder = orderRepository.save(order);

		log.debug("order saved successfully: {}", newOrder);

		// ------ métricas ----------------------------
		counterQuantidadeNovasSolicitacoes.increment();
		gaugeValueProdutosAdquiridos.set(order.getItems().stream()
				.map(OrderItem::getQuantity)
				.reduce(Integer::sum)
				.orElse(0));
		// --------------------------------------------

		return ResponseEntity
				.created(linkTo(methodOn(OrderController.class).one(newOrder.getId())).toUri())
				.body(assembler.toModel(newOrder));
	}

	@DeleteMapping("/orders/{id}/cancel")
	public ResponseEntity<?> cancel(@PathVariable Long id) {

		final Order order = orderRepository.findById(id)
				.orElseThrow(() -> new OrderNotFoundException(id));

		if (order.getStatus() == OrderStatus.IN_PROGRESS) {
			order.setStatus(OrderStatus.CANCELLED);
			return ResponseEntity.ok(assembler.toModel(orderRepository.save(order)));
		}

		log.debug("order can't be canceled: {}", order);

		return ResponseEntity 
				.status(HttpStatus.METHOD_NOT_ALLOWED) 
				.header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE) 
				.body(Problem.create() 
						.withTitle("Order can't be canceled")
						.withDetail("You can't cancel an order that is in the " + order.getStatus() + " status"));
	}

	@PutMapping("/orders/{id}/complete")
	public ResponseEntity<?> complete(@PathVariable Long id) {
		log.info("request for completing an order - ID: {}", id);

		final Order order = orderRepository.findById(id)
				.orElseThrow(() -> new OrderNotFoundException(id));

		if (order.getStatus() == OrderStatus.IN_PROGRESS) {
			log.info("order marked as complete");
			order.setStatus(OrderStatus.COMPLETED);

			final Order persistedOrder = orderRepository.save(order);
			log.debug("order persisted: {}", persistedOrder);

			return ResponseEntity.ok(assembler.toModel(persistedOrder));
		}

		log.info("order can't be completed");
		log.debug("order data: {}", order);

		return ResponseEntity
				.status(HttpStatus.METHOD_NOT_ALLOWED) 
				.header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE) 
				.body(Problem.create() 
						.withTitle("Method not allowed")
						.withDetail("You can't complete an order that is in the " + order.getStatus() + " status"));
	}
}
