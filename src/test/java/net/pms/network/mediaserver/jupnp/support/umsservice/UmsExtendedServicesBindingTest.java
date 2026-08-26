package net.pms.network.mediaserver.jupnp.support.umsservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.LocalService;

/**
 * An action whose argument has no matching A_ARG_TYPE_ state variable makes the binder throw, which
 * would take down the whole service at startup. Binding here catches that at build time.
 */
public class UmsExtendedServicesBindingTest {

	private static LocalService<?> bind() {
		return new AnnotationLocalServiceBinder().read(UmsExtendedServices.class);
	}

	@Test
	public void testServiceBinds() {
		LocalService<?> service = bind();
		assertNotNull(service);
		assertEquals("UmsExtendedServices", service.getServiceType().getType());
	}

	@Test
	public void testRadioActionsAreBound() {
		LocalService<?> service = bind();
		Set<String> actions = Arrays.stream(service.getActions()).map(Action::getName).collect(Collectors.toSet());
		assertTrue(actions.contains("SearchRadioStations"), actions.toString());
		assertTrue(actions.contains("GetRadioFilterValues"), actions.toString());
		assertTrue(actions.contains("AddRadioStationToPlaylist"), actions.toString());
	}

	@Test
	public void testSearchActionArguments() {
		Action<?> search = bind().getAction("SearchRadioStations");
		assertNotNull(search);
		Set<String> in = Arrays.stream(search.getInputArguments()).map(ActionArgument::getName).collect(Collectors.toSet());
		assertEquals(Set.of("Name", "CountryCode", "Language", "Tag", "Offset", "Limit"), in);
		assertEquals(1, search.getOutputArguments().length);
		assertEquals("Result", search.getOutputArguments()[0].getName());
	}

	@Test
	public void testFilterValuesActionArguments() {
		Action<?> filter = bind().getAction("GetRadioFilterValues");
		assertNotNull(filter);
		Set<String> in = Arrays.stream(filter.getInputArguments()).map(ActionArgument::getName).collect(Collectors.toSet());
		assertEquals(Set.of("Kind", "Search"), in);
	}

	@Test
	public void testAddStationActionArguments() {
		Action<?> add = bind().getAction("AddRadioStationToPlaylist");
		assertNotNull(add);
		Set<String> in = Arrays.stream(add.getInputArguments()).map(ActionArgument::getName).collect(Collectors.toSet());
		assertEquals(Set.of("ObjectID", "StationUuid", "Title"), in);
	}
}
