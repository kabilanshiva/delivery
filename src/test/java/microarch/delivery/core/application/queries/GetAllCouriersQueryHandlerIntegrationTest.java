package microarch.delivery.core.application.queries;

import microarch.delivery.adapters.out.postgres.CourierJpaRepository;
import microarch.delivery.adapters.out.postgres.AbstractPostgresIntegrationTest;
import microarch.delivery.core.application.queries.dto.CourierDto;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Speed;
import microarch.delivery.core.ports.CourierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GetAllCouriersQueryHandlerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private GetAllCouriersQueryHandler handler;


    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private CourierJpaRepository jpaRepository;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldReturnAllCouriers() {
        // Arrange
        var courier1 = Courier.create(
                "Иван Петров",
                Speed.create(2).getValueOrThrow(),
                Location.create(3, 4).getValueOrThrow()
        ).getValueOrThrow();

        var courier2 = Courier.create(
                "Петр Иванов",
                Speed.create(1).getValueOrThrow(),
                Location.create(7, 8).getValueOrThrow()
        ).getValueOrThrow();

        courierRepository.saveCourier(courier1);
        courierRepository.saveCourier(courier2);

        // Act
        var result = handler.handle();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        var response = result.getValue();

        assertThat(response.couriers()).hasSize(2);

        // Проверяем первого курьера
        CourierDto dto1 = findDtoById(response.couriers(), courier1.getId());
        assertThat(dto1).isNotNull();
        assertThat(dto1.name()).isEqualTo("Иван Петров");
        assertThat(dto1.location()).isEqualTo(Location.create(3, 4).getValueOrThrow());

        // Проверяем второго курьера
        CourierDto dto2 = findDtoById(response.couriers(), courier2.getId());
        assertThat(dto2).isNotNull();
        assertThat(dto2.name()).isEqualTo("Петр Иванов");
        assertThat(dto2.location()).isEqualTo(Location.create(7, 8).getValueOrThrow());
    }

    @Test
    void shouldReturnEmptyListWhenNoCouriers() {
        // Act
        var result = handler.handle();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        GetAllCouriersResponse response = result.getValue();
        assertThat(response.couriers()).isEmpty();
    }

    private CourierDto findDtoById(List<CourierDto> dtos, java.util.UUID id) {
        return dtos.stream()
                .filter(dto -> dto.id().equals(id))
                .findFirst()
                .orElse(null);
    }
}