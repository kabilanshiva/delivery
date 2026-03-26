package microarch.delivery.core.domain.model.kernel;

import jakarta.persistence.Embeddable;
import libs.ddd.ValueObject;
import libs.errs.Error;
import libs.errs.GeneralErrors;
import libs.errs.Result;
import libs.errs.UnitResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Embeddable
public class Volume extends ValueObject<Volume> {

    private final int x;
    private final int y;
    private final int z;

    public static Result<Volume, Error> create(int x, int y, int z) {
        if (x <= 0)
            return Result.failure(GeneralErrors.valueMustBeGreaterThan("x", x, 0));
        if (y <= 0)
            return Result.failure(GeneralErrors.valueMustBeGreaterThan("y", y, 0));
        if (z <= 0)
            return Result.failure(GeneralErrors.valueMustBeGreaterThan("z", z, 0));

        return Result.success(new Volume(x, y, z));
    }

    public UnitResult<Error> canHold(Volume other) {
        if (canFit(other.getX(), other.getY(), other.getZ()))
            return UnitResult.success();
        if (canFit(other.getY(), other.getZ(), other.getX()))
            return UnitResult.success();
        if (canFit(other.getZ(), other.getX(), other.getY()))
            return UnitResult.success();

        return UnitResult.failure(Errors.cantHold());
    }

    private boolean canFit(int x, int y, int z) {
        return x <= this.x && y <= this.y && z <= this.z;
    }

    @Override
    protected Iterable<Object> equalityComponents() {
        return List.of(this.x, this.y, this.z);
    }

    public static final class Errors {

        public static Error cantHold() {
            return Error.of("volume.exceed_capacity", "Volume capacity is too large for the storage");
        }
    }
}
