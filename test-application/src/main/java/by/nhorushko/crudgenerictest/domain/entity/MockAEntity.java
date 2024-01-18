package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mock_entity")
public class MockAEntity implements AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;

    public MockAEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
