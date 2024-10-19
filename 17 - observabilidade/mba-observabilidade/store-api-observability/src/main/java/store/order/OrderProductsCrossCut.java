package store.order;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import store.product.Product;

@Service
@RequiredArgsConstructor
public class OrderProductsCrossCut {

    private final RestTemplate restTemplate;

    @Value("${api.products.endpoint.url}")
    private String apiProdutsEndpointUrl;

    @Cacheable("products")
    Product findById(final Long id) {
        final String url = String.format("%s/%s", apiProdutsEndpointUrl, id);
        return restTemplate
                .getForEntity(url, Product.class)
                .getBody();
    }
}
