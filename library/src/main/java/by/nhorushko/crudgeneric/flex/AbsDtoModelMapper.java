package by.nhorushko.crudgeneric.flex;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.modelmapper.ModelMapper;

import javax.persistence.EntityManager;

public class AbsDtoModelMapper extends AbsEntityModelMapper {
    private final EntityManager entityManager;

    public AbsDtoModelMapper(ModelMapper modelMapper, EntityManager entityManager) {
        super(modelMapper);
        this.entityManager = entityManager;
    }

    public <T extends AbstractEntity<?>> T reference(AbstractDto<?> dto, Class<T> destinationClass) {
        return referenceById(dto.getId(), destinationClass);
    }

    public <T extends AbstractEntity<?>> T referenceById(Object id, Class<T> destinationClass) {
        return entityManager.getReference(destinationClass, id);
    }
}
