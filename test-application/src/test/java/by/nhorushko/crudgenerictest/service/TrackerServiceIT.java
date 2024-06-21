package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.exception.AppNotFoundException;
import by.nhorushko.crudgenerictest.domain.dto.Tracker;
import by.nhorushko.crudgenerictest.domain.entity.TrackerEntity;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TrackerServiceIT {

    @Autowired
    private TrackerServiceCRUD service;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void trackerShouldBeFoundById() {
        Optional<Tracker> optionalActual = this.service.getByIdOptional(1L);
        assertTrue(optionalActual.isPresent());
        Tracker actual = optionalActual.get();
        Tracker expected = new Tracker(1L, "355234055650192", "+37257063997");
        assertEquals(expected, actual);
    }

    @Test
    public void trackerShouldNotBeFoundById() {
        Optional<Tracker> optionalActual = this.service.getByIdOptional(MAX_VALUE);
        assertTrue(optionalActual.isEmpty());
    }

    @Test
    public void trackersShouldBeFoundByIds() {
        List<Long> givenIds = List.of(1L, 2L, 3L);

        List<Tracker> foundTrackers = this.service.getById(givenIds);
        List<Long> actual = foundTrackers.stream()
                .map(Tracker::getId)
                .collect(toList());

        assertEquals(givenIds, actual);
    }

    @Test
    public void trackersShouldNotBeFoundByIds() {
        List<Long> givenIds = List.of(111L, 222L, 3333L);

        List<Tracker> foundTrackers = this.service.getById(givenIds);
        assertTrue(foundTrackers.isEmpty());
    }

    @Test
    public void trackerShouldBeGotById() {
        Tracker actual = this.service.getById(1L);
        Tracker expected = new Tracker(1L, "355234055650192", "+37257063997");
        assertEquals(expected, actual);
    }

    @Test
    public void trackerShouldNotBeGotById() {
        assertThrows(AppNotFoundException.class, () -> this.service.getById(MAX_VALUE));
    }

    @Test
    public void trackerWithGivenIdShouldExist() {
        Long givenId = 3L;
        assertTrue(this.service.isExist(givenId));
    }

    @Test
    public void trackerWithGivenIdShouldNotExist() {
        Long givenId = MIN_VALUE;
        assertFalse(this.service.isExist(givenId));
    }

    @Test
    public void trackerShouldBeUpdated() {
        Tracker givenTrackerToUpdate = new Tracker(1L, "3550260722834532", "37257591222");
        this.service.update(givenTrackerToUpdate);

        TrackerEntity updatedTracker = findTrackerFromDB(givenTrackerToUpdate.getId());
        assertEquals("3550260722834532", updatedTracker.getImei());
        assertEquals("37257591222", updatedTracker.getPhoneNumber());
    }

    @Test
    public void trackerShouldNotBeUpdatedBecauseOfIdIsNull() {
        Tracker givenTrackerToUpdate = new Tracker(null, "3550260722834532", "37257591222");
        assertThrows(IllegalArgumentException.class, () -> this.service.update(givenTrackerToUpdate));
    }

    @Test
    public void trackerShouldNotBeUpdatedBecauseOfIdIsZero() {
        Tracker givenTrackerToUpdate = new Tracker(0L, "3550260722834532", "37257591222");
        assertThrows(IllegalArgumentException.class, () -> {
            this.service.update(givenTrackerToUpdate);
        });

    }

    @Test
    public void trackerShouldBePartialUpdated() {
        Long givenTrackerId = 1L;

        TrackerImei trackerImei = new TrackerImei("12345678912345678912");

        this.service.updatePartial(givenTrackerId, trackerImei);

        TrackerEntity updatedTracker = findTrackerFromDB(givenTrackerId);
        entityManager.flush();
        entityManager.refresh(updatedTracker);
        assertEquals(trackerImei.getImei(), updatedTracker.getImei());
    }

    @Test
    public void trackerShouldBeDeletedById() {
        Long givenTrackerId = 1L;

        this.service.delete(givenTrackerId);

        assertNull(findTrackerFromDB(givenTrackerId));
    }

    @Test
    public void trackerShouldBeSaved() {
        Tracker givenTracker = new Tracker(null, "355234055650192", "+3197011460885");

        Tracker savedTracker = this.service.save(givenTracker);
        assertNotNull(savedTracker.getId());

        TrackerEntity savedTrackedFromDB = findTrackerFromDB(savedTracker.getId());
        assertEquals(givenTracker.getImei(), savedTrackedFromDB.getImei());
        assertEquals(givenTracker.getPhoneNumber(), savedTrackedFromDB.getPhoneNumber());
    }

    @Test
    public void trackersShouldBeSaved() {
        List<Tracker> givenTrackers = List.of(
                new Tracker(null, "355234055650192", "+3197011460885"),
                new Tracker(null, "355026070834532", "+3197011405848"));

        List<Tracker> savedTrackers = this.service.saveAll(givenTrackers);
        assertTrue(savedTrackers.stream().allMatch(tracker -> tracker.getId() != null));

        List<TrackerEntity> savedTrackersFromDB = savedTrackers
                .stream()
                .map(savedTracker -> findTrackerFromDB(savedTracker.getId()))
                .collect(toList());

        range(0, givenTrackers.size())
                .forEach(i -> {
                    assertEquals(givenTrackers.get(i).getImei(), savedTrackersFromDB.get(i).getImei());
                    assertEquals(givenTrackers.get(i).getPhoneNumber(), savedTrackersFromDB.get(i).getPhoneNumber());
                });
    }

    private static class TrackerImei {
        private String imei;

        TrackerImei(String imei) {
            this.imei = imei;
        }

        String getImei() {
            return this.imei;
        }
    }

    protected TrackerEntity findTrackerFromDB(Long id) {
        this.entityManager.flush();
        this.entityManager.clear();
        return this.entityManager.find(TrackerEntity.class, id);
    }
}

