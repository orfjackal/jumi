package example;

import example.generated.*;
import fi.jumi.actors.*;
import fi.jumi.actors.mq.MessageSender;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class CodeGenerationTest {

    @Test
    public void generates_a_working_eventizer() {
        ExampleListener target = mock(ExampleListener.class);
        Eventizer<ExampleListener> eventizer = new ExampleListenerEventizer();
        MessageSender<Event<ExampleListener>> backend = eventizer.newBackend(target);
        ExampleListener frontend = eventizer.newFrontend(backend);

        frontend.onSomething();

        verify(target).onSomething();
    }
}
