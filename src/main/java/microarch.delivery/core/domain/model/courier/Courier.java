package microarch.delivery.core.domain.model.courier;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import libs.ddd.Aggregate;
import libs.errs.GeneralErrors;
import libs.errs.Result;
import libs.errs.Error;
import libs.errs.UnitResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Speed;
import microarch.delivery.core.domain.model.kernel.Volume;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Table(name = "couriers")
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class Courier extends Aggregate<UUID> {

    private final String name;

    @Embedded
    private final Speed speed;

    @Embedded
    private Location location;

    @OneToMany(orphanRemoval = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "courier_id", nullable = false)
    private final List<StoragePlace> storagePlaces = new ArrayList<>();

    private Courier(String name, Speed speed, Location location) {
        super(UUID.randomUUID());
        this.name = name;
        this.speed = speed;
        this.location = location;
        this.storagePlaces
                .add(StoragePlace.create("Backpack", Volume.create(5, 2, 1).getValueOrThrow()).getValueOrThrow());
    }

    public static Result<Courier, Error> create(String name, Speed speed, Location location) {
        if (Strings.isBlank(name))
            return Result.failure(GeneralErrors.valueIsRequired("name"));
        if (Objects.isNull(speed))
            return Result.failure(GeneralErrors.valueIsRequired("speed"));
        if (Objects.isNull(location))
            return Result.failure(GeneralErrors.valueIsRequired("location"));

        var order = new Courier(name, speed, location);
        return Result.success(order);
    }

    public UnitResult<Error> addStoragePlace(String name, Volume volume) {
        var storagePlaceResult = StoragePlace.create(name, volume);
        if (storagePlaceResult.isFailure())
            return UnitResult.failure(storagePlaceResult.getError());

        storagePlaces.add(storagePlaceResult.getValue());
        return UnitResult.success();
    }

    public UnitResult<Error> canTakeOrder(Volume orderVolume) {
        return storagePlaces.stream().anyMatch(storagePlace -> storagePlace.canStore(orderVolume).isSuccess())
                ? UnitResult.success() : UnitResult.failure(Errors.noSuitableStorageSpaceAvailable());
    }

    public UnitResult<Error> takeOrder(UUID orderId, Volume orderVolume) {
        if (Objects.isNull(orderId))
            return UnitResult.failure(GeneralErrors.valueIsRequired("orderId"));
        if (Objects.isNull(orderVolume))
            return UnitResult.failure(GeneralErrors.valueIsRequired("orderVolume"));

        return storagePlaces.stream().filter(storagePlace -> storagePlace.canStore(orderVolume).isSuccess()).findFirst()
                .map(storagePlace -> storagePlace.store(orderId, orderVolume))
                .orElse(UnitResult.failure(Errors.noSuitableStorageSpaceAvailable()));
    }

    public UnitResult<Error> completeOrder(UUID orderId) {
        if (Objects.isNull(orderId))
            return UnitResult.failure(GeneralErrors.valueIsRequired("orderId"));

        var storage = storagePlaces.stream().filter(storagePlace -> orderId.equals(storagePlace.getOrderId()))
                .findFirst();

        if (storage.isEmpty())
            return UnitResult.failure(Errors.orderNotAvailable(orderId));

        return storage.get().clear(orderId);
    }

    public Result<Integer, Error> calculateTimeToLocation(Location location) {

        if (Objects.isNull(location))
            return Result.failure(GeneralErrors.valueIsRequired("location"));

        var distanceResult = this.location.distanceTo(location);

        if (distanceResult.isFailure())
            return Result.failure(distanceResult.getError());

        return Result.success(Math.ceilDiv(distanceResult.getValue(), this.speed.getValue()));
    }

    public UnitResult<Error> move(Location target) {
        if (target == null) {
            return UnitResult.failure(GeneralErrors.valueIsRequired("target"));
        }

        int difX = target.getX() - location.getX();
        int difY = target.getY() - location.getY();
        int cruisingRange = this.speed.getValue();

        int moveX = Math.max(-cruisingRange, Math.min(difX, cruisingRange));
        cruisingRange -= Math.abs(moveX);

        int moveY = Math.max(-cruisingRange, Math.min(difY, cruisingRange));

        Result<Location, Error> locationCreateResult = Location.create(location.getX() + moveX,
                location.getY() + moveY);

        if (locationCreateResult.isFailure()) {
            return UnitResult.failure(locationCreateResult.getError());
        }

        this.location = locationCreateResult.getValue();
        return UnitResult.success();
    }

    public UnitResult<Error> isAvailable() {
        var isAvailable = storagePlaces.stream().noneMatch(StoragePlace::isOccupied);
        return isAvailable ? UnitResult.success() : UnitResult.failure(Errors.alreadyHasAnOrder());
    }

    public static final class Errors {

        public static Error noSuitableStorageSpaceAvailable() {
            return Error.of("courier.no_suitable_storage", "No suitable storage space available");
        }

        public static Error orderNotAvailable(UUID orderId) {
            return Error.of("courier.order_not_available",
                    String.format("Current courier does not have the order [%s]", orderId));
        }

        public static Error alreadyHasAnOrder() {
            return Error.of("courier.already_has_an_order", "Current courier already has an order");
        }
    }

}
