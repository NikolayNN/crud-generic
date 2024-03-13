package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;

@Configuration
public class CrudAbstractGenericConfiguration {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setAmbiguityIgnored(true)
                .setMatchingStrategy(MatchingStrategies.STANDARD)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PUBLIC)
                .setSkipNullEnabled(true);
        return modelMapper;
    }

    @Bean
    public AbsDtoModelMapper absModelMapper(ModelMapper modelMapper, EntityManager entityManager) {
        return new AbsDtoModelMapper(modelMapper, entityManager);
    }
}
