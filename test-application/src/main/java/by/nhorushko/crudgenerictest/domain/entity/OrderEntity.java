package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity implements AbstractEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    /**
     * Server-managed field, intentionally absent from every order DTO.
     */
    @Column(name = "secret_code")
    private String secretCode;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderLineEntity> lines = new ArrayList<>();
}
