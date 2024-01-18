package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.antlr.v4.runtime.misc.NotNull;

@Entity
@Data
@NoArgsConstructor
@Table(name = "driver")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverEntity implements AbstractEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;

    @NotNull
    @OneToOne
    @JoinColumn(name = "user_id")
    UserEntity user;

}
