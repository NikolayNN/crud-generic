package by.nhorushko.crudgeneric.flex.pageable;

import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;


public class FilterGroupBuilderTest {

    @Test
   public void testFilterGroupBuilder() {
        // Создаем моки для фильтров
        PageFilterRequest.Filter filter1 = mock(PageFilterRequest.Filter.class);
        PageFilterRequest.Filter filter2 = mock(PageFilterRequest.Filter.class);
        PageFilterRequest.Filter filter3 = mock(PageFilterRequest.Filter.class);
        PageFilterRequest.Filter filter4 = mock(PageFilterRequest.Filter.class);

        // Используем билдер для создания структуры фильтров
        FilterGroupBuilder builder = new FilterGroupBuilder();
        PageFilterRequest.FilterGroup group = builder
                .or(
                        FilterGroupBuilder.and(filter1, filter2),
                        FilterGroupBuilder.or(filter3, filter4)
                )
                .build();

        // Проверяем, что главная группа содержит две подгруппы
        assertEquals(2, group.getSubGroups().size());

        // Проверяем, что условие соединения для главной группы - OR
        assertEquals(PageFilterRequest.ConcatCondition.OR, group.getCondition());

        // Проверяем, что первая подгруппа имеет условие соединения - AND
        assertEquals(PageFilterRequest.ConcatCondition.AND, group.getSubGroups().get(0).getCondition());

        // Проверяем, что вторая подгруппа имеет условие соединения - OR
        assertEquals(PageFilterRequest.ConcatCondition.OR, group.getSubGroups().get(1).getCondition());

        // Проверяем, что первая подгруппа содержит два фильтра
        assertEquals(2, group.getSubGroups().get(0).getFilters().size());

        // Проверяем, что вторая подгруппа содержит два фильтра
        assertEquals(2, group.getSubGroups().get(1).getFilters().size());
    }

}
