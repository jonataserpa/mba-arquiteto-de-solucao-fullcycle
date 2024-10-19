package store.order;

public class OrderItemProductNotFoundException extends RuntimeException {
	OrderItemProductNotFoundException(long id) {
		super("Product informed not available - ID: " + id);
	}
}
