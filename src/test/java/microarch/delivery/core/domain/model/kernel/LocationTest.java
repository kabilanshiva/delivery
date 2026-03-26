package microarch.delivery.core.domain.model.kernel;

import libs.errs.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class LocationTest {

    @Test
    @DisplayName("Проверка наследования от ValueObject")
    void verifyInheritanceFromValueObject() {
        // Assert
        assertThat(Location.class.getSuperclass().getSimpleName()).isEqualTo("ValueObject");
    }

    @Test
    @DisplayName("Успешное создание локации на границах допустимого диапазона")
    void creationSucceedsAtBoundaryValues() {
        // Act: Границы 1 и 10 должны быть валидны
        var resultMin = Location.create(1, 1);
        var resultMax = Location.create(10, 10);

        // Assert
        assertThat(resultMin.isSuccess()).isTrue();
        assertThat(resultMin.getValue().getX()).isEqualTo(1);
        assertThat(resultMin.getValue().getY()).isEqualTo(1);

        assertThat(resultMax.isSuccess()).isTrue();
        assertThat(resultMax.getValue().getX()).isEqualTo(10);
        assertThat(resultMax.getValue().getY()).isEqualTo(10);
    }

    @ParameterizedTest
    @CsvSource({ "0, 5", // X ниже минимума
            "11, 5", // X выше максимума
            "5, 0", // Y ниже минимума
            "5, 11", // Y выше максимума
            "0, 0", // Оба ниже минимума
            "11, 11" // Оба выше максимума
    })
    @DisplayName("Ошибка создания при выходе координат за пределы [1, 10]")
    void creationFailsWhenCoordinatesAreOutOfBounds(int x, int y) {
        // Act
        Result<Location, ?> result = Location.create(x, y);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isNotNull();
        assertThat(result.getError().getMessage()).containsIgnoringCase("out of range");
    }

    @Test
    @DisplayName("Локации равны, если совпадают обе координаты")
    void equalityHoldsWhenBothCoordinatesMatch() {
        // Arrange
        var locA = safeCreate(7, 3);
        var locB = safeCreate(7, 3);

        // Act & Assert
        assertThat(locA).isEqualTo(locB);
        assertThat(locA.hashCode()).isEqualTo(locB.hashCode());
    }

    @Test
    @DisplayName("Локации не равны, если отличается хотя бы одна координата")
    void equalityFailsWhenCoordinatesDiffer() {
        // Arrange
        var locA = safeCreate(2, 8);
        var locB = safeCreate(2, 9); // Отличается Y
        var locC = safeCreate(3, 8); // Отличается X

        // Act & Assert
        assertThat(locA).isNotEqualTo(locB);
        assertThat(locA).isNotEqualTo(locC);
        assertThat(locA).isNotEqualTo(null);
    }

    @Test
    @DisplayName("Расчет расстояния между разными точками (Манхэттенское расстояние)")
    void calculatesManhattanDistanceCorrectly() {
        // Arrange: (2, 3) -> (5, 7). Dx=3, Dy=4. Sum=7
        var start = safeCreate(2, 3);
        var end = safeCreate(5, 7);
        int expectedDistance = 7;

        // Act
        var result = start.distanceTo(end);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(expectedDistance);
    }

    @Test
    @DisplayName("Расстояние до самой себя равно нулю")
    void distanceToSelfIsZero() {
        // Arrange
        var point = safeCreate(6, 6);

        // Act
        var result = point.distanceTo(point);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isZero();
    }

    @ParameterizedTest
    @CsvSource({ "1, 1, 2, 1, 1", // Движение вправо на 1
            "1, 1, 1, 2, 1", // Движение вверх на 1
            "10, 10, 1, 1, 18", // Диагональ через всю карту
            "5, 5, 5, 5, 0" // Одинаковые координаты через параметризацию
    })
    @DisplayName("Корректный расчет расстояния для различных сценариев перемещения")
    void verifiesDistanceCalculationAcrossGrid(int x1, int y1, int x2, int y2, int expected) {
        // Arrange
        var from = safeCreate(x1, y1);
        var to = safeCreate(x2, y2);

        // Act
        var result = from.distanceTo(to);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Выброс исключения при расчете расстояния до null")
    void throwsErrorWhenTargetLocationIsNull() {
        // Arrange
        var origin = safeCreate(1, 1);

        // Act & Assert

        var result = origin.distanceTo(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isNotNull();
        assertThat(result.getError().getMessage()).containsIgnoringCase("required");
    }

    private Location safeCreate(int x, int y) {
        var result = Location.create(x, y);
        if (result.isFailure()) {
            throw new IllegalStateException("Не удалось создать локацию для теста: " + result.getError());
        }
        return result.getValue();
    }
}
