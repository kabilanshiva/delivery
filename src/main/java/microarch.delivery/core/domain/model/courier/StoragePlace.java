package microarch.delivery.core.domain.model.courier;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import libs.ddd.BaseEntity;
import libs.errs.GeneralErrors;
import libs.errs.Result;
import libs.errs.Error;
import libs.errs.UnitResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import microarch.delivery.core.domain.model.kernel.Volume;
import org.apache.logging.log4j.util.Strings;

import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Table(name = "storage_place")
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
public class StoragePlace extends BaseEntity<UUID> {

    private final String name;
    private final Volume totalVolume;
    private UUID orderId;

    private StoragePlace(String name, Volume volume) {
        super(UUID.randomUUID());
        this.name = name;
        this.totalVolume = volume;
        this.orderId = null;
    }

    public static Result<StoragePlace, Error> create(String name, Volume volume) {
        if (Strings.isBlank(name)) return Result.failure(GeneralErrors.valueIsRequired("name"));

        var storagePlace = new StoragePlace(name, volume);
        return Result.success(storagePlace);
    }

    public Result<Boolean, Error> canStore(Volume volume) {
        if (isOccupied()) return Result.failure(Errors.occupied());

        var canHoldResult = this.totalVolume.canHold(volume);
        if (canHoldResult.isFailure()) return Result.failure(canHoldResult.getError());

        return Result.success(true);
    }

    public UnitResult<Error> store(UUID orderId, Volume volume) {

        if (Objects.isNull(orderId)) return UnitResult.failure(GeneralErrors.valueIsRequired("target"));

        var canStoreResult = canStore(volume);
        if (canStoreResult.isFailure()) {
            return UnitResult.failure(canStoreResult.getError());
        }

        this.orderId = orderId;
        return UnitResult.success();
    }

    public UnitResult<Error> clear(UUID orderId) {
        if (!isOccupied()) return UnitResult.failure(Errors.alreadyEmpty());
        if (!orderId.equals(this.orderId)) return UnitResult.failure(Errors.differentOrderStored(orderId));

        this.orderId = null;
        return UnitResult.success();
    }

    private boolean isOccupied() {
        return Objects.nonNull(this.orderId);
    }

    public static final class Errors {

        public static Error occupied() {
            return Error.of("storage_place.occupied", "Storage place is already occupied");
        }

        public static Error alreadyEmpty() {
            return Error.of("storage_place.empty", "Storage place is already empty");
        }

        public static Error differentOrderStored(UUID requestedOrderId) {
            return Error.of("storage_place.different_order_stored",
                    String.format("Storage place does not have the given order [%s]", requestedOrderId));
        }
    }
}

