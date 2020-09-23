package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.domain.AbstractEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class MockBEntity implements AbstractEntity {
    @Id
    private Long id;
    private String name;
    @OneToOne
    private MockAEntity mockAEntity;

    public MockBEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
