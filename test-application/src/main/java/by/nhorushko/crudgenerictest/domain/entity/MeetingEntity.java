package by.nhorushko.crudgenerictest.domain.entity;

import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Fixture for the flex pageable stack: one field per supported filter kind —
 * string, enum, instant, local date and a nested path via {@link RegionEntity}.
 */
@Entity
@Table(name = "meeting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingEntity implements AbstractEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MeetingStatus status;

    @Column(name = "start_time")
    private Instant startTime;

    // "day" is a reserved word in H2 2.x
    @Column(name = "meeting_day")
    private LocalDate day;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private RegionEntity region;
}
