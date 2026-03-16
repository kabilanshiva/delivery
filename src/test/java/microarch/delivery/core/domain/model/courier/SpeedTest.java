package microarch.delivery.core.domain.model.courier;

import libs.errs.Error;
import libs.errs.Result;
import microarch.delivery.core.domain.model.kernel.Speed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SpeedTest {

    @Test
    @DisplayName("Успешное создание Speed с положительным значением")
    void creationSucceedsWithPositiveValue() {
        Result<Speed, Error> result = Speed.create(10);

        assertThat(result.isSuccess()).isTrue();
        Speed speed = result.getValue();
        assertThat(speed.getValue()).isEqualTo(10);
    }

    @ParameterizedTest
    @DisplayName("Ошибка создания Speed при нулевом или отрицательном значении")
    @MethodSource("provideInvalidSpeedValues")
    void creationFailsWhenValueIsZeroOrNegative(int value) {
        Result<Speed, Error> result = Speed.create(value);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.must.be.greater.than");
        assertThat(result.getError().getMessage())
                .contains("value")
                .contains(String.valueOf(value));
    }

    static Stream<Arguments> provideInvalidSpeedValues() {
        return Stream.of(
                Arguments.of(0),
                Arguments.of(-1),
                Arguments.of(-100)
        );
    }

    @Test
    @DisplayName("Speed равны, если значения совпадают")
    void speedsAreEqualWhenValuesMatch() {
        Speed s1 = Speed.create(5).getValueOrThrow();
        Speed s2 = Speed.create(5).getValueOrThrow();
        Speed s3 = Speed.create(7).getValueOrThrow();

        assertThat(s1).isEqualTo(s2);
        assertThat(s1).isNotEqualTo(s3);
        assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
        assertThat(s1.hashCode()).isNotEqualTo(s3.hashCode());
    }
}