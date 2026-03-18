package microarch.delivery.core.application.commands;

import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.ports.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderCommandHandlerTest {

    @Mock
    private OrderRepository orderRepository;

    private CreateOrderCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateOrderCommandHandlerImpl(orderRepository);
    }

    @Test
    @DisplayName("Успешное создание нового заказа — возвращаем ID нового заказа")
    void handleShouldBeSuccessWhenOrderDoesNotExistAndCreatedSuccessfully() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        String country = "Россия";
        String city = "Москва";
        String street = "Тверская";
        String house = "10";
        String apartment = "10";
        int volumeX = 5;
        int volumeY = 2;
        int volumeZ = 1;

        var command = CreateOrderCommand.create(orderId, country, city, street, house, apartment, volumeX, volumeY, volumeZ);

        when(orderRepository.findOrderById(orderId)).thenReturn(Optional.empty());

        // Act
        Result<UUID, Error> result = handler.handle(command.getValueOrThrow());

        // Assert
        assertThat(result.isSuccess()).isTrue();
        verify(orderRepository).saveOrder(any(Order.class));
    }

    @Test
    @DisplayName("Успешное возвращение существующего заказа — не создаём и не сохраняем")
    void handleShouldReturnExistingOrderIdWhenOrderAlreadyExists() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        String country = "Россия";
        String city = "Москва";
        String street = "Тверская";
        String house = "10";
        String apartment = "10";
        int volumeX = 5;
        int volumeY = 2;
        int volumeZ = 1;

        var command = CreateOrderCommand.create(orderId, country, city, street, house, apartment, volumeX, volumeY, volumeZ);

        var orderFromDb = Order.create(
                orderId,
                Location.create(3, 4).getValueOrThrow(),
                Volume.create(volumeX, volumeY, volumeZ).getValueOrThrow()
        ).getValueOrThrow();

        when(orderRepository.findOrderById(orderId)).thenReturn(Optional.of(orderFromDb));

        // Act
        Result<UUID, Error> result = handler.handle(command.getValueOrThrow());

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(orderId);
        verify(orderRepository, never()).saveOrder(any(Order.class));
    }

    @Test
    @DisplayName("Успешное возвращение существующего заказа — не создаём и не сохраняем")
    void handleShouldReturnFailureWhenCommandIsInvalid() {
        // Arrange
        var commandResult = CreateOrderCommand.create(
                null,
                "Россия",
                "Москва",
                "Тверская",
                "10",
                "25",
                5,
                5,
                5
        );

        // Assert
        assertThat(commandResult.isFailure()).isTrue();
    }

    @Test
    void handleShouldReturnFailureWhenVolumeIsInvalid() {
        // Arrange
        var commandResult = CreateOrderCommand.create(
                UUID.randomUUID(),
                "Россия",
                "Москва",
                "Тверская",
                "10",
                "25",
                0,
                5,
                5
        );

        // Assert
        assertThat(commandResult.isFailure()).isTrue();
    }
}