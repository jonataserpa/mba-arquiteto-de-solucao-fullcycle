package store.order;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	private OrderStatus status;

	private BigDecimal totalPrice;

	@OneToMany(orphanRemoval = true,
			fetch = FetchType.EAGER,
			cascade = CascadeType.ALL,
			targetEntity = OrderItem.class)
	@JoinColumn(name = "order_id", referencedColumnName = "id")
	private List<OrderItem> items = new ArrayList<>();
}
