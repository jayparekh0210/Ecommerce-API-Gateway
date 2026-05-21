package com.ecom.ecomapigateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

@Configuration
public class Gateway {

    @Bean
    public RedisRateLimiter redisRateLimiter(){
        return new RedisRateLimiter(5, 10, 1);

    }

    @Bean
    public KeyResolver hostNameKeyResolver(){
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getHostString());
    }



    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("userService", r -> r.path("/user/**")
                        .filters(f -> f
                                .rewritePath("/user/(?<segment>.*)", "/api/user/${segment}")
                                .requestRateLimiter(rl -> rl.setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(hostNameKeyResolver()))
                                .retry(rt->rt.setRetries(2).setMethods(HttpMethod.GET))
                                .circuitBreaker(c -> c.setName("apiGatewayService")
                                        .setFallbackUri("forward:/fallback/user")
                                )

                        )
                        .uri("lb://USERSERVICE"))
                .route("productService", r -> r.path("/product/**")
                        .filters(f -> f
                                .rewritePath("/product/(?<segment>.*)", "/api/product/${segment}")
                                .requestRateLimiter(rl -> rl.setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(hostNameKeyResolver()))
                                .retry(rt->rt.setRetries(2).setMethods(HttpMethod.GET))
                                .circuitBreaker(c -> c.setName("apiGatewayService")
                                        .setFallbackUri("forward:/fallback/product")
                                )
                        )
                        .uri("lb://PRODUCTSERVICE"))
                .route("orderService", r -> r.path("/order/**")
                        .filters(f -> f
                                .rewritePath("/order/(?<segment>.*)", "/api/order/${segment}")
                                .requestRateLimiter(rl -> rl.setRateLimiter(redisRateLimiter())
                                .setKeyResolver(hostNameKeyResolver()))
                                .retry(rt->rt.setRetries(2).setMethods(HttpMethod.GET))
                                .circuitBreaker(c -> c.setName("apiGatewayService")
                                        .setFallbackUri("forward:/fallback/order")
                                )
                        )
                        .uri("lb://ORDERSERVICE"))
                .route("cartService", r -> r.path("/cart/**")
                        .filters(f -> f
                                .rewritePath("/cart/(?<segment>.*)", "/api/cart/${segment}")
                                .requestRateLimiter(rl -> rl.setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(hostNameKeyResolver()))
                                .retry(rt->rt.setRetries(2).setMethods(HttpMethod.GET))
                                .circuitBreaker(c -> c.setName("apiGatewayService")
                                        .setFallbackUri("forward:/fallback/order")
                                ))
                        .uri("lb://ORDERSERVICE"))
                .route("eurekaServer", r -> r.path("/eureka/main")
                        .filters(p -> p.setPath("/"))
                        .uri("http://localhost:8761"))
                .route("eurekaServerStatic", r -> r.path("/eureka/**")
                        .uri("http://localhost:8761"))
                .build();
    }
}
