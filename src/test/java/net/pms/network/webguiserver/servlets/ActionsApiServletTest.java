package net.pms.network.webguiserver.servlets;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.network.webguiserver.WebGuiServletHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ActionsApiServletTest {

  @Mock
  private HttpServletRequest mockServletRequest;
  @Mock
  private HttpServletResponse mockServletResponse;
  @Mock
  private Account mockAccount;
  @Mock
  private PMS mockPms;
  @Mock
  private UmsConfiguration mockUmsConfiguration;

  private JsonObject jsonObject;
  private ActionsApiServlet actionsApiServlet;

  @BeforeEach
  public void setup() {
    setupPms();
    this.jsonObject = new JsonObject();
    this.actionsApiServlet = new ActionsApiServlet();
  }

  @Test
  void testDoPostServerStopIsAuthorised() throws Exception {

    jsonObject.addProperty("operation" ,"Computer.Shutdown");
    when(mockAccount.havePermission(Permissions.COMPUTER_SHUTDOWN)).thenReturn(true);
    doNothing().when(mockPms).shutdownComputer();

    try (MockedStatic<AuthService> mockAuthService = mockStatic(AuthService.class)) {
      mockAuthService.when(() -> AuthService.getAccountLoggedIn(any())).thenReturn(mockAccount);

      try (MockedStatic<WebGuiServletHelper> mockWebGuiServletHelper = mockStatic(WebGuiServletHelper.class)) {
        mockWebGuiServletHelper.when(() -> WebGuiServletHelper.getJsonObjectFromBody(any()))
            .thenReturn(jsonObject);

        try (MockedStatic<PMS> mockedStaticPms = mockStatic(PMS.class)) {
          mockedStaticPms.when(PMS::get).thenReturn(this.mockPms);

          actionsApiServlet.doPost(mockServletRequest, mockServletResponse);

          mockedStaticPms.verify(PMS::get);
        }

        mockWebGuiServletHelper.verify(() ->
            WebGuiServletHelper.getJsonObjectFromBody(mockServletRequest));

        mockWebGuiServletHelper.verify(() ->
            WebGuiServletHelper.respond(mockServletRequest, mockServletResponse, "{}", 200, "application/json"));

        mockWebGuiServletHelper.verifyNoMoreInteractions();
      }
      mockAuthService.verify(() -> AuthService.getAccountLoggedIn(mockServletRequest));
    }
    verify(mockPms, times(1)).shutdownComputer();
  }

  @Test
  void testDoPostServerStopIsNotAuthorised() throws Exception {

    jsonObject.addProperty("operation" ,"Computer.Shutdown");
    when(mockAccount.havePermission(Permissions.COMPUTER_SHUTDOWN)).thenReturn(false);

    try (MockedStatic<AuthService> mockedStaticAuthService = mockStatic(AuthService.class)) {
      mockedStaticAuthService.when(() -> AuthService.getAccountLoggedIn(any())).thenReturn(mockAccount);

      try (MockedStatic<WebGuiServletHelper> mockedStaticWebGuiServletHelper = mockStatic(WebGuiServletHelper.class)) {
        mockedStaticWebGuiServletHelper.when(() -> WebGuiServletHelper.getJsonObjectFromBody(any()))
            .thenReturn(jsonObject);

        actionsApiServlet.doPost(mockServletRequest, mockServletResponse);

        mockedStaticWebGuiServletHelper.verify(() ->
            WebGuiServletHelper.getJsonObjectFromBody(mockServletRequest));

        mockedStaticWebGuiServletHelper.verify(() ->
            WebGuiServletHelper.respondForbidden(mockServletRequest, mockServletResponse));

        mockedStaticWebGuiServletHelper.verifyNoMoreInteractions();
      }
      mockedStaticAuthService.verify(() -> AuthService.getAccountLoggedIn(mockServletRequest));
    }
    verify(mockPms, never()).shutdownComputer();
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
