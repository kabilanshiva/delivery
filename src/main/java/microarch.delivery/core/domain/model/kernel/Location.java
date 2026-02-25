package microarch.delivery.core.domain.model.kernel;

import libs.ddd.ValueObject;
import libs.errs.GeneralErrors;
import libs.errs.Result;
import libs.errs.Error;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public final class Location extends ValueObject<Location> {

    private final int x;
    private final int y;

    public static Result<Location, Error> create(int x, int y) {

        if (x < 1 || x > 10) return Result.failure(GeneralErrors.valueIsOutOfRange("x", x, 1, 10));
        if (y < 1 || y > 10) return Result.failure(GeneralErrors.valueIsOutOfRange("y", y, 1, 10));

        return Result.success(new Location(x, y));
    }

    @Override
    protected Iterable<Object> equalityComponents() {
        return List.of(this.x, this.y);
    }

    public Result<Integer, Error> distanceTo(Location target) {

        if (Objects.isNull(target)) return Result.failure(GeneralErrors.valueIsRequired("target"));

        return Result.success(Math.abs(this.x - target.x) + Math.abs(this.y - target.y));
    }
}
