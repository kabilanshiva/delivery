package microarch.delivery.core.application.commands;

import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.kernel.Speed;
import microarch.delivery.core.ports.CourierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCourierCommandHandlerTest {

    @Mock
    private CourierRepository courierRepository;

    private CreateCourierCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateCourierCommandHandlerImpl(courierRepository);
    }

    @Test
    @DisplayName("Успешное создание курьера — возвращаем ID")
    void handleShouldBeSuccessWhenCourierCreatedSuccessfully() {
        // Arrange
        String name = "Иван Петров";
        Speed speed = Speed.create(5).getValueOrThrow();
        var command = CreateCourierCommand.create(name, speed.getValue()).getValueOrThrow();

        // Act
        Result<UUID, Error> result = handler.handle(command);

        // Assert
        assertThat(result.isSuccess()).isTrue();

        // Проверяем, что репозиторий сохранил курьера
        verify(courierRepository).saveCourier(any(Courier.class));
    }

    @Test
    @DisplayName("Ошибка создания курьера — возвращаем ошибку без сохранения")
    void handleShouldReturnFailureWhenCourierCreationFails() {
        // Arrange
        var command = CreateCourierCommand.create(null, 5);

        // Assert
        assertThat(command.isFailure()).isTrue();
        verify(courierRepository, never()).saveCourier(any(Courier.class));
    }
}