package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupEntity implements AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemEntity> items = new ArrayList<>();
}
