package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Child side of the flex ext fixture. Holds exactly one field of type
 * {@link ProjectEntity} — {@code AbsMapperExtRelation} locates the relation
 * field reflectively by that type and rejects zero or multiple matches.
 */
@Entity
@Table(name = "project_task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity implements AbstractEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectEntity project;
}
