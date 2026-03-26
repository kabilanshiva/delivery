package microarch.delivery.core.domain.model.courier;

import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.domain.model.kernel.Volume;
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
        assertThat(StoragePlace.class.getSuperclass().getSimpleName()).isEqualTo("BaseEntity");
    }

    @Test
    @DisplayName("Успешное создание склада с корректными данными")
    void creationSucceedsWithValidData() {
        var volume = Volume.create(10, 5, 2).getValue();
        var result = StoragePlace.create("Warehouse-A", volume);

        assertThat(result.isSuccess()).isTrue();
        var place = result.getValue();
        assertThat(place.getName()).isEqualTo("Warehouse-A");
        assertThat(place.getTotalVolume()).isEqualTo(volume);
        assertThat(place.getOrderId()).isNull();
        assertThat(place.getId()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({ "   , 10, 5, 2", ", 10, 5, 2", })
    @DisplayName("Ошибка создания при невалидном имени")
    void creationFailsOnInvalidName(String name, int x, int y, int z) {
        var volume = Volume.create(x, y, z).getValue();
        Result<StoragePlace, Error> result = StoragePlace.create(name, volume);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
    }

    @Test
    @DisplayName("Можно разместить заказ, если место свободно и объем подходит")
    void canStoreReturnsTrueWhenFreeAndVolumeFits() {
        var place = safeCreate("Zone-1", 10, 5, 2);
        var volume = Volume.create(4, 3, 2).getValue(); // fits

        var result = place.canStore(volume);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isTrue();
    }

    @Test
    @DisplayName("Нельзя разместить заказ, если место уже занято")
    void canStoreReturnsFalseWhenOccupied() {
        var place = safeCreate("Zone-2", 10, 5, 2);
        var orderId = UUID.randomUUID();
        var volume = Volume.create(4, 3, 2).getValue();
        place.store(orderId, volume);

        var requestVolume = Volume.create(2, 2, 2).getValue();

        var result = place.canStore(requestVolume);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("storage_place.occupied");
    }

    @Test
    @DisplayName("Успешное размещение заказа изменяет статус на занятый")
    void storeSuccessfullyAssignsOrderId() {
        var place = safeCreate("Zone-4", 10, 10, 10);
        var orderId = UUID.randomUUID();
        var volume = Volume.create(5, 5, 5).getValue();

        var result = place.store(orderId, volume);

        assertThat(result.isSuccess()).isTrue();
        assertThat(place.getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Ошибка размещения при передаче null в качестве orderId")
    void storeFailsWhenOrderIdIsNull() {
        var place = safeCreate("Zone-5", 10, 10, 10);
        var volume = Volume.create(5, 5, 5).getValue();

        var result = place.store(null, volume);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getMessage()).containsIgnoringCase("required");
    }

    @ParameterizedTest
    @CsvSource({ "10, 10, 10, 10, 10, 10", "10, 10, 10, 1, 1, 1", "10, 10, 10, 9, 9, 9" })
    @DisplayName("Размещение заказов различных объемов вплоть до лимита")
    void storeHandlesVariousVolumesUpToLimit(int totalX, int totalY, int totalZ, int orderX, int orderY, int orderZ) {
        var place = safeCreate("Zone-Boundary", totalX, totalY, totalZ);
        var orderId = UUID.randomUUID();
        var volume = Volume.create(orderX, orderY, orderZ).getValue();

        var result = place.store(orderId, volume);

        assertThat(result.isSuccess()).isTrue();
        assertThat(place.getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Успешная очистка места освобождает orderId")
    void clearSuccessfullyResetsOrderId() {
        var place = safeCreate("Zone-Clear", 10, 10, 10);
        var orderId = UUID.randomUUID();
        var volume = Volume.create(5, 5, 5).getValue();
        place.store(orderId, volume);

        var result = place.clear(orderId);

        assertThat(result.isSuccess()).isTrue();
        assertThat(place.getOrderId()).isNull();
    }

    @Test
    @DisplayName("Ошибка очистки, если место уже пусто")
    void clearFailsWhenPlaceIsEmpty() {
        var place = safeCreate("Zone-Empty", 10, 10, 10);
        var fakeOrderId = UUID.randomUUID();

        var result = place.clear(fakeOrderId);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("storage_place.empty");
    }

    @Test
    @DisplayName("Ошибка очистки, если передан ID другого заказа")
    void clearFailsWhenOrderIdDoesNotMatch() {
        var place = safeCreate("Zone-Mismatch", 10, 10, 10);
        var realOrderId = UUID.randomUUID();
        var wrongOrderId = UUID.randomUUID();
        var volume = Volume.create(5, 5, 5).getValue();

        place.store(realOrderId, volume);

        var result = place.clear(wrongOrderId);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("storage_place.different_order_stored");
        assertThat(result.getError().getMessage()).contains(wrongOrderId.toString());
    }

    // Helper method
    private StoragePlace safeCreate(String name, int x, int y, int z) {
        var volumeResult = Volume.create(x, y, z);
        if (volumeResult.isFailure()) {
            throw new IllegalStateException("Не удалось создать Volume для теста: " + volumeResult.getError());
        }
        var placeResult = StoragePlace.create(name, volumeResult.getValue());
        if (placeResult.isFailure()) {
            throw new IllegalStateException("Не удалось создать StoragePlace для теста: " + placeResult.getError());
        }
        return placeResult.getValue();
    }
}