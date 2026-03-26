package microarch.delivery.adapters.in.http.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

import org.springframework.lang.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.annotation.Generated;

/**
 * Order
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-24T15:35:30.575475+03:00[Europe/Moscow]", comments = "Generator version: 7.21.0")
public class Order {

    private UUID id;

    private Location location;

    public Order() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public Order(UUID id, Location location) {
        this.id = id;
        this.location = location;
    }

    public Order id(UUID id) {
        this.id = id;
        return this;
    }

    /**
     * Идентификатор
     *
     * @return id
     */
    @NotNull
    @Valid
    @Schema(name = "id", description = "Идентификатор", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    public UUID getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(UUID id) {
        this.id = id;
    }

    public Order location(Location location) {
        this.location = location;
        return this;
    }

    /**
     * Get location
     *
     * @return location
     */
    @NotNull
    @Valid
    @Schema(name = "location", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("location")
    public Location getLocation() {
        return location;
    }

    @JsonProperty("location")
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return Objects.equals(this.id, order.id) && Objects.equals(this.location, order.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, location);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Order {\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    location: ").append(toIndentedString(location)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces (except the first line).
     */
    private String toIndentedString(@Nullable Object o) {
        return o == null ? "null" : o.toString().replace("\n", "\n    ");
    }
}
