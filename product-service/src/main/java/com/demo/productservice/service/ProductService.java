package com.demo.productservice.service;

import com.demo.productservice.entity.Product;
import com.demo.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public void seedData() {
        if (productRepository.count() == 0) {
            productRepository.save(Product.builder().name("Laptop Dell XPS").description("High performance laptop").price(new BigDecimal("25000000")).stock(50).build());
            productRepository.save(Product.builder().name("iPhone 15 Pro").description("Latest iPhone").price(new BigDecimal("30000000")).stock(100).build());
            productRepository.save(Product.builder().name("Samsung 4K TV").description("Smart TV 55 inch").price(new BigDecimal("15000000")).stock(30).build());
            log.info("Seeded 3 demo products");
        }
    }
}
