package microarch.delivery.core.domain.model.courier;

import libs.errs.Error;
import libs.errs.Result;
import libs.errs.UnitResult;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Volume;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CourierTest {

    private static final Location DEFAULT_LOCATION = Location.create(1, 1).getValueOrThrow();
    private static final Speed DEFAULT_SPEED = Speed.create(10).getValueOrThrow();

    @Test
    @DisplayName("Успешное создание Courier с корректными данными")
    void creationSucceedsWithValidData() {
        var name = "Courier-1";
        var speed = DEFAULT_SPEED;
        var location = DEFAULT_LOCATION;

        Result<Courier, Error> result = Courier.create(name, speed, location);

        assertThat(result.isSuccess()).isTrue();
        Courier courier = result.getValue();
        assertThat(courier.getName()).isEqualTo(name);
        assertThat(courier.getSpeed()).isEqualTo(speed);
        assertThat(courier.getLocation()).isEqualTo(location);
        assertThat(courier.getStoragePlaces()).hasSize(1);
        assertThat(courier.getStoragePlaces().get(0).getName()).isEqualTo("Backpack");
    }

    @ParameterizedTest
    @DisplayName("Ошибка создания Courier при пустом имени")
    @MethodSource("provideInvalidNames")
    void creationFailsWhenNameIsBlank(String name) {
        Result<Courier, Error> result = Courier.create(name, DEFAULT_SPEED, DEFAULT_LOCATION);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
        assertThat(result.getError().getMessage()).contains("name");
    }

    static Stream<Arguments> provideInvalidNames() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("   ")
        );
    }

    @Test
    @DisplayName("Ошибка создания Courier при null speed")
    void creationFailsWhenSpeedIsNull() {
        Result<Courier, Error> result = Courier.create("Courier-1", null, DEFAULT_LOCATION);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
        assertThat(result.getError().getMessage()).contains("speed");
    }

    @Test
    @DisplayName("Ошибка создания Courier при null location")
    void creationFailsWhenLocationIsNull() {
        Result<Courier, Error> result = Courier.create("Courier-1", DEFAULT_SPEED, null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
        assertThat(result.getError().getMessage()).contains("location");
    }

    @Test
    @DisplayName("Добавление нового места хранения успешно")
    void addStoragePlaceSucceedsWithValidVolume() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();
        Volume volume = Volume.create(3, 2, 1).getValueOrThrow();

        UnitResult<Error> result = courier.addStoragePlace("Box", volume);

        assertThat(result.isSuccess()).isTrue();
        assertThat(courier.getStoragePlaces()).hasSize(2);
        assertThat(courier.getStoragePlaces().get(1).getName()).isEqualTo("Box");
        assertThat(courier.getStoragePlaces().get(1).getTotalVolume()).isEqualTo(volume);
    }

    @Test
    @DisplayName("Ошибка добавления места хранения при пустом имени")
    void addStoragePlaceFailsWhenNameIsEmpty() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();

        UnitResult<Error> result = courier.addStoragePlace("", Volume.create(3, 2, 1).getValueOrThrow());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
    }

    @Test
    @DisplayName("Проверка, что заказ может быть взят, если есть подходящее место")
    void canTakeOrderReturnsSuccessWhenStorageAvailable() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();
        Volume orderVolume = Volume.create(4, 1, 1).getValueOrThrow(); // fits in Backpack (5x2x1)

        UnitResult<Error> result = courier.canTakeOrder(orderVolume);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Проверка, что заказ не может быть взят, если нет подходящего места")
    void canTakeOrderReturnsFailureWhenNoSuitableStorage() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();
        Volume orderVolume = Volume.create(10, 10, 10).getValueOrThrow(); // too big

        UnitResult<Error> result = courier.canTakeOrder(orderVolume);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.no_suitable_storage");
    }

    @Test
    @DisplayName("Заказ берется в самое маленькое подходящее место")
    void takeOrderUsesSmallestSuitableStorage() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();
        Volume smallVolume = Volume.create(2, 1, 1).getValueOrThrow();
        Volume largeVolume = Volume.create(5, 2, 1).getValueOrThrow();

        courier.addStoragePlace("Small", smallVolume);
        courier.addStoragePlace("Large", largeVolume);

        UUID orderId = UUID.randomUUID();
        Volume orderVolume = Volume.create(3, 1, 1).getValueOrThrow();

        UnitResult<Error> result = courier.takeOrder(orderId, orderVolume);

        assertThat(result.isSuccess()).isTrue();
        assertThat(courier.getStoragePlaces().get(0).getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Ошибка взятия заказа при null orderId")
    void takeOrderFailsWhenOrderIdIsNull() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();
        Volume volume = Volume.create(1, 1, 1).getValueOrThrow();

        UnitResult<Error> result = courier.takeOrder(null, volume);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
    }

    @Test
    @DisplayName("Ошибка взятия заказа при null объеме")
    void takeOrderFailsWhenVolumeIsNull() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();

        UnitResult<Error> result = courier.takeOrder(UUID.randomUUID(), null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
    }

    @Test
    @DisplayName("Заказ завершается успешно, если привязан к курьеру")
    void completeOrderSucceedsWhenOrderExists() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();
        UUID orderId = UUID.randomUUID();
        Volume volume = Volume.create(2, 1, 1).getValueOrThrow();

        courier.takeOrder(orderId, volume);

        UnitResult<Error> result = courier.completeOrder(orderId);

        assertThat(result.isSuccess()).isTrue();
        assertThat(courier.getStoragePlaces().get(0).getOrderId()).isNull();
    }

    @Test
    @DisplayName("Ошибка завершения заказа, если заказ не найден")
    void completeOrderFailsWhenOrderNotFound() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();
        UUID wrongOrderId = UUID.randomUUID();

        UnitResult<Error> result = courier.completeOrder(wrongOrderId);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.order_not_available");
        assertThat(result.getError().getMessage()).contains(wrongOrderId.toString());
    }

    @Test
    @DisplayName("Ошибка завершения заказа при null orderId")
    void completeOrderFailsWhenOrderIdIsNull() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();

        UnitResult<Error> result = courier.completeOrder(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
    }

    @Test
    @DisplayName("Расчет времени до локации успешно, если расстояние делится на скорость")
    void calculateTimeToLocationSucceedsWithExactDivision() {
        Courier courier = Courier.create(
                "Courier-1",
                Speed.create(5).getValueOrThrow(),
                DEFAULT_LOCATION
        ).getValueOrThrow();
        Location target = Location.create(10, 1).getValueOrThrow();

        Result<Integer, Error> result = courier.calculateTimeToLocation(target);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("Ошибка расчета времени при null location")
    void calculateTimeToLocationFailsWhenLocationIsNull() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();

        Result<Integer, Error> result = courier.calculateTimeToLocation(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
    }

    @Test
    @DisplayName("Курьер перемещается в пределах диапазона")
    void moveWithinRangeSucceeds() {
        Courier courier = Courier.create(
                "Courier-1",
                Speed.create(3).getValueOrThrow(),
                DEFAULT_LOCATION
        ).getValueOrThrow();
        Location target = Location.create(2, 1).getValueOrThrow();

        UnitResult<Error> result = courier.move(target);

        assertThat(result.isSuccess()).isTrue();
        assertThat(courier.getLocation().getX()).isEqualTo(2);
        assertThat(courier.getLocation().getY()).isEqualTo(1);
    }

    @Test
    @DisplayName("Курьер не может переместиться дальше, чем его скорость")
    void moveRespectsSpeedLimit() {
        Courier courier = Courier.create(
                "Courier-1",
                Speed.create(2).getValueOrThrow(),
                DEFAULT_LOCATION
        ).getValueOrThrow();
        Location target = Location.create(5, 5).getValueOrThrow();

        UnitResult<Error> result = courier.move(target);

        assertThat(result.isSuccess()).isTrue();
        assertThat(courier.getLocation().getX()).isEqualTo(3);
        assertThat(courier.getLocation().getY()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ошибка перемещения при null target")
    void moveFailsWhenTargetIsNull() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();

        UnitResult<Error> result = courier.move(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
    }

    @Test
    @DisplayName("isAvailable возвращает success, если ни одно место хранения не занято")
    void isAvailableReturnsSuccessWhenNoStorageIsOccupied() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();

        UnitResult<Error> result = courier.isAvailable();

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("isAvailable возвращает failure, если хотя бы одно место занято")
    void isAvailableReturnsFailureWhenAtLeastOneStorageIsOccupied() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();
        Volume orderVolume = Volume.create(2, 1, 1).getValueOrThrow();
        UUID orderId = UUID.randomUUID();

        courier.takeOrder(orderId, orderVolume);

        UnitResult<Error> result = courier.isAvailable();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.already_has_an_order");
    }

    @Test
    @DisplayName("isAvailable возвращает failure, если все места хранения заняты")
    void isAvailableReturnsFailureWhenAllStoragesAreOccupied() {
        Courier courier = Courier.create("Courier-1", DEFAULT_SPEED, DEFAULT_LOCATION).getValueOrThrow();
        Volume smallVolume = Volume.create(2, 1, 1).getValueOrThrow();
        Volume largeVolume = Volume.create(5, 2, 1).getValueOrThrow();
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();

        courier.addStoragePlace("Small", smallVolume);
        courier.takeOrder(orderId1, smallVolume);
        courier.takeOrder(orderId2, largeVolume);

        UnitResult<Error> result = courier.isAvailable();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("courier.already_has_an_order");
    }
}