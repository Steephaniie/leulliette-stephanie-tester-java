package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

	// Test with a valid vehicle type (car)
	@Test
	public void testGetVehichleType_ValidInputCar() {
		// Simulate user input for a car
		when(inputReaderUtil.readSelection()).thenReturn(1);
		// Call the method
		ParkingType result = parkingService.getVehichleType();
		// Verify the result is correct
		assertEquals(ParkingType.CAR, result);
	}

	// Test with an invalid vehicle type
	@Test
	public void testGetVehichleType_InvalidInput() {
		// Simulate invalid user input
		when(inputReaderUtil.readSelection()).thenReturn(3);
		// Verify an exception is thrown
		assertThrows(IllegalArgumentException.class, () -> {
			parkingService.getVehichleType();
		});

	}

	@Mock
	private InputReaderUtil inputReaderUtil;
	@Mock
	private ParkingSpotDAO parkingSpotDAO;
	@Mock
	private TicketDAO ticketDAO;
	@InjectMocks
	private ParkingService parkingService;

	@BeforeEach
	public void setUp() {
		// Initialize mocks before each test
		MockitoAnnotations.openMocks(this);
		// Inject mocks into the service
		parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
	}

	// Test to verify that an incoming vehicle is properly recorded (case 1)
	@Test
	public void testProcessIncomingVehicle() throws Exception {
		// Mock configuration
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1); // Simulate an available spot
		when(inputReaderUtil.readSelection()).thenReturn(1); // Vehicle type: car
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABC123"); // Simulate a license plate
		// Call the method to be tested
		parkingService.processIncomingVehicle();
		// Verifications
		verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class)); // Verify the spot is updated
		verify(ticketDAO, times(1)).saveTicket(any(Ticket.class)); // Verify the ticket is recorded
	}

	// Test to verify that an exiting vehicle is properly recorded (case 1)
	@Test
	public void testProcessExitingVehicle() throws Exception {
		// Mock configuration
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false); // Create an example of an occupied spot
		Ticket ticket = new Ticket();
		ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Entry time one hour ago
		ticket.setParkingSpot(parkingSpot);
		ticket.setVehicleRegNumber("ABC123");
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABC123");
		when(ticketDAO.getTicket("ABC123")).thenReturn(ticket);
		when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
		when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
		// Call the method to be tested
		parkingService.processExitingVehicle();
		// Verifications
		verify(ticketDAO, times(1)).updateTicket(any(Ticket.class)); // Verify the ticket is updated
		verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class)); // Verify the spot is freed
	}

	// Test to verify that a ticket update failure is handled (case 2)
	@Test
	public void processExitingVehicleTestUnableUpdate() throws Exception {
		// Mock configuration
		ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
		Ticket ticket = new Ticket();
		ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
		ticket.setParkingSpot(parkingSpot);
		ticket.setVehicleRegNumber("ABC123");
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABC123");
		when(ticketDAO.getTicket("ABC123")).thenReturn(ticket);
		when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false); // Simulate update failure
		// Call the method to be tested
		parkingService.processExitingVehicle(); // Call the method
		// Verifications
		verify(ticketDAO, times(1)).updateTicket(any(Ticket.class)); // Verify the update was attempted
		verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class)); // Verify the spot was not freed

	}

	// Test to verify that an available spot is properly returned (case 3)
	@Test
	public void testGetNextParkingNumberIfAvailable() {
		// Mock configuration
		when(inputReaderUtil.readSelection()).thenReturn(1); // Vehicle type: car
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1); // Simulate an available spot
		// Call the method to be tested
		ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();
		// Verifications
		assertEquals(true, result.isAvailable()); // Verify the spot is available
		assertEquals(1, result.getId()); // Verify the ID is correct

	}

	// Test to verify that no spot is available (case 4)
	@Test
	public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
		// Mock configuration
		when(inputReaderUtil.readSelection()).thenReturn(1); // Vehicle type: car
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0); // Simulate no spot available
		// Call the method to be tested
		ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();
		// Verifications
		assertNull(result); // Verify the result is null
		verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class)); // Verify the DAO method was
																						// called
	}

	// Test to verify that an invalid vehicle type is properly handled (case 5)
	@Test
	public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() throws Exception {
		// Mock configuration
		when(inputReaderUtil.readSelection()).thenReturn(3); // Simulate an invalid vehicle type
		// Call the method
		parkingService.getNextParkingNumberIfAvailable();
		// Verify that the DAO is not called
		verify(parkingSpotDAO, never()).getNextAvailableSlot(any(ParkingType.class));

	}

}