package microarch.delivery.core.domain.model.kernel;

import jakarta.persistence.Embeddable;
import libs.ddd.ValueObject;
import libs.errs.Error;
import libs.errs.GeneralErrors;
import libs.errs.Result;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Embeddable
public class Speed extends ValueObject<Speed> {

    private final int value;

    public static Result<Speed, Error> create(int value) {
        if (value <= 0) return Result.failure(GeneralErrors.valueMustBeGreaterThan("value", value, 0));

        return Result.success(new Speed(value));
    }

    @Override
    protected Iterable<Object> equalityComponents() {
        return List.of(this.value);
    }
}