package net.pms;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import net.pms.configuration.UmsConfiguration;
import net.pms.network.webguiserver.servlets.SseApiServlet;
import net.pms.util.ProcessUtil;
import net.pms.util.TaskRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PMSTest {

  @Spy
  private PMS mockPms;
  @Mock
  private TaskRunner mockTaskRunner;
  @Mock
  private UmsConfiguration mockUmsConfiguration;

  @Captor
  private ArgumentCaptor<Runnable> shutdownComputerRunnableCaptor;

  @BeforeEach
  public void setup() {
    setupPms();
  }

  @Test
  void testShutdownComputer() {

    try(MockedStatic<TaskRunner> mockedStaticTaskRunner = mockStatic(TaskRunner.class)) {
      mockedStaticTaskRunner.when(TaskRunner::getInstance).thenReturn(mockTaskRunner);

      mockPms.shutdownComputer();
    }

    verify(mockTaskRunner).submitNamed(eq("shutdown"), eq(true), shutdownComputerRunnableCaptor.capture());

    try(MockedStatic<SseApiServlet> mockedStaticSseApiServlet = mockStatic(SseApiServlet.class)) {
      try(MockedStatic<ProcessUtil> mockedStaticProcessUtil = mockStatic(ProcessUtil.class)) {

        shutdownComputerRunnableCaptor.getValue().run();

        mockedStaticSseApiServlet.verify(() -> SseApiServlet.notify("computer-shutdown", "Shutting down computer", "Server status", "red", true));
        mockedStaticProcessUtil.verify(ProcessUtil::shutDownComputer);
      }
    }
  }

  private void setupPms() {
    try {
      Field privateField = PMS.class.getDeclaredField("umsConfiguration");
      privateField.setAccessible(true);
      privateField.set(this.mockPms, this.mockUmsConfiguration);
    } catch (Exception e) {
      fail("Error setting up PMS for test:", e);
    }
  }

}
