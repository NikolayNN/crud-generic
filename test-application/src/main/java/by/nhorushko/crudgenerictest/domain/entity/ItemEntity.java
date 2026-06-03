package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "grp_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemEntity implements AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private GroupEntity group;
}
