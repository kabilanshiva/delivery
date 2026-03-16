package microarch.delivery.core.domain.model.order;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import libs.ddd.Aggregate;
import libs.errs.GeneralErrors;
import libs.errs.Result;
import libs.errs.Error;
import libs.errs.UnitResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import microarch.delivery.core.domain.model.courier.Courier;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Volume;

import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
public class Order extends Aggregate<UUID> {

    @Embedded
    private final Location location;

    @Embedded
    private final Volume volume;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private UUID courierId;

    private Order(UUID id, Location location, Volume volume) {
        super(id);
        this.location = location;
        this.volume = volume;
        this.status = OrderStatus.CREATED;
    }

    public static Result<Order, Error> create(UUID id, Location location, Volume volume) {
        if (Objects.isNull(id)) return Result.failure(GeneralErrors.valueIsRequired("id"));
        if (Objects.isNull(location)) return Result.failure(GeneralErrors.valueIsRequired("location"));
        if (Objects.isNull(volume)) return Result.failure(GeneralErrors.valueIsRequired("volume"));

        var order = new Order(id, location, volume);
        return Result.success(order);
    }

    public UnitResult<Error> assign(Courier courier) {
        if (Objects.isNull(courier)) return UnitResult.failure(GeneralErrors.valueIsRequired("Courier"));
        this.courierId = courier.getId();
        this.status = OrderStatus.ASSIGNED;
        return UnitResult.success();
    }

    public UnitResult<Error> complete() {
        return switch (this.status) {
            case CREATED -> UnitResult.failure(Errors.orderIsNotAssigned(this.getId()));
            case COMPLETED -> UnitResult.failure(Errors.orderAlreadyCompleted(this.getId()));
            case ASSIGNED -> {
                this.status = OrderStatus.COMPLETED;
                yield UnitResult.success();
            }
        };
    }

    public static class Errors {
        public static Error orderIsNotAssigned(UUID id) {
            return Error.of("order.not_assigned",
                    String.format("Order [%s] is not assigned yet", id));
        }

        public static Error orderAlreadyCompleted(UUID id) {
            return Error.of("order.already_completed",
                    String.format("Order [%s] has been already completed", id));
        }
    }

}
