package microarch.delivery.core.application.queries;

import microarch.delivery.adapters.out.postgres.AbstractPostgresIntegrationTest;
import microarch.delivery.adapters.out.postgres.OrderJpaRepository;
import microarch.delivery.core.application.queries.dto.OrderDto;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Speed;
import microarch.delivery.core.domain.model.kernel.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.ports.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GetActiveOrdersQueryHandlerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private GetActiveOrdersQueryHandler handler;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderJpaRepository jpaRepository;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldReturnAllUncompletedOrders() {
        // Arrange
        var courier1 = Courier
                .create("Иван", Speed.create(2).getValueOrThrow(), Location.create(1, 1).getValueOrThrow())
                .getValueOrThrow();
        var courier2 = Courier
                .create("Петр", Speed.create(2).getValueOrThrow(), Location.create(2, 2).getValueOrThrow())
                .getValueOrThrow();
        var order1 = Order.create(UUID.randomUUID(), Location.create(1, 2).getValueOrThrow(),
                Volume.create(5, 2, 1).getValueOrThrow()).getValueOrThrow();

        var order2 = Order.create(UUID.randomUUID(), Location.create(3, 4).getValueOrThrow(),
                Volume.create(5, 2, 1).getValueOrThrow()).getValueOrThrow();
        order2.assign(courier1);

        var completedOrder = Order.create(UUID.randomUUID(), Location.create(5, 6).getValueOrThrow(),
                Volume.create(5, 1, 1).getValueOrThrow()).getValueOrThrow();
        completedOrder.assign(courier2);
        completedOrder.complete();

        orderRepository.saveOrder(order1);
        orderRepository.saveOrder(order2);
        orderRepository.saveOrder(completedOrder);

        // Act
        var result = handler.handle();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        GetActiveOrdersResponse response = result.getValue();

        assertThat(response.orders()).hasSize(2);

        assertThat(response.orders()).extracting(OrderDto::id).containsExactlyInAnyOrder(order1.getId(),
                order2.getId());

        assertThat(response.orders()).extracting(OrderDto::id).doesNotContain(completedOrder.getId());
    }

    @Test
    void shouldReturnEmptyListWhenNoUncompletedOrders() {
        // Arrange
        var courier = Courier.create("Иван", Speed.create(2).getValueOrThrow(), Location.create(1, 1).getValueOrThrow())
                .getValueOrThrow();
        var completedOrder = Order.create(UUID.randomUUID(), Location.create(1, 2).getValueOrThrow(),
                Volume.create(5, 1, 1).getValueOrThrow()).getValueOrThrow();
        completedOrder.assign(courier);
        completedOrder.complete();

        orderRepository.saveOrder(completedOrder);

        // Act
        var result = handler.handle();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        GetActiveOrdersResponse response = result.getValue();
        assertThat(response.orders()).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenNoOrders() {
        // Act
        var result = handler.handle();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        GetActiveOrdersResponse response = result.getValue();
        assertThat(response.orders()).isEmpty();
    }

    @Test
    void shouldReturnCorrectOrderDtoFields() {
        // Arrange
        var order = Order.create(UUID.randomUUID(), Location.create(5, 7).getValueOrThrow(),
                Volume.create(2, 1, 1).getValueOrThrow()).getValueOrThrow();
        orderRepository.saveOrder(order);

        // Act
        var result = handler.handle();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        GetActiveOrdersResponse response = result.getValue();

        assertThat(response.orders()).hasSize(1);
        OrderDto dto = response.orders().getFirst();

        // Проверяем, что DTO содержит только нужные поля
        assertThat(dto.id()).isEqualTo(order.getId());
        assertThat(dto.location()).isEqualTo(Location.create(5, 7).getValueOrThrow());
        assertThat(dto).hasNoNullFieldsOrProperties();
    }
}