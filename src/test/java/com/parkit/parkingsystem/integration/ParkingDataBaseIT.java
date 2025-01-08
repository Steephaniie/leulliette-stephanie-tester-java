package com.parkit.parkingsystem.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.util.Date;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.lenient;
import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {
	private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
	private static ParkingSpotDAO parkingSpotDAO;
	private static TicketDAO ticketDAO;
	private static DataBasePrepareService dataBasePrepareService;
	@Mock
	private static InputReaderUtil inputReaderUtil;

	/**
	 * Prepares the configuration before running the tests: - Initializes DAO
	 * objects with the test configuration. - Configures the database preparation
	 * service.
	 */
	@BeforeAll
	private static void setUp() throws Exception {
		parkingSpotDAO = new ParkingSpotDAO();
		parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
		ticketDAO = new TicketDAO();
		ticketDAO.dataBaseConfig = dataBaseTestConfig;
		dataBasePrepareService = new DataBasePrepareService();
	}

	/**
	 * Sets up the initial state before each test: - Configures mocked responses for
	 * user interactions. - Resets the database entries.
	 */
	@BeforeEach
	private void setUpPerTest() throws Exception {
		lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
		dataBasePrepareService.clearDataBaseEntries();
	}

	/**
	 * Cleans up resources after all tests.
	 */
	@AfterAll
	private static void tearDown() {
		// Added to follow good testing practices (currently empty).
	}

	/**
	 * Verifies the registration of a vehicle entering the parking lot: - Ensures
	 * the ticket is correctly saved in the database. - Ensures the used parking
	 * spot is marked as occupied.
	 */
	@Test
	public void testParkingACar() {
		// Initialize the parking service with necessary mocks
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		// Call the method to register a vehicle entering
		parkingService.processIncomingVehicle();
		// Verify that a ticket is saved in the database
		Ticket savedTicket = ticketDAO.getTicket("ABCDEF");
		assertNotNull(savedTicket, "The ticket should be saved in the database.");
		assertEquals("ABCDEF", savedTicket.getVehicleRegNumber(), "The license plate number should match.");
		// Verify that the parking spot is marked as occupied
		int parkingSpotId = savedTicket.getParkingSpot().getId();
		assertFalse(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR) == parkingSpotId,
				"The used parking spot should no longer be available.");
	}


	/**
	 * Verifies the process of a vehicle exiting the parking lot: - Ensures the
	 * ticket is updated with the exit time. - Ensures the fare is correctly
	 * calculated. - Ensures the parking spot is marked as available.
	 */
	@Test
	public void testParkingLotExit() throws InterruptedException {
		// Simulate a vehicle entering the parking lot
		testParkingACar();
		Thread.sleep(1000); // Simulate wait time to create an entry/exit time difference
		// Initialize the parking service with necessary mocks
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		// Call the method to register a vehicle exiting
		parkingService.processExitingVehicle();
		// Verify that the ticket is updated in the database
		Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");
		assertNotNull(updatedTicket.getOutTime(), "The exit time should be recorded in the ticket.");
		assertTrue(updatedTicket.getPrice() == 0, "The fare should be calculated and equal than zero.");
		// Verify that the parking spot is marked as available
		int parkingSpotId = updatedTicket.getParkingSpot().getId();
		assertEquals(parkingSpotId, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR),
				"The parking spot should be released and available.");
	}

	/**
	 * Verifies the application of the 5% discount for a recurring user: - Simulates
	 * a first entry/exit for a user. - Simulates a second entry and exit for the
	 * same user. - Ensures the discount is correctly applied to the fare.
	 */
	@Test
	public void testParkingLotExitRecurringUser() throws InterruptedException {
		// We create our first ticket
		testParkingLotExit();
		// To ensure chronological order, we set the first ticket's in-time to 2 hours ago
		Ticket firstTicket = ticketDAO.getTicket("ABCDEF");
		firstTicket.setInTime(new Date(System.currentTimeMillis() - (2 * 60 * 60 * 1000)));
		ticketDAO.updateInTimeTicket(firstTicket);

		// Initialize the parking service
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		// We create our second ticket
		// Call the method to register a vehicle entering
	   	parkingService.processIncomingVehicle();
		// To ensure a fare greater than 0 and respect chronological order, set the second ticket's in-time to 1 hour ago
		Ticket secondeTicket = ticketDAO.getTicket("ABCDEF");
		secondeTicket.setInTime(new Date(System.currentTimeMillis() - (1 * 60 * 60 * 1000)));
		ticketDAO.updateInTimeTicket(secondeTicket);
		// Simulate wait time to create an entry/exit time difference
		Thread.sleep(1000);
		// Call the method for a vehicle exiting
		parkingService.processExitingVehicle();
		// Verify that the 5% discount is applied
		Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");
		// Verify that the result corresponds to 1 hour of parking with the discount applied
		assertEquals(Fare.CAR_RATE_PER_HOUR * 0.95, updatedTicket.getPrice(),
				"The fare with a 5% discount for recurring users is incorrect.");
	}
}
