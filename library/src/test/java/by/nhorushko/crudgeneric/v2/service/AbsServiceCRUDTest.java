package by.nhorushko.crudgeneric.v2.service;

import by.nhorushko.crudgeneric.domain.dto.Message;
import by.nhorushko.crudgeneric.domain.dto.Message.GpsCoordinate;
import by.nhorushko.crudgeneric.domain.entity.MessageEntity;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityDto;
import jakarta.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class AbsServiceCRUDTest {

    @Mock
    private AbsMapperEntityDto<MessageEntity, Message> mockedMapper;

    @Mock
    private JpaRepository<MessageEntity, Long> mockedRepository;

    @Mock
    private EntityManager mockedEntityManager;

    private AbsServiceCRUD<Long, MessageEntity, Message, JpaRepository<MessageEntity, Long>> service;

    @Before
    public void initializeService() {
        this.service = new AbsServiceCRUD<>(this.mockedMapper, this.mockedRepository) {
        };
        this.service.entityManager = this.mockedEntityManager;
    }

    @Test
    public void newMessageShouldBePersisted() {
        final Message givenDto = new Message(null, new GpsCoordinate(5.5F, 6.6F), 10, 15, 20);
        final MessageEntity givenEntity = new MessageEntity(null, 5.5F, 6.6F, 10, 15, 20);
        when(this.mockedMapper.toEntity(any(Message.class))).thenReturn(givenEntity);

        final Message savedDto = new Message(255L, new GpsCoordinate(5.5F, 6.6F), 10, 15, 20);
        when(this.mockedMapper.toDto(givenEntity)).thenReturn(savedDto);

        final Message actual = this.service.save(givenDto);

        assertSame(savedDto, actual);
        assertEquals(Long.valueOf(255L), actual.getId());
        verify(this.mockedEntityManager, times(1)).persist(givenEntity);
        verify(this.mockedRepository, never()).save(any(MessageEntity.class));
    }

    @Test
    public void existingMessageShouldBeMerged() {
        final Message givenDto = new Message(5L, new GpsCoordinate(7.7F, 8.8F), 11, 16, 21);
        final MessageEntity givenEntity = new MessageEntity(5L, 7.7F, 8.8F, 11, 16, 21);
        when(this.mockedMapper.toEntity(any(Message.class))).thenReturn(givenEntity);
        when(this.mockedRepository.existsById(5L)).thenReturn(true);

        final MessageEntity mergedEntity = new MessageEntity(5L, 7.7F, 8.8F, 11, 16, 21);
        when(this.mockedRepository.save(givenEntity)).thenReturn(mergedEntity);

        final Message savedDto = new Message(5L, new GpsCoordinate(7.7F, 8.8F), 11, 16, 21);
        when(this.mockedMapper.toDto(mergedEntity)).thenReturn(savedDto);

        final Message actual = this.service.save(givenDto);

        assertSame(savedDto, actual);
        verify(this.mockedRepository, times(1)).save(givenEntity);
        verify(this.mockedEntityManager, never()).persist(any());
    }

    @Test
    public void saveAllShouldRouteEachElement() {
        final List<Message> givenDtos = List.of(
                new Message(null, new GpsCoordinate(5.5F, 6.6F), 10, 15, 20),
                new Message(5L, new GpsCoordinate(7.7F, 8.8F), 11, 16, 21)
        );
        final MessageEntity newEntity = new MessageEntity(null, 5.5F, 6.6F, 10, 15, 20);
        final MessageEntity existingEntity = new MessageEntity(5L, 7.7F, 8.8F, 11, 16, 21);
        when(this.mockedMapper.toEntities(anyCollectionOf(Message.class)))
                .thenReturn(List.of(newEntity, existingEntity));
        when(this.mockedRepository.existsById(5L)).thenReturn(true);
        when(this.mockedRepository.save(existingEntity)).thenReturn(existingEntity);

        final List<Message> savedDtos = List.of(
                new Message(255L, new GpsCoordinate(5.5F, 6.6F), 10, 15, 20),
                new Message(5L, new GpsCoordinate(7.7F, 8.8F), 11, 16, 21)
        );
        when(this.mockedMapper.toDtos(anyCollectionOf(MessageEntity.class))).thenReturn(savedDtos);

        final List<Message> actual = this.service.saveAll(givenDtos);

        assertEquals(savedDtos, actual);
        verify(this.mockedEntityManager, times(1)).persist(newEntity);
        verify(this.mockedRepository, times(1)).save(existingEntity);
        verify(this.mockedEntityManager, never()).persist(existingEntity);
        verify(this.mockedRepository, never()).save(newEntity);
    }
}
