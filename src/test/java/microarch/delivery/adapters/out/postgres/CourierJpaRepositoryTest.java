package microarch.delivery.adapters.out.postgres;

import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Speed;
import microarch.delivery.core.domain.model.kernel.Volume;
import microarch.delivery.core.ports.CourierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CourierJpaRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    CourierRepository courierRepository;

    @Autowired
    CourierJpaRepository courierJpaRepository;

    @BeforeEach
    void cleanUp() {
        courierJpaRepository.deleteAll();
    }

    private Courier createCourier(String name, Speed speed, Location location) {
        return Courier.create(name, speed, location).getValueOrThrow();
    }

    private Location createLocation(int x, int y) {
        return Location.create(x, y).getValueOrThrow();
    }

    private Speed createSpeed(int value) {
        return Speed.create(value).getValueOrThrow();
    }

    @Test
    @DisplayName("Сохранение курьера в БД и возврат сохранённого объекта")
    void saveCourier_SavesCourierAndReturnsSavedObject() {
        // Arrange
        Courier courier = createCourier("Courier-1", createSpeed(10), createLocation(1, 1));

        // Act
        Courier savedCourier = courierRepository.saveCourier(courier);

        // Assert
        assertThat(savedCourier).isNotNull();
        assertThat(savedCourier.getId()).isNotNull();
        assertThat(savedCourier.getName()).isEqualTo("Courier-1");
        assertThat(savedCourier.getSpeed().getValue()).isEqualTo(10);
        assertThat(savedCourier.getLocation().getX()).isEqualTo(1);
        assertThat(savedCourier.getLocation().getY()).isEqualTo(1);
        assertThat(savedCourier.getStoragePlaces().size()).isEqualTo(1);
        assertThat(savedCourier.getStoragePlaces().get(0).getName()).isEqualTo("Backpack");
        assertThat(savedCourier.getStoragePlaces().get(0).getTotalVolume().getX()).isEqualTo(5);
    }

    @Test
    @DisplayName("Поиск курьера по ID — успешный поиск")
    void findCourierById_FindsCourierById() {
        // Arrange
        Courier courier = createCourier("Courier-1", createSpeed(10), createLocation(1, 1));
        Courier savedCourier = courierRepository.saveCourier(courier);

        // Act
        Optional<Courier> foundCourier = courierRepository.findCourierById(savedCourier.getId());

        // Assert
        assertThat(foundCourier).isPresent();
        assertThat(foundCourier.get()).isEqualTo(savedCourier);
    }

    @Test
    @DisplayName("Поиск курьера по несуществующему ID — возвращает пустой Optional")
    void findCourierById_ReturnsEmptyForNonExistentId() {
        // Act
        Optional<Courier> foundCourier = courierRepository.findCourierById(UUID.randomUUID());

        // Assert
        assertThat(foundCourier).isEmpty();
    }

    @Test
    @DisplayName("Поиск доступных курьеров — возвращает доступных")
    void findAvailableCouriers_ReturnsAvailableCouriers() {
        // Arrange
        Courier availableCourier = createCourier("AvailableCourier", createSpeed(10), createLocation(1, 1));
        courierRepository.saveCourier(availableCourier);

        Courier busyCourier = createCourier("BusyCourier", createSpeed(5), createLocation(2, 2));
        busyCourier.takeOrder(UUID.randomUUID(), Volume.create(1, 1, 1).getValueOrThrow());
        courierRepository.saveCourier(busyCourier);

        // Act
        List<Courier> availableCouriers = courierRepository.findAvailableCouriers();

        // Assert
        assertThat(availableCouriers.size()).isEqualTo(1);
        assertThat(availableCouriers.get(0).getName()).isEqualTo("AvailableCourier");
    }

    @Test
    @DisplayName("Поиск доступных курьеров — возвращает пустой список, если нет доступных")
    void findAvailableCouriers_ReturnsEmptyIfNoAvailableCouriers() {
        // Arrange
        Courier courier = createCourier("Courier-1", createSpeed(10), createLocation(1, 1));
        courier.addStoragePlace("Backpack", Volume.create(1, 1, 1).getValueOrThrow());
        courier.takeOrder(UUID.randomUUID(), Volume.create(1, 1, 1).getValueOrThrow());
        courierRepository.saveCourier(courier);

        // Act
        List<Courier> availableCouriers = courierRepository.findAvailableCouriers();

        // Assert
        assertThat(availableCouriers.isEmpty()).isTrue();
    }
}