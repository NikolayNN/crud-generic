//package by.nhorushko.crudgeneric.v2.mapper;
//
//import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
//import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import lombok.*;
//import lombok.experimental.FieldDefaults;
//import org.junit.Test;
//import org.modelmapper.ModelMapper;
//
//import jakarta.annotation.PostConstruct;
//
//public class DtoMapperTest2 {
//
//    @Test
//    public void name() {
//        ModelMapper modelMapper = new ModelMapper();
//        LogisticStepMapper logisticStepMapper = new LogisticStepMapper(modelMapper);
//        logisticStepMapper.config();
//        LogisticStepAddressMapper logisticStepAddressMapper = new LogisticStepAddressMapper(modelMapper);
//        LogisticLocationMapper logisticLocationMapper = new LogisticLocationMapper(modelMapper);
//
//        StepAddressEntity entity = StepAddressEntity.builder()
//                .id(123L)
//                .address(LocationEntity.builder()
//                        .address("Minsk")
//                        .radius(300)
//                        .build())
//                .build();
//
//        Step map = logisticStepMapper.toDto(entity);
//    }
//
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class StepEntity implements AbstractEntity<Long> {
//        Long id;
//    }
//
//    @Data
//    public static class StepAddressEntity extends StepEntity {
//
//        LocationEntity address;
//
//        @Builder
//        public StepAddressEntity(Long id, LocationEntity address) {
//            super(id);
//            this.address = address;
//        }
//    }
//
//    @Data
//    @AllArgsConstructor
//    @Builder
//    public static class LocationEntity implements AbstractEntity<Long> {
//        Long id;
//        int radius;
//        String address;
//    }
//
//    @Data
//    @RequiredArgsConstructor
//    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//    public abstract static class Step implements AbstractDto<Long> {
//        Long id;
//
//        @JsonIgnore
//        public abstract Location getLocation();
//    }
//
//    @Value
//    @EqualsAndHashCode(callSuper = true)
//    @ToString(callSuper = true)
//    public static class StepAddress extends Step {
//        Location address;
//
//        @Builder
//        public StepAddress(Long id, Location address) {
//            super(id);
//            this.address = address;
//        }
//
//        @Override
//        public Location getLocation() {
//            return address;
//        }
//    }
//
//    @Value
//    public static class Location implements AbstractDto<Long> {
//        Long id;
//        int radius;
//        String address;
//
//        @Builder
//        public Location(Long id, int radius, String address) {
//            this.id = id;
//            this.radius = radius;
//            this.address = address;
//        }
//    }
//
//    public class LogisticStepAddressMapper extends AbsMapperEntityDto<StepAddressEntity, StepAddress> {
//
//        public LogisticStepAddressMapper(ModelMapper modelMapper) {
//            super(modelMapper, StepAddressEntity.class, StepAddress.class);
//        }
//
//        @Override
//        protected StepAddress create(StepAddressEntity entity) {
//            return new StepAddress(
//                    entity.getId(),
//                    map(entity.getAddress(), Location.class));
//        }
//    }
//
//    public class LogisticLocationMapper extends AbsMapperEntityDto<LocationEntity, Location> {
//
//        public LogisticLocationMapper(ModelMapper modelMapper) {
//            super(modelMapper, LocationEntity.class, Location.class);
//        }
//
//        @Override
//        protected Location create(LocationEntity entity) {
//            return new Location(entity.getId(), entity.getRadius(), entity.getAddress());
//        }
//    }
//
//    public class LogisticStepMapper extends AbsMapperEntityDto<StepEntity, Step> {
//
//        public LogisticStepMapper(ModelMapper modelMapper) {
//            super(modelMapper, StepEntity.class, Step.class);
//        }
//
//        @PostConstruct
//        private void config() {
//            this.modelMapper.createTypeMap(StepAddressEntity.class, Step.class)
//                .setPostConverter(context -> {
//                    final StepEntity source = context.getSource();
//                    final Step destination = context.getDestination();
//                    return context.getDestination();
//                })
//                .setProvider(request -> create((StepEntity) request.getSource()));
//        }
//
//        @Override
//        protected Step create(StepEntity entity) {
//            if (entity instanceof StepAddressEntity) {
//                return map(entity, StepAddress.class);
//            }
//            throw new IllegalArgumentException(String.format("Mapping %s -> %s is imposible", entityClass, dtoClass));
//        }
//    }
//}
