package by.nhorushko.crudgeneric.flex.mapper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class MapperAndUpdaterOfTest {

    @Test
    public void mapperOfDeclaresItsClassesAndDelegates() {
        Mapper<String, Integer> length = Mapper.of(String.class, Integer.class, String::length);

        assertEquals(String.class, length.fromClass());
        assertEquals(Integer.class, length.toClass());
        assertEquals(Integer.valueOf(5), length.map("hello"));
    }

    @Test
    public void updaterOfDeclaresItsClassesAndDelegates() {
        Updater<String, StringBuilder> append = Updater.of(String.class, StringBuilder.class, (s, sb) -> sb.append(s));
        StringBuilder target = new StringBuilder("a");

        append.update("b", target);

        assertEquals(String.class, append.fromClass());
        assertEquals(StringBuilder.class, append.toClass());
        assertEquals("ab", target.toString());
    }

    @Test
    public void ofRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> Mapper.<String, Integer>of(null, Integer.class, String::length));
        assertThrows(NullPointerException.class, () -> Mapper.<String, Integer>of(String.class, null, String::length));
        assertThrows(NullPointerException.class, () -> Mapper.<String, Integer>of(String.class, Integer.class, null));
        assertThrows(NullPointerException.class,
                () -> Updater.<String, StringBuilder>of(null, StringBuilder.class, (s, sb) -> sb.append(s)));
        assertThrows(NullPointerException.class,
                () -> Updater.<String, StringBuilder>of(String.class, null, (s, sb) -> sb.append(s)));
        assertThrows(NullPointerException.class,
                () -> Updater.<String, StringBuilder>of(String.class, StringBuilder.class, null));
    }
}
