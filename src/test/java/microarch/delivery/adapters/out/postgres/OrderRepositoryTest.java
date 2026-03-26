package microarch.delivery.adapters.out.postgres;

import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Speed;
import microarch.delivery.core.domain.model.kernel.Volume;
import microarch.delivery.core.domain.model.order.Order;
import microarch.delivery.core.domain.model.order.OrderStatus;
import microarch.delivery.core.ports.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OrderRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderJpaRepository orderJpaRepository;

    @BeforeEach
    void cleanUp() {
        orderJpaRepository.deleteAll();
    }

    private Order createOrder(UUID id, Location location, Volume volume) {
        return Order.create(id, location, volume).getValueOrThrow();
    }

    private Location createLocation(int x, int y) {
        return Location.create(x, y).getValueOrThrow();
    }

    private Volume createVolume(int x, int y, int z) {
        return Volume.create(x, y, z).getValueOrThrow();
    }

    private Speed createSpeed(int value) {
        return Speed.create(value).getValueOrThrow();
    }

    private Courier createCourier(String name, int speed, int locationX, int locationY) {
        return Courier.create(name, createSpeed(speed), createLocation(locationX, locationY)).getValueOrThrow();
    }

    @Test
    @DisplayName("Сохранение заказа в БД и возврат сохранённого объекта")
    void saveOrder_SavesOrderAndReturnsSavedObject() {
        // Arrange
        Order order = createOrder(UUID.randomUUID(), createLocation(10, 10), createVolume(3, 2, 1));

        // Act
        Order savedOrder = orderRepository.saveOrder(order);

        // Assert
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getLocation().getX()).isEqualTo(10);
        assertThat(savedOrder.getLocation().getY()).isEqualTo(10);
        assertThat(savedOrder.getVolume().getX()).isEqualTo(3);
        assertThat(savedOrder.getVolume().getY()).isEqualTo(2);
        assertThat(savedOrder.getVolume().getZ()).isEqualTo(1);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(savedOrder.getCourierId()).isNull();
    }

    @Test
    @DisplayName("Поиск заказа по ID — успешный поиск")
    void findOrderById_FindsOrderById() {
        // Arrange
        Order order = createOrder(UUID.randomUUID(), createLocation(10, 10), createVolume(3, 2, 1));
        Order savedOrder = orderRepository.saveOrder(order);

        // Act
        Optional<Order> foundOrder = orderRepository.findOrderById(savedOrder.getId());

        // Assert
        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get()).isEqualTo(savedOrder);
    }

    @Test
    @DisplayName("Поиск заказа по несуществующему ID — возвращает пустой Optional")
    void findOrderById_ReturnsEmptyForNonExistentId() {
        // Act
        Optional<Order> foundOrder = orderRepository.findOrderById(UUID.randomUUID());

        // Assert
        assertThat(foundOrder).isEmpty();
    }

    @Test
    @DisplayName("Поиск первого созданного заказа — возвращает созданный заказ")
    void findCreatedOrder_FindsCreatedOrder() {
        // Arrange
        Order createdOrder = createOrder(UUID.randomUUID(), createLocation(10, 10), createVolume(3, 2, 1));
        orderRepository.saveOrder(createdOrder);

        Order assignedOrder = createOrder(UUID.randomUUID(), createLocation(10, 10), createVolume(3, 2, 1));
        assignedOrder.assign(createCourier("Courier-1", 10, 1, 1));
        orderRepository.saveOrder(assignedOrder);

        // Act
        Optional<Order> foundOrder = orderRepository.findCreatedOrder();

        // Assert
        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get()).isEqualTo(createdOrder);
    }

    @Test
    @DisplayName("Поиск первого созданного заказа — возвращает пустой Optional, если нет созданных заказов")
    void findCreatedOrder_ReturnsEmptyIfNoCreatedOrders() {
        // Arrange
        Order assignedOrder = createOrder(UUID.randomUUID(), createLocation(10, 10), createVolume(3, 2, 1));
        assignedOrder.assign(createCourier("Courier-1", 10, 1, 1));
        orderRepository.saveOrder(assignedOrder);

        // Act
        Optional<Order> foundOrder = orderRepository.findCreatedOrder();

        // Assert
        assertThat(foundOrder).isEmpty();
    }

    @Test
    @DisplayName("Поиск всех назначенных заказов — возвращает назначенные заказы")
    void findAllAssignedOrders_ReturnsAssignedOrders() {
        // Arrange
        Order createdOrder = createOrder(UUID.randomUUID(), createLocation(10, 10), createVolume(3, 2, 1));
        orderRepository.saveOrder(createdOrder);

        Order assignedOrder = createOrder(UUID.randomUUID(), createLocation(10, 10), createVolume(3, 2, 1));
        assignedOrder.assign(createCourier("Courier-1", 10, 1, 1));
        orderRepository.saveOrder(assignedOrder);

        Order completedOrder = createOrder(UUID.randomUUID(), createLocation(10, 10), createVolume(3, 2, 1));
        completedOrder.assign(createCourier("Courier-2", 10, 1, 1));
        completedOrder.complete();
        orderRepository.saveOrder(completedOrder);

        // Act
        List<Order> assignedOrders = orderRepository.findAllAssignedOrders();

        // Assert
        assertThat(assignedOrders.size()).isEqualTo(1);
        assertThat(assignedOrders.get(0).getStatus()).isEqualTo(OrderStatus.ASSIGNED);
    }

    @Test
    @DisplayName("Поиск всех назначенных заказов — возвращает пустой список, если нет назначенных заказов")
    void findAllAssignedOrders_ReturnsEmptyIfNoAssignedOrders() {
        // Arrange
        Order createdOrder = createOrder(UUID.randomUUID(), createLocation(10, 10), createVolume(3, 2, 1));
        orderRepository.saveOrder(createdOrder);

        // Act
        List<Order> assignedOrders = orderRepository.findAllAssignedOrders();

        // Assert
        assertThat(assignedOrders.isEmpty()).isTrue();
    }
}