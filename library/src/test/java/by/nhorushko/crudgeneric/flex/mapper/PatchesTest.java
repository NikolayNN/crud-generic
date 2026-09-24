package by.nhorushko.crudgeneric.flex.mapper;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class PatchesTest {

    @Test
    public void addDeclaresOneUpdaterPerBodyClassAgainstTheEntity() {
        Patches<Target> patches = new Patches<>(Target.class);

        patches.add(NamePatch.class, (patch, target) -> target.name = patch.name())
                .add(CodePatch.class, (patch, target) -> target.code = patch.code());

        List<Updater<?, Target>> updaters = patches.updaters();
        assertEquals(2, updaters.size());
        assertEquals(NamePatch.class, updaters.get(0).fromClass());
        assertEquals(CodePatch.class, updaters.get(1).fromClass());
        assertEquals(Target.class, updaters.get(0).toClass());
        assertEquals(Target.class, updaters.get(1).toClass());
    }

    @Test
    public void declaredUpdaterAppliesTheFunction() {
        Patches<Target> patches = new Patches<>(Target.class);
        patches.add(NamePatch.class, (patch, target) -> target.name = patch.name());
        Target target = new Target();

        MapperRegistry registry = new MapperRegistry(List.of(), patches.updaters(), List.of());
        registry.getUpdater(NamePatch.class, Target.class).update(new NamePatch("new"), target);

        assertEquals("new", target.name);
    }

    @Test
    public void addRejectsNullArguments() {
        Patches<Target> patches = new Patches<>(Target.class);

        assertThrows(NullPointerException.class, () -> patches.add(null, (Object patch, Target target) -> { }));
        assertThrows(NullPointerException.class, () -> patches.add(NamePatch.class, null));
    }

    record NamePatch(String name) {
    }

    record CodePatch(String code) {
    }

    static class Target {
        String name;
        String code;
    }
}
