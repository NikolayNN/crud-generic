package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public AbsModelMapper absModelMapper(ModelMapper modelMapper) {
        return new AbsModelMapper(modelMapper);
    }
}
