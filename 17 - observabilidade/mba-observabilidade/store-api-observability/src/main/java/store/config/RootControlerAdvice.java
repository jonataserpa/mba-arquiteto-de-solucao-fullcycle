package store.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import store.customer.CustomerNotFoundException;
import store.order.OrderItemProductNotFoundException;
import store.order.OrderNotFoundException;
import store.order.OrderWithInvalidItemsException;

@Slf4j
@RestControllerAdvice
class RootControlerAdvice {

	@ExceptionHandler(CustomerNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	Problem customerNotFoundHandler(final CustomerNotFoundException e) {
		log.error(e.getMessage(), e);
		return Problem.create()
				.withTitle("Customer not found")
				.withDetail(e.getMessage());
	}

	@ExceptionHandler(OrderNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	Problem orderNotFoundHandler(final OrderNotFoundException e) {
		log.error(e.getMessage(), e);
		return Problem.create()
				.withTitle("Order not found")
				.withDetail(e.getMessage());
	}

	@ExceptionHandler(OrderItemProductNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
	Problem orderItemProductNotFoundHandler(final OrderItemProductNotFoundException e) {
		log.error(e.getMessage(), e);
		return Problem.create()
				.withTitle("Product not available")
				.withDetail(e.getMessage());
	}

	@ExceptionHandler(OrderWithInvalidItemsException.class)
	@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
	Problem orderItemProductNotFoundHandler(final OrderWithInvalidItemsException e) {
		log.error(e.getMessage(), e);
		return Problem.create()
				.withTitle("Invalid items in the cart")
				.withDetail(e.getMessage())
				.withProperties(e.getOrder());
	}

}
