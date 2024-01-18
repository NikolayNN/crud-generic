package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "tracker")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
@Builder
public class TrackerEntity implements AbstractEntity<Long> {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "imei")
    private String imei;

    @Column(name = "phone_number")
    private String phoneNumber;
}
