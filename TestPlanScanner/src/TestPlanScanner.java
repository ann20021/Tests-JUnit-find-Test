import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class TestPlanScanner {


    /**
     * Почему важен @Retention(RetentionPolicy.RUNTIME):
     * аннотация сохраняется в .class файле и доступна во время выполнения через рефлексию.
     *
     * Что сломается при других политиках:
     * - RetentionPolicy.SOURCE: аннотация существует только в исходном коде
     * и отбрасывается компилятором. В .class файл не попадает.
     * - RetentionPolicy.CLASS: аннотация сохраняется в .class файле,
     * но недоступна во время выполнения через рефлексию (JVM не будет загружать её в рантайме.
     *
     * В обоих случаях метод isAnnotationPresent(TestId.class) при запуске всегда вернёт false,
     * и сканер не найдёт ни одного тест-кейса.
     */

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface TestId {
        String value();   // номер тест-кейса
        int order();      // порядок выполнения в прогоне
    }

    public record TestStep(String id, String methodName, int order) {
    }


    public List<TestStep> buildPlan(Class<?> clazz) {
        List<TestStep> steps = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {

            if (method.isAnnotationPresent(TestId.class)) {
                TestId annotation = method.getAnnotation(TestId.class);

                steps.add(new TestStep(
                        annotation.value(),
                        method.getName(),
                        annotation.order()
                ));
            }
        }

        steps.sort(Comparator.comparingInt(TestStep::order));

        return steps;
    }


    // для красивого вывода
    public static void printPlan(Class<?> clazz, List<TestStep> plan) {
        System.out.println("План прогона для " + clazz.getSimpleName() + ":");

        if (plan.isEmpty()) {
            System.out.println("  Кейсов нет");
            return;
        }


        for (int i = 0; i < plan.size(); i++) {
            TestStep step = plan.get(i);
            System.out.printf("  %d. %s → %s%n", i + 1, step.id(), step.methodName());
        }
    }


    public static class Checklist {


        @TestId(value = "TC-103", order = 3)
        void checkCommentAdded() {
        }


        @TestId(value = "TC-101", order = 1)
        void checkLogin() {
        }


        void helperWithoutAnnotation() {
        }


        @TestId(value = "TC-104", order = 4)
        void checkLogout() {
        }


        @TestId(value = "TC-102", order = 2)
        void checkTaskCreated() {
        }
    }


    public static class EmptyChecklist {
        void someRegularMethod() {
        }
    }


    public static void main(String[] args) {
        TestPlanScanner scanner = new TestPlanScanner();

        List<TestStep> plan = scanner.buildPlan(Checklist.class);
        printPlan(Checklist.class, plan);

        System.out.println();

        List<TestStep> emptyPlan = scanner.buildPlan(EmptyChecklist.class);
        printPlan(EmptyChecklist.class, emptyPlan);
    }
}
