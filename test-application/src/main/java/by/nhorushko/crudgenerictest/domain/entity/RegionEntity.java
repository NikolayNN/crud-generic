package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "region")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionEntity implements AbstractEntity<Long> {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;
}
