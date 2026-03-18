package microarch.delivery.core.application.commands;

import libs.errs.Error;
import libs.errs.UnitResult;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Speed;
import microarch.delivery.core.domain.model.kernel.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.ports.CourierRepository;
import microarch.delivery.core.ports.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoveCourierCommandHandlerTest {

    @Mock
    private CourierRepository courierRepository;

    @Mock
    private OrderRepository orderRepository;

    private MoveCourierCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MoveCourierCommandHandlerImpl(courierRepository, orderRepository);
    }

    @Test
    void handleShouldMoveAndSaveCouriersWhenAssignedOrdersExist() {
        // Arrange

        var courier1 = Courier.create(
                "Иван",
                Speed.create(2).getValueOrThrow(),
                Location.create(1, 1).getValueOrThrow()
        ).getValueOrThrow();
        var courier2 = Courier.create(
                "Петр", Speed.create(2).getValueOrThrow(),
                Location.create(2, 2).getValueOrThrow()
        ).getValueOrThrow();

        var order1 = Order.create(
                UUID.randomUUID(),
                Location.create(5, 5).getValueOrThrow(),
                Volume.create(10, 1, 1).getValueOrThrow()
        ).getValueOrThrow();
        var order2 = Order.create(
                UUID.randomUUID(),
                Location.create(8, 8).getValueOrThrow(),
                Volume.create(10, 1, 1).getValueOrThrow()
        ).getValueOrThrow();

        courier1.takeOrder(order1.getId(), Volume.create(10,1,1).getValueOrThrow());
        courier2.takeOrder(order2.getId(), Volume.create(10,1,1).getValueOrThrow());

        order1.assign(courier1);
        order2.assign(courier2);

        when(orderRepository.findAllAssignedOrders()).thenReturn(List.of(order1, order2));
        when(courierRepository.findCourierById(courier1.getId())).thenReturn(Optional.of(courier1));
        when(courierRepository.findCourierById(courier2.getId())).thenReturn(Optional.of(courier2));

        // Act
        UnitResult<Error> result = handler.handle();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        verify(courierRepository, times(2)).saveCourier(any(Courier.class));
        verify(orderRepository, never()).saveOrder(any());
    }

    @Test
    void handleShouldCompleteOrdersAndSaveBothWhenCourierInTargetLocation() {
        // Arrange

        var targetLocation = Location.create(5, 5).getValueOrThrow();
        var courier =  Courier.create(
                "Иван",
                Speed.create(2).getValueOrThrow(),
                targetLocation
        ).getValueOrThrow();
        var order = Order.create(
                UUID.randomUUID(),
                targetLocation,
                Volume.create(5, 1, 1).getValueOrThrow()
        ).getValueOrThrow();

        courier.takeOrder(order.getId(), Volume.create(5, 1, 1).getValueOrThrow());
        order.assign(courier);

        when(orderRepository.findAllAssignedOrders()).thenReturn(List.of(order));
        when(courierRepository.findCourierById(courier.getId())).thenReturn(Optional.of(courier));

        // Act
        UnitResult<Error> result = handler.handle();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        verify(orderRepository).saveOrder(order);
        verify(courierRepository).saveCourier(courier);
    }

    @Test
    void handleShouldNotSaveWhenMoveFails() {
        // Arrange
        var courier =  Courier.create(
                "Иван",
                Speed.create(2).getValueOrThrow(),
                Location.create(5, 5).getValueOrThrow()
        ).getValueOrThrow();
        var order = Order.create(
                UUID.randomUUID(),
                Location.create(6, 6).getValueOrThrow(),
                Volume.create(5, 1, 1).getValueOrThrow()
        ).getValueOrThrow();

        courier.takeOrder(order.getId(), Volume.create(5, 1, 1).getValueOrThrow());
        order.assign(courier);

        var courierSpy = spy(courier);
        doReturn(UnitResult.failure(Error.of("move.error", "Ошибка")))
                .when(courierSpy).move(any());

        when(orderRepository.findAllAssignedOrders()).thenReturn(List.of(order));
        when(courierRepository.findCourierById(courier.getId())).thenReturn(Optional.of(courierSpy));

        // Act
        UnitResult<Error> result = handler.handle();

        // Assert
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("move_courier_command_handler.failed_partially");
        verify(courierRepository, never()).saveCourier(any());
        verify(orderRepository, never()).saveOrder(any());
    }

    @Test
    void handleShouldReturnSuccessWhenNoAssignedOrders() {
        // Arrange

        when(orderRepository.findAllAssignedOrders()).thenReturn(List.of());

        // Act
        UnitResult<Error> result = handler.handle();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        verify(courierRepository, never()).findCourierById(any());
        verify(courierRepository, never()).saveCourier(any());
        verify(orderRepository, never()).saveOrder(any());
    }

    @Test
    void handleShouldCollectErrorsWhenCourierNotFound() {
        // Arrange

        var courier =  Courier.create(
                "Иван",
                Speed.create(2).getValueOrThrow(),
                Location.create(5, 5).getValueOrThrow()
        ).getValueOrThrow();
        var order = Order.create(
                UUID.randomUUID(),
                Location.create(5, 5).getValueOrThrow(),
                Volume.create(10, 1, 1).getValueOrThrow()
        ).getValueOrThrow();
        order.assign(courier);

        when(orderRepository.findAllAssignedOrders()).thenReturn(List.of(order));
        when(courierRepository.findCourierById(courier.getId())).thenReturn(Optional.empty());
        // Act
        UnitResult<Error> result = handler.handle();

        // Assert
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("move_courier_command_handler.failed_partially");
        verify(courierRepository, never()).saveCourier(any());
        verify(orderRepository, never()).saveOrder(any());
    }
}