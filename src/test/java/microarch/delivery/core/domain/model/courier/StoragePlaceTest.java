package microarch.delivery.core.domain.model.courier;

import libs.errs.Error;
import libs.errs.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StoragePlaceTest {

    @Test
    @DisplayName("Проверка наследования от BaseEntity")
    void verifyInheritanceFromBaseEntity() {
        // Assert
        assertThat(StoragePlace.class.getSuperclass().getSimpleName())
                .isEqualTo("BaseEntity");
    }

    @Test
    @DisplayName("Успешное создание склада с корректными данными")
    void creationSucceedsWithValidData() {
        // Act
        var result = StoragePlace.create("Warehouse-A", 100);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        var place = result.getValue();
        assertThat(place.getName()).isEqualTo("Warehouse-A");
        assertThat(place.getTotalVolume()).isEqualTo(100);
        assertThat(place.getOrderId()).isNull();
        assertThat(place.getId()).isNotNull(); // UUID должен быть сгенерирован
    }

    @ParameterizedTest
    @CsvSource({
            "   , 50",      // Имя содержит только пробелы
            ", 50",         // Пустая строка
            "Box, 0",       // Объем равен 0
            "Box, -10"      // Отрицательный объем
    })
    @DisplayName("Ошибка создания при невалидном имени или объеме")
    void creationFailsOnInvalidArguments(String name, int volume) {
        // Act
        Result<StoragePlace, Error> result = StoragePlace.create(name, volume);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isNotNull();
    }

    @Test
    @DisplayName("Можно разместить заказ, если место свободно и объем подходит")
    void canStoreReturnsTrueWhenFreeAndVolumeFits() {
        // Arrange
        var place = safeCreate("Zone-1", 50);
        int requestVolume = 40;

        // Act
        var result = place.canStore(requestVolume);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isTrue();
    }

    @Test
    @DisplayName("Нельзя разместить заказ, если место уже занято")
    void canStoreReturnsFalseWhenOccupied() {
        // Arrange
        var place = safeCreate("Zone-2", 50);
        place.store(UUID.randomUUID(), 20); // Занимаем место
        int requestVolume = 10;

        // Act
        var result = place.canStore(requestVolume);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("storage_place.occupied");
    }

    @Test
    @DisplayName("Нельзя разместить заказ, если запрошенный объем больше доступного")
    void canStoreReturnsFalseWhenVolumeExceedsCapacity() {
        // Arrange
        var place = safeCreate("Zone-3", 20);
        int requestVolume = 25;

        // Act
        var result = place.canStore(requestVolume);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("storage_place.exceed_total_volume");
        assertThat(result.getError().getMessage()).contains("20").contains("25");
    }

    @Test
    @DisplayName("Успешное размещение заказа изменяет статус на занятый")
    void storeSuccessfullyAssignsOrderId() {
        // Arrange
        var place = safeCreate("Zone-4", 100);
        var orderId = UUID.randomUUID();
        int volume = 50;

        // Act
        var result = place.store(orderId, volume);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(place.getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Ошибка размещения при передаче null в качестве orderId")
    void storeFailsWhenOrderIdIsNull() {
        // Arrange
        var place = safeCreate("Zone-5", 100);

        // Act
        var result = place.store(null, 50);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).containsIgnoringCase("required");
    }

    @ParameterizedTest
    @CsvSource({
            "100, 100", // Ровно по границе
            "100, 1",   // Минимальный занимаемый объем
            "100, 99"   // Почти полный объем
    })
    @DisplayName("Размещение заказов различных объемов вплоть до лимита")
    void storeHandlesVariousVolumesUpToLimit(int totalVol, int orderVol) {
        // Arrange
        var place = safeCreate("Zone-Boundary", totalVol);
        var orderId = UUID.randomUUID();

        // Act
        var result = place.store(orderId, orderVol);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(place.getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Успешная очистка места освобождает orderId")
    void clearSuccessfullyResetsOrderId() {
        // Arrange
        var place = safeCreate("Zone-Clear", 50);
        var orderId = UUID.randomUUID();
        place.store(orderId, 20);

        // Act
        var result = place.clear(orderId);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(place.getOrderId()).isNull();
    }

    @Test
    @DisplayName("Ошибка очистки, если место уже пусто")
    void clearFailsWhenPlaceIsEmpty() {
        // Arrange
        var place = safeCreate("Zone-Empty", 50);
        var fakeOrderId = UUID.randomUUID();

        // Act
        var result = place.clear(fakeOrderId);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("storage_place.empty");
    }

    @Test
    @DisplayName("Ошибка очистки, если передан ID другого заказа")
    void clearFailsWhenOrderIdDoesNotMatch() {
        // Arrange
        var place = safeCreate("Zone-Mismatch", 50);
        var realOrderId = UUID.randomUUID();
        var wrongOrderId = UUID.randomUUID();

        place.store(realOrderId, 10);

        // Act
        var result = place.clear(wrongOrderId);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("storage_place.different_order_stored");
        assertThat(result.getError().getMessage()).contains(wrongOrderId.toString());
    }

    private StoragePlace safeCreate(String name, int volume) {
        var result = StoragePlace.create(name, volume);
        if (result.isFailure()) {
            throw new IllegalStateException("Не удалось создать StoragePlace для теста: " + result.getError());
        }
        return result.getValue();
    }
}
