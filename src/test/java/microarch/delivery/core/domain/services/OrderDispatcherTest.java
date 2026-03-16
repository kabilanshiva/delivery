package microarch.delivery.core.domain.services;

import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.courier.Speed;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.OrderStatus;
import libs.errs.Error;
import libs.errs.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderDispatcherTest {

    private final OrderDispatcher dispatcher = new OrderDispatcherImpl();

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final Volume ORDER_VOLUME = Volume.create(3, 2, 1).getValueOrThrow();
    private static final Location ORDER_LOCATION = Location.create(10, 10).getValueOrThrow();
    private static final Speed FAST_SPEED = Speed.create(10).getValueOrThrow();
    private static final Speed SLOW_SPEED = Speed.create(1).getValueOrThrow();
    private static final Location DEFAULT_LOCATION = Location.create(1, 1).getValueOrThrow();

    private Courier createCourier(String name, Speed speed, Location location) {
        return Courier.create(name, speed, location).getValueOrThrow();
    }

    private Order createOrder(UUID id, Location location, Volume volume) {
        return Order.create(id, location, volume).getValueOrThrow();
    }

    @Test
    @DisplayName("Ошибка, если заказ null")
    void dispatchFailsWhenOrderIsNull() {
        assertThatThrownBy(() -> {
            dispatcher.dispatch(null, List.of(createCourier("c1", FAST_SPEED, DEFAULT_LOCATION)));
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Ошибка, если couriers null")
    void dispatchFailsWhenCouriersIsNull() {
        Order order = createOrder(ORDER_ID, ORDER_LOCATION, ORDER_VOLUME);

        assertThatThrownBy(() -> {
            dispatcher.dispatch(order, null);
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Ошибка, если couriers пустой список")
    void dispatchFailsWhenCouriersIsEmpty() {
        Order order = createOrder(ORDER_ID, ORDER_LOCATION, ORDER_VOLUME);

        Result<Courier, Error> result = dispatcher.dispatch(order, List.of());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order_dispatcher.no_couriers_provided");
        assertThat(result.getError().getMessage()).contains("No couriers provided for the order");
    }

    @Test
    @DisplayName("Ошибка, если статус заказа не CREATED")
    void dispatchFailsWhenOrderStatusIsNotCreated() {
        Order order = createOrder(ORDER_ID, ORDER_LOCATION, ORDER_VOLUME);
        Courier courier = createCourier("c1", FAST_SPEED, DEFAULT_LOCATION);
        order.assign(courier); // Status = ASSIGNED

        Result<Courier, Error> result = dispatcher.dispatch(order, List.of(courier));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order_dispatcher.order_is_not_ready_to_be_delivered");
        assertThat(result.getError().getMessage()).contains(ORDER_ID.toString());
    }

    @Test
    @DisplayName("Ошибка, если все курьеры заняты (isAvailable() возвращает failure)")
    void dispatchFailsWhenAllCouriersAreBusy() {
        Order order = createOrder(ORDER_ID, ORDER_LOCATION, ORDER_VOLUME);

        Courier courier1 = createCourier("Courier1", FAST_SPEED, DEFAULT_LOCATION);
        Courier courier2 = createCourier("Courier2", FAST_SPEED, DEFAULT_LOCATION);

        // Simulate busy courier by clearing storage and adding a small volume
        courier1.getStoragePlaces().clear();
        courier1.addStoragePlace("Backpack", Volume.create(1, 1, 1).getValueOrThrow());

        courier2.getStoragePlaces().clear();
        courier2.addStoragePlace("Backpack", Volume.create(1, 1, 1).getValueOrThrow());

        Result<Courier, Error> result = dispatcher.dispatch(order, List.of(courier1, courier2));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order_dispatcher.no_suitable_couriers_for_the_order");
    }

    @Test
    @DisplayName("Ошибка, если курьер не может вместить заказ (canTakeOrder() возвращает failure)")
    void dispatchFailsWhenCourierCannotTakeOrder() {
        Order order = createOrder(ORDER_ID, ORDER_LOCATION, ORDER_VOLUME);

        Courier courier = createCourier("Courier1", FAST_SPEED, DEFAULT_LOCATION);

        // Clear backpack and add small volume — now courier has no space for 3x2x1
        courier.getStoragePlaces().clear();
        courier.addStoragePlace("Backpack", Volume.create(1, 1, 1).getValueOrThrow());

        Result<Courier, Error> result = dispatcher.dispatch(order, List.of(courier));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order_dispatcher.no_suitable_couriers_for_the_order");
    }

    @Test
    @DisplayName("Ошибка, если takeOrder возвращает failure (например, уже занят)")
    void dispatchFailsWhenTakeOrderFails() {
        Order order = createOrder(ORDER_ID, ORDER_LOCATION, ORDER_VOLUME);

        Courier courier = createCourier("Courier1", FAST_SPEED, DEFAULT_LOCATION);

        // Assign a different order to the courier
        courier.takeOrder(UUID.randomUUID(), Volume.create(1, 1, 1).getValueOrThrow());

        Result<Courier, Error> result = dispatcher.dispatch(order, List.of(courier));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("order_dispatcher.no_suitable_couriers_for_the_order");
    }

    @Test
    @DisplayName("Выбирается курьер с минимальным временем доставки")
    void dispatchChoosesCourierWithMinimumTime() {
        Order order = createOrder(ORDER_ID, Location.create(5,5).getValueOrThrow(), ORDER_VOLUME);

        Courier slowCourier = createCourier("SlowCourier", SLOW_SPEED, Location.create(10, 10).getValueOrThrow());
        Courier fastCourier = createCourier("FastCourier", FAST_SPEED, Location.create(1, 1).getValueOrThrow());

        Result<Courier, Error> result = dispatcher.dispatch(order, List.of(slowCourier, fastCourier));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getName()).isEqualTo("FastCourier");
        assertThat(order.getCourierId()).isEqualTo(fastCourier.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
    }

    @Test
    @DisplayName("Курьер успешно берет заказ")
    void dispatchSucceedsWhenCourierIsAvailableAndCanTakeOrder() {
        Order order = createOrder(ORDER_ID, ORDER_LOCATION, ORDER_VOLUME);

        Courier courier = createCourier("Courier1", FAST_SPEED, DEFAULT_LOCATION);

        Result<Courier, Error> result = dispatcher.dispatch(order, List.of(courier));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(courier);
        assertThat(order.getCourierId()).isEqualTo(courier.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
    }
}