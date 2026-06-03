package by.nhorushko.crudgenerictest.mockmapper;

import by.nhorushko.crudgeneric.mapper.AbstractMapper;
import by.nhorushko.crudgenerictest.domain.dto.ItemDto;
import by.nhorushko.crudgenerictest.domain.entity.ItemEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class ItemMapper extends AbstractMapper<ItemEntity, ItemDto> {
    public ItemMapper(ModelMapper modelMapper) {
        super(ItemEntity.class, ItemDto.class, modelMapper);
    }
}
