package microarch.delivery.core.domain.model.kernel;

import libs.errs.Error;
import libs.errs.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class VolumeTest {
    @Test
    @DisplayName("Успешное создание Volume с положительными размерами")
    void creationSucceedsWithPositiveDimensions() {
        Result<Volume, Error> result = Volume.create(5, 3, 2);

        assertThat(result.isSuccess()).isTrue();
        Volume volume = result.getValue();
        assertThat(volume.getX()).isEqualTo(5);
        assertThat(volume.getY()).isEqualTo(3);
        assertThat(volume.getZ()).isEqualTo(2);
    }

    @ParameterizedTest
    @DisplayName("Ошибка создания при нулевом или отрицательном значении размера")
    @MethodSource("provideInvalidDimensions")
    void creationFailsWhenDimensionIsInvalid(String dimensionName, int value, int otherValidValue) {
        // Arrange
        int x = dimensionName.equals("x") ? value : otherValidValue;
        int y = dimensionName.equals("y") ? value : otherValidValue;
        int z = dimensionName.equals("z") ? value : otherValidValue;

        // Act
        Result<Volume, Error> result = Volume.create(x, y, z);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("value.must.be.greater.than");
        assertThat(result.getError().getMessage()).contains(dimensionName).contains(String.valueOf(value));
    }

    static Stream<Arguments> provideInvalidDimensions() {
        return Stream.of(Arguments.of("x", 0, 3), Arguments.of("x", -1, 3), Arguments.of("y", 0, 5),
                Arguments.of("y", -5, 5), Arguments.of("z", 0, 5), Arguments.of("z", -10, 5));
    }

    @ParameterizedTest
    @DisplayName("Объем может вместить другой объем (с поворотом или без)")
    @MethodSource("provideFittingVolumes")
    void canHoldReturnsSuccessWhenVolumeFits(Volume container, Volume item) {
        // Act
        var result = container.canHold(item);

        // Assert
        assertThat(result.isSuccess()).isTrue();
    }

    static Stream<Arguments> provideFittingVolumes() {
        return Stream.of(Arguments.of(Volume.create(10, 8, 6).getValue(), Volume.create(5, 7, 4).getValue()),
                Arguments.of(Volume.create(4, 3, 2).getValue(), Volume.create(2, 1, 4).getValue()),
                Arguments.of(Volume.create(5, 4, 3).getValue(), Volume.create(5, 4, 3).getValue()),
                Arguments.of(Volume.create(5, 3, 2).getValue(), Volume.create(3, 2, 1).getValue()));
    }

    @ParameterizedTest
    @DisplayName("Объем не может вместить другой объем, даже с поворотом")
    @MethodSource("provideNonFittingVolumes")
    void canHoldReturnsFailureWhenVolumeCannotFit(Volume container, Volume item) {
        // Act
        var result = container.canHold(item);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("volume.exceed_capacity");
        assertThat(result.getError().getMessage()).isEqualTo("Volume capacity is too large for the storage");
    }

    static Stream<Arguments> provideNonFittingVolumes() {
        return Stream.of(Arguments.of(Volume.create(3, 2, 1).getValue(), Volume.create(4, 1, 1).getValue()),
                Arguments.of(Volume.create(5, 5, 5).getValue(), Volume.create(6, 1, 1).getValue()),
                Arguments.of(Volume.create(2, 2, 2).getValue(), Volume.create(3, 1, 1).getValue()),
                Arguments.of(Volume.create(1, 1, 10).getValue(), Volume.create(2, 2, 1).getValue()));
    }

    @Test
    @DisplayName("Объемы равны, если все размеры совпадают")
    void volumesAreEqualWhenAllDimensionsMatch() {
        Volume v1 = Volume.create(2, 3, 4).getValue();
        Volume v2 = Volume.create(2, 3, 4).getValue();
        Volume v3 = Volume.create(2, 4, 3).getValue();

        assertThat(v1).isEqualTo(v2);
        assertThat(v1).isNotEqualTo(v3);
        assertThat(v1.hashCode()).isEqualTo(v2.hashCode());
        assertThat(v1.hashCode()).isNotEqualTo(v3.hashCode());
    }

    @Test
    @DisplayName("Объемы не равны, если хотя бы одно измерение отличается")
    void volumesAreNotEqualWhenAnyDimensionDiffers() {
        Volume v1 = Volume.create(2, 3, 4).getValue();
        Volume v2 = Volume.create(2, 3, 5).getValue();
        Volume v3 = Volume.create(2, 4, 4).getValue();
        Volume v4 = Volume.create(3, 3, 4).getValue();

        assertThat(v1).isNotEqualTo(v2);
        assertThat(v1).isNotEqualTo(v3);
        assertThat(v1).isNotEqualTo(v4);
    }
}