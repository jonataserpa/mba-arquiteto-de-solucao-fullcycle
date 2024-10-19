package store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import store.customer.Customer;
import store.customer.CustomerRepository;
import store.order.Order;
import store.order.OrderItem;
import store.order.OrderRepository;
import store.order.OrderStatus;
import store.product.Product;
import store.product.ProductRepository;

import java.math.BigDecimal;
import java.util.List;


@Profile("h2 || product")
@Configuration
class StoreApplicationStartUp {

    private static final Logger log = LoggerFactory.getLogger(StoreApplicationStartUp.class);

    @Bean
    CommandLineRunner initDatabase(
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository) {

        return args -> {

            customerRepository.save(Customer.builder()
                    .name("Hringr the Melancholic")
                    .personalId(Utils.randomPersonalId())
                    .birth(Utils.randomBirthday())
                    .build());
            customerRepository.save(Customer.builder()
                    .name("Hringr the Melancholic")
                    .personalId(Utils.randomPersonalId())
                    .birth(Utils.randomBirthday())
                    .build());
            customerRepository.findAll().forEach(customer -> log.info("Customer " + customer));

            var p1 = productRepository.save(Product.builder()
                    .title("Apple MacBook Pro 14 Inch Space Grey")
                    .price(BigDecimal.valueOf(1999.99))
                    .description("The MacBook Pro 14 Inch in Space Grey is a powerful and sleek laptop, " +
                            "featuring Apple's M1 Pro chip for exceptional performance and a stunning " +
                            "Retina display.")
                    .brand("Apple")
                    .category("laptops")
                    .rating(3.13f)
                    .build());
            var p2 = productRepository.save(Product.builder()
                    .title("Sports Sneakers Off White Red")
                    .price(BigDecimal.valueOf(109.99))
                    .description("Another variant of the Sports Sneakers in Off White Red, featuring a unique design. " +
                            "These sneakers offer style and comfort for casual occasions.")
                    .brand("Off White")
                    .category("mens-shoes")
                    .rating(2.95f)
                    .build());
            productRepository.findAll().forEach(product -> log.info("Product " + product));

            var item1 = OrderItem.builder().product(p1).quantity(2).build();
            var item2 = OrderItem.builder().product(p2).quantity(1).build();
            var order1 = Order.builder()
                    .items(List.of(item1, item2))
                    .totalPrice(Utils.sum(
                            p1.getPrice().multiply(BigDecimal.valueOf(item1.getQuantity())),
                            p2.getPrice().multiply(BigDecimal.valueOf(item2.getQuantity()))
                    ))
                    .status(OrderStatus.IN_PROGRESS)
                    .build();
            orderRepository.save(order1);
            orderRepository.findAll().forEach(order -> log.info("Order " + order));

        };
    }
}
