package microarch.delivery.core.application.commands;

import libs.errs.Error;
import libs.errs.Result;
import libs.errs.UnitResult;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Speed;
import microarch.delivery.core.domain.model.kernel.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.services.OrderDispatcher;
import microarch.delivery.core.ports.CourierRepository;
import microarch.delivery.core.ports.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignOrderCommandHandlerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CourierRepository courierRepository;

    @Mock
    private OrderDispatcher orderDispatcher;

    private AssignOrderCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AssignOrderCommandHandlerImpl(orderDispatcher, courierRepository, orderRepository);
    }

    @Test
    @DisplayName("Успешное назначение заказа курьеру")
    void handleShouldBeSuccessWhenOrderAssignedToCourier() {
        // Arrange
        Order order = createOrder();
        Courier courier = createCourier();

        when(orderRepository.findCreatedOrder()).thenReturn(Optional.of(order));
        when(courierRepository.findAvailableCouriers()).thenReturn(List.of(courier));
        when(orderDispatcher.dispatch(order, List.of(courier))).thenReturn(Result.success(courier));

        // Act
        UnitResult<Error> result = handler.handle();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        verify(orderDispatcher).dispatch(order, List.of(courier));
        verify(orderRepository).saveOrder(order);
        verify(courierRepository).saveCourier(courier);
    }

    @Test
    @DisplayName("Успешное выполнение, когда нет созданных заказов")
    void handleShouldBeSuccessWhenNoCreatedOrders() {
        // Arrange
        when(orderRepository.findCreatedOrder()).thenReturn(Optional.empty());

        // Act
        UnitResult<Error> result = handler.handle();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        verify(orderRepository, never()).saveOrder(any());
        verify(courierRepository, never()).saveCourier(any());
        verify(orderDispatcher, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("Ошибка выполнения, когда нет доступных курьеров")
    void handleShouldReturnFailureWhenNoAvailableCouriers() {
        // Arrange
        Order order = createOrder();

        when(orderRepository.findCreatedOrder()).thenReturn(Optional.of(order));
        when(courierRepository.findAvailableCouriers()).thenReturn(List.of());

        Error error = Error.of("courier.is.not.found.order", "Нет доступных курьеров");
        when(orderDispatcher.dispatch(order, List.of())).thenReturn(Result.failure(error));

        // Act
        UnitResult<Error> result = handler.handle();

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.is.not.found.order");
        assertThat(result.getError().getMessage()).isEqualTo("Нет доступных курьеров");

        verify(orderRepository, never()).saveOrder(any());
        verify(courierRepository, never()).saveCourier(any());
    }

    private Order createOrder() {
        return Order.create(UUID.randomUUID(), Location.create(5, 5).getValueOrThrow(),
                Volume.create(5, 2, 1).getValueOrThrow()).getValueOrThrow();
    }

    private Courier createCourier() {
        return Courier.create("Иван Петров", Speed.create(2).getValueOrThrow(), Location.create(1, 1).getValueOrThrow())
                .getValueOrThrow();
    }
}