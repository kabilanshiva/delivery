package microarch.delivery.core.domain.model.order;

import libs.errs.Error;
import libs.errs.Result;
import libs.errs.UnitResult;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.courier.Speed;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Volume;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    private static final UUID VALID_ID = UUID.randomUUID();
    private static final Location VALID_LOCATION = Location.create(1, 1).getValueOrThrow();
    private static final Volume VALID_VOLUME = Volume.create(1, 1, 1).getValueOrThrow();

    @Test
    @DisplayName("Успешное создание Order с корректными данными")
    void creationSucceedsWithValidData() {
        Result<Order, Error> result = Order.create(VALID_ID, VALID_LOCATION, VALID_VOLUME);

        assertThat(result.isSuccess()).isTrue();
        Order order = result.getValue();
        assertThat(order.getId()).isEqualTo(VALID_ID);
        assertThat(order.getLocation()).isEqualTo(VALID_LOCATION);
        assertThat(order.getVolume()).isEqualTo(VALID_VOLUME);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getCourierId()).isNull();
    }

    @Test
    @DisplayName("Ошибка создания Order при null id")
    void creationFailsWhenIdIsNull() {
        Result<Order, Error> result = Order.create(null, VALID_LOCATION, VALID_VOLUME);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
        assertThat(result.getError().getMessage()).contains("id");
    }

    @Test
    @DisplayName("Ошибка создания Order при null location")
    void creationFailsWhenLocationIsNull() {
        Result<Order, Error> result = Order.create(VALID_ID, null, VALID_VOLUME);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
        assertThat(result.getError().getMessage()).contains("location");
    }

    @Test
    @DisplayName("Ошибка создания Order при null volume")
    void creationFailsWhenVolumeIsNull() {
        Result<Order, Error> result = Order.create(VALID_ID, VALID_LOCATION, null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
        assertThat(result.getError().getMessage()).contains("volume");
    }

    @Test
    @DisplayName("Заказ назначается курьеру успешно")
    void assignSucceedsWhenCourierIsValid() {
        Order order = Order.create(VALID_ID, VALID_LOCATION, VALID_VOLUME).getValueOrThrow();
        Courier courier = Courier.create("Courier-1", Speed.create(10).getValueOrThrow(), VALID_LOCATION).getValueOrThrow();

        UnitResult<Error> result = order.assign(courier);

        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getCourierId()).isEqualTo(courier.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
    }

    @Test
    @DisplayName("Ошибка назначения заказа при null курьере")
    void assignFailsWhenCourierIsNull() {
        Order order = Order.create(VALID_ID, VALID_LOCATION, VALID_VOLUME).getValueOrThrow();

        UnitResult<Error> result = order.assign(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.is.required");
        assertThat(result.getError().getMessage()).contains("Courier");
    }

    @Test
    @DisplayName("Заказ завершается успешно, если назначен")
    void completeSucceedsWhenAssigned() {
        Order order = Order.create(VALID_ID, VALID_LOCATION, VALID_VOLUME).getValueOrThrow();
        Courier courier = Courier.create("Courier-1", Speed.create(10).getValueOrThrow(), VALID_LOCATION).getValueOrThrow();
        order.assign(courier);

        UnitResult<Error> result = order.complete();

        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("Ошибка завершения заказа, если он не назначен")
    void completeFailsWhenNotAssigned() {
        Order order = Order.create(VALID_ID, VALID_LOCATION, VALID_VOLUME).getValueOrThrow();

        UnitResult<Error> result = order.complete();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order.not_assigned");
        assertThat(result.getError().getMessage()).contains(VALID_ID.toString());
    }

    @Test
    @DisplayName("Ошибка завершения заказа, если он уже завершен")
    void completeFailsWhenAlreadyCompleted() {
        Order order = Order.create(VALID_ID, VALID_LOCATION, VALID_VOLUME).getValueOrThrow();
        Courier courier = Courier.create("Courier-1", Speed.create(10).getValueOrThrow(), VALID_LOCATION).getValueOrThrow();
        order.assign(courier);
        order.complete();

        UnitResult<Error> result = order.complete();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order.already_completed");
        assertThat(result.getError().getMessage()).contains(VALID_ID.toString());
    }
}